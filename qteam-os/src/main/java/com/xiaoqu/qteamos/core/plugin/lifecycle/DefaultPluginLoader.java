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

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginLoader;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.api.core.event.PluginEventDispatcher;
import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginLoadedEvent;
import com.xiaoqu.qteamos.core.plugin.adapter.PluginInfoAdapter;
import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader;
import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoaderFactory;
import com.xiaoqu.qteamos.core.plugin.manager.DependencyResolver;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptorLoader;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认插件加载器实现
 * 负责插件的类加载和实例创建过程
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
@Component
public class DefaultPluginLoader implements PluginLoader {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginLoader.class);
    
    // 缓存插件类加载器
    private final Map<String, ClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();
    
    // 缓存插件实例
    private final Map<String, Plugin> pluginInstances = new ConcurrentHashMap<>();
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private DependencyResolver dependencyResolver;
    
    @Autowired
    private DynamicClassLoaderFactory classLoaderFactory;
    
    @Autowired
    private PluginStateManager stateManager;
    
    @Autowired
    private PluginEventDispatcher eventDispatcher;
    
    @Autowired
    private PluginInfoAdapter pluginInfoAdapter;
    
    @Autowired
    private PluginDescriptorLoader descriptorLoader;
    
    @Autowired
    private DefaultPluginInitializer pluginInitializer;
    
    @Override
    public boolean load(PluginInfo apiPluginInfo) throws PluginLifecycleException {
        if (apiPluginInfo == null) {
            throw new PluginLifecycleException("插件信息不能为空");
        }
        
        String pluginId = apiPluginInfo.getPluginId();
        String version = apiPluginInfo.getVersion();
        log.info("开始加载插件: {}, 版本: {}", pluginId, version);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 转换为内部PluginInfo对象
        com.xiaoqu.qteamos.core.plugin.running.PluginInfo corePluginInfo =   pluginInfoAdapter.toCorePluginInfo(apiPluginInfo);
        
        try {
            // 检查依赖
            log.debug("检查插件[{}]依赖...", pluginId);
            if (!dependencyResolver.checkDependencies(corePluginInfo.getDescriptor())) {
                log.error("插件依赖检查失败: {}", pluginId);
                corePluginInfo.setState(PluginState.DEPENDENCY_FAILED);
                stateManager.recordStateChange(pluginId, PluginState.DEPENDENCY_FAILED);
                
                // 发布依赖检查失败事件
                List<String> missingDependencies = new ArrayList<>();
                missingDependencies.add("未能满足依赖要求"); // 简化处理，实际应从dependencyResolver获取
                publishDependencyFailedEvent(pluginId, version, missingDependencies);
                
                return false;
            }
            log.debug("插件[{}]依赖检查通过", pluginId);
            
            // 检查类加载器
            log.debug("检查插件[{}]类加载器...", pluginId);
            DynamicClassLoader classLoader = corePluginInfo.getClassLoader();
            if (classLoader == null) {
                // 如果类加载器为空但有JAR路径，创建一个新的类加载器
                if (corePluginInfo.getJarPath() != null && corePluginInfo.getJarPath().toFile().exists()) {
                    log.info("插件[{}]类加载器为空，但发现有效的JAR路径，正在创建类加载器...", pluginId);
                    try {
                        classLoader = classLoaderFactory.createClassLoader(pluginId, corePluginInfo.getJarPath().toFile());
                        corePluginInfo.setClassLoader(classLoader);
                        log.info("为插件[{}]创建了类加载器: {}", pluginId, classLoader.getClass().getName());
                    } catch (Exception e) {
                        log.error("为插件[{}]创建类加载器失败: {}", pluginId, e.getMessage(), e);
                        corePluginInfo.setState(PluginState.ERROR);
                        corePluginInfo.setErrorMessage("创建类加载器失败: " + e.getMessage());
                        stateManager.recordFailure(pluginId, "创建类加载器失败: " + e.getMessage());
                        
                        // 发布错误事件
                        publishErrorEvent(pluginId, version, e);
                        
                        return false;
                    }
                } else {
                    log.error("插件[{}]类加载器为空且无有效JAR路径", pluginId);
                    corePluginInfo.setState(PluginState.ERROR);
                    corePluginInfo.setErrorMessage("类加载器为空且无有效JAR路径");
                    stateManager.recordFailure(pluginId, "类加载器为空且无有效JAR路径");
                    
                    // 发布错误事件
                    publishErrorEvent(pluginId, version, new IllegalStateException("类加载器为空且无有效JAR路径"));
                    
                    return false;
                }
            }
            log.debug("插件[{}]类加载器正常: {}", pluginId, classLoader.getClass().getName());
            
            // 缓存类加载器
            pluginClassLoaders.put(pluginId, classLoader);
            
            // 创建插件实例
            log.debug("开始为插件[{}]创建实例...", pluginId);
            Plugin pluginInstance = null;
            try {
                pluginInstance = createPluginInstance(corePluginInfo);
                log.debug("插件[{}]实例创建成功: {}", pluginId, 
                    pluginInstance != null ? pluginInstance.getClass().getName() : "null");
            } catch (Exception e) {
                log.error("创建插件[{}]实例失败: {}", pluginId, e.getMessage(), e);
                corePluginInfo.setState(PluginState.ERROR);
                corePluginInfo.setErrorMessage("创建实例失败: " + e.getMessage());
                stateManager.recordFailure(pluginId, "创建实例失败: " + e.getMessage());
                return false;
            }
            
            if (pluginInstance == null) {
                log.error("无法创建插件实例: {}", pluginId);
                corePluginInfo.setState(PluginState.FAILED);
                stateManager.recordStateChange(pluginId, PluginState.FAILED);
                return false;
            }
            
            // 缓存插件实例
            log.debug("缓存插件[{}]实例...", pluginId);
            pluginInstances.put(pluginId, pluginInstance);
            
            // 更新插件状态
            corePluginInfo.setLoadTime(new Date());
            corePluginInfo.setState(PluginState.LOADED);
            
            // 先注册到插件注册表
            pluginRegistry.registerPlugin(corePluginInfo);
            
            // 再记录状态变化
            stateManager.recordStateChange(pluginId, PluginState.LOADED);
            
            // 计算加载时间和资源统计
            long loadTime = System.currentTimeMillis() - startTime;
            int loadedClassCount = classLoader.getLoadedClassCount();
            int loadedResourceCount = classLoader.getResourceLoadCount();
            
            // 发布插件加载事件
            publishLoadedEvent(pluginId, version, loadedClassCount, loadedResourceCount, loadTime);
            
            log.info("插件[{}]加载成功", pluginId);
            return true;
        } catch (Exception e) {
            String message = String.format("加载插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            corePluginInfo.setState(PluginState.ERROR);
            corePluginInfo.setErrorMessage(message);
            stateManager.recordFailure(pluginId, message);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public PluginInfo loadFromFile(File pluginFile) throws PluginLifecycleException {
        if (pluginFile == null || !pluginFile.exists()) {
            throw new PluginLifecycleException("插件文件不存在");
        }
        
        log.info("从文件加载插件: {}", pluginFile.getAbsolutePath());
        
        try {
            // 确定是从JAR文件还是目录加载
            PluginDescriptor descriptor;
            File actualPluginFile;
            
            if (pluginFile.isDirectory()) {
                // 从目录加载
                log.info("检测到插件目录，尝试从目录加载: {}", pluginFile.getAbsolutePath());
                
                // 先尝试在目录下查找plugin.yml
                File descriptorFile = new File(pluginFile, "plugin.yml");
                if (descriptorFile.exists() && descriptorFile.isFile()) {
                    descriptor = descriptorLoader.loadFromFile(descriptorFile);
                    
                    // 查找目录下的JAR文件
                    File[] jarFiles = pluginFile.listFiles((dir, name) -> name.endsWith(".jar"));
                    if (jarFiles != null && jarFiles.length > 0) {
                        // 使用找到的第一个JAR文件
                        actualPluginFile = jarFiles[0];
                        log.info("使用目录中的JAR文件: {}", actualPluginFile.getName());
                    } else {
                        // 如果没有JAR文件，则使用目录本身
                        actualPluginFile = pluginFile;
                        log.info("目录中没有JAR文件，直接使用目录");
                    }
                } else {
                    throw new PluginLifecycleException("插件目录中未找到plugin.yml文件: " + pluginFile.getAbsolutePath());
                }
            } else {
                // 从JAR文件加载
                descriptor = descriptorLoader.loadFromJar(pluginFile);
                actualPluginFile = pluginFile;
            }
            
            String pluginId = descriptor.getPluginId();
            
            // 检查插件是否已加载
            if (pluginRegistry.hasPlugin(pluginId)) {
                log.warn("插件已经加载: {}", pluginId);
                return pluginInfoAdapter.toApiPluginInfo(
                        pluginRegistry.getPlugin(pluginId).orElse(null));
            }
            
            // 创建类加载器
            DynamicClassLoader classLoader = classLoaderFactory.createClassLoader(pluginId, actualPluginFile);
            
            // 创建插件信息
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo = 
                    com.xiaoqu.qteamos.core.plugin.running.PluginInfo.builder()
                    .descriptor(descriptor)
                    .file(pluginFile)
                    .classLoader(classLoader)
                    .state(PluginState.CREATED)
                    .build();
            
            // 加载插件
            boolean success = load(pluginInfoAdapter.toApiPluginInfo(pluginInfo));
            
            if (success) {
                return pluginInfoAdapter.toApiPluginInfo(
                        pluginRegistry.getPlugin(pluginId).orElse(null));
            } else {
                throw new PluginLifecycleException("加载插件失败: " + pluginId);
            }
        } catch (Exception e) {
            String message = String.format("从文件加载插件失败: %s, 错误: %s", 
                    pluginFile.getAbsolutePath(), e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public PluginInfo loadFromPath(String filePath) throws PluginLifecycleException {
        if (filePath == null || filePath.isEmpty()) {
            throw new PluginLifecycleException("插件文件路径不能为空");
        }
        
        File pluginFile = new File(filePath);
        return loadFromFile(pluginFile);
    }
    
    @Override
    public boolean unload(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        log.info("开始卸载插件: {}", pluginId);
        
        Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> optPluginInfo = 
                pluginRegistry.getPlugin(pluginId);
        
        if (!optPluginInfo.isPresent()) {
            log.warn("插件不存在: {}", pluginId);
            return false;
        }
        
        com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo = optPluginInfo.get();
        String version = pluginInfo.getDescriptor().getVersion();
        
        // 更新插件状态
        pluginInfo.setState(PluginState.UNLOADING);
        stateManager.recordStateChange(pluginId, PluginState.UNLOADING);
        
        try {
            // 关闭类加载器
            DynamicClassLoader classLoader = pluginInfo.getClassLoader();
            if (classLoader != null) {
                classLoader.close();
            }
            
            // 移除插件实例
            pluginInstances.remove(pluginId);
            
            // 移除类加载器
            pluginClassLoaders.remove(pluginId);
            
            // 更新插件状态
            pluginInfo.setState(PluginState.UNLOADED);
            stateManager.recordStateChange(pluginId, PluginState.UNLOADED);
            
            // 发布卸载事件
            publishUnloadedEvent(pluginId, version);
            
            log.info("插件[{}]卸载成功", pluginId);
            return true;
        } catch (Exception e) {
            String message = String.format("卸载插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            pluginInfo.setState(PluginState.ERROR);
            pluginInfo.setErrorMessage(message);
            stateManager.recordFailure(pluginId, message);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public Optional<ClassLoader> getPluginClassLoader(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(pluginClassLoaders.get(pluginId));
    }
    
    /**
     * 获取缓存的插件实例
     *
     * @param pluginId 插件ID
     * @return 插件实例
     */
    public Optional<Plugin> getPluginInstance(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.ofNullable(pluginInstances.get(pluginId));
    }
    
    /**
     * 创建插件实例
     *
     * @param pluginInfo 插件信息
     * @return 插件实例
     * @throws Exception 创建过程中的异常
     */
    private Plugin createPluginInstance(com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo) throws Exception {
        String pluginId = pluginInfo.getDescriptor().getPluginId();
        String mainClass = pluginInfo.getDescriptor().getMainClass();
        DynamicClassLoader classLoader = pluginInfo.getClassLoader();
        
        if (mainClass == null || mainClass.isEmpty()) {
            throw new IllegalStateException("插件主类不能为空");
        }
        
        if (classLoader == null) {
            throw new IllegalStateException("插件类加载器不能为空");
        }
        
        try {
            // 设置上下文类加载器为父类加载器，确保SDK和API类能正确加载
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            
            // 加载主类
            log.debug("尝试加载主类: {}", mainClass);
            Class<?> pluginClass;
            try {
                pluginClass = classLoader.loadClass(mainClass);
                log.debug("主类加载成功: {}, 类加载器: {}", mainClass, pluginClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                log.error("找不到插件主类: {}, 错误: {}", mainClass, e.getMessage());
                throw new IllegalStateException("找不到插件主类: " + mainClass, e);
            }
            
            // 检查主类是否实现Plugin接口
            if (!isPluginImplementation(pluginClass)) {
                log.error("插件主类必须实现Plugin接口: {}", mainClass);
                throw new IllegalStateException("插件主类必须实现Plugin接口: " + mainClass);
            }
            
            // 尝试创建插件实例
            Object instance;
            try {
                Constructor<?> constructor = pluginClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                instance = constructor.newInstance();
                log.debug("创建插件实例成功: {}", mainClass);
            } catch (Exception e) {
                log.error("创建插件实例失败: {}, 错误: {}", mainClass, e.getMessage());
                throw new IllegalStateException("创建插件实例失败: " + mainClass, e);
            }
            
            // 将插件实例保存到插件信息中
            pluginInfo.setPluginInstance(instance);
            
            // 创建Plugin适配器
            return createPluginAdapter(instance);
        } catch (Exception e) {
            log.error("创建插件[{}]实例时发生错误: {}", pluginId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 检查类是否实现了Plugin接口
     * 通过反射递归检查类的接口和父类
     *
     * @param clazz 要检查的类
     * @return 是否实现了Plugin接口
     */
    private boolean isPluginImplementation(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        // 检查直接实现的接口
        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            if (interfaceClass.getName().equals("com.xiaoqu.qteamos.api.core.plugin.Plugin") ||
                interfaceClass.getName().equals("com.xiaoqu.qteamos.sdk.plugin.Plugin")) {
                return true;
            }
        }
        
        // 检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return isPluginImplementation(superClass);
        }
        
        return false;
    }
    
    /**
     * 创建Plugin适配器
     * 将原始对象转换为标准的Plugin接口实现
     *
     * @param instance 原始插件实例
     * @return Plugin接口实现
     */
    @SuppressWarnings("unchecked")
    private Plugin createPluginAdapter(Object instance) {
        return new Plugin() {
            @Override
            public void init(com.xiaoqu.qteamos.api.core.plugin.PluginContext context) throws Exception {
                try {
                    // 先尝试查找并调用setContext方法
                    Method setContextMethod = findMethod(instance.getClass(), "setContext", com.xiaoqu.qteamos.api.core.plugin.PluginContext.class);
                    if (setContextMethod != null) {
                        log.debug("找到并调用setContext方法");
                        setContextMethod.setAccessible(true);
                        setContextMethod.invoke(instance, context);
                    } else {
                        // 尝试查找SDK版本的setContext方法
                        Method sdkSetContextMethod = findMethod(instance.getClass(), "setContext", com.xiaoqu.qteamos.sdk.plugin.PluginContext.class);
                        if (sdkSetContextMethod != null) {
                            log.debug("找到并调用SDK版本的setContext方法");
                            sdkSetContextMethod.setAccessible(true);
                            // 这里需要一个适配器来将API PluginContext转换为SDK PluginContext
                            // 简单起见，我们创建一个ContextAdapter，将所有请求代理到API PluginContext
                            sdkSetContextMethod.invoke(instance, createSdkContextAdapter(context));
                        }
                    }
                    
                    // 查找与API或SDK兼容的init方法
                    Method initMethod = findMethod(instance.getClass(), "init", com.xiaoqu.qteamos.api.core.plugin.PluginContext.class);
                    
                    if (initMethod != null) {
                        log.debug("找到并调用标准init方法");
                        initMethod.setAccessible(true);
                        initMethod.invoke(instance, context);
                    } else {
                        // 尝试查找SDK方法
                        Method sdkInitMethod = findMethod(instance.getClass(), "init", com.xiaoqu.qteamos.sdk.plugin.PluginContext.class);
                        
                        if (sdkInitMethod != null) {
                            log.debug("找到并调用SDK init方法");
                            sdkInitMethod.setAccessible(true);
                            // 使用SDK上下文适配器
                            sdkInitMethod.invoke(instance, createSdkContextAdapter(context));
                        } else {
                            // 尝试调用无参数的init方法
                            Method noArgInitMethod = findMethod(instance.getClass(), "init");
                            if (noArgInitMethod != null) {
                                log.debug("找到并调用无参数init方法");
                                noArgInitMethod.setAccessible(true);
                                noArgInitMethod.invoke(instance);
                            } else {
                                // 最后尝试调用initPlugin方法
                                Method initPluginMethod = findMethod(instance.getClass(), "initPlugin");
                                if (initPluginMethod != null) {
                                    log.debug("找到并调用initPlugin方法");
                                    initPluginMethod.setAccessible(true);
                                    initPluginMethod.invoke(instance);
                                } else {
                                    log.warn("未找到任何初始化方法，跳过初始化");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("调用插件init方法失败", e);
                    throw e;
                }
            }

            @Override
            public void start() throws Exception {
                Method startMethod = findMethod(instance.getClass(), "start");
                if (startMethod != null) {
                    startMethod.setAccessible(true);
                    startMethod.invoke(instance);
                }
            }

            @Override
            public void stop() throws Exception {
                Method stopMethod = findMethod(instance.getClass(), "stop");
                if (stopMethod != null) {
                    stopMethod.setAccessible(true);
                    stopMethod.invoke(instance);
                }
            }

            @Override
            public void destroy() throws Exception {
                Method destroyMethod = findMethod(instance.getClass(), "destroy");
                if (destroyMethod != null) {
                    destroyMethod.setAccessible(true);
                    destroyMethod.invoke(instance);
                }
            }

            @Override
            public void uninstall() throws Exception {
                Method uninstallMethod = findMethod(instance.getClass(), "uninstall");
                if (uninstallMethod != null) {
                    uninstallMethod.setAccessible(true);
                    uninstallMethod.invoke(instance);
                } else {
                    // 如果没有uninstall方法，则调用destroy方法
                    destroy();
                }
            }

            @Override
            public String getId() {
                try {
                    Method getIdMethod = findMethod(instance.getClass(), "getId");
                    if (getIdMethod != null) {
                        getIdMethod.setAccessible(true);
                        return (String) getIdMethod.invoke(instance);
                    }
                } catch (Exception e) {
                    log.warn("获取插件ID失败", e);
                }
                return null;
            }

            @Override
            public String getName() {
                try {
                    Method getNameMethod = findMethod(instance.getClass(), "getName");
                    if (getNameMethod != null) {
                        getNameMethod.setAccessible(true);
                        return (String) getNameMethod.invoke(instance);
                    }
                } catch (Exception e) {
                    log.warn("获取插件名称失败", e);
                }
                return null;
            }

            @Override
            public String getVersion() {
                try {
                    Method getVersionMethod = findMethod(instance.getClass(), "getVersion");
                    if (getVersionMethod != null) {
                        getVersionMethod.setAccessible(true);
                        return (String) getVersionMethod.invoke(instance);
                    }
                } catch (Exception e) {
                    log.warn("获取插件版本失败", e);
                }
                return null;
            }

            @Override
            public String getDescription() {
                try {
                    Method getDescriptionMethod = findMethod(instance.getClass(), "getDescription");
                    if (getDescriptionMethod != null) {
                        getDescriptionMethod.setAccessible(true);
                        return (String) getDescriptionMethod.invoke(instance);
                    }
                } catch (Exception e) {
                    log.warn("获取插件描述失败", e);
                }
                return null;
            }

            @Override
            public String getAuthor() {
                try {
                    Method getAuthorMethod = findMethod(instance.getClass(), "getAuthor");
                    if (getAuthorMethod != null) {
                        getAuthorMethod.setAccessible(true);
                        return (String) getAuthorMethod.invoke(instance);
                    }
                } catch (Exception e) {
                    log.warn("获取插件作者失败", e);
                }
                return null;
            }
        };
    }
    
    /**
     * 查找方法
     *
     * @param clazz 类
     * @param methodName 方法名
     * @param paramTypes 参数类型
     * @return 方法对象
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        if (clazz == null || methodName == null) {
            return null;
        }
        
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            // 在父类中查找
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && !superClass.equals(Object.class)) {
                return findMethod(superClass, methodName, paramTypes);
            }
            return null;
        }
    }
    
    /**
     * 发布插件加载事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param loadedClassCount 加载的类数量
     * @param loadedResourceCount 加载的资源数量
     * @param loadTime 加载时间
     */
    private void publishLoadedEvent(String pluginId, String version, int loadedClassCount, int loadedResourceCount, long loadTime) {
        try {
            PluginLoadedEvent event = new PluginLoadedEvent(
                    pluginId, 
                    version, 
                    loadedClassCount, 
                    loadedResourceCount, 
                    loadTime);
            eventDispatcher.publishEvent(event);
            log.debug("发布插件加载事件: {}, 版本: {}, 加载类数量: {}, 加载资源数量: {}, 加载时间: {}ms", 
                    pluginId, version, loadedClassCount, loadedResourceCount, loadTime);
        } catch (Exception e) {
            log.error("发布插件加载事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发布插件卸载事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     */
    private void publishUnloadedEvent(String pluginId, String version) {
        try {
            com.xiaoqu.qteamos.api.core.event.PluginEvent event = new com.xiaoqu.qteamos.api.core.event.PluginEvent(
                PluginEventTypes.Topics.PLUGIN, 
                PluginEventTypes.Plugin.UNLOADED, 
                "system", 
                pluginId,
                version,
                null,
                false) {};
            eventDispatcher.publishEvent(event);
            log.debug("发布插件卸载事件: {}, 版本: {}", pluginId, version);
        } catch (Exception e) {
            log.error("发布插件卸载事件失败: {}", e.getMessage(), e);
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
            com.xiaoqu.qteamos.api.core.event.PluginEvent event = new com.xiaoqu.qteamos.api.core.event.PluginEvent(
                PluginEventTypes.Topics.PLUGIN, 
                PluginEventTypes.Plugin.ERROR, 
                "system", 
                pluginId, 
                version, 
                error, 
                false) {};
            eventDispatcher.publishEvent(event);
            log.debug("发布插件错误事件: {}, 版本: {}, 错误: {}", pluginId, version, error.getMessage());
        } catch (Exception e) {
            log.error("发布插件错误事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 发布插件依赖检查失败事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param missingDependencies 缺失的依赖
     */
    private void publishDependencyFailedEvent(String pluginId, String version, List<String> missingDependencies) {
        try {
            com.xiaoqu.qteamos.api.core.event.PluginEvent event = new com.xiaoqu.qteamos.api.core.event.PluginEvent(
                PluginEventTypes.Topics.PLUGIN, 
                PluginEventTypes.Plugin.DEPENDENCY_FAILED, 
                "system", 
                pluginId, 
                version, 
                missingDependencies, 
                false) {};
            eventDispatcher.publishEvent(event);
            log.debug("发布插件依赖检查失败事件: {}, 版本: {}, 缺失依赖: {}", pluginId, version, missingDependencies);
        } catch (Exception e) {
            log.error("发布插件依赖检查失败事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建SDK上下文适配器
     * 将API层的PluginContext适配为SDK层的PluginContext
     *
     * @param apiContext API层的插件上下文
     * @return SDK层的插件上下文
     */
    private com.xiaoqu.qteamos.sdk.plugin.PluginContext createSdkContextAdapter(com.xiaoqu.qteamos.api.core.plugin.PluginContext apiContext) {
        return new com.xiaoqu.qteamos.sdk.plugin.PluginContext() {
            @Override
            public String getPluginId() {
                return apiContext.getPluginId();
            }

            @Override
            public ClassLoader getClassLoader() {
                return Thread.currentThread().getContextClassLoader();
            }

            @Override
            public com.xiaoqu.qteamos.sdk.plugin.PluginResourceProvider getResourceProvider() {
                return null; // 简化实现，实际应返回适配后的资源提供者
            }

            @Override
            public String getPluginVersion() {
                return apiContext.getPluginVersion();
            }

            @Override
            public String getConfig(String key) {
                return apiContext.getConfig(key);
            }

            @Override
            public String getConfig(String key, String defaultValue) {
                return apiContext.getConfig(key, defaultValue);
            }

            @Override
            public Map<String, String> getAllConfigs() {
                return apiContext.getAllConfigs();
            }

            @Override
            public void setConfig(String key, String value) {
                apiContext.setConfig(key, value);
            }

            @Override
            public String getDataFolderPath() {
                return apiContext.getDataFolderPath();
            }

            @Override
            public com.xiaoqu.qteamos.sdk.cache.CacheService getCacheService() {
                // 简化实现，实际应返回适配后的缓存服务
                return null;
            }

            @Override
            public com.xiaoqu.qteamos.sdk.datasource.DataSourceService getDataSourceService() {
                // 简化实现，实际应返回适配后的数据源服务
                return null;
            }

            @Override
            public com.xiaoqu.qteamos.sdk.config.ConfigService getConfigService() {
                // 简化实现，实际应返回适配后的配置服务
                return null;
            }

            @Override
            public <T> T getService(Class<T> serviceClass) {
                return apiContext.getService(serviceClass);
            }

            @Override
            public void publishEvent(Object event) {
                apiContext.publishEvent(event);
            }

            @Override
            public <T> void addEventListener(Class<T> eventType, com.xiaoqu.qteamos.sdk.plugin.PluginContext.EventListener<T> listener) {
                // 创建适配器将SDK的EventListener转换为API的PluginEventListener
                apiContext.addEventListener(eventType, new com.xiaoqu.qteamos.api.core.plugin.PluginEventListener<T>() {
                    @Override
                    public void onEvent(T event) {
                        listener.onEvent(event);
                    }
                });
            }

            @Override
            public <T> void removeEventListener(Class<T> eventType, com.xiaoqu.qteamos.sdk.plugin.PluginContext.EventListener<T> listener) {
                // 简化实现，实际需要跟踪SDK和API监听器之间的映射关系
                // 此处简单忽略移除操作
            }
        };
    }
} 