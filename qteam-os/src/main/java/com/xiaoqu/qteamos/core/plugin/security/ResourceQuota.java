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
 * 插件资源配额模型
 * 定义插件可使用的资源限制
 *
 * @author yangqijun
 * @date 2024-07-09
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.security;

import java.util.HashMap;
import java.util.Map;

public class ResourceQuota {

    /**
     * 最大内存使用量 (MB)
     */
    private long maxMemory;

    /**
     * 最大CPU使用率 (%)
     */
    private int maxCpuUsage;

    /**
     * 最大存储空间 (MB)
     */
    private long maxStorage;

    /**
     * 最大线程数
     */
    private int maxThreads;

    /**
     * 最大文件打开数
     */
    private int maxOpenFiles;

    /**
     * 网络访问控制
     * key: 主机名/IP地址
     * value: 是否允许访问
     */
    private Map<String, Boolean> networkAccess = new HashMap<>();

    /**
     * 文件系统访问控制
     * key: 路径前缀
     * value: 是否允许访问
     */
    private Map<String, Boolean> fileSystemAccess = new HashMap<>();

    /**
     * 创建默认配额
     */
    public static ResourceQuota createDefault(SandboxConfig config) {
        ResourceQuota quota = new ResourceQuota();
        quota.setMaxMemory(config.getDefaultMemoryLimit());
        quota.setMaxCpuUsage(config.getDefaultCpuLimit());
        quota.setMaxStorage(config.getDefaultStorageLimit());
        quota.setMaxThreads(config.getDefaultThreadLimit());
        quota.setMaxOpenFiles(50);
        
        // 默认允许访问本地主机
        quota.getNetworkAccess().put("localhost", true);
        quota.getNetworkAccess().put("127.0.0.1", true);
        
        // 默认允许访问插件目录
        quota.getFileSystemAccess().put("plugins", true);
        
        return quota;
    }

    // Getters and Setters

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public int getMaxCpuUsage() {
        return maxCpuUsage;
    }

    public void setMaxCpuUsage(int maxCpuUsage) {
        this.maxCpuUsage = maxCpuUsage;
    }

    public long getMaxStorage() {
        return maxStorage;
    }

    public void setMaxStorage(long maxStorage) {
        this.maxStorage = maxStorage;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMaxOpenFiles() {
        return maxOpenFiles;
    }

    public void setMaxOpenFiles(int maxOpenFiles) {
        this.maxOpenFiles = maxOpenFiles;
    }

    public Map<String, Boolean> getNetworkAccess() {
        return networkAccess;
    }

    public void setNetworkAccess(Map<String, Boolean> networkAccess) {
        this.networkAccess = networkAccess;
    }

    public Map<String, Boolean> getFileSystemAccess() {
        return fileSystemAccess;
    }

    public void setFileSystemAccess(Map<String, Boolean> fileSystemAccess) {
        this.fileSystemAccess = fileSystemAccess;
    }
} 