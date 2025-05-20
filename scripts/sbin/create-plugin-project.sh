#!/bin/bash
###
 # @Author: yangqijun youngqj@126.com
 # @Date: 2023-11-15 03:20:12
 # @LastEditors: yangqijun youngqj@126.com
 # @LastEditTime: 2025-05-03 10:13:05
 # @FilePath: /QTeam/scripts/sbin/create-plugin-project.sh
 # @Description: QTeamOS插件项目创建脚本
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
echo -e "${GREEN}        QTeamOS 插件项目创建脚本       ${NC}"
echo -e "${GREEN}========================================${NC}"

# 设置根目录
BASE_DIR=$(dirname "$(readlink -f "$0")")
PROJECT_DIR=$(cd "$BASE_DIR/../.." && pwd)

# 默认配置
DEFAULT_GROUP_ID="com.xiaoqu.plugin"
DEFAULT_VERSION="0.0.1-SNAPSHOT"
DEFAULT_DESCRIPTION="QTeamOS插件"
DEFAULT_AUTHOR="Xiaoqu"
DEFAULT_PLUGIN_TYPE="normal"
DEFAULT_REDIS_ENABLED="false"
DEFAULT_REDIS_HOST="localhost"
DEFAULT_REDIS_PORT="6379"
DEFAULT_REDIS_PASSWORD=""
DEFAULT_REDIS_DB="0"

# 用户输入
read -p "插件ID (小写+连字符，例如：my-plugin): " PLUGIN_ID
read -p "插件名称 (例如：我的插件): " PLUGIN_NAME
read -p "Group ID [$DEFAULT_GROUP_ID]: " GROUP_ID
GROUP_ID=${GROUP_ID:-$DEFAULT_GROUP_ID}
read -p "版本 [$DEFAULT_VERSION]: " VERSION
VERSION=${VERSION:-$DEFAULT_VERSION}
read -p "描述 [$DEFAULT_DESCRIPTION]: " DESCRIPTION
DESCRIPTION=${DESCRIPTION:-$DEFAULT_DESCRIPTION}
read -p "作者 [$DEFAULT_AUTHOR]: " AUTHOR
AUTHOR=${AUTHOR:-$DEFAULT_AUTHOR}
read -p "插件类型 (normal/system/theme) [$DEFAULT_PLUGIN_TYPE]: " PLUGIN_TYPE
PLUGIN_TYPE=${PLUGIN_TYPE:-$DEFAULT_PLUGIN_TYPE}

# 验证插件ID
if [[ ! $PLUGIN_ID =~ ^[a-z0-9][a-z0-9-]*[a-z0-9]$ ]]; then
    echo -e "${RED}错误：插件ID格式不正确，必须是小写字母、数字和连字符${NC}"
    exit 1
fi

# 准备项目目录名
ARTIFACT_ID="qteamos-plugin-$PLUGIN_ID"
MAIN_CLASS_NAME="${PLUGIN_ID//-/}".Plugin${PLUGIN_ID^}
MAIN_CLASS_NAME=${MAIN_CLASS_NAME//-/}
# 将连字符后的字母转为大写
MAIN_CLASS_NAME=$(echo $MAIN_CLASS_NAME | sed -r 's/(^|-)(\w)/\U\2/g')

# 显示项目信息
echo -e "${YELLOW}将创建以下插件项目:${NC}"
echo -e "  插件ID:     ${GREEN}$PLUGIN_ID${NC}"
echo -e "  插件名称:   ${GREEN}$PLUGIN_NAME${NC}"
echo -e "  项目目录:   ${GREEN}$ARTIFACT_ID${NC}"
echo -e "  Group ID:   ${GREEN}$GROUP_ID${NC}"
echo -e "  版本:       ${GREEN}$VERSION${NC}"
echo -e "  描述:       ${GREEN}$DESCRIPTION${NC}"
echo -e "  作者:       ${GREEN}$AUTHOR${NC}"
echo -e "  插件类型:   ${GREEN}$PLUGIN_TYPE${NC}"
echo -e "  主类:       ${GREEN}$GROUP_ID.$MAIN_CLASS_NAME${NC}"

# 确认创建
read -p "是否创建项目? (y/n): " CONFIRM
if [[ ! $CONFIRM =~ ^[Yy]$ ]]; then
    echo -e "${RED}已取消创建${NC}"
    exit 0
fi

# 创建项目目录
echo -e "${YELLOW}创建项目目录结构...${NC}"
mkdir -p "$ARTIFACT_ID/src/main/java"
mkdir -p "$ARTIFACT_ID/src/main/resources"
mkdir -p "$ARTIFACT_ID/src/test/java"
mkdir -p "$ARTIFACT_ID/src/test/resources"

# 创建包目录
GROUP_ID_DIRS=$(echo $GROUP_ID | sed 's/\./\//g')
PACKAGE_DIR="$ARTIFACT_ID/src/main/java/$GROUP_ID_DIRS"
mkdir -p "$PACKAGE_DIR"

# 创建pom.xml文件
echo -e "${YELLOW}创建pom.xml文件...${NC}"
cat > "$ARTIFACT_ID/pom.xml" << EOL
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>${GROUP_ID}</groupId>
    <artifactId>${ARTIFACT_ID}</artifactId>
    <version>${VERSION}</version>
    <packaging>jar</packaging>

    <name>${PLUGIN_NAME}</name>
    <description>${DESCRIPTION}</description>
    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven.compiler.source>\${java.version}</maven.compiler.source>
        <maven.compiler.target>\${java.version}</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- QTeamOS SDK -->
        <dependency>
            <groupId>com.xiaoqu</groupId>
            <artifactId>qteam-sdk</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>

        <!-- 日志 -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.9</version>
            <scope>provided</scope>
        </dependency>

        <!-- 工具 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>5.7.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>\${maven.compiler.source}</source>
                    <target>\${maven.compiler.target}</target>
                    <encoding>\${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-source-plugin</artifactId>
                <version>3.3.0</version>
                <executions>
                    <execution>
                        <id>attach-sources</id>
                        <goals>
                            <goal>jar</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addDefaultImplementationEntries>true</addDefaultImplementationEntries>
                            <addDefaultSpecificationEntries>true</addDefaultSpecificationEntries>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOL

# 创建插件主类
echo -e "${YELLOW}创建插件主类...${NC}"
cat > "$PACKAGE_DIR/$MAIN_CLASS_NAME.java" << EOL
package $GROUP_ID;

import com.xiaoqu.qteamos.sdk.plugin.Plugin;
import com.xiaoqu.qteamos.sdk.plugin.PluginContext;
import lombok.extern.slf4j.Slf4j;

/**
 * $PLUGIN_NAME 插件主类
 * 
 * @author $AUTHOR
 * @version $VERSION
 */
@Slf4j
public class $MAIN_CLASS_NAME implements Plugin {
    
    private PluginContext context;
    
    @Override
    public void initialize(PluginContext context) {
        this.context = context;
        log.info("$PLUGIN_NAME 插件初始化");
    }
    
    @Override
    public void start() {
        log.info("$PLUGIN_NAME 插件启动");
        // TODO: 在此添加插件启动逻辑
    }
    
    @Override
    public void stop() {
        log.info("$PLUGIN_NAME 插件停止");
        // TODO: 在此添加插件停止逻辑
    }
    
    @Override
    public void uninstall() {
        log.info("$PLUGIN_NAME 插件卸载");
        // TODO: 在此添加插件卸载逻辑
    }
}
EOL

# 创建plugin.yml文件
echo -e "${YELLOW}创建plugin.yml配置文件...${NC}"
cat > "$ARTIFACT_ID/src/main/resources/plugin.yml" << EOL
# 插件基本信息
pluginId: $PLUGIN_ID           # 插件唯一标识
name: $PLUGIN_NAME             # 插件显示名称
version: $VERSION              # 插件版本号
description: $DESCRIPTION      # 插件描述
author: $AUTHOR                # 插件作者
main: $GROUP_ID.$MAIN_CLASS_NAME # 插件入口类
type: $PLUGIN_TYPE             # 插件类型：normal(普通插件), system(系统插件), theme(主题插件)

# 插件元数据
metadata:
  website: "https://www.example.com"          # 插件官网
  updateUrl: "https://www.example.com/update" # 更新地址
  documentation: "https://docs.example.com"   # 文档地址

# 插件依赖配置
requires:
  # 依赖的其他插件，格式为pluginId: versionRange
  # core: ">=0.0.1"  # 依赖核心插件0.0.1及以上版本

# 插件配置
config:
  debug: false
  logLevel: "INFO"
EOL

# 创建README.md文件
echo -e "${YELLOW}创建README.md文件...${NC}"
cat > "$ARTIFACT_ID/README.md" << EOL
# $PLUGIN_NAME

$DESCRIPTION

## 简介

这是一个为QTeamOS平台开发的插件。

## 功能

- 功能1
- 功能2
- 功能3

## 安装

1. 下载插件JAR包
2. 将JAR包放入QTeamOS的plugins目录
3. 重启QTeamOS

## 使用

详细的使用说明...

## 配置

插件配置说明...

## 开发

### 环境要求

- JDK 17+
- Maven 3.6+

### 构建

```bash
mvn clean package
```

### 本地开发

1. 克隆项目
2. 导入IDE
3. 添加QTeamOS SDK依赖

## 许可证

版权所有 © $AUTHOR
EOL

# 创建.gitignore文件
echo -e "${YELLOW}创建.gitignore文件...${NC}"
cat > "$ARTIFACT_ID/.gitignore" << EOL
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
*.iws
*.ipr
.classpath
.project
.settings/
.factorypath
.vscode/

# 日志
logs/
*.log

# 系统文件
.DS_Store
Thumbs.db
EOL

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}        插件项目创建成功!              ${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "项目目录: ${GREEN}$ARTIFACT_ID${NC}"
echo -e "${YELLOW}要构建插件，请运行:${NC}"
echo -e "${GREEN}cd $ARTIFACT_ID && mvn clean package${NC}"
