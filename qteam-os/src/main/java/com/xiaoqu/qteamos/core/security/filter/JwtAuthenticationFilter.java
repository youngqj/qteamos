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
 * JWT认证过滤器
 * 拦截请求并验证JWT令牌
 *
 * @author yangqijun
 * @date 2025-07-24
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.filter;

import com.xiaoqu.qteamos.core.security.properties.SecurityProperties;
import com.xiaoqu.qteamos.core.security.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 * 从请求头提取JWT令牌并验证，验证成功后设置认证信息
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private SecurityProperties securityProperties;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    
    // 插件路径前缀
    private static final String PLUGIN_PATH_PREFIX = "/p-";
    
    // 白名单路径模式
    private final List<String> whitelistPatterns = Arrays.asList(
        "/actuator/**",
        "/error",
        "/favicon.ico",
        "/login",
        "/logout"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String requestUri = request.getRequestURI();
        log.debug("处理请求: {}", requestUri);
        
        // 检查是否是插件请求
        if (isPluginRequest(requestUri)) {
            log.debug("插件请求不验证JWT令牌: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 检查是否在白名单中
        if (isWhitelisted(requestUri)) {
            log.debug("白名单路径不验证JWT令牌: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 临时禁用JWT身份验证，方便测试
        log.debug("JWT身份验证已暂时禁用，直接放行请求: {}", requestUri);
        filterChain.doFilter(request, response);
        
        // 以下代码被暂时注释，方便测试
        /*
        String token = jwtUtils.extractToken(request);
        
        if (token == null) {
            // 没有提供令牌，由授权配置决定是否拦截
            log.debug("请求未提供JWT令牌: {}", requestUri);
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String username = jwtUtils.getUsernameFromToken(token);
            
            if (username != null && !username.isEmpty() && 
                SecurityContextHolder.getContext().getAuthentication() == null) {
                
                log.debug("处理JWT令牌: {}, 用户: {}", token.substring(0, Math.min(10, token.length())), username);
                
                // 验证令牌有效性
                if (jwtUtils.validateToken(token)) {
                    
                    // 获取用户详情
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    
                    // 创建身份验证对象
                    UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("用户[{}]身份验证成功", username);
                } else {
                    // 令牌无效但不想返回错误，可以选择忽略
                    if (!jwtProperties.isIgnoreExpiredToken()) {
                        log.warn("无效的JWT令牌: {}", token.substring(0, Math.min(10, token.length())));
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Invalid or expired JWT token");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT令牌处理失败", e);
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
        */
    }
    
    /**
     * 检查是否为插件请求
     */
    private boolean isPluginRequest(String uri) {
        return uri.contains(apiPrefix + PLUGIN_PATH_PREFIX);
    }
    
    /**
     * 检查是否在白名单中
     */
    private boolean isWhitelisted(String uri) {
        return whitelistPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }
    
    /**
     * 从请求头提取JWT令牌
     */
    private String resolveToken(HttpServletRequest request) {
        String tokenHeader = securityProperties.getAuthentication().getJwt().getTokenHeader();
        String tokenPrefix = securityProperties.getAuthentication().getJwt().getTokenPrefix();
        
        String bearerToken = request.getHeader(tokenHeader);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenPrefix)) {
            return bearerToken.substring(tokenPrefix.length()).trim();
        }
        return null;
    }
    
    /**
     * 验证JWT令牌
     */
    private boolean validateToken(String token) {
        // 如果配置了忽略令牌过期，则只要格式正确就验证通过
        if (securityProperties.getAuthentication().getJwt().isIgnoreExpiredToken()) {
            try {
                jwtUtils.parseToken(token);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        
        // 否则，全面验证令牌，包括过期时间
        return jwtUtils.validateToken(token);
    }
} 