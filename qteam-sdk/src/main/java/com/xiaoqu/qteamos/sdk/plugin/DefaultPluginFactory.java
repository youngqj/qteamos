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
 * 默认插件工厂
 * 负责创建和管理插件实例
 *
 * @author yangqijun
 * @date 2024-07-25
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.plugin;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认插件工厂
 * 负责创建和管理插件实例
 */
public class DefaultPluginFactory {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginFactory.class);
    
    private final Map<String, Plugin> pluginInstances = new ConcurrentHashMap<>();
    private final File pluginsDir;
    
    /**
     * 构造函数
     *
     * @param pluginsDir 插件目录
     */
    public DefaultPluginFactory(File pluginsDir) {
        this.pluginsDir = pluginsDir;
        if (!pluginsDir.exists() && !pluginsDir.mkdirs()) {
            log.error("无法创建插件目录: {}", pluginsDir.getAbsolutePath());
        }
    }
    
    /**
     * 创建插件实例
     *
     * @param pluginId 插件ID
     * @param mainClass 主类名称
     * @param classLoader 类加载器
     * @return 插件实例
     */
    public Plugin createPlugin(String pluginId, String mainClass, ClassLoader classLoader) {
        try {
            Class<?> clazz = classLoader.loadClass(mainClass);
            if (!Plugin.class.isAssignableFrom(clazz)) {
                log.error("插件主类 {} 未实现 Plugin 接口", mainClass);
                return null;
            }
            
            Plugin plugin = (Plugin) clazz.getDeclaredConstructor().newInstance();
            pluginInstances.put(pluginId, plugin);
            return plugin;
        } catch (Exception e) {
            log.error("创建插件实例失败: {}", mainClass, e);
            return null;
        }
    }
    
    /**
     * 根据插件ID获取插件实例
     *
     * @param pluginId 插件ID
     * @return 插件实例，如不存在则返回null
     */
    public Plugin getPlugin(String pluginId) {
        return pluginInstances.get(pluginId);
    }
    
    /**
     * 获取所有插件实例
     *
     * @return 插件实例映射表
     */
    public Map<String, Plugin> getAllPlugins() {
        return new ConcurrentHashMap<>(pluginInstances);
    }
    
    /**
     * 移除插件实例
     *
     * @param pluginId 插件ID
     * @return 被移除的插件实例，如不存在则返回null
     */
    public Plugin removePlugin(String pluginId) {
        return pluginInstances.remove(pluginId);
    }
    
    /**
     * 清除所有插件实例
     */
    public void clearPlugins() {
        pluginInstances.clear();
    }
} 