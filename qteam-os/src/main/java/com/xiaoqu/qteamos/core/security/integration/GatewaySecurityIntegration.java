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
 * 网关安全整合组件
 * 负责连接安全层和网关层，实现API权限和路由的统一管理
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.integration;

import com.xiaoqu.qteamos.core.plugin.web.PluginRequestMappingHandlerMapping;
import com.xiaoqu.qteamos.core.security.extension.PluginSecurityExtensionManager;
import com.xiaoqu.qteamos.core.security.util.ApiPathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;

/**
 * 网关安全整合组件
 * 监听API注册/注销事件，同步更新安全配置
 */
@Component
public class GatewaySecurityIntegration {
    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityIntegration.class);
    
    @Autowired
    private PluginSecurityExtensionManager securityExtensionManager;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    // 存储API路径与权限的映射关系
    private final Map<String, ApiSecurityMetadata> apiSecurityMap = new ConcurrentHashMap<>();
    
    /**
     * 初始化组件
     */
    @PostConstruct
    public void init() {
        // 使用工具类规范化API前缀
        apiPrefix = ApiPathUtils.normalizeApiPrefix(apiPrefix);
        
        log.info("网关安全整合组件初始化完成，API前缀: {}", apiPrefix);
    }
    
    /**
     * 处理API注册事件
     * 将API信息关联到安全配置
     */
    @EventListener
    public void handleApiRegistration(PluginRequestMappingHandlerMapping.PluginApiRegistrationEvent event) {
        String pluginId = event.getPluginId();
        RequestMappingInfo mappingInfo = event.getMappingInfo();
        Class<?> controllerClass = event.getControllerClass();
        Method method = event.getMethod();
        
        try {
            // 从方法和控制器中提取安全元数据
            ApiSecurityMetadata metadata = extractSecurityMetadata(pluginId, controllerClass, method);
            
            // 对每个路径模式都记录安全元数据
            for (String pattern : mappingInfo.getPatternValues()) {
                apiSecurityMap.put(pattern, metadata);
                log.debug("关联API安全元数据: {} -> {}", pattern, metadata);
            }
            
        } catch (Exception e) {
            log.error("处理API注册事件失败", e);
        }
    }
    
    /**
     * 处理API注销事件
     * 移除相关的安全配置
     */
    @EventListener
    public void handleApiUnregistration(PluginRequestMappingHandlerMapping.PluginApiUnregistrationEvent event) {
        RequestMappingInfo mappingInfo = event.getMappingInfo();
        
        // 移除所有相关路径的安全元数据
        for (String pattern : mappingInfo.getPatternValues()) {
            apiSecurityMap.remove(pattern);
            log.debug("移除API安全元数据: {}", pattern);
        }
    }
    
    /**
     * 从控制器方法中提取安全元数据
     */
    private ApiSecurityMetadata extractSecurityMetadata(String pluginId, Class<?> controllerClass, Method method) {
        ApiSecurityMetadata metadata = new ApiSecurityMetadata();
        metadata.setPluginId(pluginId);
        
        // 检查方法级别的安全注解
        org.springframework.security.access.prepost.PreAuthorize preAuthorize = 
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
        if (preAuthorize != null) {
            metadata.setPreAuthorizeExpression(preAuthorize.value());
        }
        
        org.springframework.security.access.annotation.Secured secured = 
                method.getAnnotation(org.springframework.security.access.annotation.Secured.class);
        if (secured != null) {
            metadata.setSecuredRoles(secured.value());
        }
        
        // 尝试获取JSR-250注解
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> rolesAllowedClass = 
                (Class<? extends Annotation>) Class.forName("jakarta.annotation.security.RolesAllowed");
            Annotation rolesAllowed = method.getAnnotation(rolesAllowedClass);
            if (rolesAllowed != null) {
                Method valueMethod = rolesAllowedClass.getMethod("value");
                String[] roles = (String[]) valueMethod.invoke(rolesAllowed);
                metadata.setJsr250Roles(roles);
            }
        } catch (Exception e) {
            // JSR-250可能不可用，忽略
        }
        
        // 如果方法级别没有安全注解，检查类级别
        if (!metadata.hasSecurityConstraints()) {
            // 检查类级别的安全注解
            preAuthorize = controllerClass.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            if (preAuthorize != null) {
                metadata.setPreAuthorizeExpression(preAuthorize.value());
            }
            
            secured = controllerClass.getAnnotation(org.springframework.security.access.annotation.Secured.class);
            if (secured != null) {
                metadata.setSecuredRoles(secured.value());
            }
            
            // 尝试获取类级别的JSR-250注解
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> rolesAllowedClass = 
                    (Class<? extends Annotation>) Class.forName("jakarta.annotation.security.RolesAllowed");
                Annotation rolesAllowed = controllerClass.getAnnotation(rolesAllowedClass);
                if (rolesAllowed != null) {
                    Method valueMethod = rolesAllowedClass.getMethod("value");
                    String[] roles = (String[]) valueMethod.invoke(rolesAllowed);
                    metadata.setJsr250Roles(roles);
                }
            } catch (Exception e) {
                // JSR-250可能不可用，忽略
            }
        }
        
        return metadata;
    }
    
    /**
     * 获取指定API路径的安全元数据
     */
    public ApiSecurityMetadata getApiSecurityMetadata(String path) {
        return apiSecurityMap.get(path);
    }
    
    /**
     * 获取所有API的安全元数据
     */
    public Map<String, ApiSecurityMetadata> getAllApiSecurityMetadata() {
        return new HashMap<>(apiSecurityMap);
    }
    
    /**
     * API安全元数据
     * 包含API的权限要求和安全配置
     */
    public static class ApiSecurityMetadata {
        private String pluginId;
        private String preAuthorizeExpression;
        private String[] securedRoles;
        private String[] jsr250Roles;
        
        public String getPluginId() {
            return pluginId;
        }
        
        public void setPluginId(String pluginId) {
            this.pluginId = pluginId;
        }
        
        public String getPreAuthorizeExpression() {
            return preAuthorizeExpression;
        }
        
        public void setPreAuthorizeExpression(String preAuthorizeExpression) {
            this.preAuthorizeExpression = preAuthorizeExpression;
        }
        
        public String[] getSecuredRoles() {
            return securedRoles;
        }
        
        public void setSecuredRoles(String[] securedRoles) {
            this.securedRoles = securedRoles;
        }
        
        public String[] getJsr250Roles() {
            return jsr250Roles;
        }
        
        public void setJsr250Roles(String[] jsr250Roles) {
            this.jsr250Roles = jsr250Roles;
        }
        
        /**
         * 检查是否有任何安全约束
         */
        public boolean hasSecurityConstraints() {
            return preAuthorizeExpression != null || 
                   (securedRoles != null && securedRoles.length > 0) ||
                   (jsr250Roles != null && jsr250Roles.length > 0);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ApiSecurityMetadata{");
            sb.append("pluginId='").append(pluginId).append('\'');
            
            if (preAuthorizeExpression != null) {
                sb.append(", preAuthorize='").append(preAuthorizeExpression).append('\'');
            }
            
            if (securedRoles != null && securedRoles.length > 0) {
                sb.append(", secured=[");
                for (int i = 0; i < securedRoles.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(securedRoles[i]);
                }
                sb.append(']');
            }
            
            if (jsr250Roles != null && jsr250Roles.length > 0) {
                sb.append(", jsr250=[");
                for (int i = 0; i < jsr250Roles.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(jsr250Roles[i]);
                }
                sb.append(']');
            }
            
            sb.append('}');
            return sb.toString();
        }
    }
} 