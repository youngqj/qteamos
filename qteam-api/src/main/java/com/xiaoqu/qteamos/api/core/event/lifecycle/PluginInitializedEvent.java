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
 * 插件初始化事件
 * 表示插件已成功完成初始化
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.lifecycle;

import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo;

import java.util.List;

public class PluginInitializedEvent extends PluginLifecycleEvent {
    
    /**
     * 插件信息
     */
    private final PluginInfo pluginInfo;
    
    /**
     * 初始化时间（毫秒）
     */
    private final long initTime;
    
    /**
     * 已解析的依赖列表
     */
    private final List<String> resolvedDependencies;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param pluginInfo 插件信息
     * @param initTime 初始化时间
     * @param resolvedDependencies 已解析的依赖列表
     */
    public PluginInitializedEvent(String pluginId, String version, PluginInfo pluginInfo, long initTime, List<String> resolvedDependencies) {
        super(PluginEventTypes.Plugin.INITIALIZED, pluginId, version);
        this.pluginInfo = pluginInfo;
        this.initTime = initTime;
        this.resolvedDependencies = resolvedDependencies;
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
     * 获取初始化时间
     *
     * @return 初始化时间（毫秒）
     */
    public long getInitTime() {
        return initTime;
    }
    
    /**
     * 获取已解析的依赖列表
     *
     * @return 依赖列表
     */
    public List<String> getResolvedDependencies() {
        return resolvedDependencies;
    }
} 