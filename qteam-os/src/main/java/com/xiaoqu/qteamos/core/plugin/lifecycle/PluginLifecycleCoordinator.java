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

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginLifecycleHandler;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.adapter.PluginInfoAdapter;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginHealthMonitor;
import com.xiaoqu.qteamos.api.core.event.PluginEventDispatcher;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginStartedEvent;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginStoppedEvent;
import com.xiaoqu.qteamos.api.core.event.lifecycle.PluginUnloadedEvent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 插件生命周期协调器
 * 协调插件的整个生命周期管理过程，包括加载、初始化、启动、停止和卸载
 * 将复杂的PluginLifecycleManager拆分为多个职责单一的组件，协调他们的工作
 *
 * @author yangqijun
 * @date 2025-05-28
 * @since 1.0.0
 */
@Component
public class PluginLifecycleCoordinator implements PluginLifecycleHandler {
    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleCoordinator.class);
    
    // 临时使用PluginLifecycleManager，后续会迁移到各个子组件
    @Autowired
    private PluginLifecycleManager legacyManager;
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private PluginInfoAdapter pluginInfoAdapter;
    
    // 新组件：插件加载器
    @Autowired
    private DefaultPluginLoader pluginLoader;
    
    // 新组件：插件初始化器
    @Autowired
    private DefaultPluginInitializer pluginInitializer;
    
    // 缓存上一次的插件状态
    private final Map<String, String> lastStates = new HashMap<>();
    
    // 新组件：插件状态跟踪器
    @Autowired
    private DefaultPluginStateTracker stateTracker;
    
    // 记录插件的健康状态
    private final Map<String, PluginHealthStatus> healthStatuses = new ConcurrentHashMap<>();
    
    // 自动健康检查的执行器
    private ScheduledExecutorService healthCheckExecutor;
    
    @Value("${server.url:http://localhost:8080}")
    private String serverBaseUrl;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Autowired
    private PluginHealthMonitor healthMonitor;
    
    // 注入事件分发器
    @Autowired
    private PluginEventDispatcher eventDispatcher;
    
    // HTTP客户端，用于健康检查
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-lifecycle-health-check");
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
        
        log.info("插件生命周期协调器初始化完成");
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
        
        log.info("插件生命周期协调器已关闭");
    }
    
    @Override
    public boolean loadPlugin(PluginInfo apiPluginInfo) throws PluginLifecycleException {
        if (apiPluginInfo == null) {
            throw new PluginLifecycleException("插件信息不能为空");
        }
        
        String pluginId = apiPluginInfo.getPluginId();
        log.info("请求加载插件: {}", pluginId);
        
        try {
            // 使用新的插件加载器加载插件
            return pluginLoader.load(apiPluginInfo);
        } catch (Exception e) {
            String message = String.format("加载插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public boolean initializePlugin(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        log.info("请求初始化插件: {}", pluginId);
        
        try {
            // 使用新的插件初始化器初始化插件
            return pluginInitializer.initialize(pluginId);
        } catch (Exception e) {
            String message = String.format("初始化插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public boolean startPlugin(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        log.info("请求启动插件: {}", pluginId);
        
        try {
            // 获取插件实例
            Optional<Plugin> pluginInstance = getPluginInstance(pluginId);
            if (pluginInstance.isEmpty()) {
                log.error("插件实例不存在: {}", pluginId);
                return false;
            }
            
            // 检查状态
            Optional<String> stateOpt = stateTracker.getPluginState(pluginId);
            if (stateOpt.isEmpty() || !stateOpt.get().equals("INITIALIZED")) {
                log.error("插件状态不正确，无法启动: {}, 当前状态: {}", pluginId, stateOpt.orElse("UNKNOWN"));
                return false;
            }
            
            long startTime = System.currentTimeMillis();
            
            // 启动插件
            pluginInstance.get().start();
            
            // 更新插件状态
            stateTracker.recordStateChange(pluginId, "RUNNING");
            
            // 更新健康状态
            healthStatuses.put(pluginId, PluginHealthStatus.healthy());
            
            // 获取版本和插件信息
            Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> corePluginInfoOpt = 
                pluginRegistry.getPlugin(pluginId);
            if (corePluginInfoOpt.isEmpty()) {
                log.error("插件信息不存在: {}", pluginId);
                return false;
            }
            
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo corePluginInfo = corePluginInfoOpt.get();
            String version = corePluginInfo.getDescriptor().getVersion();
            
            // 计算启动耗时
            long duration = System.currentTimeMillis() - startTime;
            
            // 使用新的事件分发机制发布事件
            PluginStartedEvent startedEvent = new PluginStartedEvent(
                pluginId, 
                version, 
                pluginInfoAdapter.toApiPluginInfo(corePluginInfo), 
                duration
            );
            eventDispatcher.publishEvent(startedEvent);
            
            // 暂时保留兼容旧的事件系统，后续可以移除
            eventBus.postEvent(PluginEvent.createStartedEvent(pluginId, version));
            
            log.info("插件启动完成: {}, 耗时: {}ms", pluginId, duration);
            return true;
        } catch (Exception e) {
            String message = String.format("启动插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public boolean stopPlugin(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        log.info("请求停止插件: {}", pluginId);
        
        try {
            // 获取插件实例
            Optional<Plugin> pluginInstance = getPluginInstance(pluginId);
            if (pluginInstance.isEmpty()) {
                log.error("插件实例不存在: {}", pluginId);
                return false;
            }
            
            // 检查状态
            Optional<String> stateOpt = stateTracker.getPluginState(pluginId);
            if (stateOpt.isEmpty() || !stateOpt.get().equals("RUNNING")) {
                log.error("插件状态不正确，无法停止: {}, 当前状态: {}", pluginId, stateOpt.orElse("UNKNOWN"));
                return false;
            }
            
            long startTime = System.currentTimeMillis();
            
            // 停止插件
            pluginInstance.get().stop();
            
            // 更新插件状态
            stateTracker.recordStateChange(pluginId, "STOPPED");
            
            // 获取版本
            Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> corePluginInfoOpt = 
                pluginRegistry.getPlugin(pluginId);
            if (corePluginInfoOpt.isEmpty()) {
                log.error("插件信息不存在: {}", pluginId);
                return false;
            }
            
            String version = corePluginInfoOpt.get().getDescriptor().getVersion();
            
            // 计算停止耗时
            long duration = System.currentTimeMillis() - startTime;
            
            // 使用新的事件分发机制发布事件
            PluginStoppedEvent stoppedEvent = new PluginStoppedEvent(
                pluginId, 
                version, 
                duration,
                PluginStoppedEvent.StopReason.USER_REQUEST, 
                true
            );
            eventDispatcher.publishEvent(stoppedEvent);
            
            // 暂时保留兼容旧的事件系统，后续可以移除
            eventBus.postEvent(PluginEvent.createStoppedEvent(pluginId, version));
            
            log.info("插件停止完成: {}, 耗时: {}ms", pluginId, duration);
            return true;
        } catch (Exception e) {
            String message = String.format("停止插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public boolean unloadPlugin(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        log.info("请求卸载插件: {}", pluginId);
        
        try {
            // 获取插件实例
            Optional<Plugin> pluginInstance = getPluginInstance(pluginId);
            if (pluginInstance.isEmpty()) {
                log.error("插件实例不存在: {}", pluginId);
                return false;
            }
            
            // 检查状态
            Optional<String> stateOpt = stateTracker.getPluginState(pluginId);
            if (stateOpt.isPresent() && stateOpt.get().equals("RUNNING")) {
                // 如果插件正在运行，先停止它
                log.info("插件[{}]正在运行中，先停止它", pluginId);
                stopPlugin(pluginId);
            }
            
            long startTime = System.currentTimeMillis();
            
            // 调用销毁方法
            pluginInstance.get().destroy();
            
            // 卸载插件(使用加载器的unload方法)
            boolean unloadResult = pluginLoader.unload(pluginId);
            if (!unloadResult) {
                log.error("插件卸载失败: {}", pluginId);
                return false;
            }
            
            // 更新插件状态
            stateTracker.recordStateChange(pluginId, "UNLOADED");
            
            // 移除健康状态
            healthStatuses.remove(pluginId);
            
            // 获取版本
            String version = "unknown";
            Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> corePluginInfoOpt = 
                pluginRegistry.getPlugin(pluginId);
            if (corePluginInfoOpt.isPresent()) {
                version = corePluginInfoOpt.get().getDescriptor().getVersion();
            }
            
            // 计算卸载耗时
            long duration = System.currentTimeMillis() - startTime;
            
            // 使用新的事件分发机制发布事件
            PluginUnloadedEvent unloadedEvent = new PluginUnloadedEvent(
                pluginId, 
                version, 
                duration,
                true, 
                "用户请求卸载"
            );
            eventDispatcher.publishEvent(unloadedEvent);
            
            // 暂时保留兼容旧的事件系统，后续可以移除
            eventBus.postEvent(PluginEvent.createUnloadedEvent(pluginId, version));
            
            log.info("插件卸载完成: {}, 耗时: {}ms", pluginId, duration);
            return true;
        } catch (Exception e) {
            String message = String.format("卸载插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public boolean reloadPlugin(String pluginId) throws PluginLifecycleException {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new PluginLifecycleException("插件ID不能为空");
        }
        
        log.info("请求重新加载插件: {}", pluginId);
        
        // 先获取插件信息，后面需要用到
        Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> corePluginInfoOpt = 
            pluginRegistry.getPlugin(pluginId);
        if (corePluginInfoOpt.isEmpty()) {
            log.error("插件不存在，无法重载: {}", pluginId);
            return false;
        }
        
        com.xiaoqu.qteamos.core.plugin.running.PluginInfo corePluginInfo = corePluginInfoOpt.get();
        PluginInfo apiPluginInfo = pluginInfoAdapter.toApiPluginInfo(corePluginInfo);
        File pluginFile = corePluginInfo.getPluginFile();
        
        if (pluginFile == null || !pluginFile.exists()) {
            log.error("插件文件不存在，无法重载: {}", pluginId);
            return false;
        }
        
        try {
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // 1. 卸载插件
            boolean unloadResult = unloadPlugin(pluginId);
            if (!unloadResult) {
                log.error("卸载插件失败，中止重载: {}", pluginId);
                return false;
            }
            
            // 2. 重新加载插件
            PluginInfo newPluginInfo = pluginLoader.loadFromFile(pluginFile);
            if (newPluginInfo == null) {
                log.error("重新加载插件失败: {}", pluginId);
                return false;
            }
            
            // 3. 初始化插件
            boolean initResult = initializePlugin(pluginId);
            if (!initResult) {
                log.error("初始化插件失败，中止重载: {}", pluginId);
                return false;
            }
            
            // 获取之前的状态，如果之前是运行状态，则需要启动
            String previousState = lastStates.getOrDefault(pluginId, "UNKNOWN");
            if ("RUNNING".equals(previousState)) {
                // 4. 启动插件
                boolean startResult = startPlugin(pluginId);
                if (!startResult) {
                    log.error("启动插件失败，重载不完整: {}", pluginId);
                    return false;
                }
            }
            
            // 计算重载耗时
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("插件重载完成: {}, 耗时: {}ms", pluginId, duration);
            return true;
        } catch (Exception e) {
            String message = String.format("重载插件 %s 失败: %s", pluginId, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public boolean updatePlugin(PluginInfo oldApiPluginInfo, PluginInfo newApiPluginInfo) 
            throws PluginLifecycleException {
        if (oldApiPluginInfo == null || newApiPluginInfo == null) {
            throw new PluginLifecycleException("插件信息不能为空");
        }
        
        String pluginId = oldApiPluginInfo.getPluginId();
        String oldVersion = oldApiPluginInfo.getVersion();
        String newVersion = newApiPluginInfo.getVersion();
        
        log.info("请求更新插件: {} 从版本 {} 到 {}", pluginId, oldVersion, newVersion);
        
        try {
            // 先卸载旧版本
            boolean unloadResult = unloadPlugin(pluginId);
            if (!unloadResult) {
                log.error("卸载旧版本插件失败，中止更新: {}", pluginId);
                return false;
            }
            
            // 加载新版本
            boolean loadResult = loadPlugin(newApiPluginInfo);
            if (!loadResult) {
                log.error("加载新版本插件失败，中止更新: {}", pluginId);
                return false;
            }
            
            // 初始化新版本
            boolean initResult = initializePlugin(pluginId);
            if (!initResult) {
                log.error("初始化新版本插件失败，中止更新: {}", pluginId);
                return false;
            }
            
            // 启动新版本
            boolean startResult = startPlugin(pluginId);
            if (!startResult) {
                log.error("启动新版本插件失败，更新不完整: {}", pluginId);
                return false;
            }
            
            log.info("插件更新完成: {} 从版本 {} 到 {}", pluginId, oldVersion, newVersion);
            return true;
        } catch (Exception e) {
            String message = String.format("更新插件 %s 从版本 %s 到 %s 失败: %s", 
                    pluginId, oldVersion, newVersion, e.getMessage());
            log.error(message, e);
            throw new PluginLifecycleException(message, e);
        }
    }
    
    @Override
    public Optional<Plugin> getPluginInstance(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return Optional.empty();
        }
        
        // 优先从新加载器获取实例
        Optional<Plugin> instanceOpt = pluginLoader.getPluginInstance(pluginId);
        
        if (instanceOpt.isPresent()) {
            return instanceOpt;
        }
        
        // 兼容模式：从旧的生命周期管理器获取
        try {
            return legacyManager.getPluginInstance(pluginId);
        } catch (Exception e) {
            log.warn("从旧生命周期管理器获取插件实例失败: {}, 错误: {}", pluginId, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public boolean checkPluginHealth(String pluginId) {
        return healthMonitor.checkPluginHealth(pluginId);
    }
    
    @Scheduled(fixedDelayString = "${plugin.health.check-interval:300000}")
    public void performHealthCheck() {
        log.debug("执行插件健康检查...");
        
        // 获取所有运行中的插件
        Set<String> runningPlugins = new HashSet<>(stateTracker.getPluginsInState("RUNNING"));
        
        for (String pluginId : runningPlugins) {
            performSinglePluginHealthCheck(pluginId);
        }
    }
    
    private boolean performSinglePluginHealthCheck(String pluginId) {
        boolean isHealthy = healthMonitor.checkPluginHealth(pluginId);
        recordHealthStatus(pluginId, isHealthy, isHealthy ? "健康检查通过" : "健康检查失败");
        return isHealthy;
    }
    
    private void recordHealthStatus(String pluginId, boolean isHealthy, String message) {
        PluginHealthStatus currentStatus = healthStatuses.computeIfAbsent(pluginId, 
            k -> isHealthy ? PluginHealthStatus.healthy() : PluginHealthStatus.unhealthy(message));
        
        currentStatus.setLastCheckTime(System.currentTimeMillis());
        
        if (isHealthy) {
            if (!currentStatus.isHealthy()) {
                log.info("插件[{}]恢复健康状态", pluginId);
            }
            currentStatus.setHealthy(true);
            currentStatus.setMessage("健康");
            currentStatus.resetFailCount();
        } else {
            if (currentStatus.isHealthy()) {
                log.warn("插件[{}]变为不健康状态: {}", pluginId, message);
            }
            currentStatus.setHealthy(false);
            currentStatus.setMessage(message);
            currentStatus.incrementFailCount();
            
            // 如果连续失败超过阈值，尝试自动恢复
            if (currentStatus.getFailCount() >= 3) {
                log.warn("插件[{}]连续{}次健康检查失败，尝试自动恢复", 
                        pluginId, currentStatus.getFailCount());
                
                try {
                    // 尝试重启插件
                    if (stopPlugin(pluginId) && startPlugin(pluginId)) {
                        log.info("插件[{}]自动恢复成功", pluginId);
                        currentStatus.resetFailCount();
                        currentStatus.setHealthy(true);
                        currentStatus.setMessage("自动恢复成功");
                    } else {
                        log.error("插件[{}]自动恢复失败", pluginId);
                    }
                } catch (Exception e) {
                    log.error("尝试自动恢复插件[{}]时出错: {}", pluginId, e.getMessage());
                }
            }
        }
        
        healthStatuses.put(pluginId, currentStatus);
    }
    
    public PluginHealthStatus getPluginHealthStatus(String pluginId) {
        return healthStatuses.getOrDefault(pluginId, PluginHealthStatus.unknown());
    }
    
    public Set<String> getUnhealthyPlugins() {
        return healthStatuses.entrySet().stream()
                .filter(entry -> !entry.getValue().isHealthy())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
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
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public int getFailCount() {
            return failCount;
        }
        
        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }
        
        public void incrementFailCount() {
            this.failCount++;
        }
        
        public long getLastCheckTime() {
            return lastCheckTime;
        }
        
        public void setLastCheckTime(long lastCheckTime) {
            this.lastCheckTime = lastCheckTime;
        }
        
        public void resetFailCount() {
            this.failCount = 0;
        }
    }
    
    /**
     * 构建插件健康检查URL
     * 
     * @param pluginId 插件ID
     * @return 健康检查URL
     */
    private String buildPluginHealthCheckUrl(String pluginId) {
        Optional<com.xiaoqu.qteamos.core.plugin.running.PluginInfo> pluginInfoOpt = 
                pluginRegistry.getPlugin(pluginId);
        
        if (pluginInfoOpt.isPresent()) {
            com.xiaoqu.qteamos.core.plugin.running.PluginInfo pluginInfo = pluginInfoOpt.get();
            
            // 获取插件描述符信息
            Map<String, Object> properties = pluginInfo.getDescriptor().getProperties();
            
            // 尝试获取健康端点配置
            String healthEndpoint = null;
            if (properties != null && properties.containsKey("healthEndpoint")) {
                healthEndpoint = (String) properties.get("healthEndpoint");
            }
            
            // 如果插件定义了健康检查端点，使用它
            if (healthEndpoint != null && !healthEndpoint.isEmpty()) {
                if (healthEndpoint.startsWith("http")) {
                    return healthEndpoint;
                } else {
                    return serverBaseUrl + (healthEndpoint.startsWith("/") ? 
                            healthEndpoint : "/" + healthEndpoint);
                }
            }
            
            // 否则构建标准的健康检查URL
            String pluginBasePath = null;
            if (properties != null && properties.containsKey("basePath")) {
                pluginBasePath = (String) properties.get("basePath");
            }
            
            if (pluginBasePath != null && !pluginBasePath.isEmpty()) {
                return serverBaseUrl + 
                       (pluginBasePath.startsWith("/") ? pluginBasePath : "/" + pluginBasePath) + 
                       "/health";
            }
            
            // 使用API网关前缀加插件ID作为健康检查路径
            return serverBaseUrl + apiPrefix + "/plugins/" + pluginId + "/health";
        }
        
        // 插件不存在，返回标准API路径
        return serverBaseUrl + apiPrefix + "/plugins/" + pluginId + "/health";
    }
    
    /**
     * 通过HTTP进行插件健康检查
     * 
     * @param healthCheckUrl 健康检查URL
     * @param timeoutMillis 超时时间(毫秒)
     * @return 健康检查结果
     */
    private boolean checkPluginHealthViaHttp(String healthCheckUrl, Integer timeoutMillis) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(healthCheckUrl))
                .timeout(Duration.ofMillis(timeoutMillis != null ? timeoutMillis : 5000))
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(
                request, 
                HttpResponse.BodyHandlers.ofString()
            );
            
            int statusCode = response.statusCode();
            boolean isHealthy = statusCode >= 200 && statusCode < 300;
            
            if (isHealthy) {
                log.debug("插件健康检查成功: {}, 状态码: {}", healthCheckUrl, statusCode);
            } else {
                log.warn("插件健康检查失败: {}, 状态码: {}", healthCheckUrl, statusCode);
            }
            
            return isHealthy;
        } catch (Exception e) {
            log.warn("插件健康检查异常: {}, 错误: {}", healthCheckUrl, e.getMessage());
            return false;
        }
    }
} 