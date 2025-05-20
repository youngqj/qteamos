#!/bin/bash
###
 # @Author: yangqijun youngqj@126.com
 # @Date: 2025-05-15 15:20:00
 # @LastEditors: yangqijun youngqj@126.com
 # @LastEditTime: 2025-05-04 22:31:07

 # @Description: 插件打包脚本，支持新旧格式plugin.yml的处理
 # 
 # Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
###

# ------------------------------------
# QTeamOS 标准插件打包脚本
# 适用于所有QTeamOS插件工程
# 支持标准plugin.yml格式
# 自动处理配置文件、依赖和资源
# 创建插件目录结构和归档文件
# ------------------------------------
# 使用示例：
# 1. 复制此脚本到插件项目根目录
# 2. 修改顶部的配置参数以适应目标插件
# 3. 执行 ./package.sh 进行打包
# 
# 可选参数:
#   --prod : 以生产环境模式打包 (默认)
#   --dev  : 以开发环境模式打包
#   --clean: 仅清理，不执行打包
# 
# 插件配置文件约定:
#   - plugin-{pluginId}.yml: 插件专用配置文件
#   - {pluginId}-{profile}.yml: 插件环境特定配置
#
# 重要说明:
#   * 插件不应修改系统级配置，application.yml和
#     application-{profile}.yml不会被打包
#   * 插件配置应使用上述专用配置文件
#   * 插件可以通过QTeamOS API获取这些配置
#
# 示例: ./package.sh --prod
# ------------------------------------
source  /etc/profile
# 设置捕获脚本错误
set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 恢复默认颜色

# 打印带颜色的信息
function echo_color() {
  case $1 in
    "info") echo -e "${BLUE}[INFO]${NC} $2" ;;
    "success") echo -e "${GREEN}[SUCCESS]${NC} $2" ;;
    "warning") echo -e "${YELLOW}[WARNING]${NC} $2" ;;
    "error") echo -e "${RED}[ERROR]${NC} $2" ;;
    *) echo -e "$2" ;;
  esac
}

# 处理打包失败的情况
handle_error() {
    echo_color "error" "$1"
    echo_color "error" "打包过程中止"
    exit 1
}

# 创建目录失败处理
handle_mkdir_error() {
    handle_error "无法创建目录: $1"
}

# 复制文件失败处理
handle_copy_error() {
    handle_error "无法复制文件: $1"
}

# 打印打包信息总结
print_summary() {
    echo_color "info" "┌─────────────────────────────────────────────────┐"
    echo_color "info" "│              插件信息摘要                       │"
    echo_color "info" "├─────────────────────────────────────────────────┤"
    echo_color "info" "│  插件名称: ${PLUGIN_NAME}"
    echo_color "info" "│  插件ID: ${PLUGIN_ID}"
    echo_color "info" "│  版本号: ${VERSION}"
    echo_color "info" "│  主类: ${MAIN_CLASS}"
    echo_color "info" "│  打包环境: ${PROFILE}"
    echo_color "info" "├─────────────────────────────────────────────────┤"
    echo_color "info" "│  Copyright © $(date +%Y) Zhejiang XiaoQu Information Technology Co., Ltd."
    echo_color "info" "│  All Rights Reserved."
    echo_color "info" "└─────────────────────────────────────────────────┘"
}

# 设置错误处理
trap 'handle_error "未知错误，脚本执行中断，行号: $LINENO"' ERR

# ------------------------------------
# 可配置参数 - 适配其他插件时修改这里
# ------------------------------------
# 默认插件信息 - 通常会从plugin.yml自动提取，仅作为备用
DEFAULT_PLUGIN_ID="helloworld-plugin"  # 默认插件ID
DEFAULT_PLUGIN_NAME="HelloWorld Plugin" # 默认插件名称
DEFAULT_VERSION="1.0.0"               # 默认版本号
DEFAULT_MAIN_CLASS="com.xiaoqu.qteamos.plugin.helloworld.HelloWorldPlugin" # 默认主类

# JAR包相关配置
ARTIFACT_ID="plugin-helloworld"       # Maven工件ID
JAR_NAME="qteam-plugin-helloworld"    # 最终JAR名称

# 目录配置
PLUGIN_BASE_DIR="../plugins"          # 插件基础目录
# ------------------------------------

# 命令行参数处理
PROFILE="prod"                      # 默认使用生产环境
CLEAN_ONLY=false                    # 是否仅清理

# 解析命令行参数
for arg in "$@"; do
  case $arg in
    --prod)
      PROFILE="prod"
      ;;
    --dev)
      PROFILE="dev"
      ;;
    --clean)
      CLEAN_ONLY=true
      ;;
    *)
      echo_color "warning" "未知参数: $arg"
      ;;
  esac
done

# 设置工作目录
SCRIPT_DIR=$(dirname "$0")
cd "$SCRIPT_DIR" || exit 1

# 检查 plugin.yml 是否存在
if [ ! -f "src/main/resources/plugin.yml" ]; then
    echo_color "error" "plugin.yml 文件不存在"
    exit 1
fi

# 从 plugin.yml 中提取信息
YAML_CONTENT=$(cat src/main/resources/plugin.yml)
echo_color "info" "解析plugin.yml配置文件..."

# 解析plugin.yml文件
VERSION=$(echo "$YAML_CONTENT" | grep "^version:" | cut -d':' -f2 | tr -d ' \r"')
PLUGIN_ID=$(echo "$YAML_CONTENT" | grep "^pluginId:" | cut -d':' -f2 | tr -d ' \r"')
PLUGIN_NAME=$(echo "$YAML_CONTENT" | grep "^name:" | cut -d':' -f2 | tr -d ' "\r')
MAIN_CLASS=$(echo "$YAML_CONTENT" | grep "^mainClass:" | cut -d':' -f2 | tr -d ' \r"')

# 必要字段检查
if [ -z "$PLUGIN_ID" ]; then
    echo_color "warning" "无法从plugin.yml提取pluginId，使用默认值: helloworld-plugin"
    PLUGIN_ID="$DEFAULT_PLUGIN_ID"
fi

if [ -z "$MAIN_CLASS" ]; then
    echo_color "warning" "无法从plugin.yml提取mainClass，这可能导致插件无法加载"
fi

# 如果无法提取，使用默认值
if [ -z "$VERSION" ]; then
    VERSION="${DEFAULT_VERSION}"
    echo_color "warning" "无法从plugin.yml提取版本号，使用默认值: $VERSION"
fi

if [ -z "$PLUGIN_NAME" ]; then
    PLUGIN_NAME="${DEFAULT_PLUGIN_NAME}"
    echo_color "warning" "无法从plugin.yml提取插件名称，使用默认值: $PLUGIN_NAME"
fi

if [ -z "$MAIN_CLASS" ]; then
    MAIN_CLASS="${DEFAULT_MAIN_CLASS}"
    echo_color "warning" "无法从plugin.yml提取主类名称，使用默认值: $MAIN_CLASS"
fi

# Maven构建相关
TARGET_JAR="target/${JAR_NAME}.jar"
RELEASE_DIR="target/release/${PLUGIN_ID}"
FULL_NAME="${PLUGIN_ID}-${VERSION}"
PLUGIN_DESTINATION="${PLUGIN_BASE_DIR}/${PLUGIN_ID}"

# 打印插件信息
echo_color "info" "====================================="
echo_color "info" "   QTeamOS Plugin Packager  "
echo_color "info" "====================================="
echo_color "info" "插件ID: ${PLUGIN_ID}"
echo_color "info" "版本号: ${VERSION}"
echo_color "info" "插件名: ${PLUGIN_NAME}"
echo_color "info" "开始打包插件..."

# 清理旧的构建文件
echo_color "info" "清理旧的构建文件..."
mvn clean

# 如果仅清理，退出脚本
if [ "$CLEAN_ONLY" = true ]; then
    echo_color "success" "清理完成，已跳过打包步骤"
    exit 0
fi

# 打包插件
echo_color "info" "使用Maven打包插件 (${PROFILE}环境)..."
mvn package -DskipTests -P${PROFILE}

# 检查打包结果
if [ ! -f ${TARGET_JAR} ]; then
  echo_color "error" "打包失败，未找到JAR文件: ${TARGET_JAR}"
  exit 1
fi

# 创建发布目录结构
echo_color "info" "创建插件目录结构..."
rm -rf ${RELEASE_DIR}
mkdir -p ${RELEASE_DIR}/{config,data,static,templates}

# 创建部署目录结构
echo_color "info" "创建部署目录结构..."
rm -rf ${PLUGIN_DESTINATION}
mkdir -p ${PLUGIN_DESTINATION}/{config,data,static,templates}

# 复制JAR文件到插件目录
echo_color "info" "复制JAR文件到插件目录"
cp ${TARGET_JAR} ${RELEASE_DIR}/${PLUGIN_ID}.jar
cp ${TARGET_JAR} ${PLUGIN_DESTINATION}/${PLUGIN_ID}.jar

if [ $? -ne 0 ]; then
  echo_color "error" "复制JAR文件失败"
  exit 1
fi

# 复制plugin.yml到插件根目录
echo_color "info" "复制plugin.yml到插件根目录"
cp src/main/resources/plugin.yml ${RELEASE_DIR}/
cp src/main/resources/plugin.yml ${PLUGIN_DESTINATION}/

# 复制配置文件到config目录
echo_color "info" "处理配置文件..."

# 插件特定配置文件命名约定：plugin-{pluginId}.yml
# 这种命名方式可避免与系统配置冲突
PLUGIN_CONFIG_FILE="plugin-${PLUGIN_ID}.yml"
PLUGIN_CONFIG_PROFILE="${PLUGIN_ID}-${PROFILE}.yml"

# 复制插件配置文件（如果存在）
if [ -f "src/main/resources/${PLUGIN_CONFIG_FILE}" ]; then
    cp "src/main/resources/${PLUGIN_CONFIG_FILE}" ${RELEASE_DIR}/config/
    cp "src/main/resources/${PLUGIN_CONFIG_FILE}" ${PLUGIN_DESTINATION}/config/
    echo_color "info" "已复制: ${PLUGIN_CONFIG_FILE} (插件专用配置)"
fi

# 复制环境特定的插件配置
if [ -f "src/main/resources/${PLUGIN_CONFIG_PROFILE}" ]; then
    cp "src/main/resources/${PLUGIN_CONFIG_PROFILE}" ${RELEASE_DIR}/config/
    cp "src/main/resources/${PLUGIN_CONFIG_PROFILE}" ${PLUGIN_DESTINATION}/config/
    echo_color "info" "已复制: ${PLUGIN_CONFIG_PROFILE} (${PROFILE}环境配置)"
fi

# 检查是否存在系统配置文件，如果存在则发出警告
if [ -f "src/main/resources/application.yml" ] || [ -f "src/main/resources/application-${PROFILE}.yml" ]; then
    echo_color "warning" "检测到系统配置文件(application.yml或application-${PROFILE}.yml)"
    echo_color "warning" "系统配置文件不会被打包，请使用插件专用配置文件："
    echo_color "warning" "  - ${PLUGIN_CONFIG_FILE} (基础配置)"
    echo_color "warning" "  - ${PLUGIN_CONFIG_PROFILE} (环境特定配置)"
fi

# 复制数据库脚本
if [ -d "src/main/resources/db" ]; then
    mkdir -p ${RELEASE_DIR}/config/db
    mkdir -p ${PLUGIN_DESTINATION}/config/db
    cp -r src/main/resources/db/* ${RELEASE_DIR}/config/db/
    cp -r src/main/resources/db/* ${PLUGIN_DESTINATION}/config/db/
    echo_color "info" "已复制: 数据库脚本"
fi

# 复制静态资源
if [ -d "src/main/resources/static" ]; then
    cp -r src/main/resources/static/* ${RELEASE_DIR}/static/ 2>/dev/null || :
    cp -r src/main/resources/static/* ${PLUGIN_DESTINATION}/static/ 2>/dev/null || :
    echo_color "info" "已复制: 静态资源"
fi

# 复制模板文件
if [ -d "src/main/resources/templates" ]; then
    cp -r src/main/resources/templates/* ${RELEASE_DIR}/templates/ 2>/dev/null || :
    cp -r src/main/resources/templates/* ${PLUGIN_DESTINATION}/templates/ 2>/dev/null || :
    echo_color "info" "已复制: 模板文件"
fi

# 处理依赖JAR包
if [ -d "lib" ]; then
    echo_color "info" "复制lib目录中的依赖JAR包"
    mkdir -p ${RELEASE_DIR}/lib
    mkdir -p ${PLUGIN_DESTINATION}/lib
    cp -r lib/* ${RELEASE_DIR}/lib/ 2>/dev/null || :
    cp -r lib/* ${PLUGIN_DESTINATION}/lib/ 2>/dev/null || :
    echo_color "info" "已复制: 依赖JAR包"
fi

# 创建归档文件
echo_color "info" "创建归档文件..."
cd target/release
tar -czf "${FULL_NAME}.tar.gz" "${PLUGIN_ID}/"
cd ../..

# 打印成功信息
echo_color "success" "插件打包完成"
echo_color "info" "======================================"
echo_color "info" "发布包已生成: target/release/${FULL_NAME}.tar.gz"
echo_color "info" "发布目录: ${RELEASE_DIR}"
echo_color "info" "部署目录: ${PLUGIN_DESTINATION}"
echo_color "info" "包含目录结构:"
echo_color "info" "  - plugin.yml (插件描述文件)"
echo_color "info" "  - ${PLUGIN_ID}.jar (插件主JAR包)"
echo_color "info" "  - config/ (配置文件)"
echo_color "info" "  - static/ (静态资源)"
echo_color "info" "  - templates/ (模板文件)"
echo_color "info" "  - data/ (数据目录)"
echo_color "info" "======================================"

# 打印安装说明
echo_color "info" "安装说明:"
echo_color "info" "1. 确保插件目录 ${PLUGIN_ID} 已复制到 QTeamOS 的 plugins 目录"
echo_color "info" "2. 确保 plugin.yml 配置正确，特别是 pluginId 和 mainClass"
echo_color "info" "3. 重启 QTeamOS 服务"
echo_color "info" "4. 在管理后台中启用插件"
echo_color "info" "======================================"

# 打印打包信息总结
print_summary 