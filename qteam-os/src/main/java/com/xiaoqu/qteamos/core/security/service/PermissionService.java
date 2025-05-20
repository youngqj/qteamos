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
 * 权限服务
 * 整合系统和插件的权限管理
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.security.extension.PluginSecurityExtensionManager;
import com.xiaoqu.qteamos.core.security.plugin.PluginSecurityExtension;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PermissionService {
    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private PluginSecurityExtensionManager extensionManager;
    
    /**
     * 检查当前用户是否有指定权限
     * 
     * @param permission 权限标识
     * @return 是否有权限
     */
    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // 超级管理员拥有所有权限
        if (hasRole("ROLE_ADMIN")) {
            return true;
        }
        
        // 检查用户权限集合中是否包含指定权限
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(permission));
    }
    
    /**
     * 检查当前用户是否有任意一个指定权限
     * 
     * @param permissions 权限列表
     * @return 是否有任意一个权限
     */
    public boolean hasAnyPermission(String... permissions) {
        for (String permission : permissions) {
            if (hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查当前用户是否有指定角色
     * 
     * @param role 角色名称
     * @return 是否有角色
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // 如果角色名称没有ROLE_前缀，添加前缀
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(roleWithPrefix));
    }
    
    /**
     * 获取系统中所有可用的权限
     * 包括系统核心权限和所有插件定义的权限
     * 
     * @return 权限列表
     */
    public List<String> getAllPermissions() {
        // 系统核心权限
        List<String> corePermissions = getCorePermissions();
        
        // 插件权限
        List<String> pluginPermissions = getPluginPermissions();
        
        // 合并并去重
        return Stream.concat(corePermissions.stream(), pluginPermissions.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 获取系统核心权限列表
     */
    private List<String> getCorePermissions() {
        // 在实际应用中，可能从数据库或配置文件获取
        return Arrays.asList(
                "core:system:view", 
                "core:system:manage",
                "core:user:view",
                "core:user:create",
                "core:user:edit",
                "core:user:delete",
                "core:role:view",
                "core:role:manage",
                "core:plugin:view",
                "core:plugin:install",
                "core:plugin:uninstall",
                "core:plugin:configure",
                "core:api:access"
        );
    }
    
    /**
     * 获取所有插件定义的权限
     */
    private List<String> getPluginPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // 1. 从插件描述符中获取
        for (PluginInfo plugin : pluginRegistry.getAllPlugins()) {
            if (plugin.getDescriptor().getPermissions() != null) {
                permissions.addAll(plugin.getDescriptor().getPermissions());
            }
        }
        
        // 2. 从插件安全扩展中获取
        for (PluginSecurityExtension extension : extensionManager.getAllExtensions()) {
            permissions.addAll(Arrays.asList(extension.getPermissions()));
        }
        
        return permissions;
    }
    
    /**
     * 获取系统中所有可用的角色
     * 
     * @return 角色列表
     */
    public List<String> getAllRoles() {
        // 系统核心角色
        List<String> coreRoles = Arrays.asList(
                "ROLE_ADMIN",
                "ROLE_USER",
                "ROLE_GUEST",
                "ROLE_OPERATOR",
                "ROLE_AUDITOR"
        );
        
        // 插件角色
        List<String> pluginRoles = new ArrayList<>();
        for (PluginSecurityExtension extension : extensionManager.getAllExtensions()) {
            pluginRoles.addAll(Arrays.asList(extension.getRoles()));
        }
        
        // 合并并去重
        return Stream.concat(coreRoles.stream(), pluginRoles.stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
} 