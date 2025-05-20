#!/bin/bash
###
 # @Author: yangqijun youngqj@126.com
 # @Date: 2025-05-05 03:06:23
 # @LastEditors: yangqijun youngqj@126.com
 # @LastEditTime: 2025-05-05 03:23:50
 # @FilePath: /QTeam/deploy-plugin.sh
 # @Description: 
 # 
 # Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
### 

# HelloWorld插件部署脚本

# 设置插件源目录和目标目录
PLUGIN_SOURCE_DIR="plugin-demos/plugin-helloworld/target"
PLUGIN_JAR_NAME="plugin-helloworld-1.0.0.jar"
PLUGIN_TARGET_DIR="plugins/helloworld-plugin"
TEMP_PLUGIN_DIR="plugins-temp"

# 输出日志
echo "=============================================="
echo "QTeamOS 插件部署脚本"
echo "=============================================="

# 创建插件目录(如果不存在)
if [ ! -d "$PLUGIN_TARGET_DIR" ]; then
    echo "创建插件目录: $PLUGIN_TARGET_DIR"
    mkdir -p "$PLUGIN_TARGET_DIR"
fi

# 创建临时目录(如果不存在)
if [ ! -d "$TEMP_PLUGIN_DIR" ]; then
    echo "创建临时目录: $TEMP_PLUGIN_DIR"
    mkdir -p "$TEMP_PLUGIN_DIR"
fi

# 检查源插件JAR是否存在
if [ ! -f "$PLUGIN_SOURCE_DIR/$PLUGIN_JAR_NAME" ]; then
    echo "错误: 插件JAR文件不存在: $PLUGIN_SOURCE_DIR/$PLUGIN_JAR_NAME"
    echo "请先构建插件项目"
    exit 1
fi

# 复制插件JAR到插件目录
echo "复制插件JAR到目标目录..."
cp "$PLUGIN_SOURCE_DIR/$PLUGIN_JAR_NAME" "$PLUGIN_TARGET_DIR/"
echo "复制到目标目录完成: $PLUGIN_TARGET_DIR/$PLUGIN_JAR_NAME"

# 复制插件JAR到临时目录(用于热部署)
echo "复制插件JAR到临时目录以触发热部署..."
cp "$PLUGIN_SOURCE_DIR/$PLUGIN_JAR_NAME" "$TEMP_PLUGIN_DIR/"
echo "复制到临时目录完成: $TEMP_PLUGIN_DIR/$PLUGIN_JAR_NAME"

# 复制plugin.yml文件
echo "复制plugin.yml文件..."
cp "plugin-demos/plugin-helloworld/src/main/resources/plugin.yml" "$PLUGIN_TARGET_DIR/"
echo "复制plugin.yml完成"

echo "=============================================="
echo "插件部署完成!"
echo "如果QTeamOS已启动，系统将自动加载插件"
echo "=============================================="

exit 0 