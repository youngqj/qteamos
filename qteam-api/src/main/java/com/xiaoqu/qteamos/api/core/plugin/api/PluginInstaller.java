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

import com.xiaoqu.qteamos.api.core.plugin.model.PluginCandidate;

import java.io.File;
import java.nio.file.Path;

/**
 * 插件安装器接口
 * 负责插件的安装、卸载和升级
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
public interface PluginInstaller {
    
    /**
     * 安装插件候选者
     *
     * @param candidate 插件候选者
     * @return 安装成功后的插件信息，失败返回null
     */
    PluginInfo installCandidate(PluginCandidate candidate);
    
    /**
     * 从文件安装插件
     *
     * @param pluginFile 插件文件路径
     * @return 安装成功后的插件信息，失败返回null
     */
    PluginInfo installFromFile(File pluginFile);
    
    /**
     * 从路径安装插件
     *
     * @param pluginPath 插件路径
     * @return 安装成功后的插件信息，失败返回null
     */
    PluginInfo installFromPath(Path pluginPath);
    
    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     */
    boolean uninstallPlugin(String pluginId);
    
    /**
     * 升级插件
     *
     * @param pluginId 插件ID
     * @param newPluginFile 新的插件文件
     * @return 升级后的插件信息，失败返回null
     */
    PluginInfo upgradePlugin(String pluginId, File newPluginFile);
    
    /**
     * 从临时目录处理插件
     *
     * @param tempFile 临时目录中的文件
     * @return 处理成功后的插件信息，失败返回null
     */
    PluginInfo processTempFile(File tempFile);
    
    /**
     * 验证插件
     *
     * @param pluginFile 插件文件
     * @return 验证是否通过
     */
    boolean validatePlugin(File pluginFile);
} 