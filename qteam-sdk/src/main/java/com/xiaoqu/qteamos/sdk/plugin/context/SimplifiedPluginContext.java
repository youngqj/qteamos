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
 * 简化的插件上下文
 * 包装core层的PluginContext实现，为插件开发者提供更友好的接口
 *
 * @author yangqijun
 * @date 2024-07-25
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.plugin.context;

import com.xiaoqu.qteamos.api.core.plugin.PluginContext;
import com.xiaoqu.qteamos.api.core.plugin.PluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简化的插件上下文
 * 为插件开发者提供更友好的接口，简化常见操作
 */
public class SimplifiedPluginContext {
    
    private static final Logger log = LoggerFactory.getLogger(SimplifiedPluginContext.class);
    
    private final PluginContext originalContext;
    private final Properties configProperties = new Properties();
    private final File configFile;
    
    /**
     * 构造函数
     *
     * @param originalContext 原始插件上下文
     */
    public SimplifiedPluginContext(PluginContext originalContext) {
        this.originalContext = originalContext;
        
        // 创建配置文件
        String dataPath = originalContext.getDataFolderPath();
        this.configFile = new File(dataPath, "plugin.properties");
        
        // 加载配置
        loadConfig();
    }
    
    /**
     * 获取插件ID
     *
     * @return 插件ID
     */
    public String getPluginId() {
        return originalContext.getPluginId();
    }
    
    /**
     * 获取插件版本
     *
     * @return 插件版本
     */
    public String getPluginVersion() {
        return originalContext.getPluginVersion();
    }
    
    /**
     * 获取插件数据目录
     *
     * @return 插件数据目录
     */
    public File getDataFolder() {
        return new File(originalContext.getDataFolderPath());
    }
    
    /**
     * 获取配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    public String getConfig(String key) {
        // 先从插件上下文获取
        String value = originalContext.getConfig(key);
        
        // 如果没有，则从本地配置获取
        if (value == null || value.isEmpty()) {
            value = configProperties.getProperty(key);
        }
        
        return value;
    }
    
    /**
     * 获取配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 设置配置值
     *
     * @param key 配置键
     * @param value 配置值
     */
    public void setConfig(String key, String value) {
        // 设置到插件上下文
        originalContext.setConfig(key, value);
        
        // 同时保存到本地配置
        configProperties.setProperty(key, value);
        saveConfig();
    }
    
    /**
     * 获取整数配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 整数配置值
     */
    public int getIntConfig(String key, int defaultValue) {
        String value = getConfig(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("配置项 {} 不是有效的整数: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取布尔配置值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 布尔配置值
     */
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getConfig(key);
        if (value != null && !value.isEmpty()) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    /**
     * 获取原始上下文
     *
     * @return 原始上下文
     */
    public PluginContext getOriginalContext() {
        return originalContext;
    }
    
    /**
     * 注册事件监听器
     *
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @param <T> 事件泛型
     */
    public <T> void addEventListener(Class<T> eventType, PluginEventListener<T> listener) {
        originalContext.addEventListener(eventType, listener);
    }
    
    /**
     * 发布事件
     *
     * @param event 事件对象
     */
    public void publishEvent(Object event) {
        originalContext.publishEvent(event);
    }
    
    /**
     * 获取服务
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 服务实例
     */
    public <T> T getService(Class<T> serviceClass) {
        return originalContext.getService(serviceClass);
    }
    
    /**
     * 获取数据库连接（已废弃）
     * 插件应该使用标准Spring Boot方式：@Autowired注入Mapper
     *
     * @return 数据库连接
     * @deprecated 请使用标准Spring Boot方式：@Autowired注入Mapper
     */
    @Deprecated
    public Object getConnection() {
        log.warn("getConnection方法已废弃，请使用标准Spring Boot方式：@Autowired注入Mapper");
        try {
            Object dataSourceService = originalContext.getService(
                    Class.forName("com.xiaoqu.qteamos.core.api.datasource.DataSourceService"));
            if (dataSourceService != null) {
                Method getConnection = dataSourceService.getClass().getMethod("getConnection");
                return getConnection.invoke(dataSourceService);
            }
        } catch (Exception e) {
            log.error("获取数据库连接失败", e);
        }
        return null;
    }
    
    /**
     * 创建代理服务
     * 当找不到服务时，创建一个空实现代理，避免NPE
     *
     * @param serviceClass 服务类型
     * @param <T> 服务泛型
     * @return 服务代理
     */
    @SuppressWarnings("unchecked")
    public <T> T createServiceProxy(Class<T> serviceClass) {
        T service = originalContext.getService(serviceClass);
        if (service != null) {
            return service;
        }
        
        // 创建一个代理实现
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                new SafeServiceInvocationHandler(serviceClass)
        );
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                configProperties.load(fis);
                log.debug("加载插件配置: {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("加载插件配置失败", e);
            }
        }
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        try {
            // 确保配置目录存在
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                log.error("创建配置目录失败: {}", parentDir.getAbsolutePath());
                return;
            }
            
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                configProperties.store(fos, "Plugin Configuration");
                log.debug("保存插件配置: {}", configFile.getAbsolutePath());
            }
        } catch (IOException e) {
            log.error("保存插件配置失败", e);
        }
    }
    
    /**
     * 安全的服务调用处理器
     * 避免服务不存在时抛出NPE
     */
    private static class SafeServiceInvocationHandler implements InvocationHandler {
        
        private final Class<?> serviceClass;
        private final Map<Method, Object> defaultReturnValues = new ConcurrentHashMap<>();
        
        public SafeServiceInvocationHandler(Class<?> serviceClass) {
            this.serviceClass = serviceClass;
            
            // 为每个方法准备默认返回值
            for (Method method : serviceClass.getMethods()) {
                Class<?> returnType = method.getReturnType();
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) {
                        defaultReturnValues.put(method, false);
                    } else if (returnType == char.class) {
                        defaultReturnValues.put(method, '\0');
                    } else if (returnType == byte.class || 
                               returnType == short.class || 
                               returnType == int.class || 
                               returnType == long.class ||
                               returnType == float.class ||
                               returnType == double.class) {
                        defaultReturnValues.put(method, 0);
                    }
                } else {
                    defaultReturnValues.put(method, null);
                }
            }
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            log.warn("服务 {} 的方法 {} 被调用但服务不存在", serviceClass.getName(), method.getName());
            return defaultReturnValues.get(method);
        }
    }
} 