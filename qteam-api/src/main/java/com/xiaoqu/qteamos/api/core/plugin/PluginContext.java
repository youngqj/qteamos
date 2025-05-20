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
 * 插件上下文
 * 提供插件与系统交互的接口，访问系统资源和服务
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.plugin;

import com.xiaoqu.qteamos.api.core.cache.CacheService;
import com.xiaoqu.qteamos.api.core.config.ConfigService;
import com.xiaoqu.qteamos.api.core.datasource.DataSourceService;

import java.util.Map;

/**
 * 插件上下文
 * 提供插件与系统交互的能力，包括访问系统资源、服务等
 */
public interface PluginContext {

    /**
     * 获取插件ID
     * 
     * @return 插件ID
     */
    String getPluginId();
    
    /**
     * 获取插件版本
     * 
     * @return 插件版本
     */
    String getPluginVersion();
    
    /**
     * 获取插件配置
     * 
     * @param key 配置键
     * @return 配置值
     */
    String getConfig(String key);
    
    /**
     * 获取插件配置
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如不存在则返回默认值
     */
    String getConfig(String key, String defaultValue);
    
    /**
     * 获取所有插件配置
     * 
     * @return 配置项集合
     */
    Map<String, String> getAllConfigs();
    
    /**
     * 设置插件配置
     * 
     * @param key 配置键
     * @param value 配置值
     */
    void setConfig(String key, String value);
    
    /**
     * 获取插件数据目录路径
     * 
     * @return 数据目录路径
     */
    String getDataFolderPath();
    
    /**
     * 获取缓存服务
     * 
     * @return 缓存服务实例
     */
    CacheService getCacheService();
    
    /**
     * 获取数据源服务
     * 
     * @return 数据源服务实例
     */
    DataSourceService getDataSourceService();
    
    /**
     * 获取配置服务
     * 
     * @return 配置服务实例
     */
    ConfigService getConfigService();
    
    /**
     * 获取系统服务
     * 
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 服务实例
     */
    <T> T getService(Class<T> serviceClass);
    
    /**
     * 发布事件
     * 
     * @param event 事件对象
     */
    void publishEvent(Object event);
    
    /**
     * 注册事件监听器
     * 
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @param <T> 事件泛型
     */
    <T> void addEventListener(Class<T> eventType, PluginEventListener<T> listener);
    
    /**
     * 移除事件监听器
     * 
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @param <T> 事件泛型
     */
    <T> void removeEventListener(Class<T> eventType, PluginEventListener<T> listener);
} 