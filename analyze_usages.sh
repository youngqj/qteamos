#!/bin/bash

echo "分析Java类使用情况..."

# 搜集所有Java类名
find . -name "*.java" | grep -v "/target/" | grep -v "/build/" | sort > analysis_results/all_java_files.txt

# 获取每个类的被引用情况
cat analysis_results/all_java_files.txt | while read file; do
  className=$(basename $file .java)
  count=$(grep -r --include="*.java" --include="*.xml" "$className" . | grep -v "$file" | wc -l)
  echo "$count $className $file" >> analysis_results/usage_counts.txt
done

# 排序输出潜在未使用的类
sort -n analysis_results/usage_counts.txt | head -50 > analysis_results/least_used.txt

echo "可能未使用的类列表已保存到 analysis_results/least_used.txt" 