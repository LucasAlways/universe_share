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

# 启动Parse Server
echo "启动Parse Server..."
parse-server --config parse-server-config.json --host 0.0.0.0 &
PARSE_SERVER_PID=$!
echo "✅ Parse Server已启动 (PID: $PARSE_SERVER_PID)"

# 启动Parse Dashboard
echo "启动Parse Dashboard..."
parse-dashboard --config parse-dashboard-config.json --port 4040 --host 0.0.0.0 &
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