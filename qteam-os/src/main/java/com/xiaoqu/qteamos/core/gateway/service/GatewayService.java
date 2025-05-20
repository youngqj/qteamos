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

package com.xiaoqu.qteamos.core.gateway.service;

import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;

/**
 * 网关服务接口
 * 定义了网关服务的核心功能
 *
 * @author yangqijun
 * @date 2025-05-03
 * @since 1.0.0
 */
public interface GatewayService {
    
    /**
     * 初始化网关服务
     */
    void initialize();
    
    /**
     * 注册插件API路由
     *
     * @param pluginId 插件ID
     * @return 注册的路由数量
     */
    int registerPluginRoutes(String pluginId);
    
    /**
     * 注册插件API路由
     *
     * @param plugin 插件信息
     * @return 注册的路由数量
     */
    int registerPluginRoutes(PluginInfo plugin);
    
    /**
     * 注销插件API路由
     *
     * @param pluginId 插件ID
     * @return 注销的路由数量
     */
    int unregisterPluginRoutes(String pluginId);
    
    /**
     * 刷新网关路由配置
     */
    void refreshRoutes();
    
    /**
     * 获取插件API统计信息
     *
     * @param pluginId 插件ID
     * @return API统计信息
     */
    Object getApiStatistics(String pluginId);
    
    /**
     * 设置插件API限流规则
     *
     * @param pluginId 插件ID
     * @param limitRate 限流速率(次/分钟)
     */
    void setApiRateLimit(String pluginId, int limitRate);
} 