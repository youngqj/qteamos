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

package com.xiaoqu.qteamos.api.core.plugin.api;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;

import java.util.Optional;

/**
 * 插件生命周期处理器接口
 * 负责管理插件的生命周期，包括加载、初始化、启动、停止和卸载
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
public interface PluginLifecycleHandler {
    
    /**
     * 加载插件
     *
     * @param pluginInfo 插件信息
     * @return 加载是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    boolean loadPlugin(PluginInfo pluginInfo) throws PluginLifecycleException;
    
    /**
     * 初始化插件
     *
     * @param pluginId 插件ID
     * @return 初始化是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    boolean initializePlugin(String pluginId) throws PluginLifecycleException;
    
    /**
     * 启动插件
     *
     * @param pluginId 插件ID
     * @return 启动是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    boolean startPlugin(String pluginId) throws PluginLifecycleException;
    
    /**
     * 停止插件
     *
     * @param pluginId 插件ID
     * @return 停止是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    boolean stopPlugin(String pluginId) throws PluginLifecycleException;
    
    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    boolean unloadPlugin(String pluginId) throws PluginLifecycleException;
    
    /**
     * 重新加载插件
     *
     * @param pluginId 插件ID
     * @return 重新加载是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    boolean reloadPlugin(String pluginId) throws PluginLifecycleException;
    
    /**
     * 更新插件
     *
     * @param oldPluginInfo 旧插件信息
     * @param newPluginInfo 新插件信息
     * @return 更新是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    boolean updatePlugin(PluginInfo oldPluginInfo, PluginInfo newPluginInfo) throws PluginLifecycleException;
    
    /**
     * 获取插件实例
     *
     * @param pluginId 插件ID
     * @return 插件实例
     */
    Optional<Plugin> getPluginInstance(String pluginId);
    
    /**
     * 检查插件健康状态
     *
     * @param pluginId 插件ID
     * @return 是否健康
     */
    boolean checkPluginHealth(String pluginId);
} 