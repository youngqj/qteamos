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
 * 插件管理器接口
 * 负责插件的加载、启动、停止和卸载等生命周期管理
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.api;

import com.xiaoqu.qteamos.core.plugin.model.PluginDescriptor;
import java.io.File;
import java.util.List;

public interface PluginManager {
    
    /**
     * 安装插件
     * 
     * @param pluginFile 插件文件
     * @return 插件描述符
     * @throws Exception 安装异常
     */
    PluginDescriptor install(File pluginFile) throws Exception;
    
    /**
     * 启动插件
     * 
     * @param pluginId 插件ID
     * @return 是否成功
     */
    boolean start(String pluginId);
    
    /**
     * 停止插件
     * 
     * @param pluginId 插件ID
     * @return 是否成功
     */
    boolean stop(String pluginId);
    
    /**
     * 卸载插件
     * 
     * @param pluginId 插件ID
     * @return 是否成功
     */
    boolean uninstall(String pluginId);
    
    /**
     * 获取已安装的所有插件
     * 
     * @return 插件列表
     */
    List<PluginDescriptor> getInstalledPlugins();
    
    /**
     * 获取插件
     * 
     * @param pluginId 插件ID
     * @return 插件实例
     */
    Plugin getPlugin(String pluginId);
    
    /**
     * 获取插件描述符
     * 
     * @param pluginId 插件ID
     * @return 插件描述符
     */
    PluginDescriptor getPluginDescriptor(String pluginId);
    
    /**
     * 更新插件
     * 
     * @param pluginFile 新版本插件文件
     * @return 更新后的插件描述符
     * @throws Exception 更新异常
     */
    PluginDescriptor update(File pluginFile) throws Exception;
    
    /**
     * 重新加载插件
     * 
     * @param pluginId 插件ID
     * @return 是否成功
     */
    boolean reload(String pluginId);
    
    /**
     * 检查插件依赖
     * 
     * @param pluginId 插件ID
     * @return 依赖检查结果，true表示所有依赖都满足
     */
    boolean checkDependencies(String pluginId);
} 