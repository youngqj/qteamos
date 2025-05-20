#!/bin/bash

# QTeamOS 启动脚本
# 版本：1.0.0
# 作者：youngqj@126.com

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 设置应用目录和JAR文件
APP_NAME="QTeamOS"
APP_HOME=${APP_HOME:-"/opt/qteamos"}
JAR_NAME=${JAR_NAME:-"qteamos.jar"}
JAVA_HOME=${JAVA_HOME:-"/usr/lib/jvm/java-17-openjdk"}
JAVA_OPTS=${JAVA_OPTS:-"-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"}
LOG_DIR=${APP_HOME}/logs
PID_FILE=${APP_HOME}/qteamos.pid
START_LOG=${LOG_DIR}/startup.log

# 确保目录存在
mkdir -p ${LOG_DIR}

# 检查是否已运行
check_if_running() {
    if [ -f "${PID_FILE}" ]; then
        pid=$(cat "${PID_FILE}")
        if ps -p ${pid} > /dev/null; then
            echo -e "${RED}${APP_NAME}已经在运行，进程ID：${pid}${NC}"
            return 0
        fi
    fi
    return 1
}

# 启动应用
start_app() {
    echo -e "${YELLOW}正在启动${APP_NAME}...${NC}"
    echo "启动时间: $(date '+%Y-%m-%d %H:%M:%S')" > ${START_LOG}
    
    # 检查JAR文件是否存在
    if [ ! -f "${APP_HOME}/lib/${JAR_NAME}" ]; then
        echo -e "${RED}错误：JAR文件 ${APP_HOME}/lib/${JAR_NAME} 不存在${NC}"
        exit 1
    fi
    
    # 切换到应用目录
    cd ${APP_HOME}
    
    # 使用nohup启动应用，并将输出重定向到日志文件
    nohup ${JAVA_HOME}/bin/java ${JAVA_OPTS} \
        -Dspring.config.location=file:${APP_HOME}/conf/ \
        -Dlogging.config=file:${APP_HOME}/conf/logback.xml \
        -Dserver.tomcat.basedir=${APP_HOME} \
        -jar ${APP_HOME}/lib/${JAR_NAME} \
        --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
        >> ${LOG_DIR}/qteamos.log 2>&1 &
    
    # 保存进程ID
    echo $! > ${PID_FILE}
    
    # 等待片刻，检查进程是否真正启动
    sleep 3
    if ps -p $(cat ${PID_FILE}) > /dev/null; then
        echo -e "${GREEN}${APP_NAME}启动成功，进程ID：$(cat ${PID_FILE})${NC}"
        echo "状态：已启动" >> ${START_LOG}
        echo "进程ID：$(cat ${PID_FILE})" >> ${START_LOG}
        return 0
    else
        echo -e "${RED}${APP_NAME}启动失败，请查看日志文件${NC}"
        echo "状态：启动失败" >> ${START_LOG}
        rm -f ${PID_FILE}
        return 1
    fi
}

# 主逻辑
if check_if_running; then
    exit 0
else
    start_app
    exit $?
fi 