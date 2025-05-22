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
 * 插件事件分发接口
 * 负责事件的注册、分发和管理
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event;

import com.xiaoqu.qteamos.api.core.event.base.BaseEvent;

/**
 * 插件事件分发接口
 * 负责事件的注册、分发和管理
 */
public interface PluginEventDispatcher {
    
    /**
     * 发布事件
     *
     * @param event 事件对象
     */
    void publishEvent(PluginEvent event);
    
    /**
     * 发布通用事件
     * 支持发布任何继承自BaseEvent的事件类型
     *
     * @param event 事件对象
     * @param <T> 事件类型
     */
    <T extends BaseEvent> void publishEvent(T event);
    
    /**
     * 注册事件监听器
     *
     * @param eventType 事件类型
     * @param topics 感兴趣的主题数组
     * @param types 感兴趣的类型数组
     * @param listener 监听器
     * @param <T> 事件类型
     * @return 注册ID
     */
    <T extends PluginEvent> String registerEventListener(
            Class<T> eventType, 
            String[] topics, 
            String[] types, 
            PluginEventListener<T> listener);
    
    /**
     * 注册事件监听器
     *
     * @param eventType 事件类型
     * @param topic 感兴趣的主题
     * @param type 感兴趣的类型
     * @param listener 监听器
     * @param <T> 事件类型
     * @return 注册ID
     */
    <T extends PluginEvent> String registerEventListener(
            Class<T> eventType, 
            String topic, 
            String type, 
            PluginEventListener<T> listener);
    
    /**
     * 注册事件监听器
     *
     * @param eventType 事件类型
     * @param listener 监听器
     * @param <T> 事件类型
     * @return 注册ID
     */
    <T extends PluginEvent> String registerEventListener(
            Class<T> eventType, 
            PluginEventListener<T> listener);
    
    /**
     * 注册带优先级的事件监听器
     *
     * @param eventType 事件类型
     * @param topics 感兴趣的主题数组
     * @param types 感兴趣的类型数组
     * @param listener 带优先级的监听器
     * @param <T> 事件类型
     * @return 注册ID
     */
    <T extends PluginEvent> String registerPriorityEventListener(
            Class<T> eventType, 
            String[] topics, 
            String[] types, 
            PriorityPluginEventListener<T> listener);
    
    /**
     * 注册带优先级的事件监听器
     *
     * @param eventType 事件类型
     * @param topic 感兴趣的主题
     * @param type 感兴趣的类型
     * @param listener 带优先级的监听器
     * @param <T> 事件类型
     * @return 注册ID
     */
    <T extends PluginEvent> String registerPriorityEventListener(
            Class<T> eventType, 
            String topic, 
            String type, 
            PriorityPluginEventListener<T> listener);
    
    /**
     * 注册带优先级的事件监听器
     *
     * @param eventType 事件类型
     * @param listener 带优先级的监听器
     * @param <T> 事件类型
     * @return 注册ID
     */
    <T extends PluginEvent> String registerPriorityEventListener(
            Class<T> eventType, 
            PriorityPluginEventListener<T> listener);
    
    /**
     * 注册通用事件监听器
     * 支持监听任何继承自BaseEvent的事件类型
     *
     * @param eventType 事件类型
     * @param listener 监听器
     * @param <T> 事件类型
     * @return 注册ID
     */
    <T extends BaseEvent> String registerGenericEventListener(
            Class<T> eventType,
            PluginEventListener<T> listener);
    
    /**
     * 取消注册事件监听器
     *
     * @param registrationId 注册ID
     * @return 是否成功取消注册
     */
    boolean unregisterEventListener(String registrationId);
    
    /**
     * 取消注册特定类型的所有事件监听器
     *
     * @param eventType 事件类型
     * @param <T> 事件类型
     * @return 取消注册的监听器数量
     */
    <T extends PluginEvent> int unregisterEventListeners(Class<T> eventType);
    
    /**
     * 设置是否使用异步模式
     * 
     * @param async 是否使用异步模式
     */
    void setAsyncMode(boolean async);
    
    /**
     * 获取是否使用异步模式
     * 
     * @return 是否使用异步模式
     */
    boolean isAsyncMode();
    
    /**
     * 获取已注册的监听器数量
     * 
     * @return 监听器数量
     */
    int getListenerCount();
    
    /**
     * 关闭事件分发器
     * 释放资源，停止线程池等
     */
    void shutdown();
} 