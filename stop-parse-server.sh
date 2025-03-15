#!/bin/bash

echo "正在强制关闭所有Parse相关服务..."

# 查找并杀死所有Parse Server和Dashboard进程
echo "关闭所有Parse进程..."
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

# 再次检查确认端口已释放
sleep 1
if [ -z "$(lsof -ti:1337)" ] && [ -z "$(lsof -ti:4040)" ]; then
  echo "✅ 所有Parse服务端口已成功释放"
else
  echo "⚠️ 某些端口可能仍被占用，请手动检查"
fi

# 询问是否关闭MongoDB
read -p "是否同时关闭MongoDB服务？(y/n): " STOP_MONGO
if [ "$STOP_MONGO" = "y" ] || [ "$STOP_MONGO" = "Y" ]; then
  echo "正在关闭MongoDB..."
  brew services stop mongodb-community
  echo "✅ MongoDB已关闭"
fi

echo ""
echo "🚀 所有Parse服务已关闭!" 