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
import com.xiaoqu.qteamos.api.core.plugin.api.PluginHealthMonitor;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginLifecycleHandler;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginStateTracker;
import com.xiaoqu.qteamos.api.core.event.PluginEventDispatcher;
import com.xiaoqu.qteamos.api.core.event.health.PluginHealthCheckEvent;
import com.xiaoqu.qteamos.api.core.event.health.PluginHealthFailedEvent;
import com.xiaoqu.qteamos.api.core.event.health.PluginHealthRecoveredEvent;
import com.xiaoqu.qteamos.api.core.event.health.PluginRecoveryAttemptEvent;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginHealthHistory;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.service.SysPluginHealthHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.xiaoqu.qteamos.common.utils.EncryptionUtils;

/**
 * 插件健康监控默认实现
 * 负责监控插件的健康状态，提供健康检查和自动恢复功能
 *
 * @author yangqijun
 * @date 2024-08-15
 * @since 1.0.0
 */
@Slf4j
@Component
public class DefaultPluginHealthMonitor implements PluginHealthMonitor {

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private PluginLifecycleHandler lifecycleHandler;
    
    @Autowired
    private PluginStateTracker stateTracker;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private SysPluginHealthHistoryService healthHistoryService;

    @Autowired
    private PluginEventDispatcher eventDispatcher;

    /**
     * 健康检查间隔时间（毫秒）
     */
    @Value("${plugin.health.check-interval:300000}")
    private long checkInterval;

    /**
     * 自动恢复的最大失败次数
     */
    @Value("${plugin.health.max-fail-count:3}")
    private int maxFailCount;

    /**
     * 健康检查请求超时（毫秒）
     */
    @Value("${plugin.health.request-timeout:5000}")
    private long requestTimeout;

    /**
     * API服务器URL
     */
    @Value("${server.url:http://localhost:8080}")
    private String serverBaseUrl;

    /**
     * API前缀
     */
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;

    /**
     * 记录插件的健康状态
     */
    private final Map<String, HealthSnapshotImpl> healthSnapshots = new ConcurrentHashMap<>();

    /**
     * 健康检查执行器
     */
    private ScheduledExecutorService healthCheckExecutor;

    /**
     * 初始化
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
            this::performHealthCheckForAllInternal,
            60, // 初始延迟60秒
            checkInterval / 1000, // 转换为秒
            TimeUnit.SECONDS
        );

        log.info("插件健康监控初始化完成，检查间隔: {}秒，最大失败次数: {}", checkInterval / 1000, maxFailCount);
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

        log.info("插件健康监控已关闭");
    }

    @Override
    public boolean checkPluginHealth(String pluginId) {
        HealthSnapshot snapshot = performHealthCheck(pluginId);
        return snapshot != null && snapshot.isHealthy();
    }

    @Override
    public HealthSnapshot performHealthCheck(String pluginId) {
        log.debug("执行插件健康检查: {}", pluginId);
        
        try {
            // 获取插件信息
            Optional<PluginInfo> pluginInfoOpt = pluginRegistry.getPlugin(pluginId);
            if (pluginInfoOpt.isEmpty()) {
                log.warn("插件不存在或未注册: {}", pluginId);
                return createUnhealthySnapshot(pluginId, "插件不存在或未注册", 0);
            }
            
            PluginInfo pluginInfo = pluginInfoOpt.get();
            String pluginVersion = pluginInfo.getDescriptor().getVersion();
            String pluginState = pluginInfo.getState().name();
            
            // 检查插件状态
            if (!"RUNNING".equals(pluginState)) {
                log.debug("插件状态不是RUNNING，跳过健康检查: {}, 当前状态: {}", pluginId, pluginState);
                return createUnhealthySnapshot(pluginId, "插件状态不是RUNNING: " + pluginState, 0, pluginVersion, pluginState);
            }
            
            // 获取插件实例
            Optional<Plugin> pluginOpt = lifecycleHandler.getPluginInstance(pluginId);
            if (pluginOpt.isEmpty()) {
                log.warn("插件实例不存在: {}", pluginId);
                return createUnhealthySnapshot(pluginId, "插件实例不存在", 0, pluginVersion, pluginState);
            }
            
            // 执行健康检查
            boolean isHealthy = performSinglePluginHealthCheck(pluginId, pluginOpt.get(), pluginInfo, pluginVersion, pluginState);
            
            // 获取最新的快照
            HealthSnapshotImpl snapshot = healthSnapshots.get(pluginId);
            if (snapshot == null) {
                snapshot = isHealthy 
                    ? createHealthySnapshot(pluginId, "健康检查通过", 0, pluginVersion, pluginState)
                    : createUnhealthySnapshot(pluginId, "健康检查失败", 1, pluginVersion, pluginState);
                healthSnapshots.put(pluginId, snapshot);
            }
            
            // 记录健康检查历史
            addHealthCheckHistory(snapshot, "MANUAL");
            
            // 发布健康检查事件
            PluginHealthCheckEvent checkEvent = new PluginHealthCheckEvent(
                pluginId, 
                pluginVersion, 
                snapshot.isHealthy(), 
                snapshot.getMessage(), 
                snapshot.getResourceUsage());
            eventDispatcher.publishEvent(checkEvent);
            
            // 兼容性：旧事件系统
            eventBus.postEvent(PluginEvent.createHealthCheckEvent(
                pluginId, snapshot.isHealthy(), snapshot.getMessage()));
            
            return snapshot;
        } catch (Exception e) {
            log.error("执行插件健康检查异常: {}", pluginId, e);
            HealthSnapshotImpl snapshot = createUnhealthySnapshot(pluginId, "健康检查异常: " + e.getMessage(), 1);
            healthSnapshots.put(pluginId, snapshot);
            
            // 记录健康检查历史
            addHealthCheckHistory(snapshot, "MANUAL");
            
            // 发布健康检查事件
            PluginHealthCheckEvent checkEvent = new PluginHealthCheckEvent(
                pluginId, 
                "unknown", 
                false, 
                snapshot.getMessage(), 
                Collections.emptyMap());
            eventDispatcher.publishEvent(checkEvent);
            
            // 兼容性：旧事件系统
            eventBus.postEvent(PluginEvent.createHealthCheckEvent(
                pluginId, false, "健康检查异常: " + e.getMessage()));
            
            return snapshot;
        }
    }

    /**
     * 执行自动健康检查
     */
    @Scheduled(fixedDelayString = "${plugin.health.check-interval:300000}")
    public void performScheduledHealthCheck() {
        performHealthCheckForAllInternal();
    }
    
    /**
     * 执行所有插件的健康检查（内部方法）
     */
    private void performHealthCheckForAllInternal() {
        log.debug("开始执行所有插件的健康检查...");
        
        try {
            Set<String> checkFailedPlugins = new HashSet<>();
            
            // 获取所有运行中的插件
            pluginRegistry.getAllPlugins().stream()
                .filter(p -> p.getState().name().equals("RUNNING"))
                .forEach(pluginInfo -> {
                    String pluginId = pluginInfo.getDescriptor().getPluginId();
                    String pluginVersion = pluginInfo.getDescriptor().getVersion();
                    String pluginState = pluginInfo.getState().name();
                    
                    try {
                        // 获取插件实例
                        Optional<Plugin> pluginOpt = lifecycleHandler.getPluginInstance(pluginId);
                        if (pluginOpt.isEmpty()) {
                            log.warn("插件实例不存在，无法执行健康检查: {}", pluginId);
                            updateUnhealthyStatus(pluginId, "插件实例不存在", pluginVersion, pluginState);
                            checkFailedPlugins.add(pluginId);
                            return;
                        }
                        
                        // 执行健康检查
                        boolean isHealthy = performSinglePluginHealthCheck(pluginId, pluginOpt.get(), pluginInfo, 
                                pluginVersion, pluginState);
                        
                        if (!isHealthy) {
                            checkFailedPlugins.add(pluginId);
                        }
                    } catch (Exception e) {
                        log.error("执行插件健康检查异常: {}", pluginId, e);
                        updateUnhealthyStatus(pluginId, "健康检查异常: " + e.getMessage(), pluginVersion, pluginState);
                        checkFailedPlugins.add(pluginId);
                    }
                });
            
            log.debug("健康检查完成，不健康的插件数量: {}", checkFailedPlugins.size());
            
            // 尝试恢复不健康的插件
            for (String pluginId : checkFailedPlugins) {
                tryRecovery(pluginId);
            }
        } catch (Exception e) {
            log.error("执行插件健康检查时发生错误: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public Map<String, Boolean> performHealthCheckForAll() {
        Map<String, Boolean> results = new HashMap<>();
        
        pluginRegistry.getAllPlugins().stream()
            .filter(p -> p.getState().name().equals("RUNNING"))
            .forEach(pluginInfo -> {
                String pluginId = pluginInfo.getDescriptor().getPluginId();
                HealthSnapshot snapshot = performHealthCheck(pluginId);
                results.put(pluginId, snapshot.isHealthy());
            });
        
        return results;
    }
    
    @Override
    public HealthSnapshot getHealthSnapshot(String pluginId) {
        // 检查缓存中是否有快照
        HealthSnapshotImpl snapshot = healthSnapshots.get(pluginId);
        if (snapshot != null) {
            return snapshot;
        }
        
        // 检查数据库中是否有记录
        Optional<SysPluginHealthHistory> historyOpt = healthHistoryService.getLastHealthCheck(pluginId);
        if (historyOpt.isPresent()) {
            SysPluginHealthHistory history = historyOpt.get();
            
            // 创建快照
            snapshot = new HealthSnapshotImpl(
                    history.getPluginId(),
                    history.getVersion(),
                    history.getState(),
                    history.getHealthy(),
                    history.getHealthMessage(),
                    history.getFailCount(),
                    LocalDateTime.now(),
                    Collections.emptyMap()
            );
            
            // 缓存快照
            healthSnapshots.put(pluginId, snapshot);
            return snapshot;
        }
        
        // 如果没有历史记录，返回未知状态
        return createUnhealthySnapshot(pluginId, "未知状态", 0);
    }

    @Override
    public List<HealthSnapshot> getAllHealthSnapshots() {
        // 首先获取所有插件ID
        Set<String> allPluginIds = new HashSet<>();
        
        // 添加注册表中的插件ID
        pluginRegistry.getAllPlugins().forEach(p -> 
            allPluginIds.add(p.getDescriptor().getPluginId()));
        
        // 添加快照缓存中的插件ID
        allPluginIds.addAll(healthSnapshots.keySet());
        
        // 为每个插件获取健康快照
        return allPluginIds.stream()
                .map(this::getHealthSnapshot)
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getUnhealthyPlugins() {
        return healthSnapshots.entrySet().stream()
                .filter(e -> !e.getValue().isHealthy())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean attemptPluginRecovery(String pluginId) {
        log.info("手动尝试恢复插件: {}", pluginId);
        return tryRecovery(pluginId);
    }

    @Override
    public void resetErrorCount(String pluginId) {
        HealthSnapshotImpl snapshot = healthSnapshots.get(pluginId);
        if (snapshot != null) {
            snapshot.setFailCount(0);
            snapshot.setLastCheckTime(LocalDateTime.now());
            
            log.info("重置插件错误计数: {}", pluginId);
            
            // 记录到历史
            addHealthCheckHistory(snapshot, "MANUAL_RESET");
        }
    }

    @Override
    public List<HealthRecord> getHealthHistory(String pluginId, int limit) {
        List<SysPluginHealthHistory> historyList = healthHistoryService.getHealthHistory(pluginId, limit);
        return historyList.stream()
                .map(this::convertToHealthRecord)
                .collect(Collectors.toList());
    }

    @Override
    public void setHealthCheckParameters(long checkInterval, int maxFailCount, long requestTimeout) {
        this.checkInterval = checkInterval;
        this.maxFailCount = maxFailCount;
        this.requestTimeout = requestTimeout;
        
        log.info("更新健康检查参数: 间隔={}ms, 最大失败次数={}, 请求超时={}ms", 
                checkInterval, maxFailCount, requestTimeout);
        
        // 重新调度健康检查任务
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
            
            healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "plugin-health-check");
                t.setDaemon(true);
                return t;
            });
            
            healthCheckExecutor.scheduleWithFixedDelay(
                this::performHealthCheckForAllInternal,
                60, // 初始延迟60秒
                checkInterval / 1000, // 转换为秒
                TimeUnit.SECONDS
            );
        }
    }
    
    /**
     * 执行单个插件的健康检查
     */
    private boolean performSinglePluginHealthCheck(String pluginId, Plugin instance, 
            PluginInfo pluginInfo, String version, String state) {
        try {
            // 1. 基本状态检查
            if (!"RUNNING".equals(state)) {
                updateUnhealthyStatus(pluginId, "插件状态不是RUNNING: " + state, version, state);
                return false;
            }
            
            // 2. 检查插件实例
            if (instance == null) {
                updateUnhealthyStatus(pluginId, "插件实例不存在", version, state);
                return false;
            }
            
            // 3. HTTP健康检查
            // 从插件描述符中获取健康检查URL
            boolean httpCheckResult = checkPluginHealthViaHttp(pluginId, pluginInfo);
            if (!httpCheckResult) {
                updateUnhealthyStatus(pluginId, "HTTP健康检查失败", version, state);
                return false;
            }
            
            // 4. 资源检查：内存使用、线程数等
            // 这里只是示例，具体实现可能会更复杂
            Map<String, Object> resourceUsage = collectResourceUsage(pluginId, pluginInfo);
            
            // 5. 更新为健康状态
            updateHealthyStatus(pluginId, "健康检查通过", version, state, resourceUsage);
            
            return true;
        } catch (Exception e) {
            log.error("插件健康检查异常: {}", pluginId, e);
            updateUnhealthyStatus(pluginId, "健康检查异常: " + e.getMessage(), version, state);
            return false;
        }
    }
    
    /**
     * 通过HTTP检查插件健康状态
     */
    private boolean checkPluginHealthViaHttp(String pluginId, PluginInfo pluginInfo) {
        try {
            // 尝试从插件描述符中获取健康检查配置
            String healthCheckUrl = null;
            Integer timeoutMillis = null;
            
            Map<String, Object> properties = pluginInfo.getDescriptor().getProperties();
            if (properties != null && properties.containsKey("healthCheck")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> healthCheck = (Map<String, Object>) properties.get("healthCheck");
                if (healthCheck != null) {
                    if (healthCheck.containsKey("url")) {
                        healthCheckUrl = (String) healthCheck.get("url");
                    }
                    if (healthCheck.containsKey("timeout")) {
                        try {
                            timeoutMillis = Integer.parseInt(healthCheck.get("timeout").toString());
                        } catch (NumberFormatException e) {
                            log.debug("解析健康检查超时配置失败: {}", e.getMessage());
                        }
                    }
                }
            }
            
            // 如果没有配置URL，使用默认URL
            if (healthCheckUrl == null || healthCheckUrl.isEmpty()) {
                healthCheckUrl = "/health";
            } else if (!healthCheckUrl.startsWith("/")) {
                healthCheckUrl = "/" + healthCheckUrl;
            }
            
            // 如果没有配置超时，使用默认超时
            if (timeoutMillis == null) {
                timeoutMillis = Long.valueOf(requestTimeout).intValue();
            }
            
            // 获取加密后的插件ID
            String encryptedPluginId = EncryptionUtils.encrypt(pluginId);
            
            // 对特殊字符进行替换，确保URL安全
            // 去除可能的等号，避免URL解析问题
            encryptedPluginId = encryptedPluginId.replace("=", "");
            // 替换斜杠为下划线
            encryptedPluginId = encryptedPluginId.replace("/", "_");
            // 替换加号为减号
            encryptedPluginId = encryptedPluginId.replace("+", "-");
            
            // 构建完整URL
            String fullUrl = String.format("%s%s/p-%s/pub%s", 
                    serverBaseUrl, apiPrefix, encryptedPluginId, healthCheckUrl);
            
            log.debug("检查插件健康状态: {}, 超时: {}ms, 原始ID: {}, 加密ID: {}", 
                    fullUrl, timeoutMillis, pluginId, encryptedPluginId);
            
            // 设置超时
            int connectTimeoutSeconds = Math.min(timeoutMillis / 1000, 10); // 最大10秒连接超时
            int requestTimeoutSeconds = Math.min(timeoutMillis / 1000, 30); // 最大30秒请求超时
            
            // 创建HTTP客户端
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                    .build();
            
            // 构建请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .GET()
                    .build();
            
            // 发送请求
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
            
            // 检查响应状态
            if (response.statusCode() == 200) {
                // 解析JSON响应
                String body = response.body();
                if (body != null && !body.isEmpty()) {
                    try {
                        // 简单解析JSON以获取健康状态
                        if (body.contains("\"healthy\":true") || body.contains("\"status\":\"UP\"")) {
                            return true;
                        }
                    } catch (Exception e) {
                        log.debug("解析健康检查响应失败: {}", e.getMessage());
                    }
                }
            }
            
            log.debug("HTTP健康检查失败，状态码: {}, 响应: {}", 
                    response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.debug("执行HTTP健康检查时发生错误: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 收集插件资源使用情况
     */
    private Map<String, Object> collectResourceUsage(String pluginId, PluginInfo pluginInfo) {
        Map<String, Object> usage = new HashMap<>();
        
        try {
            // 估计内存使用
            long memoryUsage = estimateMemoryUsage(pluginInfo);
            usage.put("memoryMb", memoryUsage);
            
            // 获取线程数量
            int threadCount = estimateThreadCount(pluginInfo);
            usage.put("threadCount", threadCount);
            
            // 其他资源指标...
        } catch (Exception e) {
            log.debug("采集资源使用情况失败: {}", e.getMessage());
        }
        
        return usage;
    }
    
    /**
     * 估计插件内存使用量（MB）
     */
    private long estimateMemoryUsage(PluginInfo pluginInfo) {
        // 简化实现，实际可能更复杂
        return 10; // 默认10MB
    }
    
    /**
     * 估计插件线程数量
     */
    private int estimateThreadCount(PluginInfo pluginInfo) {
        // 简化实现，实际可能更复杂
        return 2; // 默认2个线程
    }
    
    /**
     * 更新为健康状态
     */
    private void updateHealthyStatus(String pluginId, String message, String version, String state, 
            Map<String, Object> resourceUsage) {
        HealthSnapshotImpl snapshot = healthSnapshots.get(pluginId);
        boolean wasUnhealthy = snapshot != null && !snapshot.isHealthy();
        
        // 创建或更新快照
        if (snapshot == null) {
            snapshot = createHealthySnapshot(pluginId, message, 0, version, state);
            snapshot.setResourceUsage(resourceUsage);
            healthSnapshots.put(pluginId, snapshot);
        } else {
            snapshot.setHealthy(true);
            snapshot.setMessage(message);
            snapshot.setLastCheckTime(LocalDateTime.now());
            snapshot.setFailCount(0);
            snapshot.setResourceUsage(resourceUsage);
        }
        
        // 记录健康检查历史
        addHealthCheckHistory(snapshot, "AUTO");
        
        // 发布状态变化事件
        if (wasUnhealthy) {
            log.info("插件[{}]恢复健康: {}", pluginId, message);
            
            // 使用新的事件分发机制
            PluginHealthRecoveredEvent recoveredEvent = new PluginHealthRecoveredEvent(
                pluginId, version, message, 0);
            eventDispatcher.publishEvent(recoveredEvent);
            
            // 兼容性：旧事件系统
            eventBus.postEvent(PluginEvent.createHealthRecoveredEvent(pluginId, message));
        }
    }
    
    /**
     * 更新为不健康状态
     */
    private void updateUnhealthyStatus(String pluginId, String message, String version, String state) {
        HealthSnapshotImpl snapshot = healthSnapshots.get(pluginId);
        boolean wasHealthy = snapshot != null && snapshot.isHealthy();
        int failCount = snapshot != null ? snapshot.getFailCount() + 1 : 1;
        
        // 创建或更新快照
        if (snapshot == null) {
            snapshot = createUnhealthySnapshot(pluginId, message, failCount, version, state);
            healthSnapshots.put(pluginId, snapshot);
        } else {
            snapshot.setHealthy(false);
            snapshot.setMessage(message);
            snapshot.setFailCount(failCount);
            snapshot.setLastCheckTime(LocalDateTime.now());
        }
        
        // 记录健康检查历史
        addHealthCheckHistory(snapshot, "AUTO");
        
        // 发布状态变化事件
        if (wasHealthy) {
            log.warn("插件[{}]变为不健康: {}", pluginId, message);
            
            // 使用新的事件分发机制
            PluginHealthFailedEvent failedEvent = new PluginHealthFailedEvent(
                pluginId, version, message, failCount);
            eventDispatcher.publishEvent(failedEvent);
            
            // 兼容性：旧事件系统
            eventBus.postEvent(PluginEvent.createHealthFailedEvent(pluginId, message));
        }
    }
    
    /**
     * 尝试恢复插件
     */
    private boolean tryRecovery(String pluginId) {
        HealthSnapshotImpl snapshot = healthSnapshots.get(pluginId);
        if (snapshot == null || snapshot.isHealthy() || snapshot.getFailCount() < maxFailCount) {
            return false;
        }
        
        log.info("尝试恢复插件: {}, 失败次数: {}", pluginId, snapshot.getFailCount());
        
        try {
            // 发布恢复开始事件
            // 使用新的事件分发机制
            PluginRecoveryAttemptEvent startEvent = new PluginRecoveryAttemptEvent(
                pluginId, snapshot.getVersion(), "开始尝试恢复", snapshot.getFailCount());
            eventDispatcher.publishEvent(startEvent);
            
            // 兼容性：旧事件系统
            eventBus.postEvent(PluginEvent.createRecoveryEvent(pluginId, "开始尝试恢复"));
            
            // 获取插件版本
            String version = snapshot.getVersion();
            
            // 尝试重载插件
            boolean success = lifecycleHandler.reloadPlugin(pluginId);
            
            // 更新恢复结果
            if (success) {
                log.info("插件[{}]自动恢复成功", pluginId);
                updateHealthyStatus(pluginId, "自动恢复成功", version, "RUNNING", Collections.emptyMap());
                
                // 使用新的事件分发机制
                PluginRecoveryAttemptEvent successEvent = new PluginRecoveryAttemptEvent(
                    pluginId, version, "恢复成功", snapshot.getFailCount());
                eventDispatcher.publishEvent(successEvent);
                
                // 兼容性：旧事件系统
                eventBus.postEvent(PluginEvent.createRecoveryEvent(pluginId, "恢复成功"));
            } else {
                log.warn("插件[{}]自动恢复失败", pluginId);
                updateUnhealthyStatus(pluginId, "自动恢复失败", version, "ERROR");
                
                // 使用新的事件分发机制
                PluginRecoveryAttemptEvent failedEvent = new PluginRecoveryAttemptEvent(
                    pluginId, version, "恢复失败", snapshot.getFailCount());
                eventDispatcher.publishEvent(failedEvent);
                
                // 兼容性：旧事件系统
                eventBus.postEvent(PluginEvent.createRecoveryEvent(pluginId, "恢复失败"));
            }
            
            return success;
        } catch (Exception e) {
            log.error("恢复插件[{}]时出错: {}", pluginId, e.getMessage(), e);
            
            // 获取插件版本
            String version = snapshot.getVersion();
            
            // 更新恢复结果
            updateUnhealthyStatus(pluginId, "恢复出错: " + e.getMessage(), version, "ERROR");
            
            // 使用新的事件分发机制
            PluginRecoveryAttemptEvent errorEvent = new PluginRecoveryAttemptEvent(
                pluginId, version, "恢复过程出错: " + e.getMessage(), snapshot.getFailCount());
            eventDispatcher.publishEvent(errorEvent);
            
            // 兼容性：旧事件系统
            eventBus.postEvent(PluginEvent.createRecoveryEvent(pluginId, "恢复过程出错: " + e.getMessage()));
            
            return false;
        }
    }
    
    /**
     * 添加健康检查历史记录
     */
    private void addHealthCheckHistory(HealthSnapshotImpl snapshot, String checkType) {
        try {
            // 提取资源使用情况
            int memoryUsageMb = 0;
            int threadCount = 0;
            
            if (snapshot.getResourceUsage() != null) {
                if (snapshot.getResourceUsage().containsKey("memoryMb")) {
                    Object memoryObj = snapshot.getResourceUsage().get("memoryMb");
                    if (memoryObj instanceof Number) {
                        memoryUsageMb = ((Number) memoryObj).intValue();
                    }
                }
                
                if (snapshot.getResourceUsage().containsKey("threadCount")) {
                    Object threadObj = snapshot.getResourceUsage().get("threadCount");
                    if (threadObj instanceof Number) {
                        threadCount = ((Number) threadObj).intValue();
                    }
                }
            }
            
            // 添加历史记录
            healthHistoryService.addHealthCheck(
                    snapshot.getPluginId(),
                    snapshot.getVersion(),
                    snapshot.getState(),
                    snapshot.isHealthy(),
                    snapshot.getMessage(),
                    snapshot.getFailCount(),
                    memoryUsageMb,
                    threadCount,
                    checkType
            );
        } catch (Exception e) {
            log.error("添加健康检查历史记录失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建健康状态快照
     */
    private HealthSnapshotImpl createHealthySnapshot(String pluginId, String message, int failCount) {
        return new HealthSnapshotImpl(
                pluginId,
                "unknown",
                "UNKNOWN",
                true,
                message,
                failCount,
                LocalDateTime.now(),
                Collections.emptyMap()
        );
    }
    
    /**
     * 创建健康状态快照（带版本和状态）
     */
    private HealthSnapshotImpl createHealthySnapshot(String pluginId, String message, int failCount, 
            String version, String state) {
        return new HealthSnapshotImpl(
                pluginId,
                version,
                state,
                true,
                message,
                failCount,
                LocalDateTime.now(),
                Collections.emptyMap()
        );
    }
    
    /**
     * 创建不健康状态快照
     */
    private HealthSnapshotImpl createUnhealthySnapshot(String pluginId, String message, int failCount) {
        return new HealthSnapshotImpl(
                pluginId,
                "unknown",
                "UNKNOWN",
                false,
                message,
                failCount,
                LocalDateTime.now(),
                Collections.emptyMap()
        );
    }
    
    /**
     * 创建不健康状态快照（带版本和状态）
     */
    private HealthSnapshotImpl createUnhealthySnapshot(String pluginId, String message, int failCount, 
            String version, String state) {
        return new HealthSnapshotImpl(
                pluginId,
                version,
                state,
                false,
                message,
                failCount,
                LocalDateTime.now(),
                Collections.emptyMap()
        );
    }
    
    /**
     * 将SysPluginHealthHistory转换为HealthRecord
     */
    private HealthRecord convertToHealthRecord(SysPluginHealthHistory history) {
        return new HealthRecordImpl(
                history.getPluginId(),
                history.getVersion(),
                Boolean.TRUE.equals(history.getHealthy()),
                history.getHealthMessage(),
                history.getCollectTime(),
                history.getFailCount(),
                history.getCheckType()
        );
    }
    
    /**
     * 健康状态快照实现类
     */
    private static class HealthSnapshotImpl implements HealthSnapshot {
        private final String pluginId;
        private final String version;
        private final String state;
        private boolean healthy;
        private String message;
        private int failCount;
        private LocalDateTime lastCheckTime;
        private Map<String, Object> resourceUsage;
        
        public HealthSnapshotImpl(String pluginId, String version, String state, boolean healthy, 
                String message, int failCount, LocalDateTime lastCheckTime, 
                Map<String, Object> resourceUsage) {
            this.pluginId = pluginId;
            this.version = version;
            this.state = state;
            this.healthy = healthy;
            this.message = message;
            this.failCount = failCount;
            this.lastCheckTime = lastCheckTime;
            this.resourceUsage = resourceUsage;
        }
        
        @Override
        public String getPluginId() {
            return pluginId;
        }
        
        @Override
        public String getVersion() {
            return version;
        }
        
        @Override
        public boolean isHealthy() {
            return healthy;
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public int getFailCount() {
            return failCount;
        }
        
        @Override
        public LocalDateTime getLastCheckTime() {
            return lastCheckTime;
        }
        
        @Override
        public Map<String, Object> getResourceUsage() {
            return resourceUsage;
        }
        
        public String getState() {
            return state;
        }
        
        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }
        
        public void setLastCheckTime(LocalDateTime lastCheckTime) {
            this.lastCheckTime = lastCheckTime;
        }
        
        public void setResourceUsage(Map<String, Object> resourceUsage) {
            this.resourceUsage = resourceUsage;
        }
    }
    
    /**
     * 健康记录实现类
     */
    private static class HealthRecordImpl implements HealthRecord {
        private final String pluginId;
        private final String version;
        private final boolean healthy;
        private final String message;
        private final LocalDateTime checkTime;
        private final int failCount;
        private final String checkType;
        
        public HealthRecordImpl(String pluginId, String version, boolean healthy, 
                String message, LocalDateTime checkTime, int failCount, String checkType) {
            this.pluginId = pluginId;
            this.version = version;
            this.healthy = healthy;
            this.message = message;
            this.checkTime = checkTime;
            this.failCount = failCount;
            this.checkType = checkType;
        }
        
        @Override
        public String getPluginId() {
            return pluginId;
        }
        
        @Override
        public String getVersion() {
            return version;
        }
        
        @Override
        public boolean isHealthy() {
            return healthy;
        }
        
        @Override
        public String getMessage() {
            return message;
        }
        
        @Override
        public LocalDateTime getCheckTime() {
            return checkTime;
        }
        
        @Override
        public int getFailCount() {
            return failCount;
        }
        
        @Override
        public String getCheckType() {
            return checkType;
        }
    }
} 