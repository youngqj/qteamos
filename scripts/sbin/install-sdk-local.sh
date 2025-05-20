#!/bin/bash
###
 # @Author: yangqijun youngqj@126.com
 # @Date: 2023-11-15 23:17:31
 # @LastEditors: yangqijun youngqj@126.com
 # @LastEditTime: 2025-05-04 17:34:36
 # @FilePath: /QTeam/scripts/sbin/install-sdk-local.sh
 # @Description: 安装SDK到本地Maven仓库
 # 
 # Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
### 

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m' # No Color

# 打印脚本标题
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}        QTeamOS SDK本地安装脚本        ${NC}"
echo -e "${GREEN}========================================${NC}"

# 设置路径
SCRIPT_DIR=$(dirname "$0")
PROJECT_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

# SDK相关设置
SDK_DIR="$PROJECT_ROOT/qteam-sdk/target"
POM_FILE="$PROJECT_ROOT/qteam-sdk/pom.xml"
VERSION="0.0.1-SNAPSHOT"  # 默认版本，将从POM文件中读取

# 检查SDK目录是否存在
if [ ! -d "$SDK_DIR" ]; then
    echo -e "${YELLOW}SDK目标目录不存在，尝试编译SDK...${NC}"
    
    # 检查POM文件是否存在
    if [ ! -f "$POM_FILE" ]; then
        echo -e "${RED}错误：找不到SDK的POM文件${NC}"
        exit 1
    fi
    
    # 编译SDK
    echo -e "${YELLOW}运行: cd $PROJECT_ROOT && mvn clean package -pl qteam-sdk${NC}"
    cd "$PROJECT_ROOT" && mvn clean package -pl qteam-sdk -DskipTests
    
    # 检查编译结果
    if [ ! -d "$SDK_DIR" ]; then
        echo -e "${RED}错误：编译SDK失败${NC}"
        exit 1
    fi
fi

# 设置SDK信息
GROUP_ID="com.xiaoqu"
ARTIFACT_ID="qteam-sdk"  # 手动设置SDK的artifactId
JAR_FILE="$SDK_DIR/$ARTIFACT_ID-$VERSION.jar"

# 检查JAR文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    # 尝试查找JAR文件
    JAR_FILES=$(find "$SDK_DIR" -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar")
    JAR_COUNT=$(echo "$JAR_FILES" | wc -l)
    
    if [ "$JAR_COUNT" -eq "0" ]; then
        echo -e "${RED}错误：找不到SDK的JAR文件${NC}"
        exit 1
    elif [ "$JAR_COUNT" -eq "1" ]; then
        JAR_FILE=$JAR_FILES
    else
        echo -e "${YELLOW}发现多个JAR文件，使用第一个：${NC}"
        JAR_FILE=$(echo "$JAR_FILES" | head -n 1)
    fi
    
    echo -e "${YELLOW}使用JAR文件: $JAR_FILE${NC}"
fi

# 安装JAR到本地Maven仓库
echo -e "${YELLOW}安装SDK到本地Maven仓库...${NC}"
mvn install:install-file -Dfile="$JAR_FILE" -DgroupId="$GROUP_ID" -DartifactId="$ARTIFACT_ID" -Dversion="$VERSION" -Dpackaging=jar

# 检查安装结果
if [ $? -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}       SDK安装成功                    ${NC}"
    echo -e "${GREEN}       GroupId: $GROUP_ID              ${NC}"
    echo -e "${GREEN}       ArtifactId: $ARTIFACT_ID        ${NC}"
    echo -e "${GREEN}       Version: $VERSION               ${NC}"
    echo -e "${GREEN}========================================${NC}"
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}       SDK安装失败                    ${NC}"
    echo -e "${RED}========================================${NC}"
    exit 1
fi
