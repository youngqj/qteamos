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

package com.xiaoqu.qteamos.core.plugin.event.adapter;

import com.xiaoqu.qteamos.api.core.plugin.PluginEventListener;
import com.xiaoqu.qteamos.core.plugin.event.Event;
import com.xiaoqu.qteamos.core.plugin.event.EventHandler;
import com.xiaoqu.qteamos.core.plugin.event.GenericEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 插件事件监听器适配器
 * 将PluginEventListener<T>接口适配为EventHandler接口，使其能在EventBus中注册
 *
 * @author yangqijun
 * @date 2025-06-11
 * @since 1.0.0
 */
public class PluginEventListenerAdapter<T> implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(PluginEventListenerAdapter.class);
    
    private final String pluginId;
    private final Class<T> eventType;
    private final PluginEventListener<T> listener;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param eventType 事件类型
     * @param listener 插件事件监听器
     */
    public PluginEventListenerAdapter(String pluginId, Class<T> eventType, PluginEventListener<T> listener) {
        this.pluginId = pluginId;
        this.eventType = eventType;
        this.listener = listener;
    }
    
    @Override
    public boolean handle(Event event) {
        try {
            // 如果事件是GenericEvent类型，尝试从其payload中获取事件对象
            if (event instanceof GenericEvent) {
                GenericEvent genericEvent = (GenericEvent) event;
                Object payload = genericEvent.getPayload();
                
                // 检查payload是否为所需的事件类型
                if (eventType.isInstance(payload)) {
                    @SuppressWarnings("unchecked")
                    T typedEvent = (T) payload;
                    listener.onEvent(typedEvent);
                    return true;
                }
            } 
            // 直接检查事件对象是否为所需类型
            else if (eventType.isInstance(event)) {
                @SuppressWarnings("unchecked")
                T typedEvent = (T) event;
                listener.onEvent(typedEvent);
                return true;
            }
            
            // 事件类型不匹配，返回true继续传播
            return true;
        } catch (Exception e) {
            log.error("处理插件事件时发生异常: {}, 插件: {}, 事件类型: {}", 
                    e.getMessage(), pluginId, eventType.getName(), e);
            return isContinueOnError();
        }
    }
    
    @Override
    public String[] getTopics() {
        // 默认监听plugin.event主题
        return new String[]{"plugin.event"};
    }
    
    @Override
    public String[] getTypes() {
        // 监听所有类型的事件，具体的过滤在handle方法中进行
        return new String[]{"*"};
    }
    
    @Override
    public boolean isSynchronous() {
        // 默认同步处理事件
        return true;
    }
    
    /**
     * 获取事件类型
     *
     * @return 事件类型
     */
    public Class<T> getEventType() {
        return eventType;
    }
    
    /**
     * 获取监听器
     *
     * @return 监听器
     */
    public PluginEventListener<T> getListener() {
        return listener;
    }
    
    /**
     * 获取插件ID
     *
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginEventListenerAdapter<?> that = (PluginEventListenerAdapter<?>) o;
        return Objects.equals(pluginId, that.pluginId) &&
                Objects.equals(eventType, that.eventType) &&
                Objects.equals(listener, that.listener);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pluginId, eventType, listener);
    }
} 