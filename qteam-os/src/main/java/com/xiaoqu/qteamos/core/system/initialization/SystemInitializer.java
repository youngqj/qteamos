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
 * 系统初始化器
 * 负责协调系统的启动流程，控制各个核心组件的初始化顺序
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.system.initialization;

import com.xiaoqu.qteamos.core.cache.CacheService;
import com.xiaoqu.qteamos.core.databases.core.DatabaseService;
import com.xiaoqu.qteamos.core.gateway.GatewayService;
import com.xiaoqu.qteamos.core.plugin.coordinator.PluginSystemCoordinator;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.plugins.SystemStartupEvent;
import com.xiaoqu.qteamos.core.security.SecurityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统初始化器
 * 负责协调系统的启动流程，控制各个核心组件的初始化顺序
 */
@Component
@Order(1)  // 最高优先级
public class SystemInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SystemInitializer.class);
    
    /**
     * 系统启动属性
     */
    @Autowired
    private SystemStartupProperties properties;
    
    /**
     * 系统Banner显示
     */
    @Autowired
    private SystemBanner systemBanner;
    
    /**
     * 事件总线
     */
    @Autowired
    private EventBus eventBus;
    
    /**
     * 数据库服务
     */
    @Autowired(required = false)
    private DatabaseService databaseService;
    
    /**
     * 缓存服务
     */
    @Autowired(required = false)
    private CacheService cacheService;
    
    /**
     * 安全服务
     */
    @Autowired(required = false)
    private SecurityService securityService;
    
    /**
     * 网关服务
     */
    @Autowired(required = false)
    private GatewayService gatewayService;
    
    /**
     * 插件系统
     */
    @Autowired
    private PluginSystemCoordinator pluginSystemCoordinator;
    
    /**
     * 当前启动阶段
     */
    private final AtomicReference<StartupPhase> currentPhase = new AtomicReference<>(StartupPhase.PREPARING);
    
    /**
     * 启动时间记录
     */
    private long startTime;
    
    /**
     * 异步执行器
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    /**
     * 初始化方法
     * 在Spring容器初始化后自动执行
     */
    @PostConstruct
    public void init() {
        // 记录启动开始时间
        startTime = System.currentTimeMillis();
        // 显示系统Banner
        systemBanner.showBanner();
        log.info("系统正在启动...");
    }
    
    /**
     * 应用启动入口方法
     * 在Spring容器启动后执行
     *
     * @param args 应用程序参数
     * @throws Exception 启动异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("系统初始化器开始执行...");
        
        try {
            // 发布系统启动事件
            eventBus.postEvent(new SystemStartupEvent());
            
            // 初始化数据库
            if (databaseService != null) {
                log.info("执行数据库初始化...");
                databaseService.initialize();
            }
            
            // 初始化API网关
            if (gatewayService != null) {
                log.info("执行API网关初始化...");
                gatewayService.initialize();
            }
            
            // 初始化插件系统
            if (properties.isAutoLoadPlugins()) {
                log.info("执行插件系统初始化...");
                // 第一阶段：初始化和加载已有插件
                pluginSystemCoordinator.initExistingPlugins();
                log.info("插件系统初始化第一阶段完成");
            }
        } catch (Exception e) {
            log.error("系统初始化过程中发生错误", e);
            throw e;
        }
    }
    
    /**
     * 更新当前启动阶段
     *
     * @param phase 新的启动阶段
     */
    private void updatePhase(StartupPhase phase) {
        StartupPhase oldPhase = currentPhase.getAndSet(phase);
        log.info("系统启动阶段从 {} 变更为 {}", oldPhase.getName(), phase.getName());
    }
    
    /**
     * 启动核心服务
     *
     * @throws Exception 启动异常
     */
    private void startCoreServices() throws Exception {
        updatePhase(StartupPhase.CORE_SERVICES);
        log.info("正在启动核心服务...");
        
        // 构建服务启动顺序
        List<ServiceStartupTask> startupTasks = buildServiceStartupTasks();
        
        if (properties.isAsyncStartup()) {
            // 异步启动
            startServicesAsync(startupTasks);
        } else {
            // 同步启动
            startServicesSync(startupTasks);
        }
    }
    
    /**
     * 构建服务启动任务列表
     *
     * @return 服务启动任务列表
     */
    private List<ServiceStartupTask> buildServiceStartupTasks() {
        List<ServiceStartupTask> tasks = new ArrayList<>();
        
        // 数据库服务
        if (databaseService != null) {
            tasks.add(new ServiceStartupTask(
                "数据库服务",
                properties.getCoreServices().getDatabaseOrder(),
                () -> {
                    log.info("正在启动数据库服务...");
                    databaseService.initialize();
                    log.info("数据库服务启动完成");
                }
            ));
        }
        
        // 缓存服务
        if (cacheService != null) {
            tasks.add(new ServiceStartupTask(
                "缓存服务",
                properties.getCoreServices().getCacheOrder(),
                () -> {
                    log.info("正在启动缓存服务...");
                    cacheService.initialize();
                    log.info("缓存服务启动完成");
                }
            ));
        }
        
        // 安全服务
        if (securityService != null) {
            tasks.add(new ServiceStartupTask(
                "安全服务",
                properties.getCoreServices().getSecurityOrder(),
                () -> {
                    log.info("正在启动安全服务...");
                    securityService.initialize();
                    log.info("安全服务启动完成");
                }
            ));
        }
        
        // 网关服务
        if (gatewayService != null) {
            tasks.add(new ServiceStartupTask(
                "网关服务",
                properties.getCoreServices().getGatewayOrder(),
                () -> {
                    log.info("正在启动网关服务...");
                    gatewayService.initialize();
                    log.info("网关服务启动完成");
                }
            ));
        }
        
        // 按照配置的顺序排序
        tasks.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        
        return tasks;
    }
    
    /**
     * 同步启动服务
     *
     * @param tasks 服务启动任务列表
     * @throws Exception 启动异常
     */
    private void startServicesSync(List<ServiceStartupTask> tasks) throws Exception {
        for (ServiceStartupTask task : tasks) {
            log.info("启动服务: {} (顺序: {})", task.getName(), task.getOrder());
            long startTime = System.currentTimeMillis();
            
            try {
                task.getStartupAction().run();
                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("服务 {} 启动完成，耗时: {} 毫秒", task.getName(), elapsedTime);
            } catch (Exception e) {
                log.error("服务 {} 启动失败", task.getName(), e);
                if (!properties.isContinueOnError()) {
                    throw e;
                }
            }
        }
    }
    
    /**
     * 异步启动服务
     *
     * @param tasks 服务启动任务列表
     * @throws Exception 启动异常
     */
    private void startServicesAsync(List<ServiceStartupTask> tasks) throws Exception {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (ServiceStartupTask task : tasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                log.info("异步启动服务: {} (顺序: {})", task.getName(), task.getOrder());
                long startTime = System.currentTimeMillis();
                
                try {
                    task.getStartupAction().run();
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    log.info("服务 {} 启动完成，耗时: {} 毫秒", task.getName(), elapsedTime);
                } catch (Exception e) {
                    log.error("服务 {} 启动失败", task.getName(), e);
                    if (!properties.isContinueOnError()) {
                        throw new RuntimeException("服务启动失败: " + task.getName(), e);
                    }
                }
            }, executor);
            
            futures.add(future);
        }
        
        try {
            // 等待所有服务启动完成，设置超时时间
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(properties.getTimeoutMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("服务启动超时或异常", e);
            if (!properties.isContinueOnError()) {
                throw e;
            }
        }
    }
    
    /**
     * 初始化插件系统
     */
    private void initializePluginSystem() {
        try {
            // 初始化插件系统，已改为无返回值
            pluginSystemCoordinator.initExistingPlugins();
            log.info("插件系统初始化完成");
        } catch (Exception e) {
            log.error("插件系统初始化失败", e);
            if (!properties.isContinueOnError()) {
                throw e;
            }
        }
    }
    
    /**
     * 启动应用服务
     */
    private void startApplicationServices() {
        // 应用服务的启动逻辑
        log.info("应用服务启动完成");
    }
    
    /**
     * 服务启动任务
     */
    private static class ServiceStartupTask {
        /**
         * 服务名称
         */
        private final String name;
        
        /**
         * 启动顺序
         */
        private final int order;
        
        /**
         * 启动动作
         */
        private final Runnable startupAction;
        
        /**
         * 构造函数
         *
         * @param name 服务名称
         * @param order 启动顺序
         * @param startupAction 启动动作
         */
        public ServiceStartupTask(String name, int order, Runnable startupAction) {
            this.name = name;
            this.order = order;
            this.startupAction = startupAction;
        }
        
        /**
         * 获取服务名称
         *
         * @return 服务名称
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取启动顺序
         *
         * @return 启动顺序
         */
        public int getOrder() {
            return order;
        }
        
        /**
         * 获取启动动作
         *
         * @return 启动动作
         */
        public Runnable getStartupAction() {
            return startupAction;
        }
    }

    // 在系统完全启动后调用第二阶段初始化
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            // 系统完全启动后，开始监控和自动发现
            pluginSystemCoordinator.startMonitoring();
            log.info("插件系统初始化第二阶段完成：监控和自动发现已启动");
        } catch (Exception e) {
            log.error("启动插件监控异常", e);
        }
    }
} 