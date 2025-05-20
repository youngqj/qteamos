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
 * 插件请求拦截器
 * 用于记录插件API请求日志并处理跨插件调用
 *
 * @author yangqijun
 * @date 2025-05-17
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 插件请求拦截器
 * 记录插件API请求日志，并处理需要特殊处理的跨插件调用
 */
public class PluginRequestInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(PluginRequestInterceptor.class);
    
    // 插件ID提取正则表达式
    private final Pattern pluginIdPattern = Pattern.compile(".*/api/p-([^/]+)/.*");
    
    /**
     * 在控制器执行前调用
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 提取插件ID
        Matcher matcher = pluginIdPattern.matcher(requestURI);
        String pluginId = matcher.matches() ? matcher.group(1) : "unknown";
        
        // 记录请求信息
        log.info("插件请求: [{}] {} {}", pluginId, request.getMethod(), requestURI);
        
        // 可以在这里添加特权检查、限流等逻辑
        
        // 设置线程上下文变量，便于后续使用
        PluginContextHolder.setCurrentPluginId(pluginId);
        
        return true; // 继续执行请求
    }
    
    /**
     * 在控制器执行后调用
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // 可以在这里添加后处理逻辑
    }
    
    /**
     * 在请求完成后调用
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清理线程上下文变量
        PluginContextHolder.clear();
        
        if (ex != null) {
            log.error("插件请求处理异常: {} {}", request.getMethod(), request.getRequestURI(), ex);
        }
    }
    
    /**
     * 插件上下文持有者
     * 用于在线程内共享当前插件ID
     */
    public static class PluginContextHolder {
        private static final ThreadLocal<String> CURRENT_PLUGIN_ID = new ThreadLocal<>();
        
        public static void setCurrentPluginId(String pluginId) {
            CURRENT_PLUGIN_ID.set(pluginId);
        }
        
        public static String getCurrentPluginId() {
            return CURRENT_PLUGIN_ID.get();
        }
        
        public static void clear() {
            CURRENT_PLUGIN_ID.remove();
        }
    }
} 