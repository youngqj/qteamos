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

package com.xiaoqu.qteamos.api.core.event.health;

import com.xiaoqu.qteamos.api.core.event.PluginEvent;
import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;

import java.util.Map;

/**
 * 插件健康检查事件
 * 用于通知插件健康状态变化
 *
 * @author yangqijun
 * @date 2024-08-20
 * @since 1.0.0
 */
public class PluginHealthCheckEvent extends PluginEvent {
    
    private final boolean healthy;
    private final String message;
    private final Map<String, Object> resourceUsage;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param healthy 是否健康
     * @param message 健康状态描述消息
     * @param resourceUsage 资源使用情况
     */
    public PluginHealthCheckEvent(String pluginId, String version, boolean healthy, 
                                 String message, Map<String, Object> resourceUsage) {
        super(PluginEventTypes.Topics.PLUGIN,
              PluginEventTypes.Health.HEALTH_CHECK,
              "system",
              pluginId,
              version,
              null,
              false);
        
        this.healthy = healthy;
        this.message = message;
        this.resourceUsage = resourceUsage;
    }
    
    /**
     * 获取健康状态
     *
     * @return 是否健康
     */
    public boolean isHealthy() {
        return healthy;
    }
    
    /**
     * 获取健康状态描述消息
     *
     * @return 健康状态描述消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取资源使用情况
     *
     * @return 资源使用情况
     */
    public Map<String, Object> getResourceUsage() {
        return resourceUsage;
    }
} 