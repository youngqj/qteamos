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

package com.xiaoqu.qteamos.core.plugin.service;

import java.util.Map;

/**
 * 插件配置服务提供者接口
 * 负责插件配置的持久化、读取和管理
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
public interface ConfigServiceProvider {
    
    /**
     * 保存插件配置
     *
     * @param pluginId 插件ID
     * @param key 配置键
     * @param value 配置值
     * @return 是否保存成功
     */
    boolean savePluginConfig(String pluginId, String key, String value);
    
    /**
     * 保存插件配置（批量）
     *
     * @param pluginId 插件ID
     * @param configs 配置项
     * @return 是否保存成功
     */
    boolean savePluginConfig(String pluginId, Map<String, String> configs);
    
    /**
     * 获取插件配置
     *
     * @param pluginId 插件ID
     * @param key 配置键
     * @return 配置值
     */
    String getPluginConfig(String pluginId, String key);
    
    /**
     * 获取插件配置，如果不存在则返回默认值
     *
     * @param pluginId 插件ID
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getPluginConfig(String pluginId, String key, String defaultValue);
    
    /**
     * 获取插件的所有配置
     *
     * @param pluginId 插件ID
     * @return 所有配置项
     */
    Map<String, String> getPluginConfigs(String pluginId);
    
    /**
     * 删除插件配置
     *
     * @param pluginId 插件ID
     * @param key 配置键
     * @return 是否删除成功
     */
    boolean removePluginConfig(String pluginId, String key);
    
    /**
     * 清除插件所有配置
     *
     * @param pluginId 插件ID
     * @return 是否清除成功
     */
    boolean clearPluginConfigs(String pluginId);
} 