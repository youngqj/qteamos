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

package com.xiaoqu.qteamos.core.plugin.event.plugins;

import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;

import java.time.LocalDateTime;

/**
 * 插件卸载事件
 * 当插件被成功卸载时触发
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
public class PluginUnloadedEvent extends PluginEvent {
    private final LocalDateTime eventTime;
    
    /**
     * 构造方法
     *
     * @param pluginId 插件ID
     * @param timestamp 时间戳
     */
    public PluginUnloadedEvent(String pluginId, LocalDateTime timestamp) {
        super(PluginEvent.TYPE_UNLOADED, pluginId, null);
        this.eventTime = timestamp;
    }
    
    /**
     * 获取事件本地时间
     *
     * @return 本地时间
     */
    public LocalDateTime getEventTime() {
        return eventTime;
    }

    @Override
    public String toString() {
        return "PluginUnloadedEvent{" +
                "pluginId='" + getPluginId() + '\'' +
                ", eventTime=" + eventTime +
                '}';
    }
} 