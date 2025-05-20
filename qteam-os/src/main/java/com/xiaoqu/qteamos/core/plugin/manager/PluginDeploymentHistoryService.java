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
 * 插件部署历史服务
 * 负责记录和查询插件部署历史记录
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.manager;

import java.util.List;

/**
 * 插件部署历史服务接口
 * 负责记录和查询插件的部署、更新、回滚等操作历史
 */
public interface PluginDeploymentHistoryService {
    
    /**
     * 保存部署记录
     * 
     * @param record 部署记录
     */
    void saveDeploymentRecord(PluginHotDeployService.DeploymentRecord record);
    
    /**
     * 获取插件的部署历史记录
     * 
     * @param pluginId 插件ID
     * @return 部署历史记录列表
     */
    List<PluginHotDeployService.DeploymentRecord> getPluginDeploymentHistory(String pluginId);
    
    /**
     * 获取所有部署历史记录
     * 
     * @return 所有部署历史记录
     */
    List<PluginHotDeployService.DeploymentRecord> getAllDeploymentHistory();
    
    /**
     * 根据部署类型获取历史记录
     * 
     * @param type 部署类型
     * @return 部署历史记录列表
     */
    List<PluginHotDeployService.DeploymentRecord> getDeploymentHistoryByType(
            PluginHotDeployService.DeploymentType type);
    
    /**
     * 根据发布状态获取历史记录
     * 
     * @param status 发布状态
     * @return 部署历史记录列表
     */
    List<PluginHotDeployService.DeploymentRecord> getDeploymentHistoryByReleaseStatus(
            PluginReleaseManager.ReleaseStatus status);
} 