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

package com.xiaoqu.qteamos.core.gateway.mapping;

import com.xiaoqu.qteamos.core.plugin.web.PluginRequestMappingHandlerMapping.PluginApiRegistrationEvent;
import com.xiaoqu.qteamos.core.plugin.web.PluginRequestMappingHandlerMapping.PluginApiUnregistrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

/**
 * 网关API监听器
 * 监听插件API注册和注销事件，维护网关API统计信息
 *
 * @author yangqijun
 * @date 2025-05-03
 * @since 1.0.0
 */
@Component("enhancedGatewayPathMappingListener")
public class GatewayPathMappingListener {
    private static final Logger log = LoggerFactory.getLogger(GatewayPathMappingListener.class);
    
    /**
     * API路径前缀
     */
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    // 存储已注册的API信息：插件ID -> API路径集合
    private final Map<String, Set<String>> registeredApis = new ConcurrentHashMap<>();
    
    // 存储活跃插件ID集合
    private final Set<String> activePlugins = new HashSet<>();
    
    /**
     * 获取映射路径
     * 兼容Spring 6.x版本中RequestMappingInfo API的变化
     */
    private String getMappingPath(RequestMappingInfo mappingInfo) {
        // 1. 尝试通过patternsCondition获取 (Spring 5.x方式)
        try {
            PatternsRequestCondition patternsCondition = mappingInfo.getPatternsCondition();
            if (patternsCondition != null && !patternsCondition.getPatterns().isEmpty()) {
                return patternsCondition.getPatterns().iterator().next();
            }
        } catch (Exception e) {
            log.debug("使用getPatternsCondition获取路径失败: {}", e.getMessage());
        }
        
        // 2. 尝试通过反射获取路径集合 (Spring 6.x方式)
        try {
            Method getPathPatternsMethod = mappingInfo.getClass().getMethod("getPathPatternsCondition");
            Object pathPatternsCondition = getPathPatternsMethod.invoke(mappingInfo);
            
            if (pathPatternsCondition != null) {
                Method getPatternsMethod = pathPatternsCondition.getClass().getMethod("getPatterns");
                Set<?> patterns = (Set<?>) getPatternsMethod.invoke(pathPatternsCondition);
                
                if (patterns != null && !patterns.isEmpty()) {
                    Object pattern = patterns.iterator().next();
                    return pattern.toString();
                }
            }
        } catch (Exception e) {
            log.debug("使用反射获取路径失败: {}", e.getMessage());
        }
        
        // 3. 尝试直接从toString中解析
        String infoString = mappingInfo.toString();
        if (infoString.contains("[") && infoString.contains("]")) {
            int start = infoString.indexOf("[") + 1;
            int end = infoString.indexOf("]");
            if (start < end) {
                return infoString.substring(start, end);
            }
        }
        
        // 4. 如果都失败了，返回默认值
        log.warn("无法获取映射路径，使用占位符: {}", mappingInfo);
        return "/unknown-path";
    }
    
    /**
     * 监听API注册事件
     */
    @EventListener
    public void handleApiRegistration(PluginApiRegistrationEvent event) {
        String pluginId = event.getPluginId();
        RequestMappingInfo mappingInfo = event.getMappingInfo();
        
        try {
            // 获取API路径
            String path = getMappingPath(mappingInfo);
            
            // 获取规范化的API前缀用于检查
            String normalizedPrefix = apiPrefix;
            if (apiPrefix.endsWith("/") && apiPrefix.length() > 1) {
                normalizedPrefix = apiPrefix.substring(0, apiPrefix.length() - 1);
            }
            
            // 记录API信息
            if (path.startsWith(normalizedPrefix)) {
                // 记录活跃插件
                activePlugins.add(pluginId);
                
                // 记录API路径
                registeredApis.computeIfAbsent(pluginId, k -> new HashSet<>()).add(path);
                
                log.info("注册网关API: {} - 插件: {}", path, pluginId);
            }
        } catch (Exception e) {
            log.error("处理API注册事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 监听API注销事件
     */
    @EventListener
    public void handleApiUnregistration(PluginApiUnregistrationEvent event) {
        RequestMappingInfo mappingInfo = event.getMappingInfo();
        String pluginId = event.getPluginId();
        
        try {
            // 获取API路径
            String path = getMappingPath(mappingInfo);
            
            // 获取规范化的API前缀用于检查
            String normalizedPrefix = apiPrefix;
            if (apiPrefix.endsWith("/") && apiPrefix.length() > 1) {
                normalizedPrefix = apiPrefix.substring(0, apiPrefix.length() - 1);
            }
            
            // 移除API记录
            if (path.startsWith(normalizedPrefix)) {
                Set<String> apis = registeredApis.get(pluginId);
                if (apis != null) {
                    apis.remove(path);
                
                    // 如果插件没有API了，从活跃插件中移除
                    if (apis.isEmpty()) {
                        activePlugins.remove(pluginId);
                        registeredApis.remove(pluginId);
                    }
                }
                
                log.info("注销网关API: {} - 插件: {}", path, pluginId);
            }
            } catch (Exception e) {
                log.error("处理API注销事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取插件的API路径列表
     */
    public Set<String> getPluginApis(String pluginId) {
        return registeredApis.getOrDefault(pluginId, new HashSet<>());
        }
        
    /**
     * 获取注册的API总数
     */
    public int getRegisteredApiCount() {
        return registeredApis.values().stream().mapToInt(Set::size).sum();
    }
    
    /**
     * 获取活跃插件数量
     */
    public int getActivePluginCount() {
        return activePlugins.size();
    }

    /**
     * 网关健康检查
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // 检查注册的API数量
        health.put("apiCount", getRegisteredApiCount());
        
        // 检查活跃插件数
        health.put("activePlugins", getActivePluginCount());
        
        // 检查内存使用情况
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        health.put("memoryUsage", usedMemory);
        
        return health;
    }
} 