/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
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
 * 插件部署历史服务接口
 * 负责记录插件部署、发布和回滚的历史记录
 *
 * @author yangqijun
 * @date 2025-07-01
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.service;

import com.xiaoqu.qteamos.core.plugin.manager.PluginReleaseManager.ReleaseStatus;

import java.util.List;
import java.util.Map;

/**
 * 插件部署历史服务接口
 * 负责记录插件的部署、灰度发布和版本状态变更历史
 */
public interface PluginDeploymentHistoryService {
    
    /**
     * 记录插件部署
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @param success 是否成功
     * @param message 消息（可选）
     */
    void recordDeployment(String pluginId, String version, boolean success, String message);
    
    /**
     * 记录状态变更
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @param status 发布状态
     */
    void recordStatusChange(String pluginId, String version, ReleaseStatus status);
    
    /**
     * 记录灰度发布进度
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @param batch 批次
     * @param percentage 百分比
     * @param success 是否成功
     */
    void recordRolloutProgress(String pluginId, String version, int batch, int percentage, boolean success);
    
    /**
     * 记录版本确认
     *
     * @param pluginId 插件ID
     * @param version 版本号
     */
    void recordConfirmation(String pluginId, String version);
    
    /**
     * 记录版本拒绝并回滚
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @param rollbackVersion 回滚到的版本
     */
    void recordRejection(String pluginId, String version, String rollbackVersion);
    
    /**
     * 记录版本弃用
     *
     * @param pluginId 插件ID
     * @param version 版本号
     */
    void recordDeprecation(String pluginId, String version);
    
    /**
     * 获取插件部署历史
     *
     * @param pluginId 插件ID
     * @param limit 限制数量
     * @return 部署历史记录列表
     */
    List<Map<String, Object>> getDeploymentHistory(String pluginId, int limit);
    
    /**
     * 获取插件发布状态变更历史
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 状态变更历史记录列表
     */
    List<Map<String, Object>> getStatusChangeHistory(String pluginId, String version);
} 