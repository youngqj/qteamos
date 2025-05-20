/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * API路径工具类
 * 提供API路径处理和转换的实用方法
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.util;

import org.springframework.util.StringUtils;

/**
 * API路径工具类
 * 提供API路径处理和规范化的方法
 */
public class ApiPathUtils {
    
    /**
     * 私有构造函数，防止实例化
     */
    private ApiPathUtils() {
        throw new UnsupportedOperationException("工具类不应该被实例化");
    }
    
    /**
     * 标准化API前缀
     * 确保前缀以/开头，不以/结尾
     */
    public static String normalizeApiPrefix(String prefix) {
        if (prefix == null) {
            return "/api";
        }
        
        prefix = prefix.trim();
        
        // 确保以/开头
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        
        // 确保不以/结尾
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        
        return prefix;
    }
    
    /**
     * 标准化API路径
     * 确保路径以/开头
     */
    public static String normalizePath(String path) {
        if (path == null) {
            return "/";
        }
        
        path = path.trim();
        
        // 确保以/开头
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        return path;
    }
    
    /**
     * 将API前缀和路径组合成完整的API路径
     * 确保中间只有一个/分隔符
     */
    public static String combinePath(String prefix, String path) {
        prefix = normalizeApiPrefix(prefix);
        path = normalizePath(path);
        
        // 如果path只有/，直接返回prefix
        if ("/".equals(path)) {
            return prefix;
        }
        
        // 去掉path开头的/，避免双斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        return prefix + "/" + path;
    }
    
    /**
     * 检查路径是否以给定的前缀开头
     */
    public static boolean pathStartsWithPrefix(String path, String prefix) {
        if (path == null || prefix == null) {
            return false;
        }
        
        path = normalizePath(path);
        prefix = normalizeApiPrefix(prefix);
        
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
    
    /**
     * 从完整路径中移除API前缀，获取相对路径
     */
    public static String removePrefix(String fullPath, String prefix) {
        if (fullPath == null) {
            return "/";
        }
        
        if (prefix == null || prefix.trim().isEmpty()) {
            return normalizePath(fullPath);
        }
        
        fullPath = normalizePath(fullPath);
        prefix = normalizeApiPrefix(prefix);
        
        if (fullPath.equals(prefix)) {
            return "/";
        }
        
        if (fullPath.startsWith(prefix + "/")) {
            return "/" + fullPath.substring(prefix.length() + 1);
        }
        
        // 如果路径不以前缀开头，返回原路径
        return fullPath;
    }
} 