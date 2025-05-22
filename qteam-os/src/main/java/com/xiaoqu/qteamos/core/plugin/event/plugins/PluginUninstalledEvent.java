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

/**
 * 插件卸载事件
 * 当插件成功卸载时触发
 *
 * @author yangqijun
 * @date 2025-05-27
 * @since 1.0.0
 */
public class PluginUninstalledEvent extends PluginEvent {
    
    /**
     * 卸载事件类型
     */
    public static final String TYPE_UNINSTALLED = "uninstalled";
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     */
    public PluginUninstalledEvent(String pluginId, String version) {
        super(TYPE_UNINSTALLED, pluginId, version);
    }
} 