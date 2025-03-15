#!/bin/bash

# 获取主机IP地址
HOST_IP=$(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1' | head -n 1)
echo "检测到主机IP: $HOST_IP"

# 停止所有已运行的Parse进程
echo "关闭所有现有Parse进程..."
pkill -f "parse-server" || true
pkill -f "parse-dashboard" || true

# 释放端口
echo "检查并释放端口..."
PORT_1337=$(lsof -ti:1337) || true
PORT_4040=$(lsof -ti:4040) || true

if [ -n "$PORT_1337" ]; then
  echo "关闭占用1337端口的进程..."
  kill -9 $PORT_1337 || true
fi

if [ -n "$PORT_4040" ]; then
  echo "关闭占用4040端口的进程..."
  kill -9 $PORT_4040 || true
fi

sleep 1

# 检查MongoDB状态
echo "检查MongoDB状态..."
mongo --eval "db.adminCommand('ping')" > /dev/null 2>&1

if [ $? -ne 0 ]; then
  echo "❌ MongoDB未运行，尝试启动MongoDB..."
  brew services restart mongodb-community || mongod --dbpath ~/data/db --fork --logpath ~/data/db/mongodb.log
  
  if [ $? -ne 0 ]; then
    echo "❌ 无法启动MongoDB，请检查MongoDB安装"
    exit 1
  fi
  
  echo "✅ MongoDB已启动"
  # 给MongoDB一些启动时间
  sleep 2
else
  echo "✅ MongoDB正在运行"
fi

# 启动Parse Server
echo "启动Parse Server..."
parse-server parse-server-config.json &
PARSE_SERVER_PID=$!
echo "✅ Parse Server已启动 (PID: $PARSE_SERVER_PID)"

# 等待Parse Server完全启动
sleep 3

# 测试Parse Server是否可访问
echo "测试Parse Server是否响应..."
curl -s http://localhost:1337/parse/health > /dev/null
if [ $? -eq 0 ]; then
  echo "✅ Parse Server健康检查通过!"
else
  echo "⚠️ Parse Server健康检查未通过，但将继续..."
fi

# 启动Parse Dashboard
echo "启动Parse Dashboard..."
parse-dashboard --dev --allowInsecureHTTP --appId universe_share_app_id --masterKey universe_share_master_key --serverURL http://localhost:1337/parse --appName "泰康共享平台" &
PARSE_DASHBOARD_PID=$!
echo "✅ Parse Dashboard已启动 (PID: $PARSE_DASHBOARD_PID)"

echo ""
echo "🚀 服务已启动:"
echo "- Parse Server: http://localhost:1337/parse"
echo "  从AVD访问: http://10.0.2.2:1337/parse"
echo "- Parse Dashboard: http://localhost:4040"
echo "  用户名: admin"
echo "  密码: password"
echo ""
echo "💡 按 Ctrl+C 停止所有服务"
echo ""
echo "提示: 在Flutter应用中使用 http://10.0.2.2:1337/parse 连接到Parse服务器"

# 捕获SIGINT信号（Ctrl+C）以便优雅地关闭服务
trap "echo '正在关闭服务...'; kill $PARSE_SERVER_PID $PARSE_DASHBOARD_PID; echo '服务已关闭'; exit" SIGINT

# 保持脚本运行
wait 