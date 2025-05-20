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

/**
 * 插件安全管理器
 * 控制插件的资源访问权限
 *
 * @author yangqijun
 * @date 2024-07-09
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.PropertyPermission;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PluginSecurityManager extends SecurityManager {

    private static final Logger log = LoggerFactory.getLogger(PluginSecurityManager.class);
    
    @Autowired
    private SandboxConfig sandboxConfig;
    
    private final Map<String, PermissionCollection> pluginPermissions = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentPluginId = new ThreadLocal<>();
    private SecurityManager originalSecurityManager;
    
    /**
     * 初始化安全管理器
     */
    @PostConstruct
    public void init() {
        // 保存原始安全管理器，以便在关闭时恢复
        originalSecurityManager = System.getSecurityManager();
        
        if (sandboxConfig.isEnabled()) {
            log.info("插件安全沙箱已启用");
            if (sandboxConfig.isPermissionCheckEnabled()) {
                // 设置当前类为系统安全管理器
                System.setSecurityManager(this);
                log.info("插件权限检查已启用");
            } else {
                log.info("插件权限检查已禁用");
            }
        } else {
            log.info("插件安全沙箱已禁用，将不进行权限检查");
        }
    }
    
    /**
     * 关闭安全管理器
     */
    @PreDestroy
    public void destroy() {
        if (sandboxConfig.isEnabled() && sandboxConfig.isPermissionCheckEnabled()) {
            // 恢复原始安全管理器
            System.setSecurityManager(originalSecurityManager);
            log.info("安全管理器已恢复为原始状态");
        }
    }
    
    /**
     * 设置当前线程的插件ID
     */
    public void setCurrentPluginId(String pluginId) {
        currentPluginId.set(pluginId);
    }
    
    /**
     * 清除当前线程的插件ID
     */
    public void clearCurrentPluginId() {
        currentPluginId.remove();
    }
    
    /**
     * 为插件设置权限
     */
    public void setPermissions(String pluginId, PermissionCollection permissions) {
        pluginPermissions.put(pluginId, permissions);
    }
    
    /**
     * 创建默认权限集合
     */
    public PermissionCollection createDefaultPermissions(String pluginId) {
        Permissions permissions = new Permissions();
        
        // 基本运行权限
        permissions.add(new RuntimePermission("getClassLoader"));
        permissions.add(new RuntimePermission("getProtectionDomain"));
        permissions.add(new RuntimePermission("accessDeclaredMembers"));
        permissions.add(new RuntimePermission("createClassLoader"));
        
        // 反射权限（受限）
        permissions.add(new RuntimePermission("accessClassInPackage.java.lang"));
        
        // 允许插件读取自己的目录
        permissions.add(new FilePermission("plugins/" + pluginId + "/-", "read"));
        permissions.add(new FilePermission("plugins/" + pluginId + "/-", "write"));
        
        // 属性访问权限
        permissions.add(new PropertyPermission("user.dir", "read"));
        permissions.add(new PropertyPermission("java.io.tmpdir", "read"));
        permissions.add(new PropertyPermission("file.separator", "read"));
        permissions.add(new PropertyPermission("line.separator", "read"));
        permissions.add(new PropertyPermission("path.separator", "read"));
        
        // 本地主机网络权限（如需要）
        permissions.add(new SocketPermission("localhost:1024-", "connect,resolve"));
        permissions.add(new SocketPermission("127.0.0.1:1024-", "connect,resolve"));
        
        return permissions;
    }
    
    /**
     * 为指定插件添加额外权限
     */
    public void addPermission(String pluginId, Permission permission) {
        PermissionCollection permissions = pluginPermissions.get(pluginId);
        if (permissions == null) {
            permissions = createDefaultPermissions(pluginId);
            pluginPermissions.put(pluginId, permissions);
        }
        permissions.add(permission);
    }
    
    /**
     * 权限检查
     */
    @Override
    public void checkPermission(Permission perm) {
        // 如果沙箱或权限检查禁用，直接放行
        if (!sandboxConfig.isEnabled() || !sandboxConfig.isPermissionCheckEnabled()) {
            return;
        }
        
        // 获取当前线程关联的插件ID
        String pluginId = currentPluginId.get();
        
        // 如果不是插件调用，使用原始安全管理器或放行
        if (pluginId == null) {
            if (originalSecurityManager != null) {
                originalSecurityManager.checkPermission(perm);
            }
            return;
        }
        
        // 插件调用，检查权限
        PermissionCollection permissions = pluginPermissions.get(pluginId);
        if (permissions == null) {
            log.warn("插件 {} 没有设置权限集合，使用默认权限", pluginId);
            permissions = createDefaultPermissions(pluginId);
            pluginPermissions.put(pluginId, permissions);
        }
        
        if (!permissions.implies(perm)) {
            log.warn("插件 {} 尝试执行未授权操作: {}", pluginId, perm);
            throw new SecurityException("插件 " + pluginId + " 没有权限执行操作: " + perm);
        }
    }
    
    @Override
    public void checkPermission(Permission perm, Object context) {
        checkPermission(perm);
    }
    
    /**
     * 禁止插件退出JVM
     */
    @Override
    public void checkExit(int status) {
        String pluginId = currentPluginId.get();
        if (pluginId != null) {
            log.warn("插件 {} 尝试调用System.exit({})", pluginId, status);
            throw new SecurityException("插件不允许调用System.exit()");
        }
        
        if (originalSecurityManager != null) {
            originalSecurityManager.checkExit(status);
        }
    }
    
    /**
     * 检查是否沙箱已启用
     */
    public boolean isSandboxEnabled() {
        return sandboxConfig.isEnabled();
    }
    
    /**
     * 检查是否启用了权限检查
     */
    public boolean isPermissionCheckEnabled() {
        return sandboxConfig.isEnabled() && sandboxConfig.isPermissionCheckEnabled();
    }
} 