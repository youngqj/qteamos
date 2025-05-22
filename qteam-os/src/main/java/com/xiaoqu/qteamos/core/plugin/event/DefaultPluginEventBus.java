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
 * 默认插件事件总线实现
 * 作为组件间通信的中心枢纽，提供事件发布和订阅功能
 *
 * @author yangqijun
 * @date 2025-08-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.event;

import com.xiaoqu.qteamos.api.core.event.PluginEventBus;
import com.xiaoqu.qteamos.api.core.event.PluginEventDispatcher;
import com.xiaoqu.qteamos.api.core.event.PluginEventListener;
import com.xiaoqu.qteamos.api.core.event.PriorityPluginEventListener;
import com.xiaoqu.qteamos.api.core.event.base.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * 默认插件事件总线实现
 * 委托给事件分发器实现具体功能
 */
@Component
public class DefaultPluginEventBus implements PluginEventBus {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginEventBus.class);
    
    /**
     * 事件分发器
     */
    private final PluginEventDispatcher eventDispatcher;
    
    /**
     * 构造函数
     * 
     * @param eventDispatcher 事件分发器
     */
    @Autowired
    public DefaultPluginEventBus(PluginEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        log.info("插件事件总线初始化完成");
    }
    
    @Override
    public void publishEvent(com.xiaoqu.qteamos.api.core.event.PluginEvent event) {
        if (event == null) {
            log.warn("尝试发布空事件");
            return;
        }
        
        log.debug("事件总线发布事件: {}", event);
        eventDispatcher.publishEvent(event);
    }
    
    @Override
    public <T extends BaseEvent> void publishEvent(T event) {
        if (event == null) {
            log.warn("尝试发布空事件");
            return;
        }
        
        log.debug("事件总线发布通用事件: {}", event);
        eventDispatcher.publishEvent(event);
    }
    
    @Override
    public <T extends com.xiaoqu.qteamos.api.core.event.PluginEvent> String subscribe(Class<T> eventType, PluginEventListener<T> listener) {
        log.debug("事件总线订阅事件: {}", eventType.getName());
        return eventDispatcher.registerEventListener(eventType, listener);
    }
    
    @Override
    public <T extends com.xiaoqu.qteamos.api.core.event.PluginEvent> String subscribe(
            Class<T> eventType, String topic, String type, PluginEventListener<T> listener) {
        log.debug("事件总线订阅事件: {} [topic={}, type={}]", eventType.getName(), topic, type);
        return eventDispatcher.registerEventListener(eventType, topic, type, listener);
    }
    
    @Override
    public <T extends com.xiaoqu.qteamos.api.core.event.PluginEvent> String subscribe(
            Class<T> eventType, String[] topics, String[] types, PluginEventListener<T> listener) {
        log.debug("事件总线订阅事件: {} [topics={}, types={}]", eventType.getName(), 
                String.join(",", topics), String.join(",", types));
        return eventDispatcher.registerEventListener(eventType, topics, types, listener);
    }
    
    @Override
    public <T extends com.xiaoqu.qteamos.api.core.event.PluginEvent> String subscribe(
            Class<T> eventType, PriorityPluginEventListener<T> listener) {
        log.debug("事件总线订阅事件(优先级): {} [priority={}]", 
                eventType.getName(), listener.getPriority());
        return eventDispatcher.registerPriorityEventListener(eventType, listener);
    }
    
    @Override
    public <T extends com.xiaoqu.qteamos.api.core.event.PluginEvent> String subscribe(
            Class<T> eventType, String topic, String type, PriorityPluginEventListener<T> listener) {
        log.debug("事件总线订阅事件(优先级): {} [topic={}, type={}, priority={}]", 
                eventType.getName(), topic, type, listener.getPriority());
        return eventDispatcher.registerPriorityEventListener(eventType, topic, type, listener);
    }
    
    @Override
    public <T extends com.xiaoqu.qteamos.api.core.event.PluginEvent> String subscribe(
            Class<T> eventType, String[] topics, String[] types, PriorityPluginEventListener<T> listener) {
        log.debug("事件总线订阅事件(优先级): {} [topics={}, types={}, priority={}]", 
                eventType.getName(), String.join(",", topics), 
                String.join(",", types), listener.getPriority());
        return eventDispatcher.registerPriorityEventListener(eventType, topics, types, listener);
    }
    
    @Override
    public <T extends BaseEvent> String subscribeGeneric(Class<T> eventType, PluginEventListener<T> listener) {
        log.debug("事件总线订阅通用事件: {}", eventType.getName());
        return eventDispatcher.registerGenericEventListener(eventType, listener);
    }
    
    @Override
    public boolean unsubscribe(String subscriptionId) {
        log.debug("事件总线取消订阅: {}", subscriptionId);
        return eventDispatcher.unregisterEventListener(subscriptionId);
    }
    
    @Override
    public <T extends com.xiaoqu.qteamos.api.core.event.PluginEvent> int unsubscribeAll(Class<T> eventType) {
        log.debug("事件总线取消所有订阅: {}", eventType.getName());
        return eventDispatcher.unregisterEventListeners(eventType);
    }
    
    @Override
    public void setAsyncMode(boolean async) {
        log.debug("事件总线设置异步模式: {}", async);
        eventDispatcher.setAsyncMode(async);
    }
    
    @Override
    public boolean isAsyncMode() {
        return eventDispatcher.isAsyncMode();
    }
    
    /**
     * 关闭事件总线
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭插件事件总线");
        // 事件分发器会在各自的@PreDestroy方法中关闭
    }
} 