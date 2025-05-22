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
 * 插件加载事件
 * 表示插件已成功加载到内存
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.lifecycle;

import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;

public class PluginLoadedEvent extends PluginLifecycleEvent {
    
    /**
     * 加载的类数量
     */
    private final int loadedClassCount;
    
    /**
     * 加载的资源数量
     */
    private final int loadedResourceCount;
    
    /**
     * 加载时间（毫秒）
     */
    private final long loadTime;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param loadedClassCount 加载的类数量
     * @param loadedResourceCount 加载的资源数量
     * @param loadTime 加载时间
     */
    public PluginLoadedEvent(String pluginId, String version, int loadedClassCount, int loadedResourceCount, long loadTime) {
        super(PluginEventTypes.Plugin.LOADED, pluginId, version);
        this.loadedClassCount = loadedClassCount;
        this.loadedResourceCount = loadedResourceCount;
        this.loadTime = loadTime;
    }
    
    /**
     * 获取加载的类数量
     *
     * @return 加载的类数量
     */
    public int getLoadedClassCount() {
        return loadedClassCount;
    }
    
    /**
     * 获取加载的资源数量
     *
     * @return 加载的资源数量
     */
    public int getLoadedResourceCount() {
        return loadedResourceCount;
    }
    
    /**
     * 获取加载时间
     *
     * @return 加载时间（毫秒）
     */
    public long getLoadTime() {
        return loadTime;
    }
} 