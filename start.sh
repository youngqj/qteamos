#!/bin/bash

# QTeamOS 启动脚本
# 版本：1.0.0
# 作者：youngqj@126.com

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 获取脚本所在目录
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
APP_HOME=$(cd "${SCRIPT_DIR}" && pwd)

# 设置应用目录和JAR文件
APP_NAME="QTeamOS"
JAR_NAME="qteamos.jar"

# 寻找Java 17
if [ -z "${JAVA_HOME}" ]; then
    # 在Mac上尝试使用/usr/libexec/java_home查找Java 17
    if [ "$(uname)" = "Darwin" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
        if JAVA_17_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null); then
            echo -e "${GREEN}已找到Java 17: ${JAVA_17_HOME}${NC}"
            JAVA_CMD="${JAVA_17_HOME}/bin/java"
        else
            # 尝试使用系统中的java命令
            if command -v java >/dev/null 2>&1; then
                JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
                if [[ "$JAVA_VERSION" == 17* ]] || [[ "$JAVA_VERSION" > 17 ]]; then
                    echo -e "${GREEN}使用系统Java: $JAVA_VERSION${NC}"
                    JAVA_CMD=$(command -v java)
                else
                    echo -e "${RED}错误：系统Java版本 $JAVA_VERSION 低于要求的Java 17${NC}"
                    echo -e "${YELLOW}请安装Java 17或更高版本，或设置JAVA_HOME环境变量指向Java 17${NC}"
                    exit 1
                fi
            else
                echo -e "${RED}错误：未找到Java${NC}"
                exit 1
            fi
        fi
    else
        # 非Mac系统或java_home命令不可用，尝试使用系统java
        if command -v java >/dev/null 2>&1; then
            JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
            if [[ "$JAVA_VERSION" == 17* ]] || [[ "$JAVA_VERSION" > 17 ]]; then
                echo -e "${GREEN}使用系统Java: $JAVA_VERSION${NC}"
                JAVA_CMD=$(command -v java)
            else
                echo -e "${RED}错误：系统Java版本 $JAVA_VERSION 低于要求的Java 17${NC}"
                echo -e "${YELLOW}请安装Java 17或更高版本，或设置JAVA_HOME环境变量指向Java 17${NC}"
                exit 1
            fi
        else
            echo -e "${RED}错误：未找到Java${NC}"
            exit 1
        fi
    fi
else
    # 检查用户设置的JAVA_HOME是否符合版本要求
    if [ -x "${JAVA_HOME}/bin/java" ]; then
        JAVA_VERSION=$("${JAVA_HOME}/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
        if [[ "$JAVA_VERSION" == 17* ]] || [[ "$JAVA_VERSION" > 17 ]]; then
            echo -e "${GREEN}使用JAVA_HOME: $JAVA_HOME${NC}"
            JAVA_CMD="${JAVA_HOME}/bin/java"
        else
            echo -e "${RED}错误：JAVA_HOME指向的Java版本 $JAVA_VERSION 低于要求的Java 17${NC}"
            echo -e "${YELLOW}请设置JAVA_HOME环境变量指向Java 17安装目录${NC}"
            exit 1
        fi
    else
        echo -e "${RED}错误：JAVA_HOME环境变量指向的目录 $JAVA_HOME 不包含可执行的java命令${NC}"
        exit 1
    fi
fi

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
    
    # 清理旧的日志文件
    > ${LOG_DIR}/qteamos.log
    
    # 使用nohup启动应用，并将输出重定向到日志文件
    nohup ${JAVA_CMD} ${JAVA_OPTS} \
        -Dspring.config.location=file:${APP_HOME}/conf/ \
        -Dlogging.config=file:${APP_HOME}/conf/logback.xml \
        -Dserver.tomcat.basedir=${APP_HOME} \
        -jar ${APP_HOME}/lib/${JAR_NAME} \
        --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} \
        >> ${LOG_DIR}/qteamos.log 2>&1 &
    
    # 保存进程ID
    echo $! > ${PID_FILE}
    
    # 等待应用启动
    echo -e "${YELLOW}等待应用启动，最多等待30秒...${NC}"
    
    # 设置最大等待时间（30秒）
    max_wait=30
    wait_count=0
    started=false
    
    while [ ${wait_count} -lt ${max_wait} ]; do
        # 检查进程是否存在
        if ! ps -p $(cat ${PID_FILE} 2>/dev/null) > /dev/null 2>&1; then
            echo -e "${RED}${APP_NAME}启动失败，进程已退出${NC}"
            echo "状态：启动失败，进程已退出" >> ${START_LOG}
            
            # 检查日志中的错误信息
            if [ -f "${LOG_DIR}/qteamos.log" ]; then
                error_msg=$(grep -i "error\|exception\|failed" ${LOG_DIR}/qteamos.log | head -5)
                if [ ! -z "$error_msg" ]; then
                    echo -e "${RED}错误详情:${NC}"
                    echo "$error_msg"
                    echo "错误详情:" >> ${START_LOG}
                    echo "$error_msg" >> ${START_LOG}
                fi
            fi
            
            rm -f ${PID_FILE}
            return 1
        fi
        
        # 检查日志中是否有启动成功的标志
        if [ -f "${LOG_DIR}/qteamos.log" ] && grep -q "Started QTeamOSApplication" ${LOG_DIR}/qteamos.log; then
            echo -e "${GREEN}${APP_NAME}启动成功，进程ID：$(cat ${PID_FILE})${NC}"
            echo "状态：已启动" >> ${START_LOG}
            echo "进程ID：$(cat ${PID_FILE})" >> ${START_LOG}
            started=true
            break
        fi
        
        # 检查是否有明确的失败标志
        if [ -f "${LOG_DIR}/qteamos.log" ] && grep -q "APPLICATION FAILED TO START" ${LOG_DIR}/qteamos.log; then
            echo -e "${RED}${APP_NAME}启动失败，请查看日志文件${NC}"
            error_msg=$(grep -A 10 "APPLICATION FAILED TO START" ${LOG_DIR}/qteamos.log | head -10)
            echo -e "${RED}错误详情:${NC}"
            echo "$error_msg"
            echo "状态：启动失败" >> ${START_LOG}
            echo "错误详情:" >> ${START_LOG}
            echo "$error_msg" >> ${START_LOG}
            rm -f ${PID_FILE}
            return 1
        fi
        
        sleep 1
        wait_count=$((wait_count+1))
        
        # 每5秒显示一次等待信息
        if [ $((wait_count % 5)) -eq 0 ]; then
            echo -e "${YELLOW}已等待 ${wait_count} 秒...${NC}"
        fi
    done
    
    # 检查最终结果
    if [ "$started" = "true" ]; then
        return 0
    else
        # 如果没有明确的成功或失败标志，检查进程是否仍在运行
        if ps -p $(cat ${PID_FILE} 2>/dev/null) > /dev/null 2>&1; then
            echo -e "${YELLOW}${APP_NAME}已启动，但未检测到明确的启动成功标志，请检查日志确认应用状态${NC}"
            echo "状态：已启动，但未检测到明确的启动成功标志" >> ${START_LOG}
            echo "进程ID：$(cat ${PID_FILE})" >> ${START_LOG}
            return 0
        else
            echo -e "${RED}${APP_NAME}启动失败，进程已退出${NC}"
            echo "状态：启动失败，进程已退出" >> ${START_LOG}
            rm -f ${PID_FILE}
            return 1
        fi
    fi
}

# 主逻辑
if check_if_running; then
    exit 0
else
    start_app
    exit $?
fi 