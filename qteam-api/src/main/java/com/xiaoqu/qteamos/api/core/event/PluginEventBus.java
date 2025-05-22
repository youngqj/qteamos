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
 * 插件事件总线接口
 * 作为组件间通信的中心枢纽，提供事件发布和订阅功能
 *
 * @author yangqijun
 * @date 2025-08-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event;

import com.xiaoqu.qteamos.api.core.event.base.BaseEvent;

/**
 * 插件事件总线接口
 * 作为组件间通信的中心枢纽，提供事件发布和订阅功能
 */
public interface PluginEventBus {
    
    /**
     * 发布插件事件
     * 
     * @param event 插件事件
     */
    void publishEvent(PluginEvent event);
    
    /**
     * 发布通用事件
     * 
     * @param event 通用事件
     * @param <T> 事件类型
     */
    <T extends BaseEvent> void publishEvent(T event);
    
    /**
     * 订阅特定类型的插件事件
     * 
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @param <T> 事件类型
     * @return 订阅ID，用于取消订阅
     */
    <T extends PluginEvent> String subscribe(Class<T> eventType, PluginEventListener<T> listener);
    
    /**
     * 订阅特定主题和类型的插件事件
     * 
     * @param eventType 事件类型
     * @param topic 事件主题
     * @param type 事件类型
     * @param listener 事件监听器
     * @param <T> 事件类型
     * @return 订阅ID，用于取消订阅
     */
    <T extends PluginEvent> String subscribe(
            Class<T> eventType, 
            String topic, 
            String type, 
            PluginEventListener<T> listener);
    
    /**
     * 订阅多个主题和类型的插件事件
     * 
     * @param eventType 事件类型
     * @param topics 事件主题数组
     * @param types 事件类型数组
     * @param listener 事件监听器
     * @param <T> 事件类型
     * @return 订阅ID，用于取消订阅
     */
    <T extends PluginEvent> String subscribe(
            Class<T> eventType, 
            String[] topics, 
            String[] types, 
            PluginEventListener<T> listener);
    
    /**
     * 订阅特定类型的插件事件，带优先级
     * 
     * @param eventType 事件类型
     * @param listener 优先级事件监听器
     * @param <T> 事件类型
     * @return 订阅ID，用于取消订阅
     */
    <T extends PluginEvent> String subscribe(
            Class<T> eventType, 
            PriorityPluginEventListener<T> listener);
    
    /**
     * 订阅特定主题和类型的插件事件，带优先级
     * 
     * @param eventType 事件类型
     * @param topic 事件主题
     * @param type 事件类型
     * @param listener 优先级事件监听器
     * @param <T> 事件类型
     * @return 订阅ID，用于取消订阅
     */
    <T extends PluginEvent> String subscribe(
            Class<T> eventType, 
            String topic, 
            String type, 
            PriorityPluginEventListener<T> listener);
    
    /**
     * 订阅多个主题和类型的插件事件，带优先级
     * 
     * @param eventType 事件类型
     * @param topics 事件主题数组
     * @param types 事件类型数组
     * @param listener 优先级事件监听器
     * @param <T> 事件类型
     * @return 订阅ID，用于取消订阅
     */
    <T extends PluginEvent> String subscribe(
            Class<T> eventType, 
            String[] topics, 
            String[] types, 
            PriorityPluginEventListener<T> listener);
    
    /**
     * 订阅通用事件
     * 
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @param <T> 事件类型
     * @return 订阅ID，用于取消订阅
     */
    <T extends BaseEvent> String subscribeGeneric(
            Class<T> eventType, 
            PluginEventListener<T> listener);
    
    /**
     * 取消订阅
     * 
     * @param subscriptionId 订阅ID
     * @return 是否成功取消订阅
     */
    boolean unsubscribe(String subscriptionId);
    
    /**
     * 取消特定类型的所有订阅
     * 
     * @param eventType 事件类型
     * @param <T> 事件类型
     * @return 取消的订阅数量
     */
    <T extends PluginEvent> int unsubscribeAll(Class<T> eventType);
    
    /**
     * 设置事件处理模式
     * 
     * @param async 是否异步处理事件
     */
    void setAsyncMode(boolean async);
    
    /**
     * 获取当前事件处理模式
     * 
     * @return 是否异步处理事件
     */
    boolean isAsyncMode();
} 