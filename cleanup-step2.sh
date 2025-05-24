#!/bin/bash

echo "🚀 QTeam-OS 清理 - 步骤2: 删除重复API接口"
echo "================================================"

# 检查步骤1是否已完成
if [ ! -f "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/PluginSystem.java.step1.bak" ]; then
    echo "⚠️ 警告: 步骤1可能未完成，请先执行 ./cleanup-step1.sh"
    read -p "🤔 是否继续执行步骤2？(y/N): " confirm
    if [[ $confirm != [yY] ]]; then
        echo "❌ 操作已取消"
        exit 0
    fi
fi

echo "📋 将要删除的重复API接口："
echo "  - core.plugin.api.Plugin.java (与qteam-api.core.Plugin重复)"
echo "  - core.plugin.api.PluginManager.java (与qteam-api重复)"
echo "  - 整个api目录 (功能已迁移到qteam-api模块)"
echo ""

# 检查api目录是否存在
api_dir="qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/api"
if [ ! -d "$api_dir" ]; then
    echo "ℹ️ API目录不存在，可能已经清理过了"
    echo "✅ 步骤2跳过"
    exit 0
fi

# 显示api目录内容
echo "📁 当前API目录内容："
find "$api_dir" -name "*.java" | while read -r file; do
    echo "  - $(basename "$file")"
done
echo ""

read -p "🤔 确认删除重复的API接口吗？(y/N): " confirm
if [[ $confirm != [yY] ]]; then
    echo "❌ 操作已取消"
    exit 0
fi

echo ""
echo "🗑️ 开始删除重复API接口..."

# 备份api目录
if [ -d "$api_dir" ]; then
    mv "$api_dir" "$api_dir.step2.bak"
    echo "  ✅ api目录 → 备份为 api.step2.bak"
else
    echo "  ⚠️ api目录不存在，跳过"
fi

echo ""
echo "🔍 验证编译..."

cd qteam-os
if mvn clean compile -q > /dev/null 2>&1; then
    compile_status="✅ 编译成功"
else
    compile_status="⚠️ 编译失败"
fi
cd ..

echo "  $compile_status"

# 统计结果
api_backup_exists=$([ -d "$api_dir.step2.bak" ] && echo "yes" || echo "no")

echo ""
echo "📊 步骤2完成统计："
echo "  🗑️ API目录已备份: $api_backup_exists"
if [ "$api_backup_exists" = "yes" ]; then
    echo "  📁 备份目录: api.step2.bak"
    file_count=$(find "$api_dir.step2.bak" -name "*.java" | wc -l)
    echo "  📄 备份的Java文件数量: $file_count"
fi

echo ""
echo "✅ 步骤2完成！"
echo ""
echo "📋 下一步操作："
if [ "$compile_status" = "✅ 编译成功" ]; then
    echo "  1. 继续执行步骤3: ./cleanup-step3.sh"
    echo "  2. 如果需要回滚步骤2，执行: ./rollback-step2.sh"
else
    echo "  ⚠️ 编译失败，建议先检查依赖关系"
    echo "  1. 检查编译错误: cd qteam-os && mvn compile"
    echo "  2. 如果需要回滚，执行: ./rollback-step2.sh"
fi
echo "  3. 验证系统功能正常后，可删除备份: rm -rf qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/api.step2.bak"
echo ""
echo "🎯 预期进展: 完成65%的清理工作" 