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
 * 网关安全过滤器
 * 为网关层提供安全检查，拦截API请求并验证权限
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.integration;

import com.xiaoqu.qteamos.core.security.service.PermissionService;
import com.xiaoqu.qteamos.core.security.integration.GatewaySecurityIntegration.ApiSecurityMetadata;
import com.xiaoqu.qteamos.core.security.util.ApiPathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.expression.ExpressionUtils;
import org.springframework.security.access.expression.SecurityExpressionHandler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * 网关安全过滤器
 * 拦截所有API请求，检查权限
 */
@Component
@Order(30) // 在Spring Security主过滤器后执行
public class GatewaySecurityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityFilter.class);
    
    @Autowired
    private GatewaySecurityIntegration securityIntegration;
    
    @Autowired
    private PermissionService permissionService;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.security.enabled:true}")
    private boolean securityEnabled;
    
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();
    private final SecurityExpressionHandler<FilterInvocation> expressionHandler = new DefaultWebSecurityExpressionHandler();
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    
    @PostConstruct
    public void init() {
        // 使用工具类规范化API前缀
        apiPrefix = ApiPathUtils.normalizeApiPrefix(apiPrefix);
        
        log.info("网关安全过滤器初始化完成，API前缀: {}, 安全功能: {}", apiPrefix, securityEnabled ? "已启用" : "已禁用");
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // 如果安全功能禁用，直接放行
        if (!securityEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 获取请求路径
        String path = urlPathHelper.getPathWithinApplication(request);
        
        // 使用工具类检查路径是否以API前缀开头
        if (ApiPathUtils.pathStartsWithPrefix(path, apiPrefix)) {
            // 获取API安全元数据
            ApiSecurityMetadata metadata = findMatchingSecurityMetadata(path);
            
            if (metadata != null && metadata.hasSecurityConstraints()) {
                // 有安全约束，进行权限检查
                if (!checkAccess(request, response, metadata)) {
                    // 权限检查失败，返回403
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "权限不足");
                    return;
                }
            }
        }
        
        // 权限检查通过或没有安全约束，继续过滤链
        filterChain.doFilter(request, response);
    }
    
    /**
     * 查找匹配的安全元数据
     * 支持精确匹配和模式匹配
     */
    private ApiSecurityMetadata findMatchingSecurityMetadata(String path) {
        // 首先尝试精确匹配
        ApiSecurityMetadata metadata = securityIntegration.getApiSecurityMetadata(path);
        if (metadata != null) {
            return metadata;
        }
        
        // 遍历所有路径模式进行匹配
        // 注意：在实际实现中，应该使用更高效的匹配算法
        for (String pattern : securityIntegration.getAllApiSecurityMetadata().keySet()) {
            if (matchesPattern(path, pattern)) {
                return securityIntegration.getApiSecurityMetadata(pattern);
            }
        }
        
        return null;
    }
    
    /**
     * 简单的路径模式匹配
     * 支持*通配符
     */
    private boolean matchesPattern(String path, String pattern) {
        // 将路径模式转换为正则表达式
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        
        return path.matches(regex);
    }
    
    /**
     * 检查用户是否有权限访问
     */
    private boolean checkAccess(HttpServletRequest request, HttpServletResponse response, ApiSecurityMetadata metadata) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 未认证用户无权访问
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // 1. 检查PreAuthorize表达式
        if (metadata.getPreAuthorizeExpression() != null) {
            try {
                FilterInvocation fi = new FilterInvocation(request, response, new FilterChain() {
                    @Override
                    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                        // 不需要实现
                    }
                });
                
                EvaluationContext ctx = expressionHandler.createEvaluationContext(authentication, fi);
                Expression expr = expressionParser.parseExpression(metadata.getPreAuthorizeExpression());
                return ExpressionUtils.evaluateAsBoolean(expr, ctx);
            } catch (Exception e) {
                log.error("评估权限表达式失败: " + metadata.getPreAuthorizeExpression(), e);
                return false;
            }
        }
        
        // 2. 检查Secured角色
        if (metadata.getSecuredRoles() != null && metadata.getSecuredRoles().length > 0) {
            for (String role : metadata.getSecuredRoles()) {
                if (permissionService.hasRole(role)) {
                    return true;
                }
            }
            // 没有任何匹配的角色
            return false;
        }
        
        // 3. 检查JSR-250角色
        if (metadata.getJsr250Roles() != null && metadata.getJsr250Roles().length > 0) {
            for (String role : metadata.getJsr250Roles()) {
                if (permissionService.hasRole(role)) {
                    return true;
                }
            }
            // 没有任何匹配的角色
            return false;
        }
        
        // 如果没有约束，默认允许访问
        return true;
    }
} 