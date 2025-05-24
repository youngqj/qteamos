#!/bin/bash

echo "🚀 QTeam-OS 清理 - 步骤3: 重组目录结构"
echo "================================================"

# 检查前面步骤是否已完成
step1_done=$([ -f "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/PluginSystem.java.step1.bak" ] && echo "yes" || echo "no")
step2_done=$([ -d "qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/api.step2.bak" ] && echo "yes" || echo "no")

echo "📋 前面步骤状态："
echo "  - 步骤1 (删除过时组件): $step1_done"
echo "  - 步骤2 (删除重复API): $step2_done" 
echo ""

if [ "$step1_done" = "no" ]; then
    echo "⚠️ 警告: 步骤1未完成，建议先执行 ./cleanup-step1.sh"
fi

echo "📋 将要进行的目录重组："
echo "  - running/ → model/ (更清晰的语义命名)"
echo "  - 更新所有import语句: com.xiaoqu.qteamos.core.plugin.running → model"
echo ""

# 检查running目录是否存在
running_dir="qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/running"
if [ ! -d "$running_dir" ]; then
    echo "ℹ️ running目录不存在，可能已经重组过了"
    echo "✅ 步骤3跳过"
    exit 0
fi

# 显示running目录内容
echo "📁 当前running目录内容："
find "$running_dir" -name "*.java" | while read -r file; do
    echo "  - $(basename "$file")"
done
echo ""

# 检查是否有文件在使用running包
echo "🔍 检查import依赖..."
import_count=$(find qteam-os/src -name "*.java" -exec grep -l "com\.xiaoqu\.qteamos\.core\.plugin\.running" {} \; | wc -l)
echo "  📄 使用running包的文件数量: $import_count"

if [ "$import_count" -gt 0 ]; then
    echo "  📋 使用running包的文件："
    find qteam-os/src -name "*.java" -exec grep -l "com\.xiaoqu\.qteamos\.core\.plugin\.running" {} \; | head -5 | while read -r file; do
        echo "    - $(basename "$file")"
    done
    if [ "$import_count" -gt 5 ]; then
        echo "    - ... 和其他 $((import_count - 5)) 个文件"
    fi
fi
echo ""

read -p "🤔 确认重组目录结构吗？(y/N): " confirm
if [[ $confirm != [yY] ]]; then
    echo "❌ 操作已取消"
    exit 0
fi

echo ""
echo "📁 开始重组目录结构..."

# 1. 重命名running目录为model
model_dir="qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/model"
if [ -d "$running_dir" ]; then
    # 先创建备份
    cp -r "$running_dir" "$running_dir.step3.bak"
    echo "  ✅ 创建running目录备份: running.step3.bak"
    
    # 重命名目录
    mv "$running_dir" "$model_dir"
    echo "  ✅ running/ → model/"
else
    echo "  ⚠️ running目录不存在，跳过重命名"
fi

# 2. 更新import语句
echo "🔄 更新import语句..."

updated_files=0
total_files=0

find qteam-os/src -name "*.java" -type f | while read -r file; do
    if grep -q "com\.xiaoqu\.qteamos\.core\.plugin\.running" "$file"; then
        # 创建该文件的备份
        cp "$file" "$file.step3.bak"
        
        # 替换import语句
        sed -i.tmp 's/com\.xiaoqu\.qteamos\.core\.plugin\.running/com.xiaoqu.qteamos.core.plugin.model/g' "$file"
        rm "$file.tmp"
        
        echo "  ✅ 更新import: $(basename "$file")"
        updated_files=$((updated_files + 1))
    fi
    total_files=$((total_files + 1))
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
model_dir_exists=$([ -d "$model_dir" ] && echo "yes" || echo "no")
backup_dir_exists=$([ -d "$running_dir.step3.bak" ] && echo "yes" || echo "no")
backup_files_count=$(find qteam-os -name "*.step3.bak" | wc -l)

echo ""
echo "📊 步骤3完成统计："
echo "  📁 model目录创建成功: $model_dir_exists"
echo "  🗂️ running目录备份: $backup_dir_exists"
echo "  📄 更新的Java文件数量: $updated_files"
echo "  🗃️ 备份的文件数量: $backup_files_count"

if [ "$model_dir_exists" = "yes" ]; then
    file_count=$(find "$model_dir" -name "*.java" | wc -l)
    echo "  📄 model目录中的Java文件: $file_count"
fi

echo ""
echo "✅ 步骤3完成！"
echo ""
echo "📋 下一步操作："
if [ "$compile_status" = "✅ 编译成功" ]; then
    echo "  1. 继续执行步骤4: ./cleanup-step4.sh"
    echo "  2. 如果需要回滚步骤3，执行: ./rollback-step3.sh"
else
    echo "  ⚠️ 编译失败，建议先检查依赖关系"
    echo "  1. 检查编译错误: cd qteam-os && mvn compile"
    echo "  2. 如果需要回滚，执行: ./rollback-step3.sh"
fi
echo "  3. 验证系统功能正常后，可删除备份:"
echo "     find qteam-os -name '*.step3.bak' -delete"
echo ""
echo "🎯 预期进展: 完成85%的清理工作" 