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

/**
 * 插件健康失败事件
 * 用于通知插件从健康状态变为不健康状态
 *
 * @author yangqijun
 * @date 2024-08-20
 * @since 1.0.0
 */
public class PluginHealthFailedEvent extends PluginEvent {
    
    private final String message;
    private final int failCount;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param message 失败描述消息
     * @param failCount 失败计数
     */
    public PluginHealthFailedEvent(String pluginId, String version, String message, int failCount) {
        super(PluginEventTypes.Topics.PLUGIN,
              PluginEventTypes.Health.HEALTH_FAILED,
              "system",
              pluginId,
              version,
              null,
              false);
        
        this.message = message;
        this.failCount = failCount;
    }
    
    /**
     * 获取失败描述消息
     *
     * @return 失败描述消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取失败计数
     *
     * @return 失败计数
     */
    public int getFailCount() {
        return failCount;
    }
} 