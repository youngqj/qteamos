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
 * 插件资源隔离器
 * 限制插件对系统资源的使用
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

import java.io.IOException;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class PluginResourceIsolator {

    private static final Logger log = LoggerFactory.getLogger(PluginResourceIsolator.class);
    
    @Autowired
    private SandboxConfig sandboxConfig;
    
    private final Map<String, ResourceQuota> pluginQuotas = new ConcurrentHashMap<>();
    private final Map<String, ResourceUsage> pluginUsages = new ConcurrentHashMap<>();
    
    private ScheduledExecutorService monitorExecutor;
    
    /**
     * 初始化资源隔离器
     */
    public void init() {
        if (sandboxConfig.isEnabled() && sandboxConfig.isResourceLimitEnabled()) {
            log.info("插件资源限制已启用");
            startResourceMonitoring();
        } else {
            log.info("插件资源限制已禁用");
        }
    }
    
    /**
     * 启动资源监控
     */
    private void startResourceMonitoring() {
        monitorExecutor = Executors.newScheduledThreadPool(1);
        monitorExecutor.scheduleAtFixedRate(this::monitorResources, 0, 5, TimeUnit.SECONDS);
        log.info("插件资源监控已启动");
    }
    
    /**
     * 停止资源监控
     */
    public void shutdown() {
        if (monitorExecutor != null) {
            monitorExecutor.shutdown();
            log.info("插件资源监控已停止");
        }
    }
    
    /**
     * 监控所有插件的资源使用情况
     */
    private void monitorResources() {
        for (Map.Entry<String, ResourceUsage> entry : pluginUsages.entrySet()) {
            String pluginId = entry.getKey();
            ResourceUsage usage = entry.getValue();
            ResourceQuota quota = pluginQuotas.get(pluginId);
            
            if (quota == null) {
                continue;
            }
            
            // 检查内存使用
            if (usage.getMemoryUsage() > quota.getMaxMemory() * 1024 * 1024) {
                log.warn("插件 {} 内存使用超出限制: {}MB/{}MB", 
                        pluginId, usage.getMemoryUsage() / (1024 * 1024), quota.getMaxMemory());
            }
            
            // 检查CPU使用
            if (usage.getCpuUsage() > quota.getMaxCpuUsage()) {
                log.warn("插件 {} CPU使用超出限制: {}%/{}%", 
                        pluginId, usage.getCpuUsage(), quota.getMaxCpuUsage());
            }
            
            // 检查存储使用
            if (usage.getStorageUsage() > quota.getMaxStorage() * 1024 * 1024) {
                log.warn("插件 {} 存储使用超出限制: {}MB/{}MB", 
                        pluginId, usage.getStorageUsage() / (1024 * 1024), quota.getMaxStorage());
            }
            
            // 检查线程数
            if (usage.getThreadCount() > quota.getMaxThreads()) {
                log.warn("插件 {} 线程数超出限制: {}/{}", 
                        pluginId, usage.getThreadCount(), quota.getMaxThreads());
            }
            
            // 检查文件打开数
            if (usage.getOpenFileCount() > quota.getMaxOpenFiles()) {
                log.warn("插件 {} 文件打开数超出限制: {}/{}", 
                        pluginId, usage.getOpenFileCount(), quota.getMaxOpenFiles());
            }
        }
    }
    
    /**
     * 设置插件资源配额
     */
    public void setResourceQuota(String pluginId, ResourceQuota quota) {
        pluginQuotas.put(pluginId, quota);
        pluginUsages.put(pluginId, new ResourceUsage());
        log.info("已为插件 {} 设置资源配额", pluginId);
    }
    
    /**
     * 移除插件资源配额
     */
    public void removeResourceQuota(String pluginId) {
        pluginQuotas.remove(pluginId);
        pluginUsages.remove(pluginId);
    }
    
    /**
     * 获取插件资源使用情况
     */
    public ResourceUsage getResourceUsage(String pluginId) {
        return pluginUsages.getOrDefault(pluginId, new ResourceUsage());
    }
    
    /**
     * 检查内存分配请求
     * @return 是否允许分配内存
     */
    public boolean checkMemoryAllocation(String pluginId, long bytes) {
        if (!isResourceLimitEnabled()) {
            return true;
        }
        
        ResourceQuota quota = pluginQuotas.get(pluginId);
        ResourceUsage usage = pluginUsages.get(pluginId);
        
        if (quota == null || usage == null) {
            return true;
        }
        
        long maxBytes = quota.getMaxMemory() * 1024 * 1024;
        if (usage.getMemoryUsage() + bytes > maxBytes) {
            log.warn("插件 {} 内存分配请求被拒绝: 当前{}MB + 请求{}MB > 限制{}MB", 
                    pluginId, 
                    usage.getMemoryUsage() / (1024 * 1024), 
                    bytes / (1024 * 1024), 
                    quota.getMaxMemory());
            return false;
        }
        
        usage.incrementMemoryUsage(bytes);
        return true;
    }
    
    /**
     * 检查文件操作
     * @return 是否允许操作
     */
    public boolean checkFileOperation(String pluginId, String path, String operation) {
        if (!isResourceLimitEnabled()) {
            return true;
        }
        
        ResourceQuota quota = pluginQuotas.get(pluginId);
        if (quota == null) {
            return true;
        }
        
        // 检查文件打开数限制
        if ("open".equals(operation)) {
            ResourceUsage usage = pluginUsages.get(pluginId);
            if (usage != null && usage.getOpenFileCount() >= quota.getMaxOpenFiles()) {
                log.warn("插件 {} 打开文件数已达上限: {}", pluginId, quota.getMaxOpenFiles());
                return false;
            }
        }
        
        // 检查文件路径权限
        for (Map.Entry<String, Boolean> entry : quota.getFileSystemAccess().entrySet()) {
            String prefix = entry.getKey();
            Boolean allowed = entry.getValue();
            
            if (path.startsWith(prefix)) {
                if (!allowed) {
                    log.warn("插件 {} 尝试访问禁止的文件路径: {}", pluginId, path);
                    return false;
                }
                return true;
            }
        }
        
        // 默认拒绝未明确允许的路径
        log.warn("插件 {} 尝试访问未授权的文件路径: {}", pluginId, path);
        return false;
    }
    
    /**
     * 检查网络访问
     * @return 是否允许访问
     */
    public boolean checkNetworkAccess(String pluginId, String host, int port) {
        if (!isResourceLimitEnabled()) {
            return true;
        }
        
        ResourceQuota quota = pluginQuotas.get(pluginId);
        if (quota == null) {
            return true;
        }
        
        // 检查主机访问权限
        Map<String, Boolean> networkAccess = quota.getNetworkAccess();
        
        // 精确匹配
        if (networkAccess.containsKey(host)) {
            return networkAccess.get(host);
        }
        
        // 子域名匹配
        for (Map.Entry<String, Boolean> entry : networkAccess.entrySet()) {
            String pattern = entry.getKey();
            Boolean allowed = entry.getValue();
            
            // 处理通配符模式 *.example.com
            if (pattern.startsWith("*.")) {
                String domain = pattern.substring(2);
                if (host.endsWith("." + domain)) {
                    return allowed;
                }
            }
        }
        
        // 默认拒绝未明确允许的网络访问
        log.warn("插件 {} 尝试访问未授权的网络地址: {}:{}", pluginId, host, port);
        return false;
    }
    
    /**
     * 记录文件打开
     */
    public void recordFileOpen(String pluginId, String path) {
        if (!isResourceLimitEnabled()) {
            return;
        }
        
        ResourceUsage usage = pluginUsages.get(pluginId);
        if (usage != null) {
            usage.recordFileOpen(path);
        }
    }
    
    /**
     * 记录文件关闭
     */
    public void recordFileClose(String pluginId, String path) {
        if (!isResourceLimitEnabled()) {
            return;
        }
        
        ResourceUsage usage = pluginUsages.get(pluginId);
        if (usage != null) {
            usage.recordFileClose(path);
        }
    }
    
    /**
     * 记录网络连接
     */
    public void recordNetworkConnection(String pluginId, String host, int port) {
        if (!isResourceLimitEnabled()) {
            return;
        }
        
        ResourceUsage usage = pluginUsages.get(pluginId);
        if (usage != null) {
            usage.recordConnectionOpen(host + ":" + port);
        }
    }
    
    /**
     * 创建默认配额
     */
    public ResourceQuota createDefaultQuota() {
        return ResourceQuota.createDefault(sandboxConfig);
    }
    
    /**
     * 检查资源限制是否启用
     */
    public boolean isResourceLimitEnabled() {
        return sandboxConfig.isEnabled() && sandboxConfig.isResourceLimitEnabled();
    }
} 