#!/bin/bash

# QTeamOS 重启脚本
# 版本：1.0.0
# 作者：youngqj@126.com

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 设置应用目录和脚本路径
APP_NAME="QTeamOS"
APP_HOME=${APP_HOME:-"/opt/qteamos"}
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
STOP_SCRIPT=${SCRIPT_DIR}/stop.sh
START_SCRIPT=${SCRIPT_DIR}/start.sh

echo -e "${YELLOW}正在重启${APP_NAME}...${NC}"

# 停止应用
echo -e "${YELLOW}第1步：停止${APP_NAME}...${NC}"
${STOP_SCRIPT}
stop_status=$?

if [ ${stop_status} -ne 0 ]; then
    echo -e "${RED}停止${APP_NAME}失败，重启操作中断${NC}"
    exit ${stop_status}
fi

# 等待几秒确保应用完全停止
echo -e "${YELLOW}等待系统资源释放...${NC}"
sleep 5

# 启动应用
echo -e "${YELLOW}第2步：启动${APP_NAME}...${NC}"
${START_SCRIPT}
start_status=$?

if [ ${start_status} -ne 0 ]; then
    echo -e "${RED}启动${APP_NAME}失败，请检查日志文件${NC}"
    exit ${start_status}
else
    echo -e "${GREEN}${APP_NAME}已成功重启${NC}"
    exit 0
fi 