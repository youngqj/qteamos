#!/bin/bash

# QTeam Java代码清理脚本
# 专门用于清理重构过程中产生的冗余Java类
# 作者: QTeam 清理助手
# 日期: 2025-05-23

set -e

echo "🧹 QTeam Java代码清理工具"
echo "专门清理重构过程中产生的冗余Java类文件"
echo ""

# 1. 分析已弃用的类
echo "📊 第一步：分析已弃用的Java类"
deprecated_files=$(find . -name "*.java" -path "*/src/main/*" | xargs grep -l "@Deprecated" 2>/dev/null || true)

if [ -n "$deprecated_files" ]; then
    echo "发现以下已标记为@Deprecated的类："
    echo "$deprecated_files" | while read file; do
        # 获取类名
        class_name=$(basename "$file" .java)
        # 检查是否有其他类引用这个废弃类
        references=$(find . -name "*.java" -path "*/src/main/*" -not -path "$file" | xargs grep -l "$class_name" 2>/dev/null | wc -l)
        size=$(du -h "$file" | cut -f1)
        echo "  - $file ($size, $references 个引用)"
    done
    echo ""
else
    echo "没有发现标记为@Deprecated的类"
    echo ""
fi

# 2. 分析可能的重复类
echo "📊 第二步：分析可能的重复/冗余类"
echo "扫描可能冗余的文件模式..."

# 查找可能的重复文件
echo ""
echo "🔍 可疑的重复文件："
find . -name "*.java" -path "*/src/main/*" | grep -E "(Impl|Default|New|Old|Temp|Backup|Copy|Manager|Service)" | while read file; do
    base_name=$(basename "$file" .java)
    dir_name=$(dirname "$file")
    
    # 查找可能的重复
    similar_files=$(find "$dir_name" -name "*${base_name}*" -o -name "${base_name%Impl}*" -o -name "${base_name%Default}*" 2>/dev/null | grep -v "^$file$" || true)
    
    if [ -n "$similar_files" ]; then
        echo "  🚨 $file"
        echo "$similar_files" | while read similar; do
            if [ -f "$similar" ]; then
                echo "     ↳ 相似: $similar"
            fi
        done
        echo ""
    fi
done

# 3. 分析测试控制器和临时类
echo "📊 第三步：分析测试控制器和临时类"
test_controllers=$(find . -name "*Test*.java" -path "*/src/main/*" | grep -E "(Controller|Service|Manager)" || true)
if [ -n "$test_controllers" ]; then
    echo "发现可能的测试控制器（应该在test目录）："
    echo "$test_controllers" | while read file; do
        size=$(du -h "$file" | cut -f1)
        echo "  - $file ($size)"
    done
    echo ""
fi

# 4. 分析文件大小，找出巨大的类
echo "📊 第四步：分析大型类文件（可能需要拆分）"
echo "超过1000行的Java类："
find . -name "*.java" -path "*/src/main/*" | while read file; do
    lines=$(wc -l < "$file" 2>/dev/null || echo "0")
    if [ "$lines" -gt 1000 ]; then
        size=$(du -h "$file" | cut -f1)
        echo "  🔥 $file ($lines 行, $size)"
    fi
done

echo ""
echo "超过500行的Java类："
find . -name "*.java" -path "*/src/main/*" | while read file; do
    lines=$(wc -l < "$file" 2>/dev/null || echo "0")
    if [ "$lines" -gt 500 ] && [ "$lines" -le 1000 ]; then
        size=$(du -h "$file" | cut -f1)
        echo "  ⚠️  $file ($lines 行, $size)"
    fi
done

# 5. 生成清理建议
echo ""
echo "📋 清理建议报告"
echo "================="
echo ""

echo "🗑️  **可以考虑删除的文件**："
echo ""

# 已弃用且无引用的类
if [ -n "$deprecated_files" ]; then
    echo "$deprecated_files" | while read file; do
        class_name=$(basename "$file" .java)
        references=$(find . -name "*.java" -path "*/src/main/*" -not -path "$file" | xargs grep -l "$class_name" 2>/dev/null | wc -l)
        if [ "$references" -eq 0 ]; then
            echo "  ✅ $file (已弃用且无引用，可安全删除)"
        fi
    done
fi

# 测试控制器
if [ -n "$test_controllers" ]; then
    echo ""
    echo "  📝 需要移动到test目录的文件："
    echo "$test_controllers" | while read file; do
        echo "  📦 $file (应移动到对应的test目录)"
    done
fi

echo ""
echo "⚠️  **需要手动检查的文件**："
echo ""

# 已弃用但有引用的类
if [ -n "$deprecated_files" ]; then
    echo "$deprecated_files" | while read file; do
        class_name=$(basename "$file" .java)
        references=$(find . -name "*.java" -path "*/src/main/*" -not -path "$file" | xargs grep -l "$class_name" 2>/dev/null | wc -l)
        if [ "$references" -gt 0 ]; then
            echo "  🔍 $file (已弃用但仍有 $references 个引用，需要先迁移引用)"
        fi
    done
fi

echo ""
echo "🎯 **执行清理操作**"
echo ""
echo "1. 运行交互式清理："
echo "   ./java-cleanup.sh --interactive"
echo ""
echo "2. 仅删除安全的文件："
echo "   ./java-cleanup.sh --safe-delete"
echo ""
echo "3. 生成详细报告："
echo "   ./java-cleanup.sh --report > java-cleanup-report.txt"

# 6. 交互式模式
if [ "$1" = "--interactive" ]; then
    echo ""
    echo "🔄 进入交互式清理模式..."
    echo ""
    
    if [ -n "$deprecated_files" ]; then
        echo "$deprecated_files" | while read file; do
            class_name=$(basename "$file" .java)
            references=$(find . -name "*.java" -path "*/src/main/*" -not -path "$file" | xargs grep -l "$class_name" 2>/dev/null | wc -l)
            
            if [ "$references" -eq 0 ]; then
                echo ""
                echo "文件: $file"
                echo "状态: 已弃用，无引用"
                echo -n "是否删除? (y/N): "
                read -r answer
                if [ "$answer" = "y" ] || [ "$answer" = "Y" ]; then
                    echo "删除: $file"
                    rm "$file"
                    echo "✅ 已删除"
                else
                    echo "⏭️  跳过"
                fi
            fi
        done
    fi
fi

# 7. 安全删除模式
if [ "$1" = "--safe-delete" ]; then
    echo ""
    echo "🛡️  执行安全删除模式..."
    echo ""
    
    deleted_count=0
    if [ -n "$deprecated_files" ]; then
        echo "$deprecated_files" | while read file; do
            class_name=$(basename "$file" .java)
            references=$(find . -name "*.java" -path "*/src/main/*" -not -path "$file" | xargs grep -l "$class_name" 2>/dev/null | wc -l)
            
            if [ "$references" -eq 0 ]; then
                echo "删除: $file (已弃用且无引用)"
                rm "$file"
                deleted_count=$((deleted_count + 1))
            fi
        done
    fi
    
    echo ""
    echo "✅ 安全删除完成，共删除 $deleted_count 个文件"
fi

echo ""
echo "💡 提示：运行 'mvn clean compile' 验证清理后代码能否正常编译" 