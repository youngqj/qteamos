#!/bin/bash

# 获取脚本所在目录
SCRIPT_DIR=$(dirname "$(readlink -f "$0")")
cd "${SCRIPT_DIR}"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 配置
LAUNCHER_JAR="qelebase-launcher/target/qelebase-launcher-1.0.0.jar"
MODULES=("common" "core" "security" "plugin-api")

# 标题
echo  "${CYAN}=========================================================${NC}"
echo  "${CYAN}           QEleBase 依赖检查工具                         ${NC}"
echo  "${CYAN}=========================================================${NC}"

# 检查 jar 是否存在
if [ ! -f "${LAUNCHER_JAR}" ]; then
  echo  "${RED}[错误] ${NC}Launcher JAR包不存在: ${LAUNCHER_JAR}"
  echo  "${YELLOW}[提示] ${NC}请先构建项目，使用命令: mvn clean package"
  exit 1
fi

echo  "${BLUE}[信息] ${NC}检查JAR包: ${LAUNCHER_JAR}"
echo ""

# 显示JAR包基本信息
JAR_SIZE=$(du -h "${LAUNCHER_JAR}" | cut -f1)
echo  "${BLUE}[基本信息] ${NC}"
echo  "        JAR包大小: ${GREEN}${JAR_SIZE}${NC}"
CREATION_DATE=$(date -r "${LAUNCHER_JAR}" "+%Y-%m-%d %H:%M:%S")
echo  "        创建时间: ${GREEN}${CREATION_DATE}${NC}"
echo ""

# 使用 jar 命令列出 MANIFEST 文件内容
echo  "${BLUE}[清单文件] ${NC}"
jar tvf "${LAUNCHER_JAR}" META-INF/MANIFEST.MF | head -n 10
echo ""

# 分析 JAR 包中是否包含模块
echo  "${BLUE}[内部依赖分析] ${NC}"
echo  "检查是否包含以下模块:"

for module in "${MODULES[@]}"; do
  module_pattern="qelebase-${module}"
  
  # 检查 JAR 包中是否有这个模块的类
  if jar tvf "${LAUNCHER_JAR}" | grep -q "com/xiaoqu/qelebase/${module}/"; then
    echo  "    ✅ ${GREEN}已找到${NC}: 模块 ${GREEN}qelebase-${module}${NC} 的类已被包含"
  else
    echo  "    ❌ ${RED}未找到${NC}: 未包含模块 ${RED}qelebase-${module}${NC} 的类"
  fi
  
  # 检查 BOOT-INF/lib 目录中是否有这个模块的 JAR
  if jar tvf "${LAUNCHER_JAR}" | grep -q "BOOT-INF/lib/${module_pattern}"; then
    BOOT_JAR=$(jar tvf "${LAUNCHER_JAR}" | grep "BOOT-INF/lib/${module_pattern}" | head -1)
    echo  "        📦 包含JAR: ${YELLOW}${BOOT_JAR}${NC}"
  fi
done

echo ""

# 列出所有外部依赖
echo  "${BLUE}[外部依赖列表] ${NC}"
echo  "BOOT-INF/lib/ 目录下的前10个JAR包:"
jar tvf "${LAUNCHER_JAR}" | grep "BOOT-INF/lib/" | sort | head -10
echo  "${YELLOW}[提示] ${NC}使用以下命令查看完整依赖列表:"
echo  "    jar tvf ${LAUNCHER_JAR} | grep \"BOOT-INF/lib/\" | sort"

echo ""
echo  "${BLUE}[类加载分析] ${NC}"
echo  "查找关键类的路径:"

KEY_CLASSES=(
  "com/xiaoqu/qelebase/core/plugin/PluginManager"
  "com/xiaoqu/qelebase/common/utils/StringUtils"
  "com/xiaoqu/qelebase/security/config/SecurityConfig"
  "com/xiaoqu/qelebase/plugin/api/Plugin"
)

for class in "${KEY_CLASSES[@]}"; do
  if jar tvf "${LAUNCHER_JAR}" | grep -q "${class}"; then
    class_path=$(jar tvf "${LAUNCHER_JAR}" | grep "${class}")
    echo  "    ✅ ${GREEN}已找到${NC}: ${class_path}"
  else
    echo  "    ❌ ${RED}未找到${NC}: ${class}"
  fi
done

echo ""
echo  "${CYAN}=========================================================${NC}"
echo  "${GREEN}依赖检查完成!${NC}"
echo  "${CYAN}=========================================================${NC}" 