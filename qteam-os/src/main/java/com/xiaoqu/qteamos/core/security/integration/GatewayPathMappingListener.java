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
 * 网关路径映射监听器
 * 监听API路径映射事件，进行路径转换和规范化处理
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.integration;

import com.xiaoqu.qteamos.core.plugin.web.PluginRequestMappingHandlerMapping;
import com.xiaoqu.qteamos.core.security.util.ApiPathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关路径映射监听器
 * 负责处理API路径映射，确保路径格式一致性
 */
@Component
public class GatewayPathMappingListener {
    private static final Logger log = LoggerFactory.getLogger(GatewayPathMappingListener.class);
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    // 存储已注册的API路径
    private final Set<String> registeredPaths = ConcurrentHashMap.newKeySet();
    
    /**
     * 初始化组件
     */
    @PostConstruct
    public void init() {
        // 使用工具类规范化API前缀
        apiPrefix = ApiPathUtils.normalizeApiPrefix(apiPrefix);
        
        log.info("网关路径映射监听器初始化完成，API前缀: {}", apiPrefix);
    }
    
    /**
     * 处理API注册事件
     */
    @EventListener
    public void handleApiRegistration(PluginRequestMappingHandlerMapping.PluginApiRegistrationEvent event) {
        String pluginId = event.getPluginId();
        RequestMappingInfo mappingInfo = event.getMappingInfo();
        
        try {
            for (String pattern : mappingInfo.getPatternValues()) {
                // 记录API路径
                registeredPaths.add(pattern);
                log.debug("API路径注册: {} (插件: {})", pattern, pluginId);
            }
        } catch (Exception e) {
            log.error("处理API注册事件失败", e);
        }
    }
    
    /**
     * 处理API注销事件
     */
    @EventListener
    public void handleApiUnregistration(PluginRequestMappingHandlerMapping.PluginApiUnregistrationEvent event) {
        RequestMappingInfo mappingInfo = event.getMappingInfo();
        
        try {
            for (String pattern : mappingInfo.getPatternValues()) {
                // 移除API路径记录
                registeredPaths.remove(pattern);
                log.debug("API路径注销: {}", pattern);
            }
        } catch (Exception e) {
            log.error("处理API注销事件失败", e);
        }
    }
    
    /**
     * 检查路径是否已注册
     */
    public boolean isPathRegistered(String path) {
        return registeredPaths.contains(path);
    }
    
    /**
     * 获取所有已注册的路径
     */
    public Set<String> getAllRegisteredPaths() {
        return new HashSet<>(registeredPaths);
    }
} 