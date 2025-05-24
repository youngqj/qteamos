#!/bin/bash

echo "🔄 QTeam-OS 完全回滚 - 恢复所有更改"
echo "======================================="

# 检查各步骤的备份文件
step1_backups=$(find qteam-os -name "*.step1.bak" | wc -l)
step2_backups=$(find qteam-os -name "*.step2.bak" -o -name "api.step2.bak" | wc -l)
step3_backups=$(find qteam-os -name "*.step3.bak" -o -name "running.step3.bak" | wc -l)

echo "📋 发现的备份文件："
echo "  - 步骤1备份: $step1_backups 个"
echo "  - 步骤2备份: $step2_backups 个"
echo "  - 步骤3备份: $step3_backups 个"
echo ""

if [ $((step1_backups + step2_backups + step3_backups)) -eq 0 ]; then
    echo "ℹ️ 没有发现任何备份文件，无需回滚"
    exit 0
fi

echo "⚠️  警告: 这将完全回滚所有清理操作！"
echo "🔄 回滚顺序: 步骤3 → 步骤2 → 步骤1"
echo ""

read -p "🤔 确认完全回滚所有更改吗？(y/N): " confirm
if [[ $confirm != [yY] ]]; then
    echo "❌ 回滚已取消"
    exit 0
fi

echo ""
echo "🔄 开始完全回滚..."

# 步骤3回滚：恢复目录结构
if [ $step3_backups -gt 0 ]; then
    echo "📁 [3/3] 回滚步骤3: 恢复目录结构..."
    
    # 恢复running目录
    running_backup="qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/running.step3.bak"
    if [ -d "$running_backup" ]; then
        # 删除当前的model目录
        rm -rf "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/model"
        # 恢复running目录
        mv "$running_backup" "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/running"
        echo "  ✅ 恢复 model/ → running/"
    fi
    
    # 恢复import语句
    find qteam-os -name "*.step3.bak" | while read -r backup_file; do
        if [[ "$backup_file" != *"running.step3.bak" ]]; then
            original_file="${backup_file%.step3.bak}"
            mv "$backup_file" "$original_file"
            echo "  ✅ 恢复import: $(basename "$original_file")"
        fi
    done
fi

# 步骤2回滚：恢复API目录
if [ $step2_backups -gt 0 ]; then
    echo "📁 [2/3] 回滚步骤2: 恢复API接口..."
    
    api_backup="qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/api.step2.bak"
    if [ -d "$api_backup" ]; then
        mv "$api_backup" "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/api"
        echo "  ✅ 恢复 api目录"
    fi
fi

# 步骤1回滚：恢复过时组件
if [ $step1_backups -gt 0 ]; then
    echo "📁 [1/3] 回滚步骤1: 恢复过时组件..."
    
    find qteam-os -name "*.step1.bak" | while read -r backup_file; do
        original_file="${backup_file%.step1.bak}"
        mv "$backup_file" "$original_file"
        echo "  ✅ 恢复: $(basename "$original_file")"
    done
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

# 检查回滚结果
remaining_backups=$(find qteam-os -name "*.step*.bak" -o -name "api.step*.bak" -o -name "running.step*.bak" | wc -l)

echo ""
echo "📊 完全回滚结果："
echo "  🔄 剩余备份文件: $remaining_backups 个"
echo "  📁 编译状态: $compile_status"
echo ""

if [ $remaining_backups -eq 0 ]; then
    echo "✅ 完全回滚成功！所有更改已恢复。"
else
    echo "⚠️ 可能存在部分文件未完全回滚，请检查剩余备份文件："
    find qteam-os -name "*.step*.bak" -o -name "api.step*.bak" -o -name "running.step*.bak" | head -5
fi

echo ""
echo "🎯 系统已恢复到清理前状态。" 