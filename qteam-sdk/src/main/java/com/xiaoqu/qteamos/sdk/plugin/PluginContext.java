/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-06 18:22:17
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-06 18:25:24
 * @FilePath: /QTeam/qteam-sdk/src/main/java/com/xiaoqu/qteamos/sdk/plugin/PluginContext.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
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
 * SDK层的插件上下文接口
 * 定义插件与主系统交互的方法
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.plugin;

import com.xiaoqu.qteamos.sdk.cache.CacheService;

import java.util.Map;

/**
 * 插件上下文接口
 * 为插件提供访问系统资源和服务的统一入口
 *
 * @author yangqijun
 * @date 2025-01-20
 * @since 1.0.0
 */
public interface PluginContext {

    /**
     * 获取插件ID
     *
     * @return 插件ID
     */
    String getPluginId();

    /**
     * 获取插件名称
     *
     * @return 插件名称
     */
    String getPluginName();

    /**
     * 获取插件版本
     *
     * @return 插件版本
     */
    String getPluginVersion();

    /**
     * 获取插件数据目录路径
     *
     * @return 数据目录路径
     */
    String getDataFolderPath();

    /**
     * 获取插件配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    String getPluginConfig(String key);

    /**
     * 获取插件配置值，如果不存在则返回默认值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getPluginConfig(String key, String defaultValue);

    /**
     * 设置插件配置值
     *
     * @param key 配置键
     * @param value 配置值
     */
    void setPluginConfig(String key, String value);

    /**
     * 获取指定类型的服务
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 服务实例，如果不存在则返回null
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * 获取缓存服务
     *
     * @return 缓存服务实例
     */
    CacheService getCacheService();

    /**
     * 发布事件
     *
     * @param event 事件对象
     */
    void publishEvent(Object event);

    /**
     * 获取插件状态
     *
     * @return 插件状态映射
     */
    Map<String, Object> getPluginStatus();

    /**
     * 设置插件状态
     *
     * @param key 状态键
     * @param value 状态值
     */
    void setPluginStatus(String key, Object value);

    /**
     * 获取插件状态值
     *
     * @param key 状态键
     * @return 状态值
     */
    Object getPluginStatus(String key);
} 