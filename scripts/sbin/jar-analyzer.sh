#!/bin/bash
###
 # @Author: yangqijun youngqj@126.com
 # @Date: 2025-04-15 21:33:22
 # @LastEditors: yangqijun youngqj@126.com
 # @LastEditTime: 2025-04-15 22:23:38
 # @FilePath: /qelebase/scripts/jar-analyzer.sh
 # @Description: 
 # 
 # Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
### 

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 帮助信息
show_help() {
  echo -e "${CYAN}JAR包分析工具${NC}"
  echo "用法: $0 <jar-file> [选项]"
  echo ""
  echo "选项:"
  echo "  -p, --packages <package1,package2,...>  分析指定的包路径"
  echo "  -c, --classes <class1,class2,...>       查找特定的类名"
  echo "  -d, --details                           显示详细信息"
  echo "  -s, --save                              保存分析结果到文件"
  echo "  -h, --help                              显示帮助信息"
  echo ""
  echo "示例:"
  echo "  $0 app.jar                              分析JAR包结构"
  echo "  $0 app.jar -p com.example,org.test      分析指定包路径"
  echo "  $0 app.jar -c User,Product              查找特定类"
  echo ""
  exit 0
}

# 检查参数
if [ $# -lt 1 ]; then
  show_help
fi

# 默认参数
JAR_FILE=""
PACKAGES=""
CLASSES=""
SHOW_DETAILS=false
SAVE_TO_FILE=false

# 解析命令行参数
while [[ $# -gt 0 ]]; do
  case $1 in
    -h|--help)
      show_help
      ;;
    -p|--packages)
      PACKAGES=$2
      shift 2
      ;;
    -c|--classes)
      CLASSES=$2
      shift 2
      ;;
    -d|--details)
      SHOW_DETAILS=true
      shift
      ;;
    -s|--save)
      SAVE_TO_FILE=true
      shift
      ;;
    *)
      if [ -z "$JAR_FILE" ]; then
        JAR_FILE=$1
        shift
      else
        echo -e "${RED}错误: 未知参数 $1${NC}"
        show_help
      fi
      ;;
  esac
done

# 检查JAR文件
if [ ! -f "$JAR_FILE" ]; then
  echo -e "${RED}错误: JAR文件不存在: $JAR_FILE${NC}"
  exit 1
fi

# 准备输出文件
if [ "$SAVE_TO_FILE" = true ]; then
  OUTPUT_FILE="${JAR_FILE%.*}-analysis.txt"
  # 确保文件为空
  > "$OUTPUT_FILE"
  # 定义输出函数
  output() {
    echo -e "$1" | tee -a "$OUTPUT_FILE"
  }
else
  # 定义输出函数
  output() {
    echo -e "$1"
  }
fi

# 开始分析
output "${CYAN}=========================================================${NC}"
output "${CYAN}            JAR包分析工具                 ${NC}"
output "${CYAN}=========================================================${NC}"

# JAR基本信息
output "${BLUE}[基本信息]${NC}"
JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
JAR_NAME=$(basename "$JAR_FILE")
output "JAR文件: ${GREEN}$JAR_NAME${NC}"
output "文件路径: ${GREEN}$JAR_FILE${NC}"
output "文件大小: ${GREEN}$JAR_SIZE${NC}"

# 查看MANIFEST信息
output "\n${BLUE}[MANIFEST信息]${NC}"
TEMP_DIR=$(mktemp -d)
MANIFEST_FILE="$TEMP_DIR/MANIFEST.MF"
jar xf "$JAR_FILE" "META-INF/MANIFEST.MF" -C "$TEMP_DIR" 2>/dev/null
if [ -f "$TEMP_DIR/META-INF/MANIFEST.MF" ]; then
  mv "$TEMP_DIR/META-INF/MANIFEST.MF" "$MANIFEST_FILE"
  if [ -f "$MANIFEST_FILE" ]; then
    # 查找主要属性
    MAIN_CLASS=$(grep "Main-Class" "$MANIFEST_FILE" | cut -d' ' -f2)
    if [ ! -z "$MAIN_CLASS" ]; then
      output "主类: ${GREEN}$MAIN_CLASS${NC}"
    fi
    
    # 查找Spring Boot信息
    SPRING_BOOT_VERSION=$(grep "Spring-Boot-Version" "$MANIFEST_FILE" | cut -d' ' -f2)
    if [ ! -z "$SPRING_BOOT_VERSION" ]; then
      output "Spring Boot版本: ${GREEN}$SPRING_BOOT_VERSION${NC}"
    fi
    
    # 是否为可执行JAR
    if grep -q "Spring-Boot-Classpath-Index" "$MANIFEST_FILE"; then
      output "类型: ${GREEN}Spring Boot可执行JAR${NC}"
    else
      output "类型: ${GREEN}标准JAR包${NC}"
    fi
    
    # 完整MANIFEST
    if [ "$SHOW_DETAILS" = true ]; then
      output "\n${YELLOW}MANIFEST完整内容:${NC}"
      cat "$MANIFEST_FILE" | sed 's/^/  /'
    fi
  fi
fi

# 分析包结构
if [ ! -z "$PACKAGES" ]; then
  output "\n${BLUE}[包分析]${NC}"
  IFS=',' read -ra PACKAGE_ARRAY <<< "$PACKAGES"
  for pkg in "${PACKAGE_ARRAY[@]}"; do
    pkg_path=$(echo "$pkg" | tr '.' '/')
    output "${YELLOW}包: $pkg${NC}"

    # 检查标准类路径
    CLASS_COUNT=$(jar tvf "$JAR_FILE" | grep -c "$pkg_path/")
    if [ $CLASS_COUNT -gt 0 ]; then
      output "  ${GREEN}✓ 在标准类路径中找到 ${CLASS_COUNT} 个文件${NC}"
      if [ "$SHOW_DETAILS" = true ]; then
        SOME_CLASSES=$(jar tvf "$JAR_FILE" | grep "$pkg_path/" | grep "\.class$" | head -5)
        output "  示例类: "
        echo "$SOME_CLASSES" | sed 's/^/    /' | output
      fi
    fi
    
    # 检查BOOT-INF/classes目录
    BOOTINF_CLASS_COUNT=$(jar tvf "$JAR_FILE" | grep -c "BOOT-INF/classes/$pkg_path/")
    if [ $BOOTINF_CLASS_COUNT -gt 0 ]; then
      output "  ${GREEN}✓ 在BOOT-INF/classes中找到 ${BOOTINF_CLASS_COUNT} 个文件${NC}"
      if [ "$SHOW_DETAILS" = true ]; then
        SOME_BOOTINF_CLASSES=$(jar tvf "$JAR_FILE" | grep "BOOT-INF/classes/$pkg_path/" | grep "\.class$" | head -5)
        output "  示例类: "
        echo "$SOME_BOOTINF_CLASSES" | sed 's/^/    /' | output
      fi
    fi
    
    # 检查是否在嵌套JAR中
    jar tvf "$JAR_FILE" | grep "BOOT-INF/lib/" | while read jar_path; do
      TEMP_NESTED_DIR=$(mktemp -d)
      NESTED_JAR="$TEMP_NESTED_DIR/nested.jar"
      jar xf "$JAR_FILE" "$jar_path" -C "$TEMP_NESTED_DIR" 2>/dev/null
      NESTED_JAR_PATH=$(find "$TEMP_NESTED_DIR" -type f -name "*.jar" | head -1)
      if [ -f "$NESTED_JAR_PATH" ]; then
        NESTED_CLASS_COUNT=$(jar tvf "$NESTED_JAR_PATH" | grep -c "$pkg_path/")
        if [ $NESTED_CLASS_COUNT -gt 0 ]; then
          NESTED_JAR_NAME=$(basename "$jar_path")
          output "  ${YELLOW}! 在嵌套JAR中找到 ${NESTED_CLASS_COUNT} 个文件: ${NESTED_JAR_NAME}${NC}"
          if [ "$SHOW_DETAILS" = true ]; then
            SOME_NESTED_CLASSES=$(jar tvf "$NESTED_JAR_PATH" | grep "$pkg_path/" | grep "\.class$" | head -3)
            if [ ! -z "$SOME_NESTED_CLASSES" ]; then
              output "  示例类: "
              echo "$SOME_NESTED_CLASSES" | sed 's/^/    /' | output
            fi
          fi
        fi
      fi
      rm -rf "$TEMP_NESTED_DIR"
    done
    
    if [ $CLASS_COUNT -eq 0 ] && [ $BOOTINF_CLASS_COUNT -eq 0 ]; then
      output "  ${RED}✗ 未找到包: $pkg${NC}"
    fi
    output ""
  done
fi

# 查找特定类
if [ ! -z "$CLASSES" ]; then
  output "${BLUE}[类搜索]${NC}"
  IFS=',' read -ra CLASS_ARRAY <<< "$CLASSES"
  for class_name in "${CLASS_ARRAY[@]}"; do
    output "${YELLOW}查找类: $class_name${NC}"
    FOUND=false
    
    # 在标准路径中查找
    FOUND_CLASSES=$(jar tvf "$JAR_FILE" | grep -i "/${class_name}\.class$")
    if [ ! -z "$FOUND_CLASSES" ]; then
      output "  ${GREEN}✓ 在标准类路径中找到:${NC}"
      echo "$FOUND_CLASSES" | sed 's/^/    /' | output
      FOUND=true
    fi
    
    # 在BOOT-INF/classes中查找
    FOUND_BOOT_CLASSES=$(jar tvf "$JAR_FILE" | grep -i "BOOT-INF/classes/.*/${class_name}\.class$")
    if [ ! -z "$FOUND_BOOT_CLASSES" ]; then
      output "  ${GREEN}✓ 在BOOT-INF/classes中找到:${NC}"
      echo "$FOUND_BOOT_CLASSES" | sed 's/^/    /' | output
      FOUND=true
    fi
    
    # 在嵌套JAR中查找
    if [ "$FOUND" = false ]; then
      jar tvf "$JAR_FILE" | grep "BOOT-INF/lib/" | while read jar_path; do
        TEMP_NESTED_DIR=$(mktemp -d)
        jar xf "$JAR_FILE" "$jar_path" -C "$TEMP_NESTED_DIR" 2>/dev/null
        NESTED_JAR_PATH=$(find "$TEMP_NESTED_DIR" -type f -name "*.jar" | head -1)
        if [ -f "$NESTED_JAR_PATH" ]; then
          NESTED_FOUND=$(jar tvf "$NESTED_JAR_PATH" | grep -i "/${class_name}\.class$")
          if [ ! -z "$NESTED_FOUND" ]; then
            NESTED_JAR_NAME=$(basename "$jar_path")
            output "  ${YELLOW}! 在嵌套JAR中找到: ${NESTED_JAR_NAME}${NC}"
            echo "$NESTED_FOUND" | sed 's/^/    /' | output
            FOUND=true
          fi
        fi
        rm -rf "$TEMP_NESTED_DIR"
      done
    fi
    
    if [ "$FOUND" = false ]; then
      output "  ${RED}✗ 未找到类: $class_name${NC}"
    fi
    output ""
  done
fi

# 显示依赖关系
output "${BLUE}[依赖关系]${NC}"
TOTAL_DEPS=$(jar tvf "$JAR_FILE" | grep -c "BOOT-INF/lib/")
if [ $TOTAL_DEPS -gt 0 ]; then
  output "总依赖数: ${GREEN}${TOTAL_DEPS}${NC}"
  if [ "$SHOW_DETAILS" = true ]; then
    output "依赖库列表:"
    jar tvf "$JAR_FILE" | grep "BOOT-INF/lib/" | sort | sed 's/^/  /' | output
  else
    output "前20个依赖库:"
    jar tvf "$JAR_FILE" | grep "BOOT-INF/lib/" | sort | head -20 | sed 's/^/  /' | output
    if [ $TOTAL_DEPS -gt 20 ]; then
      output "  ... 还有 $((TOTAL_DEPS - 20)) 个依赖省略"
    fi
  fi
else
  # 检查标准JAR依赖
  LIB_DIR_COUNT=$(jar tvf "$JAR_FILE" | grep -c "^lib/")
  if [ $LIB_DIR_COUNT -gt 0 ]; then
    output "总依赖数: ${GREEN}${LIB_DIR_COUNT}${NC} (标准JAR包格式)"
    if [ "$SHOW_DETAILS" = true ]; then
      output "依赖库列表:"
      jar tvf "$JAR_FILE" | grep "^lib/" | sort | sed 's/^/  /' | output
    else
      output "前20个依赖库:"
      jar tvf "$JAR_FILE" | grep "^lib/" | sort | head -20 | sed 's/^/  /' | output
      if [ $LIB_DIR_COUNT -gt 20 ]; then
        output "  ... 还有 $((LIB_DIR_COUNT - 20)) 个依赖省略"
      fi
    fi
  else
    output "${YELLOW}未找到依赖库目录 (BOOT-INF/lib/ 或 lib/)${NC}"
  fi
fi

# 清理临时文件
rm -rf "$TEMP_DIR"

# 完成信息
output "\n${CYAN}=========================================================${NC}"
output "${CYAN}              分析完成                              ${NC}"
output "${CYAN}=========================================================${NC}"

if [ "$SAVE_TO_FILE" = true ]; then
  echo -e "\n分析结果已保存至: ${GREEN}${OUTPUT_FILE}${NC}"
fi

exit 0 