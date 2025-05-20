package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader;
import com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoaderFactory;
import com.xiaoqu.qteamos.core.plugin.manager.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptorLoader;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.PluginContext;
import com.xiaoqu.qteamos.core.plugin.service.PluginPersistenceService;
import com.xiaoqu.qteamos.core.plugin.service.PluginVersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 插件生命周期管理器
 * 负责插件的加载、初始化、启动、停止和卸载等生命周期管理
 * 增强版：支持插件版本更新、热加载和健康检查
 *
 * @author yangqijun
 * @date 2024-07-02
 */
@Component
public class PluginLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleManager.class);

    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private DependencyResolver dependencyResolver;
    
    @Autowired
    private PluginResourceBridge resourceBridge;
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private PluginStateManager stateManager;
    
    @Autowired
    private PluginPersistenceService persistenceService;
    
    @Autowired
    @Qualifier("pluginVersionManager")
    private PluginVersionManager versionManager;
    
    @Autowired
    private DynamicClassLoaderFactory classLoaderFactory;
    
    // 缓存已创建的插件实例
    private final ConcurrentMap<String, Plugin> pluginInstances = new ConcurrentHashMap<>();
    
    // 记录插件的健康状态
    private final Map<String, PluginHealthStatus> healthStatuses = new ConcurrentHashMap<>();
    
    // 记录插件的启动依赖图
    //private final Map<String, Set<String>> startupDependencyGraph = new HashMap<>();
    
    // 记录插件的版本更新历史
    private final Map<String, Map<String, String>> versionHistory = new ConcurrentHashMap<>();
    
    // 自动健康检查的执行器
    private ScheduledExecutorService healthCheckExecutor;
    
    /**
     * 初始化健康检查执行器
     */
    @PostConstruct
    public void init() {
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-health-check");
            t.setDaemon(true);
            return t;
        });
        
        // 启动定期健康检查
        healthCheckExecutor.scheduleWithFixedDelay(
            this::performHealthCheck, 
            60, // 初始延迟60秒
            300, // 每5分钟检查一次
            TimeUnit.SECONDS
        );
        
        log.info("插件生命周期管理器初始化完成");
    }
    
    /**
     * 关闭健康检查执行器
     */
    @PreDestroy
    public void shutdown() {
        if (healthCheckExecutor != null) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        log.info("插件生命周期管理器已关闭");
    }
    
    /**
     * 加载插件
     *
     * @param pluginInfo 插件信息
     * @return 加载是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    public boolean loadPlugin(PluginInfo pluginInfo) throws PluginLifecycleException {
        String pluginId = pluginInfo.getDescriptor().getPluginId();
        
        try {
            log.info("开始加载插件: {}", pluginId);
            
            // 检查依赖
            log.debug("检查插件[{}]依赖...", pluginId);
            if (!dependencyResolver.checkDependencies(pluginInfo.getDescriptor())) {
                log.error("插件依赖检查失败: {}", pluginId);
                pluginInfo.setState(PluginState.DEPENDENCY_FAILED);
                stateManager.recordStateChange(pluginId, PluginState.DEPENDENCY_FAILED);
                return false;
            }
            log.debug("插件[{}]依赖检查通过", pluginId);
            
            // 记录版本更新历史
            recordVersionUpdate(pluginInfo.getDescriptor());
            
            // 检查类加载器
            log.debug("检查插件[{}]类加载器...", pluginId);
            DynamicClassLoader classLoader = pluginInfo.getClassLoader();
            if (classLoader == null) {
                log.error("插件[{}]类加载器为空", pluginId);
                pluginInfo.setState(PluginState.ERROR);
                pluginInfo.setErrorMessage("类加载器为空");
                stateManager.recordFailure(pluginId, "类加载器为空");
                return false;
            }
            log.debug("插件[{}]类加载器正常: {}", pluginId, classLoader.getClass().getName());
            
            // 创建插件实例
            log.debug("开始为插件[{}]创建实例...", pluginId);
            Plugin pluginInstance = null;
            try {
                pluginInstance = createPluginInstance(pluginInfo);
                log.debug("插件[{}]实例创建成功: {}", pluginId, 
                    pluginInstance != null ? pluginInstance.getClass().getName() : "null");
            } catch (Exception e) {
                log.error("创建插件[{}]实例失败: {}", pluginId, e.getMessage(), e);
                pluginInfo.setState(PluginState.ERROR);
                pluginInfo.setErrorMessage("创建实例失败: " + e.getMessage());
                stateManager.recordFailure(pluginId, "创建实例失败: " + e.getMessage());
                return false;
            }
            
            if (pluginInstance == null) {
                log.error("无法创建插件实例: {}", pluginId);
                pluginInfo.setState(PluginState.FAILED);
                stateManager.recordStateChange(pluginId, PluginState.FAILED);
                return false;
            }
            
            // 缓存插件实例
            log.debug("缓存插件[{}]实例...", pluginId);
            pluginInstances.put(pluginId, pluginInstance);
            
            // 更新插件状态
            log.debug("更新插件[{}]状态为LOADED...", pluginId);
            pluginInfo.setState(PluginState.LOADED);
            pluginRegistry.updatePlugin(pluginInfo);
            log.debug("记录插件[{}]状态变更...", pluginId);
            stateManager.recordStateChange(pluginId, PluginState.LOADED);
            
            // 初始化健康状态
            healthStatuses.put(pluginId, PluginHealthStatus.healthy());
            
            log.info("插件[{}]加载完成，状态: LOADED", pluginId);
            return true;
        } catch (Exception e) {
            log.error("插件[{}]加载异常: {}", pluginId, e.getMessage(), e);
            pluginInfo.setState(PluginState.ERROR);
            pluginInfo.setErrorMessage(e.getMessage());
            pluginRegistry.updatePlugin(pluginInfo);
            stateManager.recordFailure(pluginId, e.getMessage());
            
            // 记录错误健康状态
            healthStatuses.put(pluginId, PluginHealthStatus.unhealthy("加载失败: " + e.getMessage()));
            
            throw new PluginLifecycleException("加载插件失败: " + pluginId, e);
        }
    }
    
    /**
     * 记录版本更新历史
     *
     * @param descriptor 插件描述符
     */
    private void recordVersionUpdate(PluginDescriptor descriptor) {
        String pluginId = descriptor.getPluginId();
        String currentVersion = descriptor.getVersion();
        String previousVersion = descriptor.getPreviousVersion();
        
        versionHistory.computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>())
                .put(currentVersion, previousVersion != null ? previousVersion : "初始版本");
        
        log.info("记录插件版本更新: {} {} -> {}", 
                pluginId, 
                previousVersion != null ? previousVersion : "初始版本", 
                currentVersion);
    }
    
    /**
     * 创建插件实例
     *
     * @param pluginInfo 插件信息
     * @return 插件实例
     * @throws Exception 创建异常
     */
    private Plugin createPluginInstance(PluginInfo pluginInfo) throws Exception {
        String pluginId = pluginInfo.getDescriptor().getPluginId();
        PluginDescriptor descriptor = pluginInfo.getDescriptor();
        String mainClass = descriptor.getMainClass();
        
        log.debug("准备创建插件[{}]实例，主类: {}", pluginId, mainClass);
        
        try {
            // 获取类加载器
            DynamicClassLoader classLoader = pluginInfo.getClassLoader();
            if (classLoader == null) {
                throw new IllegalStateException("插件类加载器为空");
            }
            
            // 打印类加载器信息
            log.debug("插件[{}]类加载器信息: id={}, urls={}", pluginId, classLoader.getPluginId(), 
                     java.util.Arrays.toString(classLoader.getURLs()));
            
            // 保存当前线程上下文类加载器
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            
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
                if (!Plugin.class.isAssignableFrom(pluginClass)) {
                    log.warn("使用标准检查无法确认插件主类是否实现Plugin接口，尝试使用反射检查: {}", mainClass);
                    // 尝试通过反射检查类的继承关系
                    boolean implementsPlugin = isPluginImplementation(pluginClass);
                    if (!implementsPlugin) {
                        log.error("插件主类必须实现Plugin接口: {}", mainClass);
                        throw new IllegalStateException("插件主类必须实现Plugin接口: " + mainClass);
                    } else {
                        log.debug("通过反射确认插件主类实现了Plugin接口: {}", mainClass);
                    }
                }
                
                // 尝试创建插件实例
                try {
                    // 使用无参构造函数
                    Constructor<?> constructor = pluginClass.getConstructor();
                    constructor.setAccessible(true);
                    Object instance = constructor.newInstance();
                    
                    // 检查类型兼容性
                    if (!(instance instanceof Plugin)) {
                        // 尝试使用反射判断接口兼容性
                        if (isPluginImplementation(instance.getClass())) {
                            log.warn("通过反射确认实例确实实现了Plugin接口，但类型转换失败，类加载器隔离导致的");
                            // 创建适配器解决类型兼容性问题
                            return createPluginAdapter(instance);
                        } else {
                            throw new ClassCastException("创建的实例不是Plugin类型: " + instance.getClass().getName());
                        }
                    }
                    
                    // 直接返回插件实例
                    Plugin pluginInstance = (Plugin) instance;
                    log.debug("创建插件实例成功: {}", mainClass);
                    return pluginInstance;
                } catch (Exception e) {
                    log.error("创建插件实例失败: {}", e.getMessage(), e);
                    throw new IllegalStateException("创建插件实例失败: " + mainClass, e);
                }
            } finally {
                // 恢复线程上下文类加载器
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        } catch (Exception e) {
            log.error("创建插件实例过程中发生异常: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 为插件创建适配器，解决类加载器隔离导致的类型转换问题
     *
     * @param instance 插件实例
     * @return 适配后的插件实例
     */
    private Plugin createPluginAdapter(Object instance) {
        log.info("为插件创建适配器解决类加载器隔离问题");
        
        return new Plugin() {
            private PluginContext savedContext;
            
            @Override
            public void init(PluginContext context) throws Exception {
                // 保存上下文，以便后续使用
                this.savedContext = context;
                
                try {
                    log.debug("尝试适配器初始化插件，绕过类型不匹配问题");
                    
                    // 创建一个Map传递关键数据，避免直接传递PluginContext对象
                    Map<String, Object> contextMap = new HashMap<>();
                    contextMap.put("pluginId", context.getPluginId());
                    contextMap.put("version", context.getPluginVersion());
                    contextMap.put("dataFolderPath", context.getDataFolderPath());
                    
                    // 获取配置并传递
                    Map<String, String> configs = context.getAllConfigs();
                    contextMap.put("configs", configs);
                    
                    // 创建和设置属性的方法
                    Method[] methods = instance.getClass().getMethods();
                    
                    // 先尝试setProperties或setContextData方法
                    for (Method method : methods) {
                        if (method.getName().equals("setProperties") && method.getParameterCount() == 1) {
                            log.debug("使用setProperties方法传递上下文属性");
                            method.invoke(instance, contextMap);
                            
                            // 使用无参init方法初始化
                            try {
                                Method initMethod = instance.getClass().getMethod("init");
                                log.debug("调用无参init方法");
                                initMethod.invoke(instance);
                                return;
                            } catch (NoSuchMethodException e) {
                                log.debug("无参init方法不存在");
                            }
                        }
                        else if (method.getName().equals("setConfig") && method.getParameterCount() == 2) {
                            // 逐个设置配置项
                            log.debug("使用setConfig方法逐个设置配置");
                            for (Map.Entry<String, String> entry : configs.entrySet()) {
                                method.invoke(instance, entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    
                    // 直接设置字段
                    Field[] fields = instance.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        String fieldName = field.getName();
                        if (contextMap.containsKey(fieldName)) {
                            field.setAccessible(true);
                            field.set(instance, contextMap.get(fieldName));
                        }
                    }
                    
                    // 检查是否有父类的context字段
                    try {
                        Field contextField = findField(instance.getClass(), "context");
                        if (contextField != null) {
                            contextField.setAccessible(true);
                            
                            // 创建Map形式的简单上下文对象
                            Object simpleContext = createSimpleContext(contextMap, configs);
                            contextField.set(instance, simpleContext);
                            
                            log.debug("成功设置context字段");
                        }
                    } catch (Exception e) {
                        log.debug("设置context字段失败: {}", e.getMessage());
                    }
                    
                    // 尝试特定的初始化方法
                    try {
                        log.debug("尝试调用initPlugin方法");
                        Method initMethod = instance.getClass().getMethod("initPlugin");
                        initMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("initPlugin方法不存在");
                    }
                    
                    log.debug("所有初始化尝试都已完成");
                    
                } catch (Exception e) {
                    log.error("插件初始化失败", e);
                    throw e;
                }
            }
            
            @Override
            public void start() throws Exception {
                try {
                    // 尝试不同名称的启动方法
                    Method startMethod = null;
                    
                    // 1. 尝试标准的start方法
                    try {
                        startMethod = instance.getClass().getMethod("start");
                        log.debug("使用标准start方法启动插件");
                        startMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("插件没有标准start方法，尝试替代方法");
                    }
                    
                    // 2. 尝试startPlugin方法
                    try {
                        startMethod = instance.getClass().getMethod("startPlugin");
                        log.debug("使用startPlugin方法启动插件");
                        startMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("插件没有startPlugin方法");
                    }
                    
                    // 3. 尝试onStart方法
                    try {
                        startMethod = instance.getClass().getMethod("onStart");
                        log.debug("使用onStart方法启动插件");
                        startMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("插件没有onStart方法");
                    }
                    
                    // 如果没有找到启动方法，记录警告但不抛出异常
                    log.warn("没有找到插件启动方法，插件可能不需要特殊启动逻辑");
                    
                } catch (Exception e) {
                    log.error("调用插件start方法失败", e);
                    throw e;
                }
            }
            
            @Override
            public void stop() throws Exception {
                try {
                    // 尝试不同名称的停止方法
                    Method stopMethod = null;
                    
                    // 1. 尝试标准的stop方法
                    try {
                        stopMethod = instance.getClass().getMethod("stop");
                        log.debug("使用标准stop方法停止插件");
                        stopMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("插件没有标准stop方法，尝试替代方法");
                    }
                    
                    // 2. 尝试stopPlugin方法
                    try {
                        stopMethod = instance.getClass().getMethod("stopPlugin");
                        log.debug("使用stopPlugin方法停止插件");
                        stopMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("插件没有stopPlugin方法");
                    }
                    
                    // 3. 尝试onStop方法
                    try {
                        stopMethod = instance.getClass().getMethod("onStop");
                        log.debug("使用onStop方法停止插件");
                        stopMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("插件没有onStop方法");
                    }
                    
                    // 4. 尝试destroy方法
                    try {
                        stopMethod = instance.getClass().getMethod("destroy");
                        log.debug("使用destroy方法停止插件");
                        stopMethod.invoke(instance);
                        return;
                    } catch (NoSuchMethodException e) {
                        log.debug("插件没有destroy方法");
                    }
                    
                    // 如果没有找到停止方法，记录警告但不抛出异常
                    log.warn("没有找到插件停止方法，插件可能不需要特殊清理逻辑");
                    
                } catch (Exception e) {
                    log.error("调用插件stop方法失败", e);
                    throw e;
                }
            }
            
            @Override
            public void uninstall() throws Exception {
                // 通过反射调用实际插件的uninstall方法
                try {
                    Method uninstallMethod = instance.getClass().getMethod("uninstall");
                    uninstallMethod.invoke(instance);
                } catch (NoSuchMethodException e) {
                    log.warn("插件未实现uninstall方法");
                } catch (Exception e) {
                    log.error("调用uninstall方法失败", e);
                    throw e;
                }
            }
            
            @Override
            public void destroy() throws Exception {
                // 通过反射调用实际插件的destroy方法
                try {
                    Method destroyMethod = instance.getClass().getMethod("destroy");
                    destroyMethod.invoke(instance);
                } catch (NoSuchMethodException e) {
                    log.warn("插件未实现destroy方法");
                } catch (Exception e) {
                    log.error("调用destroy方法失败", e);
                    throw e;
                }
            }
            
            @Override
            public String getId() {
                try {
                    Method getIdMethod = instance.getClass().getMethod("getId");
                    return (String) getIdMethod.invoke(instance);
                } catch (Exception e) {
                    log.error("调用getId方法失败", e);
                    return "unknown";
                }
            }
            
            @Override
            public String getName() {
                try {
                    Method getNameMethod = instance.getClass().getMethod("getName");
                    return (String) getNameMethod.invoke(instance);
                } catch (Exception e) {
                    log.error("调用getName方法失败", e);
                    return "Unknown Plugin";
                }
            }
            
            @Override
            public String getVersion() {
                try {
                    Method getVersionMethod = instance.getClass().getMethod("getVersion");
                    return (String) getVersionMethod.invoke(instance);
                } catch (Exception e) {
                    log.error("调用getVersion方法失败", e);
                    return "0.0.0";
                }
            }
            
            @Override
            public String getDescription() {
                try {
                    Method getDescriptionMethod = instance.getClass().getMethod("getDescription");
                    return (String) getDescriptionMethod.invoke(instance);
                } catch (Exception e) {
                    log.error("调用getDescription方法失败", e);
                    return "适配器代理的插件";
                }
            }
            
            @Override
            public String getAuthor() {
                try {
                    Method getAuthorMethod = instance.getClass().getMethod("getAuthor");
                    return (String) getAuthorMethod.invoke(instance);
                } catch (Exception e) {
                    log.error("调用getAuthor方法失败", e);
                    return "unknown";
                }
            }
        };
    }
    
    /**
     * 创建简单上下文对象
     */
    private Object createSimpleContext(Map<String, Object> contextMap, Map<String, String> configs) {
        try {
            // 尝试创建一个简单的模拟上下文对象
            return new SimplePluginContext(contextMap, configs);
        } catch (Exception e) {
            log.debug("创建简单上下文对象失败: {}", e.getMessage());
            return contextMap;
        }
    }
    
    /**
     * 简单的插件上下文实现
     */
    private static class SimplePluginContext {
        private final Map<String, Object> contextData;
        private final Map<String, String> configs;
        
        public SimplePluginContext(Map<String, Object> contextData, Map<String, String> configs) {
            this.contextData = contextData;
            this.configs = configs;
        }
        
        public String getPluginId() {
            return (String) contextData.get("pluginId");
        }
        
        public String getPluginVersion() {
            return (String) contextData.get("version");
        }
        
        public String getDataFolderPath() {
            return (String) contextData.get("dataFolderPath");
        }
        
        public String getConfig(String key) {
            return configs.get(key);
        }
        
        public String getConfig(String key, String defaultValue) {
            return configs.getOrDefault(key, defaultValue);
        }
        
        public Map<String, String> getAllConfigs() {
            return configs;
        }
    }
    
    /**
     * 递归查找字段
     */
    private Field findField(Class<?> clazz, String fieldName) {
        // 检查当前类
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // 检查父类
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null && !superClass.equals(Object.class)) {
                return findField(superClass, fieldName);
            }
        }
        return null;
    }
    
    /**
     * 初始化插件
     *
     * @param pluginId 插件ID
     * @return 初始化是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    public boolean initializePlugin(String pluginId) throws PluginLifecycleException {
        log.info("开始初始化插件: {}", pluginId);
        
        Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.error("插件不存在: {}", pluginId);
            return false;
        }
        
        PluginInfo pluginInfo = optPluginInfo.get();
        if (pluginInfo.getState() != PluginState.LOADED) {
            log.error("插件状态不正确，无法初始化: {}, 当前状态: {}", pluginId, pluginInfo.getState());
            return false;
        }
        
        Plugin pluginInstance = pluginInstances.get(pluginId);
        if (pluginInstance == null) {
            log.error("插件实例不存在: {}", pluginId);
            return false;
        }
        
        try {
            // 创建插件上下文
            PluginContext context = createPluginContext(pluginInfo);
            
            // 初始化插件 - 使用适配器模式初始化
            try {
                // 记录初始化信息
                log.debug("调用插件init方法初始化...");
                pluginInstance.init(context);
                log.debug("插件init方法调用成功");
                
                // 更新插件状态
                pluginInfo.setState(PluginState.INITIALIZED);
                pluginRegistry.updatePlugin(pluginInfo);
                stateManager.recordStateChange(pluginId, PluginState.INITIALIZED);
                
                log.info("插件初始化完成: {}", pluginId);
                return true;
            } catch (Exception e) {
                log.error("插件初始化异常: " + pluginId, e);
                pluginInfo.setState(PluginState.FAILED);
                pluginInfo.setErrorMessage(e.getMessage());
                pluginRegistry.updatePlugin(pluginInfo);
                stateManager.recordFailure(pluginId, "初始化失败: " + e.getMessage());
                
                // 更新健康状态
                healthStatuses.put(pluginId, PluginHealthStatus.unhealthy("初始化失败: " + e.getMessage()));
                
                throw new PluginLifecycleException("初始化插件失败: " + pluginId, e);
            }
        } catch (Exception e) {
            throw new PluginLifecycleException("初始化插件失败: " + pluginId, e);
        }
    }
    
    /**
     * 创建插件上下文
     *
     * @param pluginInfo 插件信息
     * @return 插件上下文
     */
    private PluginContext createPluginContext(PluginInfo pluginInfo) {
        // 由resourceBridge创建带有资源访问能力的上下文
        return resourceBridge.createPluginContext(pluginInfo);
    }
    
    /**
     * 启动插件
     *
     * @param pluginId 插件ID
     * @return 启动是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    public boolean startPlugin(String pluginId) throws PluginLifecycleException {
        log.info("开始启动插件: {}", pluginId);
        
        Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.error("插件不存在: {}", pluginId);
            return false;
        }
        
        PluginInfo pluginInfo = optPluginInfo.get();
        if (pluginInfo.getState() != PluginState.INITIALIZED) {
            log.error("插件状态不正确，无法启动: {}, 当前状态: {}", pluginId, pluginInfo.getState());
            return false;
        }
        
        Plugin pluginInstance = pluginInstances.get(pluginId);
        if (pluginInstance == null) {
            log.error("插件实例不存在: {}", pluginId);
            return false;
        }
        
        try {
            // 启动插件
            pluginInstance.start();
            
            // 更新插件状态
            pluginInfo.setState(PluginState.RUNNING);
            pluginInfo.setEnabled(true);
            pluginRegistry.updatePlugin(pluginInfo);
            stateManager.recordStateChange(pluginId, PluginState.RUNNING);
            
            // 更新健康状态
            healthStatuses.put(pluginId, PluginHealthStatus.healthy());
            
            log.info("插件启动完成: {}", pluginId);
            return true;
        } catch (Exception e) {
            log.error("插件启动异常: " + pluginId, e);
            pluginInfo.setState(PluginState.FAILED);
            pluginInfo.setErrorMessage(e.getMessage());
            pluginRegistry.updatePlugin(pluginInfo);
            stateManager.recordFailure(pluginId, "启动失败: " + e.getMessage());
            
            // 更新健康状态
            healthStatuses.put(pluginId, PluginHealthStatus.unhealthy("启动失败: " + e.getMessage()));
            
            throw new PluginLifecycleException("启动插件失败: " + pluginId, e);
        }
    }
    
    /**
     * 停止插件
     *
     * @param pluginId 插件ID
     * @return 停止是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    public boolean stopPlugin(String pluginId) throws PluginLifecycleException {
        log.info("开始停止插件: {}", pluginId);
        
        Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.error("插件不存在: {}", pluginId);
            return false;
        }
        
        PluginInfo pluginInfo = optPluginInfo.get();
        if (pluginInfo.getState() != PluginState.RUNNING) {
            log.error("插件状态不正确，无法停止: {}, 当前状态: {}", pluginId, pluginInfo.getState());
            return false;
        }
        
        Plugin pluginInstance = pluginInstances.get(pluginId);
        if (pluginInstance == null) {
            log.error("插件实例不存在: {}", pluginId);
            return false;
        }
        
        try {
            // 停止插件
            pluginInstance.stop();
            
            // 更新插件状态
            pluginInfo.setState(PluginState.STOPPED);
            pluginInfo.setEnabled(false);
            pluginRegistry.updatePlugin(pluginInfo);
            stateManager.recordStateChange(pluginId, PluginState.STOPPED);
            
            log.info("插件停止完成: {}", pluginId);
            return true;
        } catch (Exception e) {
            log.error("插件停止异常: " + pluginId, e);
            
            // 记录错误但不改变状态为失败，因为有些插件停止失败但仍可以卸载
            pluginInfo.setErrorMessage("停止错误: " + e.getMessage());
            pluginRegistry.updatePlugin(pluginInfo);
            
            throw new PluginLifecycleException("停止插件失败: " + pluginId, e);
        }
    }
    
    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    public boolean unloadPlugin(String pluginId) throws PluginLifecycleException {
        log.info("开始卸载插件: {}", pluginId);
        
        Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.error("插件不存在: {}", pluginId);
            return false;
        }
        
        PluginInfo pluginInfo = optPluginInfo.get();
        
        // 如果插件正在运行，先停止
        if (pluginInfo.getState() == PluginState.RUNNING) {
            stopPlugin(pluginId);
        }
        
        Plugin pluginInstance = pluginInstances.get(pluginId);
        if (pluginInstance != null) {
            try {
                // 销毁插件
                pluginInstance.uninstall();
            } catch (Exception e) {
                log.error("插件卸载异常: " + pluginId, e);
            }
            
            // 从缓存中移除插件实例
            pluginInstances.remove(pluginId);
        }
        
        try {
            // 释放类加载器资源
            DynamicClassLoader classLoader = pluginInfo.getClassLoader();
            if (classLoader != null) {
                classLoader.close();
            }
            
            // 更新插件状态
            pluginInfo.setState(PluginState.UNLOADED);
            pluginRegistry.updatePlugin(pluginInfo);
            stateManager.recordStateChange(pluginId, PluginState.UNLOADED);
            
            // 清理健康记录
            healthStatuses.remove(pluginId);
            
            log.info("插件卸载完成: {}", pluginId);
            return true;
        } catch (Exception e) {
            log.error("插件卸载异常: " + pluginId, e);
            throw new PluginLifecycleException("卸载插件失败: " + pluginId, e);
        }
    }
    
    /**
     * 获取插件实例
     *
     * @param pluginId 插件ID
     * @return 插件实例
     */
    public Optional<Plugin> getPluginInstance(String pluginId) {
        return Optional.ofNullable(pluginInstances.get(pluginId));
    }
    
    /**
     * 重载插件
     *
     * @param pluginId 插件ID
     * @return 重载是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    public boolean reloadPlugin(String pluginId) throws PluginLifecycleException {
        log.info("开始重载插件: {}", pluginId);
        
        // 卸载插件
        if (!unloadPlugin(pluginId)) {
            return false;
        }
        
        Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.error("插件不存在: {}", pluginId);
            return false;
        }
        
        PluginInfo pluginInfo = optPluginInfo.get();
        
        // 重新加载插件
        if (!loadPlugin(pluginInfo)) {
            return false;
        }
        
        // 初始化插件
        if (!initializePlugin(pluginId)) {
            return false;
        }
        
        // 启动插件
        return startPlugin(pluginId);
    }
    
    /**
     * 执行插件版本更新
     *
     * @param oldPluginInfo 旧版本插件信息
     * @param newPluginInfo 新版本插件信息
     * @return 更新是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    public boolean updatePlugin(PluginInfo oldPluginInfo, PluginInfo newPluginInfo) throws PluginLifecycleException {
        String pluginId = oldPluginInfo.getDescriptor().getPluginId();
        String oldVersion = oldPluginInfo.getDescriptor().getVersion();
        String newVersion = newPluginInfo.getDescriptor().getVersion();
        
        log.info("开始更新插件: {} {} -> {}", pluginId, oldVersion, newVersion);
        
        // 设置新插件的前置版本为当前版本
        newPluginInfo.getDescriptor().getUpdateInfo().put("previousVersion", oldVersion);
        
        // 停止并卸载老版本
        if (oldPluginInfo.getState() == PluginState.RUNNING) {
            stopPlugin(pluginId);
        }
        unloadPlugin(pluginId);
        
        // 加载新版本
        if (!loadPlugin(newPluginInfo)) {
            log.error("加载新版本插件失败: {} {}", pluginId, newVersion);
            return false;
        }
        
        // 初始化新版本
        if (!initializePlugin(pluginId)) {
            log.error("初始化新版本插件失败: {} {}", pluginId, newVersion);
            return false;
        }
        
        // 启动新版本
        if (!startPlugin(pluginId)) {
            log.error("启动新版本插件失败: {} {}", pluginId, newVersion);
            return false;
        }
        
        log.info("插件更新完成: {} {} -> {}", pluginId, oldVersion, newVersion);
        return true;
    }
    
    /**
     * 热加载插件
     * 尝试在不停止系统的情况下重新加载插件
     *
     * @param pluginId 插件ID
     * @return 热加载是否成功
     */
    public boolean hotReloadPlugin(String pluginId) {
        log.info("尝试热加载插件: {}", pluginId);
        
        try {
            // 获取当前插件信息
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                log.error("插件不存在，无法热加载: {}", pluginId);
                return false;
            }
            
            PluginInfo currentInfo = optPluginInfo.get();
            boolean wasRunning = currentInfo.getState() == PluginState.RUNNING;
            
            // 停止并卸载旧版本
            if (wasRunning) {
                stopPlugin(pluginId);
            }
            unloadPlugin(pluginId);
            
            // 重新加载
            if (!loadPlugin(currentInfo)) {
                log.error("热加载失败，插件加载失败: {}", pluginId);
                return false;
            }
            
            // 初始化
            if (!initializePlugin(pluginId)) {
                log.error("热加载失败，插件初始化失败: {}", pluginId);
                return false;
            }
            
            // 如果之前是运行状态，则启动
            if (wasRunning && !startPlugin(pluginId)) {
                log.error("热加载失败，插件启动失败: {}", pluginId);
                return false;
            }
            
            log.info("插件热加载成功: {}", pluginId);
            return true;
        } catch (Exception e) {
            log.error("插件热加载异常: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 执行定期健康检查
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void performHealthCheck() {
        log.debug("执行插件健康检查...");
        
        for (PluginInfo pluginInfo : pluginRegistry.getAllPlugins()) {
            String pluginId = pluginInfo.getDescriptor().getPluginId();
            
            // 只检查运行中的插件
            if (pluginInfo.getState() == PluginState.RUNNING) {
                try {
                    Plugin instance = pluginInstances.get(pluginId);
                    if (instance == null) {
                        log.warn("运行中的插件实例不存在: {}", pluginId);
                        healthStatuses.put(pluginId, PluginHealthStatus.unhealthy("实例不存在"));
                        continue;
                    }
                    
                    // 执行健康检查
                    boolean healthy = performSinglePluginHealthCheck(pluginId, instance);
                    
                    if (!healthy) {
                        log.warn("插件健康检查失败: {}", pluginId);
                        
                        // 获取健康状态
                        PluginHealthStatus status = healthStatuses.get(pluginId);
                        if (status != null && !status.isHealthy()) {
                            // 累计失败次数
                            status.incrementFailCount();
                            
                            // 如果连续失败超过阈值，尝试自动恢复
                            if (status.getFailCount() >= 3) {
                                log.warn("插件连续健康检查失败{}次，尝试自动恢复: {}", 
                                        status.getFailCount(), pluginId);
                                attemptPluginRecovery(pluginId);
                            }
                        }
                    } else {
                        // 健康状态正常，重置失败计数
                        healthStatuses.put(pluginId, PluginHealthStatus.healthy());
                    }
                } catch (Exception e) {
                    log.error("执行插件健康检查异常: " + pluginId, e);
                    healthStatuses.put(pluginId, PluginHealthStatus.unhealthy(e.getMessage()));
                }
            }
        }
    }
    
    /**
     * 对单个插件执行健康检查
     *
     * @param pluginId 插件ID
     * @param instance 插件实例
     * @return 是否健康
     */
    private boolean performSinglePluginHealthCheck(String pluginId, Plugin instance) {
        try {
            // 通过反射调用健康检查方法（如果存在）
            try {
                if (instance.getClass().getMethod("healthCheck") != null) {
                    return (boolean) instance.getClass().getMethod("healthCheck").invoke(instance);
                }
            } catch (NoSuchMethodException e) {
                // 插件未实现健康检查方法，视为健康
                return true;
            }
            
            // 检查插件状态
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty() || optPluginInfo.get().getState() != PluginState.RUNNING) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("健康检查异常: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 尝试恢复插件
     *
     * @param pluginId 插件ID
     */
    private void attemptPluginRecovery(String pluginId) {
        log.info("尝试恢复插件: {}", pluginId);
        
        try {
            // 先尝试重启插件
            if (stopPlugin(pluginId) && startPlugin(pluginId)) {
                log.info("插件通过重启成功恢复: {}", pluginId);
                return;
            }
            
            // 如果重启失败，尝试完全重载
            if (reloadPlugin(pluginId)) {
                log.info("插件通过重载成功恢复: {}", pluginId);
                return;
            }
            
            log.error("插件恢复失败，无法自动恢复: {}", pluginId);
            
            // 更新状态
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isPresent()) {
                PluginInfo info = optPluginInfo.get();
                info.setState(PluginState.FAILED);
                info.setErrorMessage("自动恢复失败");
                pluginRegistry.updatePlugin(info);
                stateManager.recordFailure(pluginId, "自动恢复失败");
            }
        } catch (Exception e) {
            log.error("尝试恢复插件时发生异常: {}", pluginId, e);
        }
    }
    
    /**
     * 获取插件健康状态
     *
     * @param pluginId 插件ID
     * @return 健康状态
     */
    public PluginHealthStatus getPluginHealthStatus(String pluginId) {
        return healthStatuses.getOrDefault(pluginId, PluginHealthStatus.unknown());
    }
    
    /**
     * 获取所有不健康的插件ID
     *
     * @return 不健康的插件ID集合
     */
    public Set<String> getUnhealthyPlugins() {
        return healthStatuses.entrySet().stream()
                .filter(entry -> !entry.getValue().isHealthy())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    
    /**
     * 获取插件版本更新历史
     *
     * @param pluginId 插件ID
     * @return 版本更新历史
     */
    public Map<String, String> getPluginVersionHistory(String pluginId) {
        return versionHistory.getOrDefault(pluginId, new HashMap<>());
    }
    
    /**
     * 插件健康状态
     */
    public static class PluginHealthStatus {
        private boolean healthy;
        private String message;
        private int failCount;
        private long lastCheckTime;
        
        private PluginHealthStatus(boolean healthy, String message) {
            this.healthy = healthy;
            this.message = message;
            this.failCount = 0;
            this.lastCheckTime = System.currentTimeMillis();
        }
        
        public static PluginHealthStatus healthy() {
            return new PluginHealthStatus(true, "健康");
        }
        
        public static PluginHealthStatus unhealthy(String reason) {
            return new PluginHealthStatus(false, reason);
        }
        
        public static PluginHealthStatus unknown() {
            return new PluginHealthStatus(false, "未知状态");
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getFailCount() {
            return failCount;
        }
        
        public void incrementFailCount() {
            this.failCount++;
            this.lastCheckTime = System.currentTimeMillis();
        }
        
        public long getLastCheckTime() {
            return lastCheckTime;
        }
    }

    /**
     * 安装插件
     *
     * @param pluginFile 插件文件
     * @return 安装的插件信息
     * @throws PluginLifecycleException 安装失败异常
     */
    public PluginInfo installPlugin(File pluginFile) throws PluginLifecycleException {
        try {
            log.info("开始安装插件: {}", pluginFile.getName());
            
            // 这里应该有插件文件解析和加载逻辑
            PluginInfo pluginInfo = parsePluginFile(pluginFile); // 添加实际解析逻辑
            if (pluginInfo != null) {
                persistenceService.savePluginInfo(pluginInfo);
                persistenceService.updatePluginStatus(pluginInfo);
                persistenceService.savePluginVersion(pluginInfo, versionManager);
            }
            
            return pluginInfo;
        } catch (Exception e) {
            log.error("安装插件失败: {}", pluginFile.getName(), e);
            throw new PluginLifecycleException("安装插件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 应用程序启动时恢复插件状态
     */
    @PostConstruct
    public void restorePluginState() {
        log.info("开始恢复插件状态...");
        try {
            List<String> pluginsToStart = persistenceService.restorePluginStatus();
            for (String pluginId : pluginsToStart) {
                try {
                    startPlugin(pluginId);
                    log.info("成功恢复插件: {}", pluginId);
                } catch (Exception e) {
                    log.error("恢复插件状态失败: {}, 错误: {}", pluginId, e.getMessage());
                }
            }
            log.info("插件状态恢复完成, 共恢复{}个插件", pluginsToStart.size());
        } catch (Exception e) {
            log.error("恢复插件状态过程中发生错误: {}", e.getMessage());
        }
    }

    /**
     * 获取插件信息
     *
     * @param pluginId 插件ID
     * @return 插件信息
     */
    public Optional<PluginInfo> getPluginInfo(String pluginId) {
        return pluginRegistry.getPlugin(pluginId);
    }

    /**
     * 从文件路径加载插件
     *
     * @param filePath 插件文件路径
     * @param initialize 是否初始化插件
     * @return 插件信息
     * @throws PluginLifecycleException 生命周期异常
     */
    public PluginInfo loadPlugin(String filePath, boolean initialize) throws PluginLifecycleException {
        log.info("从文件路径加载插件: {}, 初始化: {}", filePath, initialize);
        File pluginFile = new File(filePath);
        
        if (!pluginFile.exists()) {
            throw new PluginLifecycleException("插件文件不存在: " + filePath);
        }
        
        PluginInfo pluginInfo = installPlugin(pluginFile);
        
        // 如果需要初始化插件，则调用initializePlugin方法
        if (initialize && pluginInfo != null) {
            String pluginId = pluginInfo.getDescriptor().getPluginId();
            initializePlugin(pluginId);
        }
        
        return pluginInfo;
    }

    /**
     * 通过反射递归检查类是否实现了Plugin接口
     * 解决类加载器隔离导致的接口兼容性问题
     *
     * @param clazz 要检查的类
     * @return 是否实现了Plugin接口
     */
    private boolean isPluginImplementation(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        
        // 检查类实现的所有接口
        Class<?>[] interfaces = clazz.getInterfaces();
        
        for (Class<?> iface : interfaces) {
            // 检查接口是否是Plugin接口（检查准确的包名）
            if (iface.getName().equals("com.xiaoqu.qteamos.api.core.plugin.Plugin") || 
                iface.getName().equals("com.xiaoqu.qteamos.sdk.plugin.Plugin")) {
                return true;
            }
        }
        
        // 递归检查父类
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return isPluginImplementation(superClass);
        }
        
        return false;
    }

    /**
     * 解析插件文件
     *
     * @param pluginFile 插件文件
     * @return 插件信息
     */
    private PluginInfo parsePluginFile(File pluginFile) {
        try {
            log.debug("解析插件文件: {}", pluginFile.getName());
            // 使用PluginDescriptorLoader加载描述符
            PluginDescriptorLoader loader = new PluginDescriptorLoader();
            PluginDescriptor descriptor = loader.loadFromJar(pluginFile);
            
            // 创建插件信息对象
            PluginInfo pluginInfo = PluginInfo.builder()
                .descriptor(descriptor)
                .state(PluginState.CREATED)
                .jarPath(Paths.get(pluginFile.getAbsolutePath()))
                .pluginFile(pluginFile)
                .build();
            
            // 创建类加载器
            try {
                DynamicClassLoader classLoader = classLoaderFactory.createClassLoader(
                    descriptor.getPluginId(),
                    pluginFile
                );
                pluginInfo.setClassLoader(classLoader);
            } catch (IOException e) {
                log.error("创建插件类加载器失败: {}", pluginFile.getName(), e);
                return null;
            }
            
            return pluginInfo;
        } catch (Exception e) {
            log.error("解析插件文件失败: {}", pluginFile.getName(), e);
            return null;
        }
    }
} 