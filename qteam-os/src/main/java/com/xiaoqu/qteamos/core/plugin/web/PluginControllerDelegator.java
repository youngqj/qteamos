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
 * 插件控制器委托
 * 专门处理插件请求，不处理核心系统请求
 * 通过通配符匹配插件URL请求并将它们委托给正确的插件处理器
 *
 * @author yangqijun
 * @date 2025-05-17
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import org.springframework.web.method.HandlerMethod;


/**
 * 插件控制器委托类
 * 用于匹配和委托处理插件URL请求
 * 注意：此类仅处理插件请求，不处理核心系统请求
 */
@Controller
public class PluginControllerDelegator {
    private static final Logger log = LoggerFactory.getLogger(PluginControllerDelegator.class);

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private PluginRequestMappingHandlerMapping pluginRequestMappingHandlerMapping;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.gateway.html-path-prefix:/html}")
    private String htmlPrefix;
    
    // 用于解析请求URL中的插件ID的模式
    private Pattern apiPluginUrlPattern;
    private Pattern htmlPluginUrlPattern;
    
    // 核心系统URL模式
    private Pattern coreSystemUrlPattern;
    
    // 缓存已解析的处理器，提高性能
    private final Map<String, Object> handlersCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化方法，在所有属性注入完成后执行
     */
    @PostConstruct
    public void init() {
        // 创建用于解析API插件ID的正则表达式
        String apiPatternStr = ".*/" + (apiPrefix.startsWith("/") ? apiPrefix.substring(1) : apiPrefix) + "/p-([^/]+)/.*";
        apiPluginUrlPattern = Pattern.compile(apiPatternStr);
        
        // 创建用于解析HTML插件ID的正则表达式
        String htmlPatternStr = ".*/" + (htmlPrefix.startsWith("/") ? htmlPrefix.substring(1) : htmlPrefix) + "/p-([^/]+)/.*";
        htmlPluginUrlPattern = Pattern.compile(htmlPatternStr);
        
        // 创建匹配核心系统请求的正则表达式
        String corePatternStr = "^(/api/(?!p-)|/admin/|/system/|/core/|/api/plugins/).*$";
        coreSystemUrlPattern = Pattern.compile(corePatternStr);
        
        log.info("初始化插件控制器委托 - API URL模式: {}, HTML URL模式: {}, 核心系统URL模式: {}", 
                apiPatternStr, htmlPatternStr, corePatternStr);
    }
    
    /**
     * 匹配所有插件的公共API接口
     * 注意：这个方法不会实际处理请求，只是为了让Spring MVC能够识别这个路径模式
     * 实际处理委托给插件的控制器进行
     */
    @RequestMapping("${qteamos.gateway.api-prefix}/p-*/pub/**")
    public void handlePluginPublicRequests(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("接收到插件API请求: {}", request.getRequestURI());
        // 不直接处理请求，让请求继续传递给实际的处理器
        boolean handled = handlePluginRequest(request, response);
        if (handled) {
            // 请求已被处理或出错，不需要进一步处理
            return;
        }
        
        // 未找到处理器，记录警告日志
        log.warn("未找到插件公共API请求的处理器: {}", request.getRequestURI());
    }
    
    /**
     * 匹配所有插件的管理API接口
     * 注意：这个方法不会实际处理请求，只是为了让Spring MVC能够识别这个路径模式
     * 实际处理委托给插件的控制器进行
     */
    @RequestMapping("${qteamos.gateway.api-prefix}/p-*/admin/**")
    public void handlePluginAdminRequests(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("接收到插件管理API请求: {}", request.getRequestURI());
        // 不直接处理请求，让请求继续传递给实际的处理器
        boolean handled = handlePluginRequest(request, response);
        if (handled) {
            // 请求已被处理或出错，不需要进一步处理
            return;
        }
        
        // 未找到处理器，记录警告日志
        log.warn("未找到插件管理API请求的处理器: {}", request.getRequestURI());
    }
    
    /**
     * 匹配所有插件的保护API接口
     * 注意：这个方法不会实际处理请求，只是为了让Spring MVC能够识别这个路径模式
     * 实际处理委托给插件的控制器进行
     */
    @RequestMapping("${qteamos.gateway.api-prefix}/p-*/protected/**")
    public void handlePluginProtectedRequests(HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("接收到插件保护API请求: {}", request.getRequestURI());
        // 不直接处理请求，让请求继续传递给实际的处理器
        boolean handled = handlePluginRequest(request, response);
        if (handled) {
            // 请求已被处理或出错，不需要进一步处理
            return;
        }
        
        // 未找到处理器，记录警告日志
        log.warn("未找到插件保护API请求的处理器: {}", request.getRequestURI());
    }
    
    /**
     * 处理所有插件的API请求
     * 这个方法确定插件请求应该由哪个插件处理器处理
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     * @return 如果请求已被处理或出现错误返回true，否则返回false让请求继续传递
     * @throws Exception 处理过程中的异常
     */
    public boolean handlePluginRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String requestURI = request.getRequestURI();
        
        // 判断是否是核心系统请求
        if (coreSystemUrlPattern.matcher(requestURI).matches()) {
            log.debug("检测到核心系统请求，不通过插件处理: {}", requestURI);
            return false;
        }
        
        // 提取插件ID
        String pluginId = extractPluginId(requestURI);
        if (pluginId == null) {
            log.debug("无法从URL提取插件ID: {}", requestURI);
            return false;
        }
        
        log.info("提取到插件ID: {}, URL: {}", pluginId, requestURI);
        
        // 查找插件
        Optional<PluginInfo> pluginInfoOpt = pluginRegistry.getPlugin(pluginId);
        if (!pluginInfoOpt.isPresent()) {
            log.warn("找不到插件: {}", pluginId);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "找不到插件: " + pluginId);
            return true;
        }
        
        PluginInfo pluginInfo = pluginInfoOpt.get();
        log.info("找到插件信息: {}, 状态: {}", pluginId, pluginInfo.getState());
        
        // 获取处理器
        HandlerExecutionChain chain;
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 使用当前插件的类加载器
            ClassLoader pluginClassLoader = pluginInfo.getClassLoader();
            if (pluginClassLoader == null) {
                log.error("插件[{}]的类加载器为空", pluginId);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "插件类加载器为空");
                return true;
            }
            
            log.info("切换到插件类加载器: {}, 插件: {}", pluginClassLoader, pluginId);
            Thread.currentThread().setContextClassLoader(pluginClassLoader);
            
            // 打印请求信息，帮助调试
            log.info("处理插件请求: {}, 方法: {}, ContentType: {}, Accept: {}", 
                  requestURI, request.getMethod(), 
                  request.getContentType(), 
                  request.getHeader("Accept"));
            
            // 获取处理此请求的处理器
            chain = handlerMapping.getHandler(request);
            
            if (chain == null) {
                log.warn("没有找到[{}]请求的处理器: {}", pluginId, requestURI);
                return false;
            }
            
            log.info("找到插件请求处理器: {}", chain.getHandler());
            
            // 执行处理器链
            if (chain.getHandler() instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) chain.getHandler();
                log.info("执行插件处理器方法: {}.{}", 
                       handlerMethod.getBeanType().getSimpleName(), 
                       handlerMethod.getMethod().getName());
                
                // 验证返回值类型，确保不是void
                if (handlerMethod.getMethod().getReturnType() == void.class) {
                    log.warn("插件控制器方法返回值为void，可能导致空响应: {}.{}", 
                           handlerMethod.getBeanType().getSimpleName(), 
                           handlerMethod.getMethod().getName());
                }
            }
            
            // 不直接处理请求，返回false让Spring继续处理
            // 这样ResponseEntity<Map<String, Object>>才能被正确转换为JSON响应
            return false;
        } catch (Exception e) {
            log.error("处理插件请求时出错: {}", e.getMessage(), e);
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"插件请求处理异常: " + 
                    e.getMessage().replace("\"", "\\\"") + "\"}");
            }
            return true;
        } finally {
            // 确保总是恢复原始类加载器，即使发生异常
            Thread.currentThread().setContextClassLoader(originalClassLoader);
            log.debug("已恢复原始类加载器");
        }
    }
    
    /**
     * 从请求URI中提取插件ID
     * 
     * @param uri 请求URI
     * @return 插件ID，如果无法提取则返回null
     */
    private String extractPluginId(String uri) {
        log.debug("尝试从URI提取插件ID: {}", uri);
        
        // 判断是否是核心系统请求
        if (coreSystemUrlPattern.matcher(uri).matches()) {
            log.debug("核心系统请求不提取插件ID: {}", uri);
            return null;
        }
        
        Pattern pattern = null;
        
        // 根据URI判断使用哪个模式
        if (uri.contains(apiPrefix)) {
            pattern = apiPluginUrlPattern;
        } else if (uri.contains(htmlPrefix)) {
            pattern = htmlPluginUrlPattern;
        }
        
        if (pattern == null) {
            log.warn("无法确定URI前缀: {}", uri);
            return null;
        }
        
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            String encryptedPluginId = matcher.group(1);
            log.debug("成功提取加密的插件ID: {}", encryptedPluginId);
            
            // 解密插件ID
            String decryptedPluginId = pluginRequestMappingHandlerMapping.decryptPluginId(encryptedPluginId);
            log.debug("解密插件ID: {} -> {}", encryptedPluginId, decryptedPluginId);
            
            return decryptedPluginId;
        }
        
        log.warn("无法从URI提取插件ID: {}", uri);
        return null;
    }
} 