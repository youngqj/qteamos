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
 * 插件启动事件
 * 表示插件已成功启动并运行
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.lifecycle;

import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo;

import java.util.Map;

public class PluginStartedEvent extends PluginLifecycleEvent {
    
    /**
     * 插件信息
     */
    private final PluginInfo pluginInfo;
    
    /**
     * 启动时间（毫秒）
     */
    private final long startTime;
    
    /**
     * 插件启动参数
     */
    private final Map<String, Object> startParameters;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param pluginInfo 插件信息
     * @param startTime 启动时间（毫秒）
     */
    public PluginStartedEvent(String pluginId, String version, PluginInfo pluginInfo, long startTime) {
        this(pluginId, version, pluginInfo, startTime, null);
    }
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param pluginInfo 插件信息
     * @param startTime 启动时间（毫秒）
     * @param startParameters 启动参数
     */
    public PluginStartedEvent(String pluginId, String version, PluginInfo pluginInfo, long startTime, Map<String, Object> startParameters) {
        super(PluginEventTypes.Plugin.STARTED, pluginId, version);
        this.pluginInfo = pluginInfo;
        this.startTime = startTime;
        this.startParameters = startParameters;
    }
    
    /**
     * 获取插件信息
     *
     * @return 插件信息
     */
    public PluginInfo getPluginInfo() {
        return pluginInfo;
    }
    
    /**
     * 获取启动时间
     *
     * @return 启动时间（毫秒）
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * 获取启动参数
     *
     * @return 启动参数
     */
    public Map<String, Object> getStartParameters() {
        return startParameters;
    }
} 