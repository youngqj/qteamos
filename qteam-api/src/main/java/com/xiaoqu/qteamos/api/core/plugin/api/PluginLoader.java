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

import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import java.io.File;
import java.util.Optional;

/**
 * 插件加载器接口
 * 负责插件的类加载和实例创建过程
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
public interface PluginLoader {
    
    /**
     * 从插件信息加载插件
     *
     * @param pluginInfo 插件信息
     * @return 加载是否成功
     * @throws PluginLifecycleException 加载过程中的异常
     */
    boolean load(PluginInfo pluginInfo) throws PluginLifecycleException;
    
    /**
     * 从插件文件加载插件
     *
     * @param pluginFile 插件文件
     * @return 加载的插件信息
     * @throws PluginLifecycleException 加载过程中的异常
     */
    PluginInfo loadFromFile(File pluginFile) throws PluginLifecycleException;
    
    /**
     * 从插件文件路径加载插件
     *
     * @param filePath 插件文件路径
     * @return 加载的插件信息
     * @throws PluginLifecycleException 加载过程中的异常
     */
    PluginInfo loadFromPath(String filePath) throws PluginLifecycleException;
    
    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     * @throws PluginLifecycleException 卸载过程中的异常
     */
    boolean unload(String pluginId) throws PluginLifecycleException;
    
    /**
     * 获取插件类加载器
     *
     * @param pluginId 插件ID
     * @return 插件类加载器
     */
    Optional<ClassLoader> getPluginClassLoader(String pluginId);
} 