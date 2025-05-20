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

package com.xiaoqu.qteamos.core.gateway.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API统计信息模型
 * 记录插件API的调用统计数据
 *
 * @author yangqijun
 * @date 2025-05-03
 * @since 1.0.0
 */
public class ApiStatistics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 总请求次数
     */
    private AtomicLong totalRequests = new AtomicLong(0);
    
    /**
     * 成功请求次数
     */
    private AtomicLong successRequests = new AtomicLong(0);
    
    /**
     * 失败请求次数
     */
    private AtomicLong failedRequests = new AtomicLong(0);
    
    /**
     * 路径请求统计(路径 -> 请求次数)
     */
    private Map<String, AtomicLong> pathRequestCounts = new HashMap<>();
    
    /**
     * 平均响应时间(毫秒)
     */
    private double averageResponseTime = 0;
    
    /**
     * 最大响应时间(毫秒)
     */
    private long maxResponseTime = 0;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 统计开始时间
     */
    private LocalDateTime startTime;
    
    public ApiStatistics() {
        this.startTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
    
    public ApiStatistics(String pluginId) {
        this();
        this.pluginId = pluginId;
    }
    
    /**
     * 记录请求
     * 
     * @param path 请求路径
     * @param success 是否成功
     * @param responseTime 响应时间(毫秒)
     */
    public void recordRequest(String path, boolean success, long responseTime) {
        totalRequests.incrementAndGet();
        
        if (success) {
            successRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        // 更新路径请求计数
        pathRequestCounts.computeIfAbsent(path, k -> new AtomicLong(0)).incrementAndGet();
        
        // 更新响应时间统计
        double totalTime = averageResponseTime * (totalRequests.get() - 1);
        averageResponseTime = (totalTime + responseTime) / totalRequests.get();
        
        // 更新最大响应时间
        if (responseTime > maxResponseTime) {
            maxResponseTime = responseTime;
        }
        
        // 更新时间
        updateTime = LocalDateTime.now();
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getSuccessRequests() {
        return successRequests.get();
    }

    public long getFailedRequests() {
        return failedRequests.get();
    }

    public Map<String, Long> getPathRequestCounts() {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, AtomicLong> entry : pathRequestCounts.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    /**
     * 重置统计数据
     */
    public void reset() {
        totalRequests.set(0);
        successRequests.set(0);
        failedRequests.set(0);
        pathRequestCounts.clear();
        averageResponseTime = 0;
        maxResponseTime = 0;
        startTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "ApiStatistics{" +
                "pluginId='" + pluginId + '\'' +
                ", totalRequests=" + totalRequests +
                ", successRequests=" + successRequests +
                ", failedRequests=" + failedRequests +
                ", pathRequestCounts=" + pathRequestCounts +
                ", averageResponseTime=" + averageResponseTime +
                ", maxResponseTime=" + maxResponseTime +
                ", updateTime=" + updateTime +
                ", startTime=" + startTime +
                '}';
    }
} 