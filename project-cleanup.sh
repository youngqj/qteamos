#!/bin/bash

# QTeam项目文件清理脚本
# 专门清理临时文件和编译产物，不影响开发环境
# 作者: QTeam 清理助手
# 日期: 2025-05-23

set -e

echo "🧹 开始清理QTeam项目临时文件..."

# 第一阶段：安全删除确定无用的临时文件
echo ""
echo "📁 第一阶段：删除临时文件和编译产物"

# 删除编译产物
if [ -d "build" ]; then
    echo "删除编译产物: build/"
    rm -rf build/
fi

if [ -d "target" ]; then
    echo "删除Maven编译产物: target/"
    rm -rf target/
fi

# 删除子项目的target目录
find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true

# 删除临时日志文件
if [ -d "logs" ]; then
    echo "删除日志文件: logs/"
    rm -rf logs/
fi

# 删除分析结果文件
if [ -d "analysis_results" ]; then
    echo "删除分析结果: analysis_results/"
    rm -rf analysis_results/
fi

# 删除OS临时文件（但保留IDE配置）
echo "删除OS临时文件..."
find . -name ".DS_Store" -delete 2>/dev/null || true
find . -name "Thumbs.db" -delete 2>/dev/null || true

# 删除备份文件
echo "删除备份文件..."
find . -name "*.backup" -delete 2>/dev/null || true
find . -name "*.bak" -delete 2>/dev/null || true
find . -name "*~" -delete 2>/dev/null || true

echo "✅ 第一阶段完成！"

# 第二阶段：检查插件目录状态
echo ""
echo "📂 第二阶段：插件目录状态检查"
echo "QTeam插件热部署目录结构："

# 检查并显示插件目录状态
echo ""
echo "🏭 生产环境："
if [ -d "plugins-temp" ]; then
    temp_count=$(find plugins-temp -name "*.jar" 2>/dev/null | wc -l)
    echo "  - plugins-temp/     (临时投放: ${temp_count} 个插件)"
else
    echo "  - plugins-temp/     (目录不存在，建议创建)"
fi

if [ -d "plugins" ]; then
    prod_count=$(find plugins -name "*.jar" 2>/dev/null | wc -l)
    echo "  - plugins/          (正式运行: ${prod_count} 个插件)"
else
    echo "  - plugins/          (目录不存在，建议创建)"
fi

echo ""
echo "🧪 测试环境："
if [ -d "plugins-temp-dev" ]; then
    temp_dev_count=$(find plugins-temp-dev -name "*.jar" 2>/dev/null | wc -l)
    echo "  - plugins-temp-dev/ (临时投放: ${temp_dev_count} 个插件)"
else
    echo "  - plugins-temp-dev/ (目录不存在，建议创建)"
fi

if [ -d "plugins-dev" ]; then
    dev_count=$(find plugins-dev -name "*.jar" 2>/dev/null | wc -l)
    echo "  - plugins-dev/      (测试运行: ${dev_count} 个插件)"
else
    echo "  - plugins-dev/      (目录不存在，建议创建)"
fi

echo ""
echo "📚 示例参考："
if [ -d "plugin-demos" ]; then
    demo_count=$(find plugin-demos -name "*.jar" 2>/dev/null | wc -l)
    echo "  - plugin-demos/     (示例插件: ${demo_count} 个)"
else
    echo "  - plugin-demos/     (目录不存在)"
fi

# 创建docs目录并移动文档
echo ""
echo "📝 第三阶段：整理文档文件..."
mkdir -p docs

# 移动文档文件到docs目录
moved_docs=0
for file in *.md *.txt; do
    if [ -f "$file" ] && [ "$file" != "README.md" ] && [ "$file" != "PROJECT_STRUCTURE.md" ]; then
        echo "移动文档: $file -> docs/"
        mv "$file" docs/
        moved_docs=$((moved_docs + 1))
    fi
done

if [ $moved_docs -eq 0 ]; then
    echo "没有需要移动的文档文件"
fi

# 创建scripts目录并移动脚本
echo ""
echo "📜 第四阶段：整理脚本文件..."
mkdir -p scripts

# 移动脚本文件到scripts目录
moved_scripts=0
for file in *.sh; do
    if [ -f "$file" ] && [ "$file" != "project-cleanup.sh" ] && [ "$file" != "java-cleanup.sh" ]; then
        echo "移动脚本: $file -> scripts/"
        mv "$file" scripts/
        moved_scripts=$((moved_scripts + 1))
    fi
done

if [ $moved_scripts -eq 0 ]; then
    echo "没有需要移动的脚本文件"
fi

echo ""
echo "🎉 项目文件清理完成！"
echo ""
echo "📋 建议的后续步骤："
echo "1. 运行 './java-cleanup.sh' 清理冗余的Java类文件"
echo "2. 确认插件目录结构符合热部署机制要求"
echo "3. 运行 'mvn clean compile' 验证项目编译"
echo "4. 提交清理后的代码"
echo ""
echo "💡 插件部署提醒："
echo "  - 新插件投放到 plugins-temp/ 或 plugins-temp-dev/"
echo "  - 系统会自动扫描并移动到对应的正式目录"
echo "  - 不要直接操作 plugins/ 和 plugins-dev/ 目录"
echo ""
echo "🧹 Java代码清理："
echo "  - 运行 './java-cleanup.sh' 分析冗余Java类"
echo "  - 运行 './java-cleanup.sh --interactive' 交互式清理"
echo "  - 运行 './java-cleanup.sh --safe-delete' 自动删除安全的废弃类" 