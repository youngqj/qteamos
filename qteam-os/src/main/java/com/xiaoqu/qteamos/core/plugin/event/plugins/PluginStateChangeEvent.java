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

import com.xiaoqu.qteamos.core.plugin.event.Event;

/**
 * 插件状态变更事件
 * 当插件状态发生变化时触发此事件
 *
 * @author yangqijun
 * @date 2024-08-10
 */
public class PluginStateChangeEvent extends Event {
    private final String pluginId;
    private final String version;
    private final String oldState;
    private final String newState;
    private final String message;

    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param oldState 旧状态
     * @param newState 新状态
     * @param message 状态变更消息
     */
    public PluginStateChangeEvent(String pluginId, String version, String oldState, String newState, String message) {
        super("plugin", "state_change", pluginId);
        this.pluginId = pluginId;
        this.version = version;
        this.oldState = oldState;
        this.newState = newState;
        this.message = message;
    }

    /**
     * 获取插件ID
     *
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 获取插件版本
     *
     * @return 插件版本
     */
    public String getVersion() {
        return version;
    }

    /**
     * 获取旧状态
     *
     * @return 旧状态
     */
    public String getOldState() {
        return oldState;
    }

    /**
     * 获取新状态
     *
     * @return 新状态
     */
    public String getNewState() {
        return newState;
    }

    /**
     * 获取状态变更消息
     *
     * @return 状态变更消息
     */
    public String getMessage() {
        return message;
    }
} 