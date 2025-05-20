#!/bin/bash

# 获取脚本所在目录 - Mac兼容版本
get_script_dir() {
    SOURCE="${BASH_SOURCE[0]}"
    while [ -h "$SOURCE" ]; do
        DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
        SOURCE="$(readlink "$SOURCE")"
        [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
    done
    echo "$( cd -P "$( dirname "$SOURCE" )" && pwd )"
}

SCRIPT_DIR=$(get_script_dir)
cd "${SCRIPT_DIR}"

# 显示当前目录，用于调试
echo "当前目录: $(pwd)"

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 配置
APP_NAME="QTeamOS"
VERSION="0.0.1"
COPYRIGHT="版权所有 © 2023 浙江小趣信息技术有限公司"
AUTHOR="yangqijun@xiaoquio.com"
BUILD_DATE=$(date +"%Y-%m-%d")

# 获取配置文件中的端口
CONFIG_FILE="conf/application.yml"
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

# 检查服务状态
PID_FILE="qteamos.pid"
LOG_FILE="logs/qteamos.log"
EXIT_LOG="logs/startup.log"

STATUS_STR=""
STATUS_DETAIL=""

if [ -f "${PID_FILE}" ]; then
  PID=$(cat ${PID_FILE})
  if ps -p ${PID} > /dev/null; then
    # 检查端口是否在监听
    if command -v lsof > /dev/null && lsof -i:${SERVER_PORT} > /dev/null 2>&1; then
      STATUS="${GREEN}运行中${NC} (PID: ${PID}, 端口: ${SERVER_PORT}已监听)"
      STATUS_DETAIL="应用正常运行"
    else
      # 检查日志中是否有成功启动的标志
      if [ -f "${LOG_FILE}" ] && grep -q "Started QTeamOSApplication" ${LOG_FILE}; then
        STATUS="${GREEN}运行中${NC} (PID: ${PID})"
        STATUS_DETAIL="应用已启动完成"
      else
        STATUS="${YELLOW}启动中${NC} (PID: ${PID}, 端口: ${SERVER_PORT}未监听)"
        STATUS_DETAIL="应用正在启动中或启动异常"
      fi
    fi
  else
    # 检查日志中的退出原因
    if [ -f "${EXIT_LOG}" ]; then
      EXIT_REASON=$(grep "启动失败" ${EXIT_LOG} | tail -1)
      if [ ! -z "${EXIT_REASON}" ]; then
        STATUS="${RED}已停止${NC} (进程已退出, PID文件仍存在)"
        STATUS_DETAIL="启动失败原因: ${EXIT_REASON}"
        
        # 获取更详细的错误信息
        if grep -q "错误详情" ${EXIT_LOG}; then
          ERROR_DETAILS=$(sed -n '/错误详情/,/进程ID/p' ${EXIT_LOG})
          STATUS_DETAIL="${STATUS_DETAIL}\n${ERROR_DETAILS}"
        fi
      else
        STATUS="${RED}已停止${NC} (PID文件存在但进程不存在)"
        STATUS_DETAIL="进程可能异常终止"
      fi
    else
      STATUS="${RED}已停止${NC} (PID文件存在但进程不存在)"
      STATUS_DETAIL="进程可能异常终止"
    fi
  fi
else
  STATUS="${RED}已停止${NC} (PID文件不存在)"
  STATUS_DETAIL="应用未启动"
fi

# 检查日志文件中的最后几行
if [ -f "${LOG_FILE}" ]; then
  LAST_LOG=$(tail -5 ${LOG_FILE})
  LOG_SIZE=$(du -h ${LOG_FILE} | awk '{print $1}')
else
  LAST_LOG="日志文件不存在"
  LOG_SIZE="0"
fi

# 显示应用信息
echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}         ${APP_NAME} 系统信息         ${NC}"
echo -e "${GREEN}=======================================${NC}"
echo -e "${BLUE}应用名称:${NC} ${APP_NAME}"
echo -e "${BLUE}版本:${NC} ${VERSION}"
echo -e "${BLUE}构建日期:${NC} ${BUILD_DATE}"
echo -e "${BLUE}服务状态:${NC} ${STATUS}"
if [ ! -z "${STATUS_DETAIL}" ]; then
  echo -e "${BLUE}状态详情:${NC} ${STATUS_DETAIL}"
fi
echo -e "${BLUE}服务端口:${NC} ${SERVER_PORT}"
echo -e "${BLUE}上下文路径:${NC} ${CONTEXT_PATH}"
echo -e "${BLUE}安装目录:${NC} $(pwd)"
echo -e "${BLUE}日志目录:${NC} $(pwd)/logs"
echo -e "${BLUE}配置目录:${NC} $(pwd)/conf"
echo -e "${BLUE}数据目录:${NC} $(pwd)/data"
echo -e "${BLUE}插件目录:${NC} $(pwd)/plugins"
echo -e "${BLUE}日志大小:${NC} ${LOG_SIZE}"
echo -e "${GREEN}=======================================${NC}"
echo -e "${CYAN}${COPYRIGHT}${NC}"
echo -e "${CYAN}技术支持: ${AUTHOR}${NC}"
echo -e "${GREEN}=======================================${NC}"

# 最后显示最近的日志
echo -e "${YELLOW}最近日志:${NC}"
echo -e "${LAST_LOG}" 