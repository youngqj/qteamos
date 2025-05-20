#!/bin/bash

###########################################
# QTeamOS 插件SDK打包脚本
# 作者: yangqijun@xiaoquio.com
# 版本: 1.0.0
# 日期: 2023-11-16
###########################################

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 版本信息
VERSION="0.0.1-SNAPSHOT"
BUILD_DATE=$(date +"%Y-%m-%d %H:%M:%S")
SDK_NAME="qteam-sdk"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       QTeamOS 插件SDK打包脚本        ${NC}"
echo -e "${GREEN}       版本: $VERSION                  ${NC}"
echo -e "${GREEN}       构建时间: $BUILD_DATE           ${NC}"
echo -e "${GREEN}========================================${NC}"

# 设置根目录和目标目录
BASE_DIR=$(dirname "$(readlink -f "$0")")
# 从scripts/sbin路径获取项目根目录
PROJECT_DIR=$(cd "$BASE_DIR/../.." && pwd)
SDK_TARGET_DIR="$PROJECT_DIR/build/sdk"

# 确保目标目录存在
mkdir -p "$SDK_TARGET_DIR"

# 清理之前的构建文件
echo -e "${YELLOW}清理之前的构建文件...${NC}"
cd "$PROJECT_DIR" && mvn clean -pl "$SDK_NAME"

# 打包SDK模块
echo -e "${YELLOW}开始打包SDK模块...${NC}"
cd "$PROJECT_DIR" && mvn package -pl "$SDK_NAME" -DskipTests

# 检查打包结果
if [ $? -ne 0 ]; then
    echo -e "${RED}SDK打包失败！请检查错误信息。${NC}"
    exit 1
fi

# 获取内核版本
KERNEL_VERSION=$(grep "<version>" "$PROJECT_DIR/qteamos/pom.xml" | head -1 | sed -E 's/.*<version>(.*)<\/version>.*/\1/')
if [ -z "$KERNEL_VERSION" ]; then
    KERNEL_VERSION="$VERSION" # 如果无法获取，使用默认版本
fi

# 复制JAR文件
echo -e "${YELLOW}复制SDK JAR文件到build/sdk目录...${NC}"
cp "$PROJECT_DIR/$SDK_NAME/target/$SDK_NAME-$VERSION.jar" "$SDK_TARGET_DIR/"
cp "$PROJECT_DIR/$SDK_NAME/target/$SDK_NAME-$VERSION-sources.jar" "$SDK_TARGET_DIR/" 2>/dev/null || echo -e "${YELLOW}源码JAR不存在，跳过复制${NC}"

# 创建README文件
echo -e "${YELLOW}创建SDK说明文档...${NC}"
cat > "$SDK_TARGET_DIR/README.md" << EOL
# QTeamOS 插件开发SDK

## 版本信息
- SDK版本: $VERSION
- 内核版本: $KERNEL_VERSION
- 构建时间: $BUILD_DATE

## 文件说明
- $SDK_NAME-$VERSION.jar: SDK主JAR包
- $SDK_NAME-$VERSION-sources.jar: 源码JAR包（如果存在）

## 本地开发使用指南

### 1. 添加到本地Maven仓库

将SDK添加到本地Maven仓库：

\`\`\`bash
mvn install:install-file -Dfile=$SDK_NAME-$VERSION.jar -DgroupId=com.xiaoqu -DartifactId=$SDK_NAME -Dversion=$VERSION -Dpackaging=jar
\`\`\`

### 2. 在IDE中使用（不使用Maven）

直接将JAR包添加到项目的依赖路径中：

- **IntelliJ IDEA**: 
  File > Project Structure > Libraries > + > Java > 选择SDK JAR文件

- **Eclipse**: 
  项目右键 > Build Path > Configure Build Path > Libraries > Add External JARs > 选择SDK JAR文件

### 3. 在Maven项目中使用

在pom.xml中添加依赖：

\`\`\`xml
<dependency>
    <groupId>com.xiaoqu</groupId>
    <artifactId>$SDK_NAME</artifactId>
    <version>$VERSION</version>
</dependency>
\`\`\`

## 插件开发指南

插件开发完整指南请访问：http://docs.xiaoquio.com/qteamos/plugin-dev-guide

### 快速入门

1. 创建一个Maven项目
2. 添加SDK依赖
3. 创建一个实现Plugin接口的类
4. 添加plugin.yml配置文件到resources目录
5. 打包为JAR文件

### 插件示例

\`\`\`java
package com.example.plugin;

import com.xiaoqu.qteamos.plugin.api.Plugin;
import com.xiaoqu.qteamos.plugin.api.PluginContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExamplePlugin implements Plugin {
    
    private PluginContext context;
    
    @Override
    public void initialize(PluginContext context) {
        this.context = context;
        log.info("插件初始化");
    }
    
    @Override
    public void start() {
        log.info("插件启动");
    }
    
    @Override
    public void stop() {
        log.info("插件停止");
    }
    
    @Override
    public void uninstall() {
        log.info("插件卸载");
    }
}
\`\`\`

plugin.yml示例：
\`\`\`yaml
pluginId: example-plugin
name: 示例插件
version: 1.0.0
description: 这是一个示例插件
author: 开发者名称
main: com.example.plugin.ExamplePlugin
\`\`\`
EOL

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}       QTeamOS SDK打包完成            ${NC}"
echo -e "${GREEN}       版本: $VERSION                  ${NC}"
echo -e "${GREEN}       内核版本: $KERNEL_VERSION       ${NC}"
echo -e "${GREEN}       输出目录: $SDK_TARGET_DIR       ${NC}"
echo -e "${GREEN}========================================${NC}" 