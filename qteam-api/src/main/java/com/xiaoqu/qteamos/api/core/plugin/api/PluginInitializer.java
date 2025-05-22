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

package com.xiaoqu.qteamos.api.core.plugin.api;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.PluginContext;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;

/**
 * 插件初始化器接口
 * 负责插件的初始化过程，包括创建上下文和调用插件的init方法
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
public interface PluginInitializer {
    
    /**
     * 初始化插件
     *
     * @param pluginId 插件ID
     * @return 初始化是否成功
     * @throws PluginLifecycleException 初始化过程中的异常
     */
    boolean initialize(String pluginId) throws PluginLifecycleException;
    
    /**
     * 为插件实例创建上下文
     *
     * @param pluginId 插件ID
     * @return 插件上下文
     * @throws PluginLifecycleException 创建上下文过程中的异常
     */
    PluginContext createContext(String pluginId) throws PluginLifecycleException;
    
    /**
     * 对插件实例调用初始化方法
     *
     * @param plugin 插件实例
     * @param context 插件上下文
     * @return 初始化是否成功
     * @throws PluginLifecycleException 调用初始化方法过程中的异常
     */
    boolean invokeInitMethod(Plugin plugin, PluginContext context) throws PluginLifecycleException;
    
    /**
     * 检查插件初始化状态
     *
     * @param pluginId 插件ID
     * @return 是否已初始化
     */
    boolean isInitialized(String pluginId);
} 