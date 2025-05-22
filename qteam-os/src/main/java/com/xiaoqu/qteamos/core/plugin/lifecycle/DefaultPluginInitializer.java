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

package com.xiaoqu.qteamos.core.plugin.lifecycle;

import com.xiaoqu.qteamos.api.core.cache.CacheService;
import com.xiaoqu.qteamos.api.core.config.ConfigService;
import com.xiaoqu.qteamos.api.core.datasource.DataSourceService;
import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.PluginContext;
import com.xiaoqu.qteamos.api.core.plugin.PluginEventListener;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInitializer;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.api.core.event.PluginEventDispatcher;
import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginInitializedEvent;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.Event;
import com.xiaoqu.qteamos.core.plugin.event.GenericEvent;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.event.adapter.PluginEventListenerAdapter;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.service.ConfigServiceProvider;
import com.xiaoqu.qteamos.core.plugin.service.PluginPersistenceService;
import com.xiaoqu.qteamos.core.plugin.service.ServiceLocator;
import com.xiaoqu.qteamos.core.plugin.adapter.PluginInfoAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认插件初始化器实现
 * 负责插件的初始化过程，包括创建上下文和调用插件的init方法
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
@Component
public class DefaultPluginInitializer implements PluginInitializer {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginInitializer.class);
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private PluginStateManager stateManager;
    
    @Autowired
    private PluginPersistenceService persistenceService;
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private DefaultPluginLoader pluginLoader;
    
    @Autowired
    private ServiceLocator serviceLocator;
    
    @Autowired
    private ConfigServiceProvider configServiceProvider;
    
    @Autowired
    private PluginEventDispatcher eventDispatcher;
    
    @Autowired
    private com.xiaoqu.qteamos.core.plugin.adapter.PluginInfoAdapter pluginInfoAdapter;
    
    // 记录已初始化的插件
    private final Set<String> initializedPlugins = ConcurrentHashMap.newKeySet();
    
    // 存储插件上下文实例，用于在卸载时清理资源
    private final Map<String, SimplePluginContext> pluginContexts = new ConcurrentHashMap<>();
    
    @Override
    public boolean initialize(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        // 检查插件是否已初始化
        if (initializedPlugins.contains(pluginId)) {
            log.info("插件已经初始化: {}", pluginId);
            return true;
        }
        
        log.info("开始初始化插件: {}", pluginId);
        
        long startTime = System.currentTimeMillis();
        
        Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.error("插件不存在: {}", pluginId);
            return false;
        }
        
        com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo = optPluginInfo.get();
        String version = pluginInfo.getVersion();
        
        if (pluginInfo.getState() != PluginState.LOADED) {
            log.error("插件状态不正确，无法初始化: {}, 当前状态: {}", pluginId, pluginInfo.getState());
            return false;
        }
        
        Optional<Plugin> pluginInstance = pluginLoader.getPluginInstance(pluginId);
        if (pluginInstance.isEmpty()) {
            log.error("插件实例不存在: {}", pluginId);
            return false;
        }
        
        try {
            // 创建插件上下文
            PluginContext context = createContext(pluginId);
            
            // 调用插件初始化方法
            boolean success = invokeInitMethod(pluginInstance.get(), context);
            if (!success) {
                log.error("插件初始化失败: {}", pluginId);
                pluginInfo.setState(PluginState.FAILED);
                stateManager.recordStateChange(pluginId, PluginState.FAILED);
                
                // 发布初始化失败事件
                publishErrorEvent(pluginId, version, new PluginLifecycleException("插件初始化失败"));
                
                return false;
            }
            
            // 更新插件状态
            pluginInfo.setState(PluginState.INITIALIZED);
            stateManager.recordStateChange(pluginId, PluginState.INITIALIZED);
            
            // 记录已初始化的插件
            initializedPlugins.add(pluginId);
            
            // 计算初始化耗时
            long initTime = System.currentTimeMillis() - startTime;
            
            // 发布插件初始化事件（使用新的事件机制）
            publishInitializedEvent(pluginId, version, pluginInfo, initTime);
            
            log.info("插件初始化成功: {}, 耗时: {}ms", pluginId, initTime);
            return true;
        } catch (Exception e) {
            String message = String.format("初始化插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            pluginInfo.setState(PluginState.FAILED);
            stateManager.recordStateChange(pluginId, PluginState.FAILED);
            
            // 发布错误事件
            publishErrorEvent(pluginId, version, e);
            
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public PluginContext createContext(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        log.debug("为插件创建上下文: {}", pluginId);
        
        Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            throw new PluginLifecycleException("插件不存在: " + pluginId);
        }
        
        com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo = optPluginInfo.get();
        
        // 获取插件配置
        Map<String, String> configs = persistenceService.getPluginConfig(pluginId);
        if (configs == null) {
            configs = new HashMap<>();
        }
        
        // 获取类加载器
        Optional<ClassLoader> classLoaderOpt = pluginLoader.getPluginClassLoader(pluginId);
        if (classLoaderOpt.isEmpty()) {
            throw new PluginLifecycleException("插件类加载器不存在: " + pluginId);
        }
        
        ClassLoader classLoader = classLoaderOpt.get();
        
        // 创建上下文
        SimplePluginContext context = new SimplePluginContext(pluginId, pluginInfo.getVersion(), classLoader);
        
        // 设置插件数据目录
        File dataFolder = persistenceService.getPluginDataDir(pluginId);
        context.setDataFolderPath(dataFolder.getAbsolutePath());
        
        // 设置插件配置
        context.setConfigs(configs);
        
        // 设置服务
        context.setEventBus(eventBus);
        context.setServiceLocator(serviceLocator);
        context.setConfigServiceProvider(configServiceProvider);
        
        // 保存上下文实例，用于后续清理
        pluginContexts.put(pluginId, context);
        
        log.debug("插件上下文创建成功: {}", pluginId);
        return context;
    }
    
    @Override
    public boolean invokeInitMethod(Plugin plugin, PluginContext context) throws PluginLifecycleException {
        if (plugin == null) {
            throw new PluginLifecycleException("插件实例不能为空");
        }
        
        if (context == null) {
            throw new PluginLifecycleException("插件上下文不能为空");
        }
        
        String pluginId = context.getPluginId();
        log.debug("调用插件初始化方法: {}", pluginId);
        
        try {
            // 调用插件的init方法
            plugin.init(context);
            log.debug("插件初始化方法调用成功: {}", pluginId);
            return true;
        } catch (Exception e) {
            String message = String.format("调用插件初始化方法失败: %s, 错误: %s", pluginId, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public boolean isInitialized(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return false;
        }
        
        return initializedPlugins.contains(pluginId);
    }
    
    /**
     * 清理插件上下文和相关资源
     * 应在插件卸载时调用
     *
     * @param pluginId 插件ID
     */
    public void cleanupPluginContext(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return;
        }
        
        SimplePluginContext context = pluginContexts.remove(pluginId);
        if (context != null) {
            log.info("清理插件上下文资源: {}", pluginId);
            
            // 清理注册的事件监听器
            context.cleanup();
            
            // 从已初始化插件列表中移除
            initializedPlugins.remove(pluginId);
            
            log.debug("插件上下文资源清理完成: {}", pluginId);
        }
    }
    
    /**
     * 获取插件上下文
     *
     * @param pluginId 插件ID
     * @return 插件上下文，如果不存在返回null
     */
    public SimplePluginContext getPluginContext(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return null;
        }
        
        return pluginContexts.get(pluginId);
    }
    
    /**
     * 发布插件初始化事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param pluginInfo 插件信息
     * @param initTime 初始化时间
     */
    private void publishInitializedEvent(String pluginId, String version, 
                                        com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo, 
                                        long initTime) {
        try {
            // 创建新的初始化事件
            List<String> resolvedDependencies = new ArrayList<>();
            if (pluginInfo.getDescriptor() != null && pluginInfo.getDescriptor().getDependencies() != null) {
                pluginInfo.getDescriptor().getDependencies().forEach(dep -> 
                    resolvedDependencies.add(dep.getPluginId() + ":" + dep.getVersion())
                );
            }
            
            PluginInitializedEvent event = new PluginInitializedEvent(
                pluginId, 
                version, 
                pluginInfoAdapter.toApiPluginInfo(pluginInfo), 
                initTime, 
                resolvedDependencies
            );
            
            // 使用新的事件分发器
            eventDispatcher.publishEvent(event);
            
            log.debug("发布插件初始化事件: {}, 版本: {}, 初始化时间: {}ms", pluginId, version, initTime);
            
            // 不再发布到旧的事件总线，确保使用统一的事件机制
        } catch (Exception e) {
            log.error("发布插件初始化事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发布插件错误事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param error 错误信息
     */
    private void publishErrorEvent(String pluginId, String version, Throwable error) {
        try {
            // 创建新的错误事件
            com.xiaoqu.qteamos.api.core.event.PluginEvent event = new com.xiaoqu.qteamos.api.core.event.PluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.ERROR,
                "system",
                pluginId,
                version,
                error,
                false) {};
                
            // 使用新的事件分发器
            eventDispatcher.publishEvent(event);
            
            log.debug("发布插件错误事件: {}, 版本: {}, 错误: {}", pluginId, version, error.getMessage());
            
            // 不再发布到旧的事件总线，确保使用统一的事件机制
        } catch (Exception e) {
            log.error("发布插件错误事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 简化的插件上下文实现类
     */
    private static class SimplePluginContext implements PluginContext {
        private final String pluginId;
        private final String version;
        private final ClassLoader classLoader;
        private String dataFolderPath;
        private Map<String, String> configs;
        private EventBus eventBus;
        private ServiceLocator serviceLocator;
        private ConfigServiceProvider configServiceProvider;
        
        // 保存注册的事件监听器，用于后续移除
        private final Map<Class<?>, Map<PluginEventListener<?>, PluginEventListenerAdapter<?>>> listenerAdapters = new ConcurrentHashMap<>();
        
        /**
         * 构造函数
         *
         * @param pluginId 插件ID
         * @param version 插件版本
         * @param classLoader 类加载器
         */
        public SimplePluginContext(String pluginId, String version, ClassLoader classLoader) {
            this.pluginId = pluginId;
            this.version = version;
            this.classLoader = classLoader;
            this.configs = new HashMap<>();
        }
        
        @Override
        public String getPluginId() {
            return pluginId;
        }
        
        @Override
        public String getPluginVersion() {
            return version;
        }
        
        @Override
        public String getConfig(String key) {
            return configs.get(key);
        }
        
        @Override
        public String getConfig(String key, String defaultValue) {
            return configs.getOrDefault(key, defaultValue);
        }
        
        @Override
        public Map<String, String> getAllConfigs() {
            return new HashMap<>(configs);
        }
        
        @Override
        public void setConfig(String key, String value) {
            configs.put(key, value);
            // 如果配置服务可用，将更新持久化
            if (configServiceProvider != null) {
                configServiceProvider.savePluginConfig(pluginId, key, value);
            }
        }
        
        @Override
        public String getDataFolderPath() {
            return dataFolderPath;
        }
        
        @Override
        public CacheService getCacheService() {
            return getService(CacheService.class);
        }
        
        @Override
        public DataSourceService getDataSourceService() {
            return getService(DataSourceService.class);
        }
        
        @Override
        public ConfigService getConfigService() {
            return getService(ConfigService.class);
        }
        
        @Override
        public <T> T getService(Class<T> serviceClass) {
            if (serviceLocator == null) {
                return null;
            }
            return serviceLocator.getService(serviceClass);
        }
        
        @Override
        public void publishEvent(Object event) {
            if (event == null) {
                return;
            }
            
            try {
                if (event instanceof com.xiaoqu.qteamos.api.core.event.PluginEvent) {
                    // 如果是新的事件类型，统一使用新的事件总线发布
                    com.xiaoqu.qteamos.api.core.event.PluginEvent pluginEvent = 
                        (com.xiaoqu.qteamos.api.core.event.PluginEvent) event;
                    eventBus.postEvent(new GenericEvent(pluginId, "plugin.generic", pluginEvent));
                    
                    log.debug("插件[{}]发布事件: {}", pluginId, pluginEvent.getClass().getName());
                } else {
                    // 兼容旧的事件发布方式
                    eventBus.postEvent(new GenericEvent(pluginId, "plugin.generic", event));
                    log.debug("插件[{}]发布通用事件: {}", pluginId, event.getClass().getName());
                }
            } catch (Exception e) {
                log.error("插件[{}]发布事件失败: {}", pluginId, e.getMessage());
            }
        }
        
        @Override
        public <T> void addEventListener(Class<T> eventType, PluginEventListener<T> listener) {
            if (eventBus == null) {
                LoggerFactory.getLogger(SimplePluginContext.class)
                    .warn("事件总线未初始化，无法添加事件监听器: {}, {}", eventType.getName(), listener);
                return;
            }
            
            if (eventType == null || listener == null) {
                LoggerFactory.getLogger(SimplePluginContext.class)
                    .warn("事件类型或监听器为空，无法添加事件监听器");
                return;
            }
            
            // 创建适配器
            PluginEventListenerAdapter<T> adapter = new PluginEventListenerAdapter<>(pluginId, eventType, listener);
            
            // 注册到事件总线
            eventBus.registerHandler(adapter);
            
            // 保存适配器，用于后续移除
            listenerAdapters
                .computeIfAbsent(eventType, k -> new ConcurrentHashMap<>())
                .put(listener, adapter);
            
            LoggerFactory.getLogger(SimplePluginContext.class)
                .debug("添加事件监听器: 插件={}, 事件类型={}, 监听器={}", pluginId, eventType.getName(), listener.getClass().getName());
        }
        
        @Override
        public <T> void removeEventListener(Class<T> eventType, PluginEventListener<T> listener) {
            if (eventBus == null) {
                LoggerFactory.getLogger(SimplePluginContext.class)
                    .warn("事件总线未初始化，无法移除事件监听器: {}, {}", eventType.getName(), listener);
                return;
            }
            
            if (eventType == null || listener == null) {
                LoggerFactory.getLogger(SimplePluginContext.class)
                    .warn("事件类型或监听器为空，无法移除事件监听器");
                return;
            }
            
            // 查找适配器
            Map<PluginEventListener<?>, PluginEventListenerAdapter<?>> typeListeners = listenerAdapters.get(eventType);
            if (typeListeners != null) {
                PluginEventListenerAdapter<?> adapter = typeListeners.remove(listener);
                if (adapter != null) {
                    // 从事件总线移除
                    eventBus.unregisterHandler(adapter);
                    
                    LoggerFactory.getLogger(SimplePluginContext.class)
                        .debug("移除事件监听器: 插件={}, 事件类型={}, 监听器={}", 
                                pluginId, eventType.getName(), listener.getClass().getName());
                    
                    // 如果该类型的监听器为空，移除整个映射
                    if (typeListeners.isEmpty()) {
                        listenerAdapters.remove(eventType);
                    }
                }
            }
        }
        
        /**
         * 设置数据目录路径
         *
         * @param dataFolderPath 数据目录路径
         */
        public void setDataFolderPath(String dataFolderPath) {
            this.dataFolderPath = dataFolderPath;
        }
        
        /**
         * 设置配置项
         *
         * @param configs 配置项
         */
        public void setConfigs(Map<String, String> configs) {
            if (configs != null) {
                this.configs = new HashMap<>(configs);
            }
        }
        
        /**
         * 设置事件总线
         *
         * @param eventBus 事件总线
         */
        public void setEventBus(EventBus eventBus) {
            this.eventBus = eventBus;
        }
        
        /**
         * 设置服务定位器
         *
         * @param serviceLocator 服务定位器
         */
        public void setServiceLocator(ServiceLocator serviceLocator) {
            this.serviceLocator = serviceLocator;
        }
        
        /**
         * 设置配置服务提供者
         *
         * @param configServiceProvider 配置服务提供者
         */
        public void setConfigServiceProvider(ConfigServiceProvider configServiceProvider) {
            this.configServiceProvider = configServiceProvider;
        }
        
        /**
         * 清理所有注册的事件监听器
         * 应在插件卸载前调用
         */
        public void cleanup() {
            if (eventBus != null && !listenerAdapters.isEmpty()) {
                LoggerFactory.getLogger(SimplePluginContext.class)
                    .info("清理插件 {} 注册的所有事件监听器", pluginId);
                
                // 遍历所有注册的适配器，从事件总线中移除
                for (Map<PluginEventListener<?>, PluginEventListenerAdapter<?>> typeListeners : listenerAdapters.values()) {
                    for (PluginEventListenerAdapter<?> adapter : typeListeners.values()) {
                        eventBus.unregisterHandler(adapter);
                    }
                }
                
                // 清空监听器映射
                listenerAdapters.clear();
            }
        }
    }
} 