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

import java.nio.file.Path;

import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;

/**
 * 插件安装事件
 * 当插件安装成功时触发
 *
 * @author yangqijun
 * @date 2025-05-27
 * @since 1.0.0
 */
public class PluginInstalledEvent extends PluginEvent {
    
    /**
     * 安装事件类型
     */
    public static final String TYPE_INSTALLED = "installed";
    
    private final Path pluginPath;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param pluginPath 插件路径
     */
    public PluginInstalledEvent(String pluginId, String version, Path pluginPath) {
        super(TYPE_INSTALLED, pluginId, version, pluginPath);
        this.pluginPath = pluginPath;
    }
    
    /**
     * 获取插件路径
     *
     * @return 插件路径
     */
    public Path getPluginPath() {
        return pluginPath;
    }
} 