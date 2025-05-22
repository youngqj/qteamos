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

package com.xiaoqu.qteamos.core.plugin.coordinator;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginFileWatcher;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInstaller;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginLifecycleHandler;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginScanner;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.event.plugins.SystemShutdownEvent;
import com.xiaoqu.qteamos.core.plugin.manager.DependencyResolver;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager;
import com.xiaoqu.qteamos.core.plugin.manager.persistence.PluginStatePersistenceManager;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.api.core.event.PluginEventDispatcher;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginLoadedEvent;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginInitializedEvent;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginStartedEvent;
import com.xiaoqu.qteamos.core.plugin.adapter.PluginInfoAdapter;

import jakarta.annotation.PreDestroy;

/**
 * 插件系统协调器
 * 作为插件系统的中央协调组件，整合扫描、监控、安装和生命周期管理等组件
 * 提供统一的对外接口，实现高内聚低耦合的插件系统架构
 *
 * @author yangqijun
 * @date 2025-05-26
 * @since 1.0.0
 */
@Component
public class PluginSystemCoordinator {
    private static final Logger log = LoggerFactory.getLogger(PluginSystemCoordinator.class);
    
    @Value("${plugin.storage-path:./plugins}")
    private String pluginDir;
    
    @Value("${plugin.temp-dir:./plugins-temp}")
    private String pluginTempDir;
    
    @Value("${plugin.auto-discover:true}")
    private boolean autoDiscoverEnabled;
    
    // 核心组件依赖
    private final PluginRegistry pluginRegistry;
    private final PluginLifecycleHandler lifecycleHandler;
    private final PluginStateManager stateManager;
    private final DependencyResolver dependencyResolver;
    private final EventBus eventBus;
    private final PluginScanner pluginScanner;
    private final PluginFileWatcher fileWatcher;
    private final PluginInstaller pluginInstaller;
    private final PluginStatePersistenceManager persistenceManager;
    private final PluginEventDispatcher eventDispatcher;
    private final PluginInfoAdapter pluginInfoAdapter;
    
    // 线程池
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "plugin-system-worker");
        t.setDaemon(true);
        return t;
    });
    
    // 记录插件操作状态
    private final Map<String, OperationStatus> pluginOperationStatus = new ConcurrentHashMap<>();
    
    /**
     * 构造函数，注入所有必要组件
     */
    @Autowired
    public PluginSystemCoordinator(
            PluginRegistry pluginRegistry,
            PluginLifecycleHandler lifecycleHandler,
            PluginStateManager stateManager,
            DependencyResolver dependencyResolver,
            EventBus eventBus,
            PluginScanner pluginScanner,
            PluginFileWatcher fileWatcher,
            PluginInstaller pluginInstaller,
            PluginStatePersistenceManager persistenceManager,
            PluginEventDispatcher eventDispatcher,
            PluginInfoAdapter pluginInfoAdapter) {
        
        this.pluginRegistry = pluginRegistry;
        this.lifecycleHandler = lifecycleHandler;
        this.stateManager = stateManager;
        this.dependencyResolver = dependencyResolver;
        this.eventBus = eventBus;
        this.pluginScanner = pluginScanner;
        this.fileWatcher = fileWatcher;
        this.pluginInstaller = pluginInstaller;
        this.persistenceManager = persistenceManager;
        this.eventDispatcher = eventDispatcher;
        this.pluginInfoAdapter = pluginInfoAdapter;
    }
    
    /**
     * 初始化插件系统
     * 此方法已被拆分为两个阶段：initExistingPlugins和startMonitoring
     * 保留此方法仅为兼容性考虑
     * 
     * @deprecated 使用initExistingPlugins()和startMonitoring()替代
     */
    @Deprecated
    public void init() {
        log.info("初始化插件系统协调器...");
        
        // 第一阶段：加载已有插件
        initExistingPlugins();
        
        // 第二阶段：启动监控和自动发现
        startMonitoring();
        
        log.info("插件系统协调器初始化完成");
    }
    
    /**
     * 初始化阶段一：从数据库加载已有插件
     * 此方法应在系统启动时调用，加载已有插件并完成实例化和路由注册
     */
    public void initExistingPlugins() {
        log.info("初始化插件系统 - 阶段一：加载已有插件...");
        
        // 设置类加载系统属性
        setupClassLoadingProperties();
        
        // 创建必要的目录
        createRequiredDirectories();
        
        // 从数据库加载已有插件
        loadExistingPlugins();
        
        log.info("插件系统阶段一初始化完成：已有插件加载完毕");
    }
    
    /**
     * 初始化阶段二：启动插件监控和自动发现
     * 此方法应在系统完全启动后调用，开始监控插件目录和扫描新插件
     */
    public void startMonitoring() {
        log.info("初始化插件系统 - 阶段二：启动监控和自动发现...");
        
        // 启动文件监控，用于监测新插件
        startFileWatching();
        
        // 如果开启了自动发现，则启动新插件发现机制
        if (autoDiscoverEnabled) {
            startPluginDiscovery();
        }
        
        log.info("插件系统阶段二初始化完成：文件监控和自动发现已启动");
    }
    
    /**
     * 设置类加载相关系统属性
     */
    private void setupClassLoadingProperties() {
        // 设置系统类加载器
        System.setProperty("java.system.class.loader", 
                "com.xiaoqu.qteamos.core.plugin.loader.DynamicClassLoader");
        
        // 允许从父类加载器重复加载类
        System.setProperty("spring.classloader.overrideStandard", "true");
        
        // 设置类加载器共享包
        System.setProperty("plugin.shared.packages", 
                "com.xiaoqu.qteamos.sdk,com.xiaoqu.qteamos.api," +
                "com.xiaoqu.qteamos.sdk.plugin,com.xiaoqu.qteamos.api.core.plugin");
        
        // 允许插件使用线程上下文类加载器
        System.setProperty("plugin.thread.contextClassLoader", "system");
    }
    
    /**
     * 创建必要的目录结构
     */
    private void createRequiredDirectories() {
        // 创建插件目录
        File dirFile = new File(pluginDir);
        if (!dirFile.exists() && dirFile.mkdirs()) {
            log.info("创建插件目录: {}", dirFile.getAbsolutePath());
        }
        
        // 创建临时插件目录
        File tempDirFile = new File(pluginTempDir);
        if (!tempDirFile.exists() && tempDirFile.mkdirs()) {
            log.info("创建临时插件目录: {}", tempDirFile.getAbsolutePath());
        }
    }
    
    /**
     * 从数据库加载已有插件
     */
    private void loadExistingPlugins() {
        log.info("从数据库加载已有插件...");
        try {
            // 从持久化存储获取所有已注册的插件信息
            Collection<PluginInfo> existingPlugins = persistenceManager.getAllPlugins();
            
            if (existingPlugins.isEmpty()) {
                log.info("数据库中没有已注册的插件");
                return;
            }

            log.info("发现{}个已注册插件，开始加载...", existingPlugins.size());
            
            for (PluginInfo pluginInfo : existingPlugins) {
                try {
                    String pluginId = pluginInfo.getDescriptor().getPluginId();
                    Path jarPath = pluginInfo.getJarPath();
                    
                    // 处理相对路径：如果是相对路径，则转换为绝对路径
                    if (jarPath != null && !jarPath.isAbsolute()) {
                        // 获取当前运行目录
                        String userDir = System.getProperty("user.dir");
                        // 创建绝对路径基目录（当前目录 + 配置的插件目录）
                        File baseDir = new File(userDir, pluginDir).getAbsoluteFile();
                        
                        // 结合传入的相对路径
                        String pathStr = jarPath.toString();
                        if (pathStr.startsWith("./")) {
                            pathStr = pathStr.substring(2);
                        }
                        
                        File resolvedFile = new File(baseDir, pathStr);
                        // 获取最终的规范化绝对路径
                        jarPath = resolvedFile.toPath().normalize();
                        log.info("将相对路径转换为真正的绝对路径: {} -> {}", jarPath, resolvedFile.toPath());
                        // 更新插件信息中的路径
                        pluginInfo.setJarPath(jarPath);
                    }
                    
                    // 验证插件文件是否存在
                    if (!jarPath.toFile().exists()) {
                        log.warn("插件文件不存在，跳过加载: {}", jarPath);
                        continue;
                    }

                    // 转换为API层PluginInfo
                    com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo apiPluginInfo = 
                        pluginInfoAdapter.toApiPluginInfo(pluginInfo);

                    // 根据插件状态决定处理方式
                    if (pluginInfo.isEnabled() ) {
                        log.info("加载已启用的插件: {}", pluginId);
                        // 对于之前运行的插件，执行完整的加载流程
                        processPluginLoading(jarPath, apiPluginInfo);
                    } else {
                        log.info("注册未启用的插件: {}", pluginId);
                        // 对于未启用的插件，只进行基础加载
                        lifecycleHandler.loadPlugin(apiPluginInfo);
                    }
                } catch (Exception e) {
                    log.error("加载已有插件失败: {}", pluginInfo.getDescriptor().getPluginId(), e);
                }
            }
            
            log.info("已有插件加载完成");
            
        } catch (Exception e) {
            log.error("加载已有插件过程中发生错误", e);
        }
    }
    
    /**
     * 启动插件发现过程
     */
    private void startPluginDiscovery() {
        executor.submit(() -> {
            log.info("开始扫描新插件...");
            
            // 获取已知插件路径列表
            Set<Path> knownPluginPaths = new HashSet<>();
            pluginRegistry.getAllPlugins().forEach(plugin -> 
                knownPluginPaths.add(plugin.getJarPath()));
            
            // 扫描新插件，排除已知插件
            try {
                // 使用插件扫描器扫描插件目录，跳过已知插件
                pluginScanner.scanPlugins(Path.of(pluginDir));
                log.info("新插件扫描完成");
            } catch (Exception e) {
                log.error("扫描新插件异常", e);
            }
            
            // 处理临时目录中的插件
            processTempDirectory();
        });
    }
    
    /**
     * 启动文件监控
     */
    private void startFileWatching() {
        executor.submit(() -> {
            try {
                log.info("启动插件目录监控...");
                
                // 获取已知插件路径，用于文件监控过滤
                Set<Path> knownPaths = getExcludedPaths();
                
                // 创建过滤器
                Predicate<Path> filter = path -> !knownPaths.contains(path);
                
                // 开始监控插件目录，配置监控过滤器
                fileWatcher.startWatchingWithFilter(Path.of(pluginDir), filter);
                
                // 添加临时目录到监控
                fileWatcher.addWatchDirectory(Path.of(pluginTempDir));
                
                log.info("插件目录监控已启动");
            } catch (Exception e) {
                log.error("启动文件监控失败", e);
            }
        });
    }
    
    /**
     * 获取需要排除监控的路径
     */
    private Set<Path> getExcludedPaths() {
        Set<Path> excludePaths = new HashSet<>();
        // 添加已知插件路径
        pluginRegistry.getAllPlugins().forEach(plugin -> 
            excludePaths.add(plugin.getJarPath()));
        // 可以添加其他需要排除的路径
        return excludePaths;
    }
    
    /**
     * 处理插件加载的完整流程
     * 
     * @param jarPath 插件JAR路径
     * @param apiPluginInfo 插件信息
     * @throws PluginLifecycleException 生命周期异常
     */
    private void processPluginLoading(Path jarPath, com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo apiPluginInfo) 
            throws PluginLifecycleException {
        String pluginId = apiPluginInfo.getPluginId();
        String version = apiPluginInfo.getVersion();
        
        log.info("开始处理插件加载流程: {} v{}", pluginId, version);
        
        // 检查插件类型和信任级别
        boolean isSystemPlugin = "system".equals(apiPluginInfo.getType());
        boolean isTrusted = "trusted".equals(apiPluginInfo.getTrust());
        
        log.info("====> 插件类型: {}, 信任级别: {}, 是否系统插件: {}, 是否可信: {}", 
            apiPluginInfo.getType(), apiPluginInfo.getTrust(), isSystemPlugin, isTrusted);
        
        // 记录操作状态
        OperationStatus status = new OperationStatus(pluginId, OperationType.LOAD);
        pluginOperationStatus.put(pluginId, status);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // 1. 加载插件
            log.info("加载插件: {}", pluginId);
            lifecycleHandler.loadPlugin(apiPluginInfo);
            
            // 获取更新后的核心插件信息
            Optional<PluginInfo> corePluginInfoOpt = pluginRegistry.getPlugin(pluginId);
            if (corePluginInfoOpt.isEmpty()) {
                throw new PluginLifecycleException("加载后无法获取插件信息: " + pluginId);
            }
            PluginInfo corePluginInfo = corePluginInfoOpt.get();
            
            // 发布加载事件
            publishLoadedEvent(pluginId, version, corePluginInfo, startTime);
            
            // 2. 根据插件类型和信任级别决定后续处理
            if (isSystemPlugin && isTrusted) {
                // 系统级可信插件，自动初始化和启动
                log.info("====> 检测到系统级可信插件，自动初始化并启动: {}", pluginId);
                
                // 初始化插件
                log.info("初始化插件: {}", pluginId);
                lifecycleHandler.initializePlugin(pluginId);
                
                // 发布初始化事件
                publishInitializedEvent(pluginId, version);
                
                // 启动插件
                log.info("启动插件: {}", pluginId);
                lifecycleHandler.startPlugin(pluginId);
                
                // 发布启动事件
                publishStartedEvent(pluginId, version);
            } else {
                // 普通插件或不可信的系统插件，仅完成加载
                log.info("====> 检测到普通插件或不可信系统插件，仅完成加载: {}", pluginId);
                log.info("插件已加载: {}，需要通过管理界面启用", pluginId);
            }
            
            // 更新操作状态
            status.setSuccess(true);
            status.setMessage("插件加载流程处理完成");
            
            log.info("插件加载流程处理完成: {} v{}", pluginId, version);
            
        } catch (Exception e) {
            String errorMsg = "插件加载流程异常: " + e.getMessage();
            log.error(errorMsg, e);
            
            // 更新操作状态
            status.setSuccess(false);
            status.setMessage(errorMsg);
            
            // 记录失败状态
            stateManager.recordFailure(pluginId, errorMsg);
            
            throw new PluginLifecycleException(errorMsg, e);
        }
    }
    
    /**
     * 发布插件加载事件
     */
    private void publishLoadedEvent(String pluginId, String version, PluginInfo corePluginInfo, long startTime) {
        // 发布新事件
        PluginLoadedEvent loadedEvent = new PluginLoadedEvent(
            pluginId,
            version,
            corePluginInfo.getClassLoader() != null ? corePluginInfo.getClassLoader().getLoadedClassCount() : 0,
            corePluginInfo.getClassLoader() != null ? corePluginInfo.getClassLoader().getResourceLoadCount() : 0,
            System.currentTimeMillis() - startTime
        );
        eventDispatcher.publishEvent(loadedEvent);
        
        // 兼容性：发布旧事件
        eventBus.postEvent(PluginEvent.createLoadedEvent(pluginId, version));
    }
    
    /**
     * 发布插件初始化事件
     */
    private void publishInitializedEvent(String pluginId, String version) {
        // 发布新事件
        PluginInitializedEvent initializedEvent = new PluginInitializedEvent(
            pluginId,
            version,
            null, // pluginInfo
            0,    // initializationTime
            null  // dependencies
        );
        eventDispatcher.publishEvent(initializedEvent);
        
        // 兼容性：发布旧事件
        eventBus.postEvent(PluginEvent.createInitializedEvent(pluginId, version));
    }
    
    /**
     * 发布插件启动事件
     */
    private void publishStartedEvent(String pluginId, String version) {
        // 发布新事件
        PluginStartedEvent startedEvent = new PluginStartedEvent(
            pluginId,
            version,
            null, // pluginInfo
            0     // startupTime
        );
        eventDispatcher.publishEvent(startedEvent);
        
        // 兼容性：发布旧事件
        eventBus.postEvent(PluginEvent.createStartedEvent(pluginId, version));
    }

    /**
     * 加载插件
     *
     * @param jarPath 插件JAR路径
     * @return 插件ID
     * @throws PluginLifecycleException 生命周期异常
     */
    public String loadPlugin(Path jarPath) throws PluginLifecycleException {
        log.info("开始加载插件: {}", jarPath);
        
        // 处理相对路径：如果是相对路径，则转换为绝对路径
        if (jarPath != null && !jarPath.isAbsolute()) {
            // 获取当前运行目录
            String userDir = System.getProperty("user.dir");
            // 创建绝对路径基目录（当前目录 + 配置的插件目录）
            File baseDir = new File(userDir, pluginDir).getAbsoluteFile();
            
            // 结合传入的相对路径
            String pathStr = jarPath.toString();
            if (pathStr.startsWith("./")) {
                pathStr = pathStr.substring(2);
            }
            
            File resolvedFile = new File(baseDir, pathStr);
            // 获取最终的规范化绝对路径
            jarPath = resolvedFile.toPath().normalize();
            log.info("将相对路径转换为真正的绝对路径: {} -> {}", jarPath, resolvedFile.toPath());
        }
        
        // 通过安装器安装插件
        com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo apiPluginInfo = pluginInstaller.installFromPath(jarPath);
        if (apiPluginInfo == null) {
            throw new PluginLifecycleException("安装插件失败: " + jarPath);
        }
        
        // 处理完整的插件加载流程
        processPluginLoading(jarPath, apiPluginInfo);
        
        return apiPluginInfo.getPluginId();
    }
    
    /**
     * 卸载插件
     *
     * @param pluginId 插件ID
     * @return 卸载是否成功
     */
    public boolean unloadPlugin(String pluginId) {
        log.info("卸载插件: {}", pluginId);
        
        // 记录操作状态
        OperationStatus status = new OperationStatus(pluginId, OperationType.UNLOAD);
        pluginOperationStatus.put(pluginId, status);
        
        try {
            // 检查插件是否存在
            if (!pluginRegistry.hasPlugin(pluginId)) {
                status.setSuccess(false);
                status.setMessage("插件不存在");
                return false;
            }
            
            // 检查依赖关系
            if (!canUnloadPlugin(pluginId)) {
                status.setSuccess(false);
                status.setMessage("插件被其他插件依赖，无法卸载");
                return false;
            }
            
            // 获取插件信息
            Optional<PluginInfo> infoOpt = pluginRegistry.getPlugin(pluginId);
            if (infoOpt.isEmpty()) {
                status.setSuccess(false);
                status.setMessage("无法获取插件信息");
                return false;
            }
            String version = infoOpt.get().getDescriptor().getVersion();
            
            // 执行卸载
            boolean result = lifecycleHandler.unloadPlugin(pluginId);
            
            if (result) {
                // 使用新的事件分发机制发布事件
                com.xiaoqu.qteamos.api.core.event.lifecycle.PluginUnloadedEvent unloadedEvent = 
                    new com.xiaoqu.qteamos.api.core.event.lifecycle.PluginUnloadedEvent(
                        pluginId, 
                        version, 
                        System.currentTimeMillis(), 
                        true, 
                        "用户请求卸载"
                    );
                eventDispatcher.publishEvent(unloadedEvent);
                
                // 兼容性：发布旧事件
                eventBus.postEvent(PluginEvent.createUnloadedEvent(pluginId, version));
                
                // 更新操作状态
                status.setSuccess(true);
                status.setMessage("卸载成功");
            } else {
                status.setSuccess(false);
                status.setMessage("卸载失败");
            }
            
            return result;
        } catch (Exception e) {
            status.setSuccess(false);
            status.setMessage("卸载异常: " + e.getMessage());
            log.error("卸载插件异常: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 检查插件是否可以卸载
     * 
     * @param pluginId 插件ID
     * @return 是否可以卸载
     */
    private boolean canUnloadPlugin(String pluginId) {
        // 检查是否有其他插件依赖此插件
        return dependencyResolver.getDependentPlugins(pluginId).isEmpty();
    }
    
    /**
     * 启用插件
     *
     * @param pluginId 插件ID
     * @return 启用是否成功
     */
    public boolean enablePlugin(String pluginId) {
        log.info("启用插件: {}", pluginId);
        
        OperationStatus status = new OperationStatus(pluginId, OperationType.ENABLE);
        pluginOperationStatus.put(pluginId, status);
        
        try {
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                status.setSuccess(false);
                status.setMessage("插件不存在");
                return false;
            }
            
            PluginInfo pluginInfo = optPluginInfo.get();
            if (pluginInfo.isEnabled()) {
                status.setSuccess(true);
                status.setMessage("插件已处于启用状态");
                return true;
            }
            
            // 根据插件状态执行不同的启用流程
            boolean result = enablePluginByState(pluginId, pluginInfo);
            
            status.setSuccess(result);
            status.setMessage(result ? "启用成功" : "启用失败");
            
            return result;
        } catch (Exception e) {
            status.setSuccess(false);
            status.setMessage("启用异常: " + e.getMessage());
            log.error("启用插件异常: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 根据插件状态执行不同的启用流程
     * 
     * @param pluginId 插件ID
     * @param pluginInfo 插件信息
     * @return 启用是否成功
     * @throws PluginLifecycleException 生命周期异常
     */
    private boolean enablePluginByState(String pluginId, PluginInfo pluginInfo) throws PluginLifecycleException {
        PluginState state = pluginInfo.getState();
        String version = pluginInfo.getDescriptor().getVersion();
        boolean result = false;
        
        switch (state) {
            case CREATED:
                // 需要完整的加载、初始化和启动流程
                // 转换为API层PluginInfo
                com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo apiPluginInfo = 
                    pluginInfoAdapter.toApiPluginInfo(pluginInfo);
                lifecycleHandler.loadPlugin(apiPluginInfo);
                lifecycleHandler.initializePlugin(pluginId);
                result = lifecycleHandler.startPlugin(pluginId);
                break;
                
            case LOADED:
                // 需要初始化和启动
                lifecycleHandler.initializePlugin(pluginId);
                result = lifecycleHandler.startPlugin(pluginId);
                break;
                
            case INITIALIZED:
                // 只需要启动
                result = lifecycleHandler.startPlugin(pluginId);
                break;
                
            case STOPPED:
                // 重新启动
                result = lifecycleHandler.startPlugin(pluginId);
                break;
                
            case RUNNING:
                // 已经运行，只需标记为启用
                pluginInfo.setEnabled(true);
                pluginRegistry.updatePlugin(pluginInfo);
                result = true;
                break;
                
            default:
                log.warn("插件状态不正确，无法启用: {}, 当前状态: {}", pluginId, state);
                return false;
        }
        
        if (result) {
            // 发布插件启用事件
            eventBus.postEvent(PluginEvent.createEnabledEvent(pluginId, version));
        }
        
        return result;
    }
    
    /**
     * 禁用插件
     *
     * @param pluginId 插件ID
     * @return 禁用是否成功
     */
    public boolean disablePlugin(String pluginId) {
        log.info("禁用插件: {}", pluginId);
        
        OperationStatus status = new OperationStatus(pluginId, OperationType.DISABLE);
        pluginOperationStatus.put(pluginId, status);
        
        try {
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                status.setSuccess(false);
                status.setMessage("插件不存在");
                return false;
            }
            
            PluginInfo pluginInfo = optPluginInfo.get();
            if (!pluginInfo.isEnabled()) {
                status.setSuccess(true);
                status.setMessage("插件已处于禁用状态");
                return true;
            }
            
            boolean result = true;
            
            // 如果插件处于运行状态，需要先停止
            if (pluginInfo.getState() == PluginState.RUNNING) {
                result = lifecycleHandler.stopPlugin(pluginId);
            }
            
            // 标记为禁用
            pluginInfo.setEnabled(false);
            pluginRegistry.updatePlugin(pluginInfo);
            
            // 保存状态
            persistenceManager.savePluginInfo(pluginInfo);
            
            // 发布禁用事件
            if (result) {
                eventBus.postEvent(PluginEvent.createDisabledEvent(pluginId, pluginInfo.getDescriptor().getVersion()));
            }
            
            status.setSuccess(result);
            status.setMessage(result ? "禁用成功" : "禁用失败");
            
            return result;
        } catch (Exception e) {
            status.setSuccess(false);
            status.setMessage("禁用异常: " + e.getMessage());
            log.error("禁用插件异常: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 重载插件
     *
     * @param pluginId 插件ID
     * @return 重载是否成功
     */
    public boolean reloadPlugin(String pluginId) {
        log.info("重载插件: {}", pluginId);
        
        OperationStatus status = new OperationStatus(pluginId, OperationType.RELOAD);
        pluginOperationStatus.put(pluginId, status);
        
        try {
            Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
            if (optPluginInfo.isEmpty()) {
                status.setSuccess(false);
                status.setMessage("插件不存在");
                return false;
            }
            
            // 记录旧版本和启用状态
            PluginInfo pluginInfo = optPluginInfo.get();
            Path jarPath = pluginInfo.getJarPath();
            boolean wasEnabled = pluginInfo.isEnabled();
            
            // 卸载插件
            if (!unloadPlugin(pluginId)) {
                status.setSuccess(false);
                status.setMessage("卸载插件失败，无法重载");
                return false;
            }
            
            // 重新加载插件
            try {
                loadPlugin(jarPath);
                
                // 如果之前是禁用状态，则重载后也保持禁用
                if (!wasEnabled) {
                    disablePlugin(pluginId);
                }
                
                status.setSuccess(true);
                status.setMessage("重载成功");
                return true;
            } catch (Exception e) {
                status.setSuccess(false);
                status.setMessage("重载失败: " + e.getMessage());
                log.error("重载插件失败: {}", pluginId, e);
                return false;
            }
        } catch (Exception e) {
            status.setSuccess(false);
            status.setMessage("重载异常: " + e.getMessage());
            log.error("重载插件异常: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 获取所有插件信息
     *
     * @return 所有插件信息
     */
    public Collection<PluginInfo> getAllPlugins() {
        return pluginRegistry.getAllPlugins();
    }
    
    /**
     * 获取已启用的插件
     *
     * @return 已启用的插件
     */
    public Collection<PluginInfo> getEnabledPlugins() {
        return pluginRegistry.getPluginsByState(true);
    }
    
    /**
     * 获取已禁用的插件
     *
     * @return 已禁用的插件
     */
    public Collection<PluginInfo> getDisabledPlugins() {
        return pluginRegistry.getPluginsByState(false);
    }
    
    /**
     * 获取插件信息
     *
     * @param pluginId 插件ID
     * @return 插件信息
     */
    public Optional<PluginInfo> getPlugin(String pluginId) {
        return pluginRegistry.getPlugin(pluginId);
    }
    
    /**
     * 获取插件实例
     *
     * @param pluginId 插件ID
     * @return 插件实例
     */
    public Optional<Plugin> getPluginInstance(String pluginId) {
        return lifecycleHandler.getPluginInstance(pluginId);
    }
    
    /**
     * 获取插件操作状态
     *
     * @param pluginId 插件ID
     * @return 操作状态
     */
    public Optional<OperationStatus> getPluginOperationStatus(String pluginId) {
        return Optional.ofNullable(pluginOperationStatus.get(pluginId));
    }
    
    /**
     * 关闭插件系统
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭插件系统...");
        
        // 中断所有执行任务
        executor.shutdownNow();
        
        // 停止文件监控
        fileWatcher.stopWatching();
        
        // 发布系统关闭事件
        eventBus.postEvent(new SystemShutdownEvent(SystemShutdownEvent.ShutdownReason.NORMAL));
        
        // 卸载所有插件
        for (PluginInfo pluginInfo : pluginRegistry.getAllPlugins()) {
            try {
                String pluginId = pluginInfo.getDescriptor().getPluginId();
                lifecycleHandler.unloadPlugin(pluginId);
            } catch (Exception e) {
                log.error("卸载插件异常", e);
            }
        }
        
        log.info("插件系统已关闭");
    }
    
    /**
     * 插件操作类型
     */
    public enum OperationType {
        LOAD,
        UNLOAD,
        ENABLE,
        DISABLE,
        RELOAD,
        INSTALL,
        UNINSTALL,
        UPDATE
    }
    
    /**
     * 插件操作状态
     */
    public static class OperationStatus {
        private final String pluginId;
        private final OperationType type;
        private final long startTime;
        private long endTime;
        private boolean success;
        private String message;
        
        public OperationStatus(String pluginId, OperationType type) {
            this.pluginId = pluginId;
            this.type = type;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public OperationType getType() {
            return type;
        }
        
        public long getStartTime() {
            return startTime;
        }
        
        public long getEndTime() {
            return endTime;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
            if (this.endTime == 0) {
                this.endTime = System.currentTimeMillis();
            }
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getDuration() {
            return (endTime > 0 ? endTime : System.currentTimeMillis()) - startTime;
        }
        
        public boolean isCompleted() {
            return endTime > 0;
        }
    }

    /**
     * 处理临时目录中的插件
     */
    private void processTempDirectory() {
        File tempDir = new File(pluginTempDir);
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            return;
        }
        
        File[] files = tempDir.listFiles(file -> 
            !file.getName().startsWith(".") && !file.getName().equals("README.txt"));
            
        if (files == null || files.length == 0) {
            return;
        }
        
        log.info("处理临时目录中的{}个文件", files.length);
        for (File file : files) {
            try {
                if (file.isDirectory() || file.getName().endsWith(".jar")) {
                    pluginInstaller.processTempFile(file);
                } else {
                    log.warn("忽略不支持的文件: {}", file.getName());
                }
            } catch (Exception e) {
                log.error("处理临时文件失败: {}", file.getName(), e);
            }
        }
    }
} 