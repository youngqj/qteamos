package com.xiaoqu.qteamos.core.plugin.monitoring;

import com.xiaoqu.qteamos.core.plugin.error.ErrorRecord;
import com.xiaoqu.qteamos.core.plugin.error.PluginErrorHandler;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager.PluginHealthStatus;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 插件监控服务
 * 负责收集、聚合和提供插件运行状态数据
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Service
@Slf4j
public class PluginMonitoringService {

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private PluginLifecycleManager lifecycleManager;

    @Autowired
    private PluginErrorHandler errorHandler;

    /**
     * 健康检查阈值（毫秒）
     */
    @Value("${plugin.monitoring.health-check-threshold:3000}")
    private long healthCheckThreshold;

    /**
     * 内存使用阈值（MB）
     */
    @Value("${plugin.monitoring.memory-threshold:100}")
    private long memoryThreshold;

    /**
     * 插件健康状态缓存
     */
    private final Map<String, PluginHealthSnapshot> healthSnapshots = new ConcurrentHashMap<>();

    /**
     * 初始化监控服务
     */
    @PostConstruct
    public void init() {
        log.info("插件监控服务初始化完成，健康检查阈值: {}ms, 内存使用阈值: {}MB",
                healthCheckThreshold, memoryThreshold);
    }

    /**
     * 定时执行健康状态快照
     */
    @Scheduled(fixedDelayString = "${plugin.monitoring.snapshot-interval:60000}")
    public void takeHealthSnapshots() {
        log.debug("执行插件健康状态快照采集");
        
        // 获取所有插件
        Collection<PluginInfo> allPlugins = pluginRegistry.getAllPlugins();
        
        // 更新快照
        for (PluginInfo plugin : allPlugins) {
            String pluginId = plugin.getDescriptor().getPluginId();
            try {
                updatePluginHealthSnapshot(pluginId, plugin);
            } catch (Exception e) {
                log.error("获取插件[{}]健康状态时发生错误", pluginId, e);
            }
        }
        
        // 检查异常情况
        checkForAnomalies();
    }

    /**
     * 更新单个插件的健康快照
     */
    private void updatePluginHealthSnapshot(String pluginId, PluginInfo plugin) {
        PluginHealthStatus healthStatus = lifecycleManager.getPluginHealthStatus(pluginId);
        Optional<ErrorRecord> errorRecord = errorHandler.getErrorRecord(pluginId);
        
        PluginHealthSnapshot snapshot = healthSnapshots.computeIfAbsent(
                pluginId, k -> new PluginHealthSnapshot(pluginId));
        
        // 更新基本状态
        snapshot.setState(plugin.getState());
        snapshot.setVersion(plugin.getDescriptor().getVersion());
        snapshot.setLastUpdated(LocalDateTime.now());
        
        // 更新健康状态
        snapshot.setHealthy(healthStatus.isHealthy());
        snapshot.setHealthMessage(healthStatus.getMessage());
        snapshot.setFailCount(healthStatus.getFailCount());
        
        // 更新错误信息
        errorRecord.ifPresent(record -> {
            snapshot.setTotalErrorCount(record.getTotalErrorCount());
            snapshot.setConsecutiveErrorCount(record.getConsecutiveErrorCount());
            snapshot.setLastErrorTime(record.getLastErrorTime());
            snapshot.setLastErrorMessage(record.getLastErrorMessage());
        });
        
        // 估算资源使用情况（这里提供一个简单实现，实际应用中可以使用JMX或其他方式获取更精确的数据）
        snapshot.setMemoryUsageMB(estimatePluginMemoryUsage(pluginId));
        snapshot.setThreadCount(estimatePluginThreadCount(pluginId));
        
        log.debug("更新插件[{}]健康快照: 状态={}, 健康={}, 错误数={}, 内存={}MB",
                pluginId, plugin.getState(), healthStatus.isHealthy(), 
                snapshot.getTotalErrorCount(), snapshot.getMemoryUsageMB());
    }

    /**
     * 估算插件内存使用（示例实现）
     */
    private long estimatePluginMemoryUsage(String pluginId) {
        // 这里只是一个示例，实际应用中应该使用更精确的方法测量插件内存使用
        // 例如通过JMX或其他方式获取ClassLoader加载的类的内存使用情况
        // 由于精确测量单个插件的内存使用非常复杂，这里使用一个随机值来模拟
        Random random = new Random();
        return 10 + random.nextInt(90); // 返回10-100MB之间的随机值
    }

    /**
     * 估算插件线程数（示例实现）
     */
    private int estimatePluginThreadCount(String pluginId) {
        // 同样，这里只是一个示例
        // 实际应用中应该识别插件创建的线程，例如通过线程名称前缀等方式
        Random random = new Random();
        return 1 + random.nextInt(5); // 返回1-5之间的随机值
    }

    /**
     * 检查异常情况
     */
    private void checkForAnomalies() {
        // 检查内存使用过高的插件
        List<String> highMemoryPlugins = healthSnapshots.values().stream()
                .filter(s -> s.getMemoryUsageMB() > memoryThreshold && s.getState() == PluginState.STARTED)
                .map(PluginHealthSnapshot::getPluginId)
                .collect(Collectors.toList());
                
        if (!highMemoryPlugins.isEmpty()) {
            log.warn("检测到内存使用过高的插件: {}", highMemoryPlugins);
            // 这里可以触发告警或其他操作
        }
        
        // 检查连续错误次数过多的插件
        List<String> highErrorPlugins = healthSnapshots.values().stream()
                .filter(s -> s.getConsecutiveErrorCount() > 0 && s.getState() == PluginState.STARTED)
                .map(PluginHealthSnapshot::getPluginId)
                .collect(Collectors.toList());
                
        if (!highErrorPlugins.isEmpty()) {
            log.warn("检测到有错误记录的插件: {}", highErrorPlugins);
            // 这里可以触发告警或其他操作
        }
    }

    /**
     * 获取指定插件的健康快照
     */
    public Optional<PluginHealthSnapshot> getPluginHealthSnapshot(String pluginId) {
        return Optional.ofNullable(healthSnapshots.get(pluginId));
    }

    /**
     * 获取所有插件的健康快照
     */
    public Collection<PluginHealthSnapshot> getAllHealthSnapshots() {
        return Collections.unmodifiableCollection(healthSnapshots.values());
    }

    /**
     * 获取所有不健康插件的健康快照
     */
    public Collection<PluginHealthSnapshot> getUnhealthySnapshots() {
        return healthSnapshots.values().stream()
                .filter(s -> !s.isHealthy() || s.getState() == PluginState.ERROR || s.getState() == PluginState.ISOLATED)
                .collect(Collectors.toList());
    }
    
    /**
     * 重置插件错误计数
     */
    public boolean resetPluginErrorCount(String pluginId) {
        PluginHealthSnapshot snapshot = healthSnapshots.get(pluginId);
        if (snapshot != null) {
            errorHandler.clearErrorRecord(pluginId);
            snapshot.setConsecutiveErrorCount(0);
            snapshot.setTotalErrorCount(0);
            log.info("重置插件[{}]错误计数", pluginId);
            return true;
        }
        return false;
    }
} 