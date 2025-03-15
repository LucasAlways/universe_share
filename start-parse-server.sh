#!/bin/bash

# 检查MongoDB是否在运行
echo "检查MongoDB状态..."
mongo --eval "db.adminCommand('ping')" > /dev/null 2>&1

if [ $? -ne 0 ]; then
  echo "❌ MongoDB未运行，尝试启动MongoDB..."
  brew services start mongodb-community || mongod --dbpath ~/data/db --fork --logpath ~/data/db/mongodb.log
  
  if [ $? -ne 0 ]; then
    echo "❌ 无法启动MongoDB，请检查MongoDB安装"
    exit 1
  fi
  
  echo "✅ MongoDB已启动"
else
  echo "✅ MongoDB正在运行"
fi

# 确保端口未被占用
echo "确保端口未被占用..."
PORT_1337=$(lsof -ti:1337) || true
PORT_4040=$(lsof -ti:4040) || true

if [ -n "$PORT_1337" ]; then
  echo "端口1337已被占用，尝试释放..."
  kill -9 $PORT_1337 || true
fi

if [ -n "$PORT_4040" ]; then
  echo "端口4040已被占用，尝试释放..."
  kill -9 $PORT_4040 || true
fi

sleep 1

# 启动Parse Server，直接使用配置文件
echo "启动Parse Server..."
parse-server parse-server-config.json &
PARSE_SERVER_PID=$!
echo "✅ Parse Server已启动 (PID: $PARSE_SERVER_PID)"

# 等待Parse Server完全启动
sleep 2

# 启动Parse Dashboard
echo "启动Parse Dashboard..."
parse-dashboard --dev --appId universe_share_app_id --masterKey universe_share_master_key --serverURL http://localhost:1337/parse --appName "泰康共享平台" &
PARSE_DASHBOARD_PID=$!
echo "✅ Parse Dashboard已启动 (PID: $PARSE_DASHBOARD_PID)"

echo ""
echo "🚀 服务已启动:"
echo "- Parse Server:    http://localhost:1337/parse"
echo "- Parse Dashboard: http://localhost:4040"
echo "  用户名: admin"
echo "  密码: password"
echo ""
echo "💡 按 Ctrl+C 停止所有服务"

# 捕获SIGINT信号（Ctrl+C）以便优雅地关闭服务
trap "echo '正在关闭服务...'; kill $PARSE_SERVER_PID $PARSE_DASHBOARD_PID; exit" SIGINT

# 保持脚本运行
wait 