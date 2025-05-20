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
 * 配置服务接口
 * 提供插件读取和更新配置的能力
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.config;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 配置服务接口
 * 提供插件读取系统和自身配置的能力
 */
public interface ConfigService {
    
    /**
     * 获取系统配置项
     * 
     * @param key 配置键
     * @return 配置值
     */
    String getSystemConfig(String key);
    
    /**
     * 获取系统配置项，不存在则使用默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getSystemConfig(String key, String defaultValue);
    
    /**
     * 获取整型配置
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 整型配置值
     */
    int getIntConfig(String key, int defaultValue);
    
    /**
     * 获取长整型配置
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 长整型配置值
     */
    long getLongConfig(String key, long defaultValue);
    
    /**
     * 获取布尔型配置
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 布尔型配置值
     */
    boolean getBooleanConfig(String key, boolean defaultValue);
    
    /**
     * 获取插件配置
     * 
     * @param pluginId 插件ID
     * @param key 配置键
     * @return 配置值
     */
    String getPluginConfig(String pluginId, String key);
    
    /**
     * 获取插件配置，不存在则使用默认值
     * 
     * @param pluginId 插件ID
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getPluginConfig(String pluginId, String key, String defaultValue);
    
    /**
     * 获取插件所有配置
     * 
     * @param pluginId 插件ID
     * @return 配置集合
     */
    Map<String, String> getAllPluginConfig(String pluginId);
    
    /**
     * 获取插件配置属性对象
     * 
     * @param pluginId 插件ID
     * @return 配置属性对象
     */
    Properties getPluginProperties(String pluginId);
    
    /**
     * 更新插件配置
     * 
     * @param pluginId 插件ID
     * @param key 配置键
     * @param value 配置值
     * @return 是否更新成功
     */
    boolean updatePluginConfig(String pluginId, String key, String value);
    
    /**
     * 批量更新插件配置
     * 
     * @param pluginId 插件ID
     * @param configs 配置集合
     * @return 是否更新成功
     */
    boolean updatePluginConfigs(String pluginId, Map<String, String> configs);
    
    // ===================== 以下为从SDK合并的方法 =====================
    
    /**
     * 获取字符串配置
     * 
     * @param key 配置键
     * @return 配置值，如果不存在返回null
     */
    String getString(String key);
    
    /**
     * 获取字符串配置，如果不存在返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getString(String key, String defaultValue);
    
    /**
     * 获取整数配置
     * 
     * @param key 配置键
     * @return 配置值，如果不存在或转换失败返回0
     */
    int getInt(String key);
    
    /**
     * 获取整数配置，如果不存在返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    int getInt(String key, int defaultValue);
    
    /**
     * 获取长整型配置
     * 
     * @param key 配置键
     * @return 配置值，如果不存在或转换失败返回0L
     */
    long getLong(String key);
    
    /**
     * 获取长整型配置，如果不存在返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    long getLong(String key, long defaultValue);
    
    /**
     * 获取布尔配置
     * 
     * @param key 配置键
     * @return 配置值，如果不存在返回false
     */
    boolean getBoolean(String key);
    
    /**
     * 获取布尔配置，如果不存在返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    boolean getBoolean(String key, boolean defaultValue);
    
    /**
     * 获取浮点数配置
     * 
     * @param key 配置键
     * @return 配置值，如果不存在或转换失败返回0.0
     */
    double getDouble(String key);
    
    /**
     * 获取浮点数配置，如果不存在返回默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    double getDouble(String key, double defaultValue);
    
    /**
     * 获取字符串列表配置
     * 
     * @param key 配置键
     * @return 配置值列表，如果不存在返回空列表
     */
    List<String> getStringList(String key);
    
    /**
     * 获取嵌套配置对象
     * 
     * @param key 配置键
     * @return 配置对象，如果不存在返回null
     */
    ConfigService getSection(String key);
    
    /**
     * 获取Map形式的配置
     * 
     * @param key 配置键
     * @return 配置Map，如果不存在返回空Map
     */
    Map<String, Object> getMap(String key);
    
    /**
     * 设置配置值
     * 
     * @param key 配置键
     * @param value 配置值
     */
    void set(String key, Object value);
    
    /**
     * 保存配置
     * 将当前配置保存到文件
     * 
     * @return 是否成功
     */
    boolean save();
    
    /**
     * 重新加载配置
     * 从文件重新加载配置
     * 
     * @return 是否成功
     */
    boolean reload();
    
    /**
     * 检查配置是否存在
     * 
     * @param key 配置键
     * @return 是否存在
     */
    boolean contains(String key);
    
    /**
     * 获取所有配置
     * 
     * @return 所有配置的Map
     */
    Map<String, Object> getAll();
} 