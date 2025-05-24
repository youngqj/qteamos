#!/bin/bash

echo "🚀 QTeam-OS 清理 - 步骤1: 删除过时组件"
echo "================================================"

# 检查qteam-os目录是否存在
if [ ! -d "qteam-os" ]; then
    echo "❌ 错误: qteam-os目录不存在"
    exit 1
fi

echo "📋 将要删除的过时组件："
echo "  - PluginSystem.java (1704行 → 193行，已被PluginSystemCoordinator替代)"
echo "  - PluginLifecycleManager.java (1448行 → 223行，已被PluginLifecycleCoordinator替代)"
echo "  - PluginRolloutManager.java (943行 → 213行，已被新部署组件替代)"
echo "  - PluginHotDeployService.java (845行，功能已拆分到新组件)"
echo "  - PluginUpdateService.java (功能已整合)"
echo "  - PluginStateManager.java (已被DefaultPluginStateTracker替代)"
echo "  - DependencyResolver.java (功能重复)"
echo ""

read -p "🤔 确认删除这些过时组件吗？(y/N): " confirm
if [[ $confirm != [yY] ]]; then
    echo "❌ 操作已取消"
    exit 0
fi

echo ""
echo "🗑️ 开始删除过时组件..."

# 统计删除前的文件数量
before_count=0

# 1. 删除主要过时大类
echo "📁 处理主要过时大类..."

main_components=(
    "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/PluginSystem.java"
    "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/PluginLifecycleManager.java"
)

for component in "${main_components[@]}"; do
    if [ -f "$component" ]; then
        before_count=$((before_count + 1))
        mv "$component" "$component.step1.bak"
        echo "  ✅ $(basename "$component") → 备份为 .step1.bak"
    else
        echo "  ⚠️ $(basename "$component") 不存在，跳过"
    fi
done

# 2. 删除manager目录下的过时组件
echo "📁 处理manager目录下的过时组件..."

manager_components=(
    "PluginRolloutManager.java"
    "PluginHotDeployService.java" 
    "PluginUpdateService.java"
    "PluginStateManager.java"
    "DependencyResolver.java"
)

for component in "${manager_components[@]}"; do
    file_path="qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/manager/$component"
    if [ -f "$file_path" ]; then
        before_count=$((before_count + 1))
        mv "$file_path" "$file_path.step1.bak"
        echo "  ✅ $component → 备份为 .step1.bak"
    else
        echo "  ⚠️ $component 不存在，跳过"
    fi
done

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
backup_count=$(find qteam-os -name "*.step1.bak" | wc -l)

echo ""
echo "📊 步骤1完成统计："
echo "  🗑️ 备份的文件数量: $backup_count"
echo "  📁 备份文件列表:"
find qteam-os -name "*.step1.bak" | while read -r file; do
    echo "    - $(basename "$file" .step1.bak)"
done

echo ""
echo "✅ 步骤1完成！"
echo ""
echo "📋 下一步操作："
echo "  1. 如果编译成功，继续执行: ./cleanup-step2.sh"
echo "  2. 如果需要回滚步骤1，执行: ./rollback-step1.sh" 
echo "  3. 验证系统功能正常后，可删除备份: find qteam-os -name '*.step1.bak' -delete"
echo ""
echo "🎯 预期进展: 完成40%的清理工作" 