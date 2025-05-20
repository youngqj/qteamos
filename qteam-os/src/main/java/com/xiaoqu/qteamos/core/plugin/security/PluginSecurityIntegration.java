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
 * 插件安全集成
 * 负责将安全沙箱组件与插件系统集成
 *
 * @author yangqijun
 * @date 2024-07-09
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.security;

import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.util.List;

@Component
public class PluginSecurityIntegration {

    private static final Logger log = LoggerFactory.getLogger(PluginSecurityIntegration.class);
    
    @Autowired
    private SandboxConfig sandboxConfig;
    
    @Autowired
    private PluginSecurityManager securityManager;
    
    @Autowired
    private PluginResourceIsolator resourceIsolator;
    
    @Autowired
    private EventBus eventBus;
    
    /**
     * 初始化插件安全集成
     */
    @PostConstruct
    public void init() {
        log.info("初始化插件安全集成...");
        resourceIsolator.init();
        
        // 注册事件监听
        // 这里可以注册插件加载、卸载等事件的监听，以便设置或清除安全配置
        
        if (sandboxConfig.isEnabled()) {
            log.info("插件安全沙箱已启用，安全级别: {}", getSecurityLevel());
        } else {
            log.info("插件安全沙箱已禁用");
        }
    }
    
    /**
     * 获取当前安全级别描述
     */
    private String getSecurityLevel() {
        StringBuilder sb = new StringBuilder();
        
        if (sandboxConfig.isPermissionCheckEnabled()) {
            sb.append("权限检查");
        }
        
        if (sandboxConfig.isResourceLimitEnabled()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("资源限制");
        }
        
        if (sandboxConfig.isClassIsolationEnabled()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("类隔离");
        }
        
        if (sandboxConfig.isSignatureVerificationEnabled()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("签名验证");
        }
        
        return sb.toString();
    }
    
    /**
     * 关闭插件安全集成
     */
    @PreDestroy
    public void shutdown() {
        resourceIsolator.shutdown();
        log.info("插件安全集成已关闭");
    }
    
    /**
     * 为插件配置安全环境
     * 在插件加载时调用
     */
    public void setupPluginSecurity(PluginInfo pluginInfo) {
        if (!sandboxConfig.isEnabled()) {
            return;
        }
        
        String pluginId = pluginInfo.getPluginId();
        log.info("为插件 {} 设置安全环境", pluginId);
        
        // 1. 设置资源配额
        ResourceQuota quota = createResourceQuota(pluginInfo);
        resourceIsolator.setResourceQuota(pluginId, quota);
        
        // 2. 设置安全权限
        if (sandboxConfig.isPermissionCheckEnabled()) {
            PermissionCollection permissions = createPermissions(pluginInfo);
            securityManager.setPermissions(pluginId, permissions);
        }
        
        // 记录安全环境设置完成
        log.info("插件 {} 安全环境设置完成", pluginId);
    }
    
    /**
     * 创建插件资源配额
     */
    private ResourceQuota createResourceQuota(PluginInfo pluginInfo) {
        ResourceQuota quota = resourceIsolator.createDefaultQuota();
        
        // 根据插件信息自定义配额
        String pluginId = pluginInfo.getPluginId();
        String pluginType = pluginInfo.getDescriptor().getType();
        
        // 系统插件给予更高的配额
        if ("system".equals(pluginType)) {
            quota.setMaxMemory(quota.getMaxMemory() * 2);
            quota.setMaxCpuUsage(quota.getMaxCpuUsage() * 2);
            quota.setMaxThreads(quota.getMaxThreads() * 2);
        }
        
        // 添加插件特定的文件系统访问权限
        quota.getFileSystemAccess().put("plugins/" + pluginId, true);
        quota.getFileSystemAccess().put("tmp", true);
        
        return quota;
    }
    
    /**
     * 创建插件权限集合
     */
    private PermissionCollection createPermissions(PluginInfo pluginInfo) {
        PluginDescriptor descriptor = pluginInfo.getDescriptor();
        String pluginId = pluginInfo.getPluginId();
        
        // 获取基础权限
        PermissionCollection permissions = securityManager.createDefaultPermissions(pluginId);
        
        // 根据插件类型添加额外权限
        if ("system".equals(descriptor.getType())) {
            addSystemPluginPermissions(permissions, pluginId);
        }
        
        // 根据插件声明的权限添加
        List<String> requestedPermissions = descriptor.getPermissions();
        if (requestedPermissions != null) {
            for (String permissionName : requestedPermissions) {
                Permission permission = createPermissionFromName(permissionName, pluginId);
                if (permission != null) {
                    permissions.add(permission);
                }
            }
        }
        
        return permissions;
    }
    
    /**
     * 为系统插件添加额外权限
     */
    private void addSystemPluginPermissions(PermissionCollection permissions, String pluginId) {
        // 系统插件具有更多权限
        permissions.add(new RuntimePermission("getenv.*"));
        permissions.add(new RuntimePermission("modifyThread"));
        permissions.add(new RuntimePermission("getFileSystemAttributes"));
        permissions.add(new SocketPermission("*", "connect,resolve"));
        permissions.add(new FilePermission("/-", "read"));
    }
    
    /**
     * 从权限名称创建Permission对象
     */
    private Permission createPermissionFromName(String permissionName, String pluginId) {
        try {
            switch (permissionName) {
                case "file.read":
                    return new FilePermission("/-", "read");
                    
                case "file.write":
                    // 限制只能写入插件自己的目录和临时目录
                    return new FilePermission("plugins/" + pluginId + "/-", "write");
                    
                case "net.connect":
                    return new SocketPermission("*", "connect,resolve");
                    
                case "runtime.exec":
                    return new RuntimePermission("exec");
                    
                default:
                    log.warn("插件 {} 请求了未知权限: {}", pluginId, permissionName);
                    return null;
            }
        } catch (Exception e) {
            log.error("创建权限对象失败: " + permissionName, e);
            return null;
        }
    }
    
    /**
     * 清理插件安全环境
     * 在插件卸载时调用
     */
    public void cleanupPluginSecurity(String pluginId) {
        if (!sandboxConfig.isEnabled()) {
            return;
        }
        
        log.info("清理插件 {} 的安全环境", pluginId);
        
        // 清理资源配额
        resourceIsolator.removeResourceQuota(pluginId);
        
        // 清理安全设置
        securityManager.clearCurrentPluginId();
        
        log.info("插件 {} 安全环境已清理", pluginId);
    }
    
    /**
     * 设置当前线程的插件上下文
     * 在调用插件方法前设置
     */
    public void setPluginContext(String pluginId) {
        if (!sandboxConfig.isEnabled()) {
            return;
        }
        
        securityManager.setCurrentPluginId(pluginId);
    }
    
    /**
     * 清除当前线程的插件上下文
     * 在调用插件方法后清除
     */
    public void clearPluginContext() {
        if (!sandboxConfig.isEnabled()) {
            return;
        }
        
        securityManager.clearCurrentPluginId();
    }
    
    /**
     * 检查插件是否可以访问指定资源
     */
    public boolean checkPluginAccess(String pluginId, String resourceType, String resourceName) {
        if (!sandboxConfig.isEnabled()) {
            return true;
        }
        
        switch (resourceType) {
            case "file":
                return resourceIsolator.checkFileOperation(pluginId, resourceName, "access");
                
            case "network":
                String[] parts = resourceName.split(":");
                String host = parts[0];
                int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;
                return resourceIsolator.checkNetworkAccess(pluginId, host, port);
                
            default:
                log.warn("未知的资源类型检查: {}", resourceType);
                return false;
        }
    }
    
    /**
     * 检查沙箱是否启用
     */
    public boolean isSandboxEnabled() {
        return sandboxConfig.isEnabled();
    }
} 