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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * API授权过滤器
 * 根据路径前缀判断是否需要认证
 *
 * @author yangqijun
 * @date 2025-05-03
 * @since 1.0.0
 */
@Component
@Order(1)  // 高优先级，在Spring Security之前执行
public class ApiAuthorizationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(ApiAuthorizationFilter.class);
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.gateway.public-path-prefix:/pub}")
    private String publicPathPrefix;
    
    @Value("${qteamos.gateway.admin-path-prefix:/admin}")
    private String adminPathPrefix;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // 检查是否是API路径
        if (path.startsWith(apiPrefix)) {
            log.debug("处理API请求: {}", path);
            
            // 插件相关的路径判断
            if (path.contains(apiPrefix + "/p-")) {
                // 检查是否是插件公共API - /api/p-*/pub/
                if (path.contains("/pub/")) {
                    log.debug("插件公共API访问: {}", path);
                    // 公共API，不需要认证，直接放行
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 其他插件API - 需要认证
                log.debug("插件私有API访问: {}", path);
            } 
            // 原有的公共路径检查
            else if (path.contains(publicPathPrefix)) {
                log.debug("公共API访问: {}", path);
                // 公共API，不需要认证，直接放行
                filterChain.doFilter(request, response);
                return;
            } 
            // 原有的管理路径检查
            else if (path.contains(adminPathPrefix)) {
                log.debug("管理API访问: {}", path);
                // 管理API，需要管理员权限
                // 这里交给Spring Security处理，只添加日志
                //TODO: 管理员权限认证
            }
            // 壳子普通API路径
            else {
                log.debug("壳子API访问: {}", path);
            }
            
            // 其他API需要基本认证
            log.debug("需要认证的API访问: {}", path);
            // 交给Spring Security进行认证处理
        }
        
        // 继续过滤链
        filterChain.doFilter(request, response);
    }
} 