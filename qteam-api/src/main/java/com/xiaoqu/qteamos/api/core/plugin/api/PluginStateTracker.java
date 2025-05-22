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
import java.util.Optional;

/**
 * 插件状态跟踪器
 * 负责跟踪和管理插件的状态变化，记录状态转换历史，并支持状态持久化
 *
 * @author yangqijun
 * @date 2024-08-10
 * @since 1.0.0
 */
public interface PluginStateTracker {

    /**
     * 记录插件状态变化
     *
     * @param pluginId 插件ID
     * @param newState 新状态
     */
    void recordStateChange(String pluginId, String newState);

    /**
     * 记录插件状态变化并带有附加信息
     *
     * @param pluginId 插件ID
     * @param newState 新状态
     * @param message 附加信息
     */
    void recordStateChange(String pluginId, String newState, String message);

    /**
     * 记录插件失败状态和错误信息
     *
     * @param pluginId 插件ID
     * @param errorMessage 错误信息
     */
    void recordFailure(String pluginId, String errorMessage);

    /**
     * 获取插件当前状态
     *
     * @param pluginId 插件ID
     * @return 插件状态
     */
    Optional<String> getPluginState(String pluginId);

    /**
     * 检查插件是否处于特定状态
     *
     * @param pluginId 插件ID
     * @param state 状态
     * @return 是否处于该状态
     */
    boolean isInState(String pluginId, String state);

    /**
     * 获取处于特定状态的插件列表
     *
     * @param state 状态
     * @return 插件ID列表
     */
    List<String> getPluginsInState(String state);

    /**
     * 获取失败的插件列表
     *
     * @return 失败的插件ID列表
     */
    List<String> getFailedPlugins();

    /**
     * 获取插件状态变化历史
     *
     * @param pluginId 插件ID
     * @param limit 最大记录数
     * @return 状态变化历史
     */
    List<StateChangeRecord> getStateHistory(String pluginId, int limit);

    /**
     * 获取插件最后一次状态变化记录
     *
     * @param pluginId 插件ID
     * @return 最后一次状态变化记录
     */
    Optional<StateChangeRecord> getLastStateChange(String pluginId);

    /**
     * 清除插件状态记录
     *
     * @param pluginId 插件ID
     */
    void clearStateRecord(String pluginId);

    /**
     * 清除所有状态记录
     */
    void clearAllStateRecords();

    /**
     * 状态变化记录
     */
    interface StateChangeRecord {
        
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
         * 获取旧状态
         * 
         * @return 旧状态
         */
        String getOldState();
        
        /**
         * 获取新状态
         * 
         * @return 新状态
         */
        String getNewState();
        
        /**
         * 获取变化时间
         * 
         * @return 变化时间
         */
        LocalDateTime getChangeTime();
        
        /**
         * 获取附加信息
         * 
         * @return 附加信息
         */
        String getMessage();
    }
} 