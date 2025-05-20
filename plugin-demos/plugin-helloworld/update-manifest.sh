#!/bin/bash
# 更新JAR包中的MANIFEST.MF文件

PLUGIN_ID="helloworld-plugin"
PLUGIN_DIR="plugins-dev/${PLUGIN_ID}"
JAR_PATH="${PLUGIN_DIR}/${PLUGIN_ID}.jar"

echo "开始更新JAR包中的MANIFEST.MF..."
echo "JAR路径: ${JAR_PATH}"

# 检查JAR文件是否存在
if [ ! -f "${JAR_PATH}" ]; then
    echo "错误: JAR文件不存在: ${JAR_PATH}"
    exit 1
fi

# 创建临时目录
TEMP_DIR=$(mktemp -d)
MANIFEST_FILE="${TEMP_DIR}/MANIFEST.MF"

# 创建更新后的MANIFEST.MF
cat > "${MANIFEST_FILE}" << EOF
Manifest-Version: 1.0
Created-By: QTeamOS Plugin System
Build-Jdk-Spec: 17
Plugin-Id: helloworld-plugin
Plugin-Version: 1.0.0
Plugin-Provider: com.xiaoqu.qteamos.plugin
Plugin-Class: com.xiaoqu.qteamos.plugin.helloworld.HelloWorldPlugin
Plugin-Name: HelloWorld Plugin
Plugin-Description: A simple HelloWorld plugin for QTeamOS
Plugin-License: Mulan PSL v2
Implementation-Title: QTeamOS HelloWorld Plugin
Implementation-Version: 1.0.0
Implementation-Vendor: XiaoQu Team
EOF

# 更新JAR包中的MANIFEST.MF
echo "更新JAR包中的MANIFEST.MF..."
jar umf "${MANIFEST_FILE}" "${JAR_PATH}"

# 清理临时文件
rm -rf "${TEMP_DIR}"

echo "MANIFEST.MF更新完成！"

# 输出更新后的MANIFEST.MF验证
echo "更新后的MANIFEST.MF内容:"
jar xf "${JAR_PATH}" META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF
rm -rf META-INF 