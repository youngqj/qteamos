package com.xiaoqu.qteamos.core.plugin.monitoring;

import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 插件健康状态快照
 * 用于存储插件运行时健康状态的数据
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Data
public class PluginHealthSnapshot {

    /**
     * 插件ID
     */
    private final String pluginId;
    
    /**
     * 插件版本
     */
    private String version;
    
    /**
     * 插件当前状态
     */
    private PluginState state;
    
    /**
     * 是否健康
     */
    private boolean healthy;
    
    /**
     * 健康状态消息
     */
    private String healthMessage;
    
    /**
     * 失败计数
     */
    private int failCount;
    
    /**
     * 总错误次数
     */
    private int totalErrorCount;
    
    /**
     * 连续错误次数
     */
    private int consecutiveErrorCount;
    
    /**
     * 最后错误时间
     */
    private LocalDateTime lastErrorTime;
    
    /**
     * 最后错误消息
     */
    private String lastErrorMessage;
    
    /**
     * 估计内存使用（MB）
     */
    private long memoryUsageMB;
    
    /**
     * 使用线程数
     */
    private int threadCount;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime lastUpdated;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     */
    public PluginHealthSnapshot(String pluginId) {
        this.pluginId = pluginId;
        this.lastUpdated = LocalDateTime.now();
    }
} 