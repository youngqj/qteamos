package com.xiaoqu.qteamos.core.plugin.monitoring;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 插件资源使用快照
 * 用于记录插件在某一时刻的资源使用情况
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Data
public class ResourceUsageSnapshot {

    /**
     * 插件ID
     */
    private final String pluginId;
    
    /**
     * CPU使用率（百分比）
     */
    private int cpuUsagePercent;
    
    /**
     * 内存使用量（MB）
     */
    private long memoryUsageMB;
    
    /**
     * 线程数量
     */
    private int threadCount;
    
    /**
     * 文件描述符数量
     */
    private int fileDescriptorCount;
    
    /**
     * 网络连接数
     */
    private int networkConnectionCount;
    
    /**
     * 快照创建时间
     */
    private LocalDateTime snapshotTime;
    
    /**
     * 历史CPU使用率（记录最近几次快照的数据用于趋势分析）
     */
    private int[] historicalCpuUsage = new int[5];
    
    /**
     * 历史内存使用量（记录最近几次快照的数据用于趋势分析）
     */
    private long[] historicalMemoryUsage = new long[5];
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     */
    public ResourceUsageSnapshot(String pluginId) {
        this.pluginId = pluginId;
        this.snapshotTime = LocalDateTime.now();
    }
    
    /**
     * 更新快照时间
     */
    public void updateSnapshotTime() {
        this.snapshotTime = LocalDateTime.now();
    }
    
    /**
     * 更新CPU使用率并记录历史
     *
     * @param cpuUsagePercent CPU使用率
     */
    public void setCpuUsagePercent(int cpuUsagePercent) {
        // 记录历史数据，移动数组元素
        for (int i = historicalCpuUsage.length - 1; i > 0; i--) {
            historicalCpuUsage[i] = historicalCpuUsage[i - 1];
        }
        historicalCpuUsage[0] = cpuUsagePercent;
        
        this.cpuUsagePercent = cpuUsagePercent;
        updateSnapshotTime();
    }
    
    /**
     * 更新内存使用量并记录历史
     *
     * @param memoryUsageMB 内存使用量
     */
    public void setMemoryUsageMB(long memoryUsageMB) {
        // 记录历史数据，移动数组元素
        for (int i = historicalMemoryUsage.length - 1; i > 0; i--) {
            historicalMemoryUsage[i] = historicalMemoryUsage[i - 1];
        }
        historicalMemoryUsage[0] = memoryUsageMB;
        
        this.memoryUsageMB = memoryUsageMB;
        updateSnapshotTime();
    }
    
    /**
     * 设置线程数
     *
     * @param threadCount 线程数
     */
    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        updateSnapshotTime();
    }
    
    /**
     * 设置文件描述符数量
     *
     * @param fileDescriptorCount 文件描述符数量
     */
    public void setFileDescriptorCount(int fileDescriptorCount) {
        this.fileDescriptorCount = fileDescriptorCount;
        updateSnapshotTime();
    }
    
    /**
     * 设置网络连接数
     *
     * @param networkConnectionCount 网络连接数
     */
    public void setNetworkConnectionCount(int networkConnectionCount) {
        this.networkConnectionCount = networkConnectionCount;
        updateSnapshotTime();
    }
    
    /**
     * 获取CPU使用率历史数据
     *
     * @return CPU使用率历史数据
     */
    public int[] getHistoricalCpuUsage() {
        return historicalCpuUsage.clone();
    }
    
    /**
     * 获取内存使用量历史数据
     *
     * @return 内存使用量历史数据
     */
    public long[] getHistoricalMemoryUsage() {
        return historicalMemoryUsage.clone();
    }
    
    /**
     * 获取CPU使用率趋势（是否上升）
     *
     * @return 如果趋势上升返回true，否则返回false
     */
    public boolean isCpuUsageIncreasing() {
        if (historicalCpuUsage.length < 2) {
            return false;
        }
        
        return historicalCpuUsage[0] > historicalCpuUsage[1];
    }
    
    /**
     * 获取内存使用趋势（是否上升）
     *
     * @return 如果趋势上升返回true，否则返回false
     */
    public boolean isMemoryUsageIncreasing() {
        if (historicalMemoryUsage.length < 2) {
            return false;
        }
        
        return historicalMemoryUsage[0] > historicalMemoryUsage[1];
    }
} 