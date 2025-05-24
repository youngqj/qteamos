#!/bin/bash

echo "🔄 QTeam-OS 回滚 - 步骤1: 恢复过时组件"
echo "============================================"

echo "📋 将要恢复的组件："
find qteam-os -name "*.step1.bak" | while read -r backup_file; do
    original_file="${backup_file%.step1.bak}"
    echo "  - $(basename "$original_file")"
done
echo ""

read -p "🤔 确认回滚步骤1吗？(y/N): " confirm
if [[ $confirm != [yY] ]]; then
    echo "❌ 回滚已取消"
    exit 0
fi

echo ""
echo "🔄 开始回滚步骤1..."

restored_count=0

# 恢复所有.step1.bak文件
find qteam-os -name "*.step1.bak" | while read -r backup_file; do
    original_file="${backup_file%.step1.bak}"
    if [ -f "$backup_file" ]; then
        mv "$backup_file" "$original_file"
        echo "  ✅ 恢复: $(basename "$original_file")"
        restored_count=$((restored_count + 1))
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

echo ""
echo "📊 回滚步骤1完成："
echo "  🔄 恢复的文件数量: $(find qteam-os -name "*.step1.bak" | wc -l 2>/dev/null || echo 0)"
echo "  📁 编译状态: $compile_status"

echo ""
echo "✅ 步骤1回滚完成！" 