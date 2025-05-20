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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import com.xiaoqu.qteamos.common.result.Result;
import com.xiaoqu.qteamos.common.result.ResultCode;
import com.xiaoqu.qteamos.core.gateway.service.impl.GatewayServiceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API限流过滤器
 * 使用令牌桶算法对插件API进行限流
 *
 * @author yangqijun
 * @date 2025-05-05
 * @since 1.0.0
 */
@Component
@Order(2)  // 在认证过滤器之后执行
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);
    
    @Autowired
    private GatewayServiceImpl gatewayService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.gateway.enable-rate-limit:true}")
    private boolean enableRateLimit;
    
    @Value("${qteamos.gateway.default-rate-limit:100}")
    private int defaultRateLimit;
    
    // 存储插件的限流器，pluginId -> RateLimiter
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    // 提取插件ID的正则表达式
    private static final Pattern PLUGIN_ID_PATTERN = Pattern.compile("/api/p-([^/]+)/?.*");
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // 检查是否是API路径
        if (!enableRateLimit || !path.startsWith(apiPrefix)) {
            // 不启用限流或非API路径，直接放行
            filterChain.doFilter(request, response);
            return;
        }
        
        // 提取插件ID
        String pluginId = extractPluginId(path);
        if (pluginId == null) {
            // 无法提取插件ID，直接放行
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取或创建令牌桶
        RateLimiter limiter = getRateLimiter(pluginId);
        
        // 尝试获取令牌
        if (limiter.tryAcquire()) {
            // 获取到令牌，放行请求
            try {
                // 记录API调用
                gatewayService.recordApiCall(pluginId, path);
                
                // 执行过滤链
                filterChain.doFilter(request, response);
            } finally {
                // 可以在这里记录响应时间
                // gatewayService.recordApiResponseTime(pluginId, path, System.currentTimeMillis() - startTime);
            }
        } else {
            // 未获取到令牌，返回限流响应
            handleRateLimitExceeded(response, pluginId);
        }
    }
    
    /**
     * 处理超过限流的请求
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String pluginId) throws IOException {
        log.warn("插件[{}]请求超过限流阈值", pluginId);
        
        // 设置响应状态码和内容类型
        response.setStatus(429); // 429 - Too Many Requests
        response.setContentType("application/json;charset=UTF-8");
        
        // 创建Result响应对象
        Result<String> result = Result.failed(
            ResultCode.FAILED, 
            "请求频率超过限制，请稍后再试", 
            "插件ID: " + pluginId
        );
        
        // 将结果写入响应
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
    
    /**
     * 获取插件的限流器
     */
    private RateLimiter getRateLimiter(String pluginId) {
        // 从缓存中获取限流器，如果不存在则创建
        return rateLimiters.computeIfAbsent(pluginId, k -> {
            // 获取插件的自定义限流配置，如果没有则使用默认值
            int limit = gatewayService.getPluginRateLimit(pluginId).orElse(defaultRateLimit);
            
            // 创建令牌桶限流器，转换为每秒速率
            double permitsPerSecond = limit / 60.0;
            
            log.info("为插件[{}]创建限流器，限流规则：{}次/分钟，{}次/秒", 
                    pluginId, limit, String.format("%.2f", permitsPerSecond));
                    
            return RateLimiter.create(permitsPerSecond);
        });
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