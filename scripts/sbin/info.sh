#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "${SCRIPT_DIR}"

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置
APP_NAME="QEleBase"
VERSION="1.0.0"
COPYRIGHT="版权所有 © 2025 浙江小趣信息技术有限公司"
AUTHOR="yangqijun@xiaoquio.com"
BUILD_DATE=$(date +"%Y-%m-%d")

# 获取配置文件中的端口
CONFIG_FILE="qelebase-launcher/src/main/resources/application.yml"
if [ -f "${CONFIG_FILE}" ]; then
  SERVER_PORT=$(grep "port:" ${CONFIG_FILE} | head -1 | awk '{print $2}')
  CONTEXT_PATH=$(grep "context-path:" ${CONFIG_FILE} | head -1 | awk '{print $2}')
else
  SERVER_PORT="8081"
  CONTEXT_PATH="/"
fi

# 确保context-path格式正确
if [ "${CONTEXT_PATH}" = "" ]; then
  CONTEXT_PATH="/"
fi

# 输出信息
echo -e "${CYAN}=========================================================${NC}"
echo -e "${CYAN}                ${APP_NAME} 系统信息                     ${NC}"
echo -e "${CYAN}=========================================================${NC}"

echo -e "${BLUE}[版本] ${NC}系统版本信息"
echo -e "        内核版本: ${GREEN}${VERSION}${NC}"
echo -e "        构建日期: ${GREEN}${BUILD_DATE}${NC}"
echo -e "        开发作者: ${GREEN}${AUTHOR}${NC}"

echo -e "${BLUE}[版权] ${NC}版权信息"
echo -e "        ${GREEN}${COPYRIGHT}${NC}"

echo -e "${BLUE}[访问] ${NC}系统访问信息"
echo -e "        服务端口: ${GREEN}${SERVER_PORT}${NC}"
echo -e "        上下文路径: ${GREEN}${CONTEXT_PATH}${NC}"
echo -e "        访问地址: ${GREEN}http://localhost:${SERVER_PORT}${CONTEXT_PATH}${NC}"
echo -e "        API文档: ${GREEN}http://localhost:${SERVER_PORT}${CONTEXT_PATH}swagger-ui.html${NC}"

echo -e "${BLUE}[插件] ${NC}插件系统信息"
echo -e "        插件目录: ${GREEN}plugins${NC}"
PLUGIN_COUNT=$(ls -l plugins 2>/dev/null | grep -c "^d" || echo "0")
echo -e "        已安装插件: ${GREEN}${PLUGIN_COUNT}${NC} 个"

echo -e "${BLUE}[状态] ${NC}系统运行状态"
PID_FILE="logs/application.pid"
if [ -f "${PID_FILE}" ]; then
  PID=$(cat "${PID_FILE}")
  if ps -p ${PID} > /dev/null 2>&1; then
    echo -e "        运行状态: ${GREEN}运行中${NC}"
    echo -e "        进程ID: ${GREEN}${PID}${NC}"
    UPTIME=$(ps -o etime= -p ${PID})
    echo -e "        运行时间: ${GREEN}${UPTIME}${NC}"
    MEM=$(ps -o rss= -p ${PID} | awk '{printf "%.2f MB", $1/1024}')
    echo -e "        内存使用: ${GREEN}${MEM}${NC}"
  else
    echo -e "        运行状态: ${YELLOW}已停止${NC} (PID文件存在但进程不存在)"
  fi
else
  echo -e "        运行状态: ${YELLOW}已停止${NC}"
fi

echo -e "${CYAN}=========================================================${NC}" 