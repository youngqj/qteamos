/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.core.gateway.filter;

import com.xiaoqu.qteamos.core.gateway.service.impl.GatewayServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API请求日志过滤器
 * 记录API请求和响应信息，计算响应时间
 *
 * @author yangqijun
 * @date 2025-05-05
 * @since 1.0.0
 */
@Component
@Order(1)  // 在所有过滤器之前执行
public class ApiRequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);
    
    @Autowired
    private GatewayServiceImpl gatewayService;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.gateway.enable-request-logging:true}")
    private boolean enableRequestLogging;
    
    // 提取插件ID的正则表达式
    private static final Pattern PLUGIN_ID_PATTERN = Pattern.compile("/api/p-([^/]+)/?.*");
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        if (!enableRequestLogging || !request.getRequestURI().startsWith(apiPrefix)) {
            // 不记录日志或非API请求，直接放行
            filterChain.doFilter(request, response);
            return;
        }
        
        // 包装请求和响应以便读取内容
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        String path = request.getRequestURI();
        String pluginId = extractPluginId(path);
        
        try {
            // 执行过滤链
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            // 计算响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 记录请求和响应信息
            if (pluginId != null) {
                // 记录响应时间
                gatewayService.recordApiResponseTime(pluginId, path, responseTime);
                
                // 记录日志
                if (log.isDebugEnabled()) {
                    logRequest(requestWrapper, pluginId, path);
                    logResponse(responseWrapper, responseTime);
                } else {
                    log.info("API请求: {} {} - 响应状态: {} - 响应时间: {}ms - 插件ID: {}", 
                            request.getMethod(), path, responseWrapper.getStatus(), responseTime, pluginId);
                }
            }
            
            // 重要：复制响应内容到原始响应
            responseWrapper.copyBodyToResponse();
        }
    }
    
    /**
     * 记录请求详情
     */
    private void logRequest(ContentCachingRequestWrapper request, String pluginId, String path) {
        // 获取请求体内容
        String requestBody = "";
        try {
            if (request.getContentLength() > 0) {
                byte[] content = request.getContentAsByteArray();
                requestBody = new String(content, request.getCharacterEncoding());
                
                // 截断过长的请求体
                if (requestBody.length() > 1000) {
                    requestBody = requestBody.substring(0, 997) + "...";
                }
            }
        } catch (Exception e) {
            requestBody = "[无法读取请求体]";
        }
        
        log.debug("API请求: {} {} - 插件ID: {} - 请求体: {}", 
                request.getMethod(), path, pluginId, requestBody);
    }
    
    /**
     * 记录响应详情
     */
    private void logResponse(ContentCachingResponseWrapper response, long responseTime) {
        // 获取响应体内容
        String responseBody = "";
        try {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                responseBody = new String(content, response.getCharacterEncoding());
                
                // 截断过长的响应体
                if (responseBody.length() > 1000) {
                    responseBody = responseBody.substring(0, 997) + "...";
                }
            }
        } catch (Exception e) {
            responseBody = "[无法读取响应体]";
        }
        
        log.debug("API响应: 状态码 {} - 响应时间: {}ms - 响应体: {}", 
                response.getStatus(), responseTime, responseBody);
    }
    
    /**
     * 从路径中提取插件ID
     */
    private String extractPluginId(String path) {
        Matcher matcher = PLUGIN_ID_PATTERN.matcher(path);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
} 