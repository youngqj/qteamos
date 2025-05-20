#!/bin/bash
###
 # @Author: yangqijun youngqj@126.com
 # @Date: 2023-11-15 09:36:28
 # @LastEditors: yangqijun youngqj@126.com
 # @LastEditTime: 2025-05-03 10:29:08
 # @FilePath: /QTeam/scripts/sbin/build-kernel.sh
 # @Description: QTeamOS内核打包脚本，确保核心模块被正确打包
 # 
 # Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
### 

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       QTeamOS 1.0 内核打包脚本          ${NC}"
echo -e "${GREEN}========================================${NC}"

# 设置基础变量
BASE_DIR=$(dirname "$(readlink -f "$0")")
PROJECT_DIR=$(dirname "$BASE_DIR")

PROJECT_DIR=$(cd "$BASE_DIR/../.." && pwd)
BUILD_DIR="$PROJECT_DIR/build"


DIST_DIR="$BUILD_DIR/qteam-os-kernel"
LIB_DIR="$DIST_DIR/lib"
BIN_DIR="$DIST_DIR/bin"
PLUGINS_DIR="$DIST_DIR/plugins"
CONF_DIR="$DIST_DIR/conf"
PLUGIN_CONF_DIR="$CONF_DIR/plugins"
LOGS_DIR="$DIST_DIR/logs"
DATA_DIR="$DIST_DIR/data"

# 清理并创建目录
echo -e "${YELLOW}清理构建目录...${NC}"
rm -rf "$DIST_DIR"
mkdir -p "$LIB_DIR" "$PLUGINS_DIR" "$CONF_DIR" "$PLUGIN_CONF_DIR" "$LOGS_DIR" "$DATA_DIR" "$BIN_DIR"

# 打包内核核心模块
echo -e "${YELLOW}打包核心模块...${NC}"
cd "$PROJECT_DIR" && mvn clean package -DskipTests

# 检查构建是否成功
if [ $? -ne 0 ]; then
  echo -e "${RED}构建失败，请检查Maven错误${NC}"
  exit 1
fi

# 复制配置文件
echo -e "${YELLOW}复制配置文件...${NC}"
if [ ! -d "$CONF_DIR" ]; then
  mkdir -p "$CONF_DIR"
fi
cp "$PROJECT_DIR/qteam-os/src/main/resources/application.yml" "$CONF_DIR/" 2>/dev/null
cp "$PROJECT_DIR/qteam-os/src/main/resources/application-dev.yml" "$CONF_DIR/" 2>/dev/null
cp "$PROJECT_DIR/qteam-os/src/main/resources/application-prod.yml" "$CONF_DIR/" 2>/dev/null


# 复制核心JAR包到lib目录
echo -e "${YELLOW}复制核心JAR包...${NC}"
CORE_JAR="$PROJECT_DIR/qteam-os/target/qteam-os-0.0.1-SNAPSHOT.jar"
cp "$CORE_JAR" "$LIB_DIR/qteam-os.jar"

# 复制依赖库（如果不在JAR中）
if [ -d "$PROJECT_DIR/qteam-os/target/lib" ]; then
  echo -e "${YELLOW}复制外部依赖库...${NC}"
  mkdir -p "$LIB_DIR/ext"
  cp "$PROJECT_DIR/qteam-os/target/lib"/*.jar "$LIB_DIR/ext/" 2>/dev/null
fi

# 生成日志配置
echo -e "${YELLOW}生成日志配置...${NC}"
cat > "$CONF_DIR/logback.xml" << 'EOL'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATH" value="logs"/>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/qteam-os.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/qteam-os-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>60</maxHistory>
            <totalSizeCap>20GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>60</maxHistory>
            <totalSizeCap>2GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>

    <!-- 应用日志级别 -->
    <logger name="com.xiaoqu" level="DEBUG"/>
    
    <!-- 框架日志级别 -->
    <logger name="org.springframework" level="INFO"/>
    <logger name="org.hibernate" level="INFO"/>
    <logger name="org.mybatis" level="INFO"/>
    <logger name="com.baomidou" level="INFO"/>
</configuration>
EOL

# 生成info.sh脚本
echo -e "${YELLOW}生成info.sh脚本...${NC}"
cat > "$BIN_DIR/info.sh" << 'EOL'
#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "${SCRIPT_DIR}"

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
if [ -f "${PID_FILE}" ]; then
  PID=$(cat ${PID_FILE})
  if ps -p ${PID} > /dev/null; then
    STATUS="${GREEN}运行中${NC} (PID: ${PID})"
  else
    STATUS="${RED}已停止${NC} (PID文件存在但进程不存在)"
  fi
else
  STATUS="${RED}已停止${NC} (PID文件不存在)"
fi

# 显示应用信息
echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}         ${APP_NAME} 系统信息         ${NC}"
echo -e "${GREEN}=======================================${NC}"
echo -e "${BLUE}应用名称:${NC} ${APP_NAME}"
echo -e "${BLUE}版本:${NC} ${VERSION}"
echo -e "${BLUE}构建日期:${NC} ${BUILD_DATE}"
echo -e "${BLUE}服务状态:${NC} ${STATUS}"
echo -e "${BLUE}服务端口:${NC} ${SERVER_PORT}"
echo -e "${BLUE}上下文路径:${NC} ${CONTEXT_PATH}"
echo -e "${BLUE}安装目录:${NC} $(pwd)"
echo -e "${BLUE}日志目录:${NC} $(pwd)/logs"
echo -e "${BLUE}配置目录:${NC} $(pwd)/conf"
echo -e "${BLUE}数据目录:${NC} $(pwd)/data"
echo -e "${BLUE}插件目录:${NC} $(pwd)/plugins"
echo -e "${GREEN}=======================================${NC}"
echo -e "${CYAN}${COPYRIGHT}${NC}"
echo -e "${CYAN}技术支持: ${AUTHOR}${NC}"
echo -e "${GREEN}=======================================${NC}"
EOL

# 生成start.sh脚本
echo -e "${YELLOW}生成start.sh脚本...${NC}"
cat > "$BIN_DIR/start.sh" << 'EOL'
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
JAR_NAME="qteam-os.jar"
JAVA_HOME=${JAVA_HOME:-"/usr/lib/jvm/java-17-openjdk"}
JAVA_OPTS=${JAVA_OPTS:-"-Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"}
LOG_DIR=${APP_HOME}/logs
PID_FILE=${APP_HOME}/qteam-os.pid
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
        >> ${LOG_DIR}/qteam-os.log 2>&1 &
    
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
EOL

# 生成stop.sh脚本
echo -e "${YELLOW}生成stop.sh脚本...${NC}"
cat > "$BIN_DIR/stop.sh" << 'EOL'
#!/bin/bash

# QTeamOS 停止脚本
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

# 设置应用目录和PID文件
APP_NAME="QTeamOS"
PID_FILE=${APP_HOME}/qteam-os.pid
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
EOL

# 生成restart.sh脚本
echo -e "${YELLOW}生成restart.sh脚本...${NC}"
cat > "$BIN_DIR/restart.sh" << 'EOL'
#!/bin/bash

# QTeamOS 重启脚本
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

# 设置脚本路径
STOP_SCRIPT=${SCRIPT_DIR}/stop.sh
START_SCRIPT=${SCRIPT_DIR}/start.sh

APP_NAME="QTeamOS"

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
EOL

# 设置脚本的执行权限
echo -e "${YELLOW}设置脚本执行权限...${NC}"
chmod +x "$BIN_DIR"/*.sh

# 为应用根目录创建启动、停止和重启脚本
echo -e "${YELLOW}创建应用根目录脚本...${NC}"
cp "$BIN_DIR/start.sh" "$DIST_DIR/"
cp "$BIN_DIR/stop.sh" "$DIST_DIR/"
cp "$BIN_DIR/restart.sh" "$DIST_DIR/"
cp "$BIN_DIR/info.sh" "$DIST_DIR/"
chmod +x "$DIST_DIR"/*.sh

# 创建安装目录readme文件
echo -e "${YELLOW}创建README文件...${NC}"
cat > "$DIST_DIR/README.md" << 'EOL'
# QTeamOS 1.0

## 目录结构

- `bin/`: 管理脚本目录
- `conf/`: 配置文件目录
- `lib/`: 核心库和依赖库目录
- `plugins/`: 插件目录
- `logs/`: 日志目录
- `data/`: 数据目录

## 快速开始

### 启动应用

```bash
./start.sh
```

### 停止应用

```bash
./stop.sh
```

### 重启应用

```bash
./restart.sh
```

### 查看系统信息

```bash
./info.sh
```

## 配置

主要配置文件位于 `conf/` 目录：

- `application.yml`: 主配置文件
- `application-dev.yml`: 开发环境配置
- `application-prod.yml`: 生产环境配置
- `logback.xml`: 日志配置

## 插件管理

插件放置在 `plugins/` 目录下。

## 日志查看

日志文件位于 `logs/` 目录：

- `qteamos.log`: 主日志文件
- `error.log`: 错误日志文件

## 系统服务安装

要将应用注册为系统服务，可以使用提供的 systemd 服务文件：

1. 复制服务文件：
   ```bash
   sudo cp conf/qteam-os-systemd.service /etc/systemd/system/qteam-os.service
   ```

2. 重新加载 systemd 配置：
   ```bash
   sudo systemctl daemon-reload
   ```

3. 启用服务（开机自启）：
   ```bash
   sudo systemctl enable qteam-os
   ```

4. 启动服务：
   ```bash
   sudo systemctl start qteam-os
   ```

## 版权信息

版权所有 © 2023 浙江小趣信息技术有限公司。保留所有权利。
EOL

# 创建插件目录README
cat > "$PLUGINS_DIR/README.md" << 'EOL'
# QTeamOS 插件目录

将插件JAR文件放置在此目录中，系统将自动扫描并加载。

## 插件格式

每个插件应为标准JAR文件，并包含以下文件：

- `plugin.yml`: 插件描述文件，包含插件的元数据和配置信息
- 插件主类: 实现了插件接口的Java类

## 插件配置

每个插件的配置文件应放置在 `../conf/plugins/插件ID/` 目录下，系统启动时会自动加载。
EOL

# 复制systemd服务文件
echo -e "${YELLOW}复制systemd服务文件...${NC}"
mkdir -p "$CONF_DIR/service"
cat > "$CONF_DIR/service/qteam-os-systemd.service" << 'EOL'
[Unit]
Description=QTeamOS应用服务
After=network.target mysql.service redis.service
Wants=mysql.service redis.service

[Service]
Type=simple
User=qteam-os
Group=qteam-os
WorkingDirectory=/opt/qteam-os
ExecStart=/bin/bash /opt/qteam-os/start.sh
ExecStop=/bin/bash /opt/qteam-os/stop.sh
Restart=on-failure
RestartSec=30
SuccessExitStatus=143
TimeoutStopSec=120
LimitNOFILE=65536

Environment="JAVA_HOME=/usr/lib/jvm/java-17-openjdk"
Environment="SPRING_PROFILES_ACTIVE=prod"

[Install]
WantedBy=multi-user.target
EOL

# 获取内核版本
KERNEL_VERSION=$(grep "<version>" "$PROJECT_DIR/qteam-os/pom.xml" | head -1 | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
if [ -z "$KERNEL_VERSION" ]; then
    KERNEL_VERSION="$VERSION" # 如果无法获取，使用默认版本
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       QTeamOS 1.0 打包完成            ${NC}"
echo -e "${GREEN}       输出目录: $DIST_DIR             ${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${YELLOW}可执行以下命令启动应用:${NC}"
echo -e "${GREEN}cd $DIST_DIR && ./start.sh${NC}"

