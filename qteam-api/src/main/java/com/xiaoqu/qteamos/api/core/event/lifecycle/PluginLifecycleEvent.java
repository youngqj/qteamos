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
 * 插件生命周期事件基类
 * 表示插件生命周期过程中的事件
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.lifecycle;

import com.xiaoqu.qteamos.api.core.event.PluginEvent;
import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;

public abstract class PluginLifecycleEvent extends PluginEvent {
    
    /**
     * 构造函数
     *
     * @param type 事件类型
     * @param pluginId 插件ID
     * @param version 插件版本
     */
    protected PluginLifecycleEvent(String type, String pluginId, String version) {
        this(type, pluginId, version, null);
    }
    
    /**
     * 构造函数
     *
     * @param type 事件类型
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param data 事件数据
     */
    protected PluginLifecycleEvent(String type, String pluginId, String version, Object data) {
        super(PluginEventTypes.Topics.PLUGIN, type, "system", pluginId, version, data, false);
    }
} 