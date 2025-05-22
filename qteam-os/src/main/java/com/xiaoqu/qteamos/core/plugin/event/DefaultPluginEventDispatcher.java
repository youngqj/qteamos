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
 * 默认插件事件分发器实现
 * 负责事件的注册、分发和管理，支持同步和异步事件处理
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.event;

import com.xiaoqu.qteamos.api.core.event.PluginEvent;
import com.xiaoqu.qteamos.api.core.event.PluginEventDispatcher;
import com.xiaoqu.qteamos.api.core.event.PluginEventListener;
import com.xiaoqu.qteamos.api.core.event.PriorityPluginEventListener;
import com.xiaoqu.qteamos.api.core.event.base.BaseEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;

@Component
public class DefaultPluginEventDispatcher implements PluginEventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginEventDispatcher.class);
    
    // 线程池配置
    private static final int CORE_POOL_SIZE = 2;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 100;
    private static final int KEEP_ALIVE_TIME = 60;
    
    // 按主题和类型分类的监听器映射
    private final Map<String, Map<String, Set<ListenerRegistration<?>>>> listenersByTopicAndType = new ConcurrentHashMap<>();
    
    // 按监听器类型分类的监听器映射
    private final Map<Class<?>, Set<ListenerRegistration<?>>> listenersByType = new ConcurrentHashMap<>();
    
    // 注册ID到注册信息的映射
    private final Map<String, ListenerRegistration<?>> registrationsById = new ConcurrentHashMap<>();
    
    // 异步事件处理线程池
    private final ExecutorService executorService;
    
    // 是否使用异步模式
    private volatile boolean asyncMode = true;
    
    /**
     * 构造函数
     */
    public DefaultPluginEventDispatcher() {
        // 创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "plugin-event-dispatcher-" + counter++);
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.executorService = executor;
        
        // 默认使用同步模式，确保测试稳定
        this.asyncMode = false;
        
        log.info("插件事件分发器初始化完成");
    }
    
    @Override
    public void publishEvent(PluginEvent event) {
        if (event == null) {
            log.warn("尝试发布空事件");
            return;
        }
        
        log.debug("发布事件: {}，主题: {}，类型: {}", event.getClass().getName(), event.getTopic(), event.getType());
        
        // 获取匹配的监听器
        Set<ListenerRegistration<?>> matchedListeners = getMatchedListenersForPluginEvent(event);
        
        if (matchedListeners.isEmpty()) {
            log.debug("没有匹配的监听器处理事件: {}", event);
            return;
        }
        
        log.debug("找到 {} 个匹配的监听器处理事件: {}", matchedListeners.size(), event);
        
        // 分发事件
        dispatchEvent(event, matchedListeners);
    }
    
    @Override
    public <T extends BaseEvent> void publishEvent(T event) {
        if (event == null) {
            log.warn("尝试发布空事件");
            return;
        }
        
        log.debug("发布通用事件: {}", event.getClass().getName());
        
        // 获取匹配的监听器
        Set<ListenerRegistration<?>> matchedListeners = getMatchedListenersForBaseEvent(event);
        
        if (matchedListeners.isEmpty()) {
            log.debug("没有匹配的监听器处理通用事件: {}", event);
            return;
        }
        
        log.debug("找到 {} 个匹配的监听器处理通用事件: {}", matchedListeners.size(), event);
        
        // 分发事件
        dispatchEvent(event, matchedListeners);
    }
    
    /**
     * 获取匹配插件事件的监听器
     *
     * @param event 事件
     * @return 匹配的监听器集合
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<ListenerRegistration<?>> getMatchedListenersForPluginEvent(PluginEvent event) {
        Set<ListenerRegistration<?>> result = new HashSet<>();
        String topic = event.getTopic();
        String type = event.getType();
        Class<?> eventClass = event.getClass();
        
        log.debug("查找事件监听器: 事件类型={}, 主题={}, 类型={}", eventClass.getName(), topic, type);
        
        // 使用通配符匹配获取主题和类型匹配的监听器
        result.addAll(getListenersForTopicAndType(topic, type));
        
        // 添加按类型匹配的监听器（包括父类类型的监听器）
        for (Map.Entry<Class<?>, Set<ListenerRegistration<?>>> entry : listenersByType.entrySet()) {
            Class<?> listenerEventType = entry.getKey();
            if (listenerEventType.isAssignableFrom(eventClass)) {
                log.debug("找到类型匹配的监听器: 监听器类型={}, 事件类型={}", listenerEventType.getName(), eventClass.getName());
                result.addAll(entry.getValue());
            }
        }
        
        log.debug("共找到 {} 个匹配的监听器", result.size());
        return result;
    }
    
    /**
     * 获取匹配基础事件的监听器
     *
     * @param event 事件
     * @return 匹配的监听器集合
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends BaseEvent> Set<ListenerRegistration<?>> getMatchedListenersForBaseEvent(T event) {
        Set<ListenerRegistration<?>> result = new HashSet<>();
        Class<?> eventClass = event.getClass();
        
        // 添加按类型匹配的监听器（包括父类类型的监听器）
        for (Map.Entry<Class<?>, Set<ListenerRegistration<?>>> entry : listenersByType.entrySet()) {
            Class<?> listenerEventType = entry.getKey();
            if (listenerEventType.isAssignableFrom(eventClass)) {
                result.addAll(entry.getValue());
            }
        }
        
        return result;
    }
    
    /**
     * 收集特定主题和类型的监听器
     *
     * @param result 结果集合
     * @param topic 主题
     * @param type 类型
     */
    private void collectListeners(Set<ListenerRegistration<?>> result, String topic, String type) {
        Map<String, Set<ListenerRegistration<?>>> topicListeners = listenersByTopicAndType.get(topic);
        if (topicListeners != null) {
            Set<ListenerRegistration<?>> typeListeners = topicListeners.get(type);
            if (typeListeners != null) {
                result.addAll(typeListeners);
            }
        }
    }
    
    /**
     * 分发事件到监听器
     *
     * @param event 事件
     * @param listeners 监听器集合
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void dispatchEvent(T event, Set<ListenerRegistration<?>> listeners) {
        // 排序监听器，按优先级从高到低
        List<ListenerRegistration<?>> sortedListeners = new ArrayList<>(listeners);
        sortedListeners.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        
        log.debug("准备分发事件 {} 给 {} 个监听器", event.getClass().getSimpleName(), sortedListeners.size());
        
        boolean isCancellable = false;
        boolean isCancelled = false;
        
        // 判断事件是否可取消
        if (event instanceof BaseEvent) {
            BaseEvent baseEvent = (BaseEvent) event;
            isCancellable = baseEvent.isCancellable();
            isCancelled = baseEvent.isCancelled();
        }
        
        for (ListenerRegistration<?> registration : sortedListeners) {
            // 检查事件是否已被取消
            if (isCancellable && isCancelled) {
                log.debug("事件已取消，停止分发: {}", event);
                break;
            }
            
            Class<?> eventType = registration.getEventType();
            PluginEventListener<?> listener = registration.getListener();
            
            // 类型安全检查
            if (eventType.isInstance(event)) {
                log.debug("分发事件 {} 到监听器 {}", event.getClass().getSimpleName(), listener.getClass().getSimpleName());
                
                try {
                    PluginEventListener typedListener = (PluginEventListener) listener;
                    
                    if (asyncMode && !registration.isSynchronous()) {
                        // 异步处理
                        executorService.submit(() -> {
                            try {
                                typedListener.onEvent(event);
                            } catch (Exception e) {
                                log.error("异步处理事件异常: {}, 监听器: {}", 
                                        event.getClass().getName(), listener.getClass().getName(), e);
                            }
                        });
                    } else {
                        // 同步处理
                        typedListener.onEvent(event);
                    }
                } catch (Exception e) {
                    log.error("事件处理异常: {}, 监听器: {}", 
                            event.getClass().getName(), listener.getClass().getName(), e);
                }
            } else {
                log.debug("事件类型不匹配，跳过监听器: 事件={}, 监听器期望类型={}", 
                        event.getClass().getName(), eventType.getName());
            }
            
            // 事件分发后，检查是否被取消
            if (event instanceof BaseEvent) {
                BaseEvent baseEvent = (BaseEvent) event;
                isCancelled = baseEvent.isCancelled();
            }
        }
    }
    
    @Override
    public <T extends PluginEvent> String registerEventListener(
            Class<T> eventType, 
            String[] topics, 
            String[] types, 
            PluginEventListener<T> listener) {
        
        if (eventType == null || listener == null || topics == null || types == null) {
            log.warn("注册事件监听器参数无效");
            return null;
        }
        
        // 创建注册信息
        String registrationId = UUID.randomUUID().toString();
        
        int priority = PriorityPluginEventListener.DEFAULT_PRIORITY;
        boolean synchronous = false;
        
        // 如果是优先级监听器，获取其优先级和同步标志
        if (listener instanceof PriorityPluginEventListener) {
            PriorityPluginEventListener<T> priorityListener = (PriorityPluginEventListener<T>) listener;
            priority = priorityListener.getPriority();
            synchronous = priorityListener.isSynchronous();
        }
        
        ListenerRegistration<T> registration = new ListenerRegistration<>(
                registrationId, eventType, listener, topics, types, priority, synchronous);
        
        // 注册到ID映射
        registrationsById.put(registrationId, registration);
        
        // 注册到类型映射
        Set<ListenerRegistration<?>> typeListeners = listenersByType.computeIfAbsent(
                eventType, k -> ConcurrentHashMap.newKeySet());
        typeListeners.add(registration);
        
        // 注册到主题和类型映射
        for (String topic : topics) {
            for (String type : types) {
                Map<String, Set<ListenerRegistration<?>>> topicListeners = listenersByTopicAndType.computeIfAbsent(
                        topic, k -> new ConcurrentHashMap<>());
                Set<ListenerRegistration<?>> typeListenersMap = topicListeners.computeIfAbsent(
                        type, k -> ConcurrentHashMap.newKeySet());
                typeListenersMap.add(registration);
            }
        }
        
        log.debug("注册事件监听器: eventType={}, topics={}, types={}, listener={}, priority={}, synchronous={}",
                eventType.getName(), Arrays.toString(topics), Arrays.toString(types), 
                listener.getClass().getName(), priority, synchronous);
        
        return registrationId;
    }
    
    @Override
    public <T extends PluginEvent> String registerEventListener(
            Class<T> eventType, 
            String topic, 
            String type, 
            PluginEventListener<T> listener) {
        
        if (topic == null || type == null) {
            log.warn("注册事件监听器的主题或类型为空");
            return null;
        }
        
        log.debug("注册单主题单类型的事件监听器: eventType={}, topic={}, type={}", 
                eventType.getName(), topic, type);
        String[] topics = new String[] { topic };
        String[] types = new String[] { type };
        
        return registerEventListener(eventType, topics, types, listener);
    }
    
    @Override
    public <T extends PluginEvent> String registerEventListener(
            Class<T> eventType, 
            PluginEventListener<T> listener) {
        
        return registerEventListener(eventType, new String[]{"*"}, new String[]{"*"}, listener);
    }
    
    @Override
    public <T extends PluginEvent> String registerPriorityEventListener(
            Class<T> eventType, 
            String[] topics, 
            String[] types, 
            PriorityPluginEventListener<T> listener) {
        
        return registerEventListener(eventType, topics, types, listener);
    }
    
    @Override
    public <T extends PluginEvent> String registerPriorityEventListener(
            Class<T> eventType, 
            String topic, 
            String type, 
            PriorityPluginEventListener<T> listener) {
        
        return registerEventListener(eventType, new String[]{topic}, new String[]{type}, listener);
    }
    
    @Override
    public <T extends PluginEvent> String registerPriorityEventListener(
            Class<T> eventType, 
            PriorityPluginEventListener<T> listener) {
        
        return registerEventListener(eventType, new String[]{"*"}, new String[]{"*"}, listener);
    }
    
    @Override
    public <T extends BaseEvent> String registerGenericEventListener(
            Class<T> eventType,
            PluginEventListener<T> listener) {
        
        if (eventType == null || listener == null) {
            log.warn("注册通用事件监听器参数无效");
            return null;
        }
        
        // 创建注册信息
        String registrationId = UUID.randomUUID().toString();
        
        int priority = PriorityPluginEventListener.DEFAULT_PRIORITY;
        boolean synchronous = false;
        
        // 如果是优先级监听器，获取其优先级和同步标志
        if (listener instanceof PriorityPluginEventListener) {
            PriorityPluginEventListener<T> priorityListener = (PriorityPluginEventListener<T>) listener;
            priority = priorityListener.getPriority();
            synchronous = priorityListener.isSynchronous();
        }
        
        ListenerRegistration<T> registration = new ListenerRegistration<>(
                registrationId, eventType, listener, new String[0], new String[0], priority, synchronous);
        
        // 注册到ID映射
        registrationsById.put(registrationId, registration);
        
        // 注册到类型映射
        Set<ListenerRegistration<?>> typeListeners = listenersByType.computeIfAbsent(
                eventType, k -> ConcurrentHashMap.newKeySet());
        typeListeners.add(registration);
        
        log.debug("注册通用事件监听器: eventType={}, listener={}, priority={}, synchronous={}",
                eventType.getName(), listener.getClass().getName(), priority, synchronous);
        
        return registrationId;
    }
    
    @Override
    public boolean unregisterEventListener(String registrationId) {
        if (registrationId == null) {
            return false;
        }
        
        ListenerRegistration<?> registration = registrationsById.remove(registrationId);
        if (registration == null) {
            return false;
        }
        
        // 从类型映射中移除
        Set<ListenerRegistration<?>> typeListeners = listenersByType.get(registration.getEventType());
        if (typeListeners != null) {
            typeListeners.remove(registration);
            if (typeListeners.isEmpty()) {
                listenersByType.remove(registration.getEventType());
            }
        }
        
        // 从主题和类型映射中移除
        for (String topic : registration.getTopics()) {
            for (String type : registration.getTypes()) {
                Map<String, Set<ListenerRegistration<?>>> topicListeners = listenersByTopicAndType.get(topic);
                if (topicListeners != null) {
                    Set<ListenerRegistration<?>> typeListenersMap = topicListeners.get(type);
                    if (typeListenersMap != null) {
                        typeListenersMap.remove(registration);
                        if (typeListenersMap.isEmpty()) {
                            topicListeners.remove(type);
                            if (topicListeners.isEmpty()) {
                                listenersByTopicAndType.remove(topic);
                            }
                        }
                    }
                }
            }
        }
        
        log.debug("注销事件监听器: {}", registrationId);
        return true;
    }
    
    @Override
    public <T extends PluginEvent> int unregisterEventListeners(Class<T> eventType) {
        if (eventType == null) {
            return 0;
        }
        
        Set<ListenerRegistration<?>> registrations = listenersByType.remove(eventType);
        if (registrations == null || registrations.isEmpty()) {
            return 0;
        }
        
        int count = registrations.size();
        
        // 从ID映射中移除
        for (ListenerRegistration<?> registration : registrations) {
            registrationsById.remove(registration.getRegistrationId());
            
            // 从主题和类型映射中移除
            for (String topic : registration.getTopics()) {
                for (String type : registration.getTypes()) {
                    Map<String, Set<ListenerRegistration<?>>> topicListeners = listenersByTopicAndType.get(topic);
                    if (topicListeners != null) {
                        Set<ListenerRegistration<?>> typeListenersMap = topicListeners.get(type);
                        if (typeListenersMap != null) {
                            typeListenersMap.remove(registration);
                            if (typeListenersMap.isEmpty()) {
                                topicListeners.remove(type);
                                if (topicListeners.isEmpty()) {
                                    listenersByTopicAndType.remove(topic);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        log.debug("注销事件类型所有监听器: {}, 数量: {}", eventType.getName(), count);
        return count;
    }
    
    @Override
    public void setAsyncMode(boolean async) {
        this.asyncMode = async;
        log.debug("设置事件分发模式: {}", async ? "异步" : "同步");
    }
    
    @Override
    public boolean isAsyncMode() {
        return asyncMode;
    }
    
    @Override
    public int getListenerCount() {
        return registrationsById.size();
    }
    
    @PreDestroy
    @Override
    public void shutdown() {
        log.info("关闭事件分发器");
        
        // 清空所有注册信息
        registrationsById.clear();
        listenersByType.clear();
        listenersByTopicAndType.clear();
        
        // 关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 监听器注册信息
     * 
     * @param <T> 事件类型
     */
    private static class ListenerRegistration<T> {
        private final String registrationId;
        private final Class<T> eventType;
        private final PluginEventListener<T> listener;
        private final String[] topics;
        private final String[] types;
        private final int priority;
        private final boolean synchronous;
        
        /**
         * 构造函数
         * 
         * @param registrationId 注册ID
         * @param eventType 事件类型
         * @param listener 监听器
         * @param topics 主题数组
         * @param types 类型数组
         */
        public ListenerRegistration(
                String registrationId,
                Class<T> eventType,
                PluginEventListener<T> listener,
                String[] topics,
                String[] types) {
            this(registrationId, eventType, listener, topics, types, 
                    PriorityPluginEventListener.DEFAULT_PRIORITY, false);
        }
        
        /**
         * 构造函数
         * 
         * @param registrationId 注册ID
         * @param eventType 事件类型
         * @param listener 监听器
         * @param topics 主题数组
         * @param types 类型数组
         * @param priority 优先级
         * @param synchronous 是否同步执行
         */
        public ListenerRegistration(
                String registrationId,
                Class<T> eventType,
                PluginEventListener<T> listener,
                String[] topics,
                String[] types,
                int priority,
                boolean synchronous) {
            this.registrationId = registrationId;
            this.eventType = eventType;
            this.listener = listener;
            this.topics = topics;
            this.types = types;
            this.priority = priority;
            this.synchronous = synchronous;
        }
        
        public String getRegistrationId() {
            return registrationId;
        }
        
        public Class<T> getEventType() {
            return eventType;
        }
        
        public PluginEventListener<T> getListener() {
            return listener;
        }
        
        public String[] getTopics() {
            return topics;
        }
        
        public String[] getTypes() {
            return types;
        }
        
        public int getPriority() {
            return priority;
        }
        
        public boolean isSynchronous() {
            return synchronous;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerRegistration<?> that = (ListenerRegistration<?>) o;
            return Objects.equals(registrationId, that.registrationId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(registrationId);
        }
    }
    
    /**
     * 匹配非通配符字符串，支持"*"作为通配符
     *
     * @param pattern 模式字符串，可包含"*"通配符
     * @param text 待匹配文本
     * @return 是否匹配
     */
    private boolean matchWildcard(String pattern, String text) {
        if (pattern == null || text == null) {
            return false;
        }
        
        // 特殊情况：模式为"*"，匹配任何内容
        if ("*".equals(pattern)) {
            return true;
        }
        
        // 特殊情况：完全相等
        return pattern.equals(text);
    }
    
    /**
     * 获取精确或通配符匹配特定主题和类型的监听器
     *
     * @param topic 当前事件主题
     * @param type 当前事件类型
     * @return 匹配的监听器集合
     */
    public Set<ListenerRegistration<?>> getListenersForTopicAndType(String topic, String type) {
        Set<ListenerRegistration<?>> result = ConcurrentHashMap.newKeySet();
        
        log.debug("查找主题={}, 类型={}的监听器", topic, type);
        
        // 遍历所有注册的主题
        for (Map.Entry<String, Map<String, Set<ListenerRegistration<?>>>> topicEntry : listenersByTopicAndType.entrySet()) {
            String registeredTopic = topicEntry.getKey();
            
            // 只有当注册主题是通配符或精确匹配时才进一步检查
            if (matchWildcard(registeredTopic, topic)) {
                Map<String, Set<ListenerRegistration<?>>> typeListenersMap = topicEntry.getValue();
                
                // 遍历所有注册的类型
                for (Map.Entry<String, Set<ListenerRegistration<?>>> typeEntry : typeListenersMap.entrySet()) {
                    String registeredType = typeEntry.getKey();
                    
                    // 只有当注册类型是通配符或精确匹配时才添加监听器
                    if (matchWildcard(registeredType, type)) {
                        log.debug("匹配到监听器: 注册主题={}, 注册类型={}, 事件主题={}, 事件类型={}",
                                registeredTopic, registeredType, topic, type);
                        result.addAll(typeEntry.getValue());
                    }
                }
            }
        }
        
        return result;
    }
} 