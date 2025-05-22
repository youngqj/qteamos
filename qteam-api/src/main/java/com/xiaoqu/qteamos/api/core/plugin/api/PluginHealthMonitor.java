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

package com.xiaoqu.qteamos.api.core.plugin.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 插件健康监控接口
 * 负责监控插件的健康状态，提供健康检查和自动恢复功能
 *
 * @author yangqijun
 * @date 2024-08-15
 * @since 1.0.0
 */
public interface PluginHealthMonitor {
    
    /**
     * 检查插件健康状态
     *
     * @param pluginId 插件ID
     * @return 是否健康
     */
    boolean checkPluginHealth(String pluginId);
    
    /**
     * 执行插件健康检查并刷新状态
     *
     * @param pluginId 插件ID
     * @return 健康状态快照
     */
    HealthSnapshot performHealthCheck(String pluginId);
    
    /**
     * 执行所有插件的健康检查
     *
     * @return 健康检查结果（插件ID -> 是否健康）
     */
    Map<String, Boolean> performHealthCheckForAll();
    
    /**
     * 获取插件的健康状态快照
     *
     * @param pluginId 插件ID
     * @return 健康状态快照
     */
    HealthSnapshot getHealthSnapshot(String pluginId);
    
    /**
     * 获取所有插件的健康状态快照
     *
     * @return 健康状态快照列表
     */
    List<HealthSnapshot> getAllHealthSnapshots();
    
    /**
     * 获取不健康的插件ID列表
     *
     * @return 不健康的插件ID集合
     */
    Set<String> getUnhealthyPlugins();
    
    /**
     * 尝试恢复插件
     *
     * @param pluginId 插件ID
     * @return 恢复是否成功
     */
    boolean attemptPluginRecovery(String pluginId);
    
    /**
     * 重置插件的错误计数
     *
     * @param pluginId 插件ID
     */
    void resetErrorCount(String pluginId);
    
    /**
     * 获取插件健康检查历史
     *
     * @param pluginId 插件ID
     * @param limit 最大记录数
     * @return 健康检查历史
     */
    List<HealthRecord> getHealthHistory(String pluginId, int limit);
    
    /**
     * 设置健康检查参数
     *
     * @param checkInterval 检查间隔（毫秒）
     * @param maxFailCount 最大失败次数
     * @param requestTimeout 请求超时（毫秒）
     */
    void setHealthCheckParameters(long checkInterval, int maxFailCount, long requestTimeout);
    
    /**
     * 插件健康状态快照
     */
    interface HealthSnapshot {
        /**
         * 获取插件ID
         *
         * @return 插件ID
         */
        String getPluginId();
        
        /**
         * 获取插件版本
         *
         * @return 插件版本
         */
        String getVersion();
        
        /**
         * 是否健康
         *
         * @return 是否健康
         */
        boolean isHealthy();
        
        /**
         * 获取状态描述消息
         *
         * @return 状态描述消息
         */
        String getMessage();
        
        /**
         * 获取失败计数
         *
         * @return 失败计数
         */
        int getFailCount();
        
        /**
         * 获取最后检查时间
         *
         * @return 最后检查时间
         */
        LocalDateTime getLastCheckTime();
        
        /**
         * 获取资源使用情况
         *
         * @return 资源使用情况
         */
        Map<String, Object> getResourceUsage();
    }
    
    /**
     * 插件健康记录
     */
    interface HealthRecord {
        /**
         * 获取插件ID
         *
         * @return 插件ID
         */
        String getPluginId();
        
        /**
         * 获取插件版本
         *
         * @return 插件版本
         */
        String getVersion();
        
        /**
         * 是否健康
         *
         * @return 是否健康
         */
        boolean isHealthy();
        
        /**
         * 获取状态描述消息
         *
         * @return 状态描述消息
         */
        String getMessage();
        
        /**
         * 获取检查时间
         *
         * @return 检查时间
         */
        LocalDateTime getCheckTime();
        
        /**
         * 获取失败计数
         *
         * @return 失败计数
         */
        int getFailCount();
        
        /**
         * 获取检查类型
         *
         * @return 检查类型（自动/手动）
         */
        String getCheckType();
    }
} 