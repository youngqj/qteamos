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
 * 插件卸载事件
 * 表示插件已从内存中卸载
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.lifecycle;

import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;

public class PluginUnloadedEvent extends PluginLifecycleEvent {
    
    /**
     * 卸载时间（毫秒）
     */
    private final long unloadTime;
    
    /**
     * 是否完全卸载
     */
    private final boolean fullyUnloaded;
    
    /**
     * 卸载信息
     */
    private final String unloadInfo;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     */
    public PluginUnloadedEvent(String pluginId, String version) {
        this(pluginId, version, 0, true, null);
    }
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param unloadTime 卸载时间
     * @param fullyUnloaded 是否完全卸载
     * @param unloadInfo 卸载信息
     */
    public PluginUnloadedEvent(String pluginId, String version, long unloadTime, boolean fullyUnloaded, String unloadInfo) {
        super(PluginEventTypes.Plugin.UNLOADED, pluginId, version);
        this.unloadTime = unloadTime;
        this.fullyUnloaded = fullyUnloaded;
        this.unloadInfo = unloadInfo;
    }
    
    /**
     * 获取卸载时间
     *
     * @return 卸载时间（毫秒）
     */
    public long getUnloadTime() {
        return unloadTime;
    }
    
    /**
     * 是否完全卸载
     *
     * @return 是否完全卸载
     */
    public boolean isFullyUnloaded() {
        return fullyUnloaded;
    }
    
    /**
     * 获取卸载信息
     *
     * @return 卸载信息
     */
    public String getUnloadInfo() {
        return unloadInfo;
    }
} 