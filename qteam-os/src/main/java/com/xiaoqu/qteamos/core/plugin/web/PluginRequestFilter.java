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
 * 插件请求过滤器
 * 专门处理插件请求，不处理核心系统请求
 * 拦截以/api/p-和/html/p-开头的请求，交由插件控制器处理
 *
 * @author yangqijun
 * @date 2025-05-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.web;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理插件API请求的过滤器
 * 匹配所有 /api/p-* 和 /html/p-* 格式的URL，交由PluginControllerDelegator处理
 * 注意：此过滤器不处理核心系统请求
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class PluginRequestFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(PluginRequestFilter.class);
    
    @Autowired
    private PluginControllerDelegator pluginControllerDelegator;
    
    @Autowired
    private PluginRequestMappingHandlerMapping pluginRequestMappingHandlerMapping;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.gateway.html-path-prefix:/html}")
    private String htmlPrefix;
    
    // 插件URL模式
    private Pattern apiPluginUrlPattern;
    private Pattern htmlPluginUrlPattern;
    
    // 核心系统URL模式
    private Pattern coreSystemUrlPattern;
    
    @PostConstruct
    public void init() {
        // 创建匹配API插件请求的正则表达式
        String apiPatternStr = "^" + (apiPrefix.startsWith("/") ? apiPrefix : "/" + apiPrefix) + "/p-[^/]+/.*$";
        apiPluginUrlPattern = Pattern.compile(apiPatternStr);
        
        // 创建匹配HTML插件请求的正则表达式
        String htmlPatternStr = "^" + (htmlPrefix.startsWith("/") ? htmlPrefix : "/" + htmlPrefix) + "/p-[^/]+/.*$";
        htmlPluginUrlPattern = Pattern.compile(htmlPatternStr);
        
        // 创建匹配核心系统请求的正则表达式
        String corePatternStr = "^(/api/(?!p-)|/admin/|/system/|/core/|/api/plugins/).*$";
        coreSystemUrlPattern = Pattern.compile(corePatternStr);
        
        log.info("初始化插件请求过滤器 - API URL模式: {}, HTML URL模式: {}, 核心系统URL模式: {}", 
                apiPatternStr, htmlPatternStr, corePatternStr);
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String requestURI = httpRequest.getRequestURI();
        
        // 判断是否是核心系统请求
        boolean isCoreSystemRequest = coreSystemUrlPattern.matcher(requestURI).matches();
        if (isCoreSystemRequest) {
            log.debug("检测到核心系统请求，不通过插件处理: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }
        
        // 判断是否是插件请求
        boolean isApiPluginRequest = apiPluginUrlPattern.matcher(requestURI).matches();
        boolean isHtmlPluginRequest = htmlPluginUrlPattern.matcher(requestURI).matches();
        
        if (isApiPluginRequest || isHtmlPluginRequest) {
            log.debug("拦截到插件请求: {}", requestURI);
            
            try {
                // 委托给PluginControllerDelegator处理
                boolean handled = pluginControllerDelegator.handlePluginRequest(httpRequest, httpResponse);
                
                if (handled) {
                    // 请求已处理，不继续传递
                    log.debug("插件请求已处理，不继续传递: {}", requestURI);
                    return;
                }
            } catch (Exception e) {
                log.error("处理插件请求时出错: {}", e.getMessage(), e);
                httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                        "处理插件请求时出错: " + e.getMessage());
                return;
            }
        }
        
        // 非插件请求或未被处理的请求，继续传递
        chain.doFilter(request, response);
    }
    
    /**
     * 从请求URI中提取插件ID
     * 
     * @param uri 请求URI
     * @return 插件ID，如果无法提取则返回null
     */
    private String extractPluginId(String uri) {
        // 判断是否是核心系统请求
        if (coreSystemUrlPattern.matcher(uri).matches()) {
            log.debug("核心系统请求不提取插件ID: {}", uri);
            return null;
        }
        
        Pattern pattern = null;
        
        // 根据URI判断使用哪个模式
        if (uri.startsWith(apiPrefix) || uri.startsWith("/" + apiPrefix)) {
            pattern = apiPluginUrlPattern;
        } else if (uri.startsWith(htmlPrefix) || uri.startsWith("/" + htmlPrefix)) {
            pattern = htmlPluginUrlPattern;
        }
        
        if (pattern == null) {
            return null;
        }
        
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            // 从URI中提取p-xxx部分
            int prefixLength = uri.indexOf("/p-") + 3;
            int nextSlash = uri.indexOf("/", prefixLength);
            if (nextSlash > prefixLength) {
                String encryptedPluginId = uri.substring(prefixLength, nextSlash);
                // 使用PluginRequestMappingHandlerMapping解密插件ID
                return pluginRequestMappingHandlerMapping.decryptPluginId(encryptedPluginId);
            }
        }
        
        return null;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 过滤器初始化
    }
    
    @Override
    public void destroy() {
        // 过滤器销毁
    }
} 