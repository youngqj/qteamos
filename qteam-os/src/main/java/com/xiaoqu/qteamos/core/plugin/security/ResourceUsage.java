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
 * 插件资源使用跟踪
 * 记录插件运行时资源消耗
 *
 * @author yangqijun
 * @date 2024-07-09
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.security;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ResourceUsage {

    /**
     * 当前内存使用量 (bytes)
     */
    private final AtomicLong memoryUsage = new AtomicLong(0);

    /**
     * 当前CPU使用率 (%)
     */
    private final AtomicInteger cpuUsage = new AtomicInteger(0);

    /**
     * 当前存储使用量 (bytes)
     */
    private final AtomicLong storageUsage = new AtomicLong(0);

    /**
     * 当前线程数
     */
    private final AtomicInteger threadCount = new AtomicInteger(0);

    /**
     * 当前打开文件数
     */
    private final AtomicInteger openFileCount = new AtomicInteger(0);

    /**
     * 打开的文件路径集合
     */
    private final Map<String, Boolean> openFiles = new ConcurrentHashMap<>();

    /**
     * 活跃网络连接
     * key: 远程地址
     * value: 连接数
     */
    private final Map<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();

    /**
     * 增加内存使用量
     */
    public void incrementMemoryUsage(long bytes) {
        memoryUsage.addAndGet(bytes);
    }

    /**
     * 减少内存使用量
     */
    public void decrementMemoryUsage(long bytes) {
        memoryUsage.addAndGet(-bytes);
    }

    /**
     * 更新CPU使用率
     */
    public void updateCpuUsage(int usage) {
        cpuUsage.set(usage);
    }

    /**
     * 增加存储使用量
     */
    public void incrementStorageUsage(long bytes) {
        storageUsage.addAndGet(bytes);
    }

    /**
     * 减少存储使用量
     */
    public void decrementStorageUsage(long bytes) {
        storageUsage.addAndGet(-bytes);
    }

    /**
     * 增加线程数
     */
    public void incrementThreadCount() {
        threadCount.incrementAndGet();
    }

    /**
     * 减少线程数
     */
    public void decrementThreadCount() {
        threadCount.decrementAndGet();
    }

    /**
     * 记录文件打开
     */
    public void recordFileOpen(String path) {
        openFileCount.incrementAndGet();
        openFiles.put(path, true);
    }

    /**
     * 记录文件关闭
     */
    public void recordFileClose(String path) {
        openFileCount.decrementAndGet();
        openFiles.remove(path);
    }

    /**
     * 记录网络连接建立
     */
    public void recordConnectionOpen(String remoteAddress) {
        activeConnections.computeIfAbsent(remoteAddress, k -> new AtomicInteger(0))
                        .incrementAndGet();
    }

    /**
     * 记录网络连接关闭
     */
    public void recordConnectionClose(String remoteAddress) {
        AtomicInteger count = activeConnections.get(remoteAddress);
        if (count != null && count.decrementAndGet() <= 0) {
            activeConnections.remove(remoteAddress);
        }
    }

    /**
     * 重置所有计数器
     */
    public void reset() {
        memoryUsage.set(0);
        cpuUsage.set(0);
        storageUsage.set(0);
        threadCount.set(0);
        openFileCount.set(0);
        openFiles.clear();
        activeConnections.clear();
    }

    // Getters

    public long getMemoryUsage() {
        return memoryUsage.get();
    }

    public int getCpuUsage() {
        return cpuUsage.get();
    }

    public long getStorageUsage() {
        return storageUsage.get();
    }

    public int getThreadCount() {
        return threadCount.get();
    }

    public int getOpenFileCount() {
        return openFileCount.get();
    }

    public Map<String, Boolean> getOpenFiles() {
        return openFiles;
    }

    public Map<String, AtomicInteger> getActiveConnections() {
        return activeConnections;
    }
} 