#!/bin/bash

# 定义颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # 无颜色

# 脚本目录
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
PROJECT_ROOT=$(dirname "$(dirname "$SCRIPT_DIR")")
cd "$PROJECT_ROOT" || { echo -e "${RED}无法进入项目根目录${NC}"; exit 1; }

# 配置参数
VERSION=$(grep "<version>" pom.xml | head -1 | sed -e 's/<version>\(.*\)<\/version>/\1/' -e 's/^[[:space:]]*//')
DOCKER_IMAGE_NAME="qelebase/server"
DOCKER_IMAGE_TAG="${VERSION}"
DOCKERFILE_PATH="$SCRIPT_DIR/Dockerfile"
JAR_PATH="qelebase-launcher/target/qelebase-launcher-${VERSION}.jar"

# 帮助信息
show_help() {
    echo -e "${GREEN}QEleBase Docker构建工具${NC}"
    echo "构建Docker镜像并可选择推送到Docker仓库"
    echo ""
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -h, --help        显示帮助信息"
    echo "  -v, --version     指定版本号 (默认: $VERSION)"
    echo "  -n, --name        指定镜像名称 (默认: $DOCKER_IMAGE_NAME)"
    echo "  -t, --tag         指定镜像标签 (默认: 当前版本号)"
    echo "  -p, --push        构建后推送到Docker仓库"
    echo "  -f, --file        指定Dockerfile路径 (默认: $DOCKERFILE_PATH)"
    echo "  -s, --skip-build  跳过Maven构建，直接构建Docker镜像"
    echo ""
    echo "示例:"
    echo "  $0 --version 1.0.0 --name myorg/qelebase --push"
    echo "  $0 --skip-build --tag latest"
    exit 0
}

# 解析命令行参数
PUSH_IMAGE=false
SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -h|--help)
            show_help
            ;;
        -v|--version)
            VERSION="$2"
            DOCKER_IMAGE_TAG="$VERSION"
            shift 2
            ;;
        -n|--name)
            DOCKER_IMAGE_NAME="$2"
            shift 2
            ;;
        -t|--tag)
            DOCKER_IMAGE_TAG="$2"
            shift 2
            ;;
        -p|--push)
            PUSH_IMAGE=true
            shift
            ;;
        -f|--file)
            DOCKERFILE_PATH="$2"
            shift 2
            ;;
        -s|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        *)
            echo -e "${RED}未知选项: $1${NC}"
            show_help
            ;;
    esac
done

# 检查Docker是否安装
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker未安装${NC}"
    exit 1
fi

# 创建Dockerfile（如果不存在）
if [ ! -f "$DOCKERFILE_PATH" ]; then
    echo -e "${YELLOW}Dockerfile不存在，正在创建...${NC}"
    cat > "$DOCKERFILE_PATH" << EOL
FROM openjdk:17-jdk-slim

# 添加环境变量
ENV APP_HOME=/opt/qelebase
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Shanghai

# 创建应用目录
RUN mkdir -p \$APP_HOME/logs \$APP_HOME/plugins \$APP_HOME/config

# 设置工作目录
WORKDIR \$APP_HOME

# 复制应用JAR包
COPY qelebase-launcher-*.jar \$APP_HOME/qelebase-launcher.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-Xms1G", "-Xmx2G", "-XX:+UseG1GC", "-jar", "qelebase-launcher.jar"]
EOL
    echo -e "${GREEN}Dockerfile已创建:${NC} $DOCKERFILE_PATH"
fi

# 先构建应用
if [ "$SKIP_BUILD" != "true" ]; then
    echo -e "${GREEN}正在构建QEleBase应用...${NC}"
    if [ -f "$SCRIPT_DIR/build-kernel.sh" ]; then
        bash "$SCRIPT_DIR/build-kernel.sh"
    else
        mvn clean package -DskipTests
    fi
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Maven构建失败!${NC}"
        exit 1
    fi
fi

# 检查JAR包是否存在
if [ ! -f "$JAR_PATH" ]; then
    echo -e "${RED}错误: JAR包不存在: $JAR_PATH${NC}"
    echo -e "${YELLOW}请先构建应用或检查JAR路径${NC}"
    exit 1
fi

# 构建Docker镜像
echo -e "${GREEN}正在构建Docker镜像: ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}...${NC}"
cp "$JAR_PATH" "$(dirname "$DOCKERFILE_PATH")/qelebase-launcher-${VERSION}.jar"
docker build -t "${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}" -f "$DOCKERFILE_PATH" "$(dirname "$DOCKERFILE_PATH")"
BUILD_STATUS=$?

# 清理临时JAR文件
rm "$(dirname "$DOCKERFILE_PATH")/qelebase-launcher-${VERSION}.jar"

# 检查构建结果
if [ $BUILD_STATUS -ne 0 ]; then
    echo -e "${RED}Docker镜像构建失败!${NC}"
    exit 1
fi

echo -e "${GREEN}Docker镜像构建成功: ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}${NC}"

# 推送镜像（如果需要）
if [ "$PUSH_IMAGE" = "true" ]; then
    echo -e "${GREEN}正在推送Docker镜像到仓库...${NC}"
    docker push "${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Docker镜像推送失败!${NC}"
        echo -e "${YELLOW}请确保您已登录到Docker仓库并有推送权限${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Docker镜像已推送: ${DOCKER_IMAGE_NAME}:${DOCKER_IMAGE_TAG}${NC}"
fi

echo -e "${GREEN}完成!${NC}" 