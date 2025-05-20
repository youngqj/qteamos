#!/bin/bash

# QTeamOS 停止脚本
# 版本：1.0.0
# 作者：youngqj@126.com

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 设置应用目录和PID文件
APP_NAME="QTeamOS"
APP_HOME=${APP_HOME:-"/opt/qteamos"}
PID_FILE=${APP_HOME}/qteamos.pid
LOG_DIR=${APP_HOME}/logs
STOP_LOG=${LOG_DIR}/shutdown.log

# 确保日志目录存在
mkdir -p ${LOG_DIR}

# 停止应用函数
stop_app() {
    echo -e "${YELLOW}正在停止${APP_NAME}...${NC}"
    echo "停止时间: $(date '+%Y-%m-%d %H:%M:%S')" > ${STOP_LOG}
    
    # 检查PID文件是否存在
    if [ ! -f "${PID_FILE}" ]; then
        echo -e "${YELLOW}${APP_NAME}未运行或PID文件不存在${NC}"
        echo "状态：未运行" >> ${STOP_LOG}
        return 0
    fi
    
    # 读取PID
    pid=$(cat "${PID_FILE}")
    
    # 检查进程是否存在
    if ! ps -p ${pid} > /dev/null; then
        echo -e "${YELLOW}${APP_NAME}未运行，但PID文件存在。清理PID文件...${NC}"
        rm -f "${PID_FILE}"
        echo "状态：未运行，已清理PID文件" >> ${STOP_LOG}
        return 0
    fi
    
    # 尝试优雅停止（发送SIGTERM信号）
    echo -e "${YELLOW}尝试优雅停止进程 ${pid}...${NC}"
    kill ${pid}
    
    # 等待进程终止，最多等待30秒
    wait_count=0
    while ps -p ${pid} > /dev/null && [ ${wait_count} -lt 30 ]; do
        sleep 1
        wait_count=$((wait_count+1))
    done
    
    # 检查进程是否已终止
    if ps -p ${pid} > /dev/null; then
        echo -e "${YELLOW}优雅停止超时，强制终止进程...${NC}"
        kill -9 ${pid}
        sleep 2
    fi
    
    # 最终检查
    if ps -p ${pid} > /dev/null; then
        echo -e "${RED}${APP_NAME}停止失败，请手动终止进程 ${pid}${NC}"
        echo "状态：停止失败" >> ${STOP_LOG}
        return 1
    else
        echo -e "${GREEN}${APP_NAME}已成功停止${NC}"
        rm -f "${PID_FILE}"
        echo "状态：已停止" >> ${STOP_LOG}
        return 0
    fi
}

# 主逻辑
stop_app
exit $? 