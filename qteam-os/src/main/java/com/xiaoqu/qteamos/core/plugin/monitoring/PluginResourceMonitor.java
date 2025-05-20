package com.xiaoqu.qteamos.core.plugin.monitoring;

import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件资源监控器
 * 负责实时监控各插件的资源使用情况
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
@Component
public class PluginResourceMonitor {

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private PluginMonitoringService monitoringService;

    /**
     * JMX监控Bean
     */
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;

    /**
     * 资源使用快照记录
     */
    private final Map<String, ResourceUsageSnapshot> resourceSnapshots = new ConcurrentHashMap<>();

    /**
     * CPU使用阈值(%)
     */
    @Value("${plugin.monitoring.cpu-threshold:80}")
    private int cpuThreshold;

    /**
     * 内存使用阈值(MB)
     */
    @Value("${plugin.monitoring.memory-threshold:100}")
    private int memoryThreshold;

    /**
     * 文件描述符使用阈值
     */
    @Value("${plugin.monitoring.fd-threshold:100}")
    private int fileDescriptorThreshold;

    /**
     * 线程使用阈值
     */
    @Value("${plugin.monitoring.thread-threshold:20}")
    private int threadThreshold;

    /**
     * 构造函数
     */
    public PluginResourceMonitor() {
        // 初始化JMX监控Bean
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * 定时收集资源使用数据
     */
    @Scheduled(fixedDelayString = "${plugin.monitoring.resource-check-interval:30000}")
    public void collectResourceUsage() {
        log.debug("开始收集插件资源使用情况...");
        
        Collection<PluginInfo> plugins = pluginRegistry.getAllPlugins();
        for (PluginInfo plugin : plugins) {
            String pluginId = plugin.getDescriptor().getPluginId();
            try {
                collectPluginResourceUsage(pluginId, plugin);
            } catch (Exception e) {
                log.error("收集插件[{}]资源使用情况时发生错误", pluginId, e);
            }
        }
        
        // 检查资源异常情况
        checkResourceAnomalies();
    }

    /**
     * 收集单个插件的资源使用情况
     */
    private void collectPluginResourceUsage(String pluginId, PluginInfo plugin) {
        // 创建或获取已有的资源快照
        ResourceUsageSnapshot snapshot = resourceSnapshots.computeIfAbsent(
                pluginId, id -> new ResourceUsageSnapshot(id));
        
        // 更新CPU使用率（示例实现，实际应通过JMX或其他方式获取更精确的数据）
        // 这里只是示例，实际中应考虑插件的隔离性，采用更精确的度量方式
        snapshot.setCpuUsagePercent(estimatePluginCpuUsage(pluginId));
        
        // 更新内存使用情况
        snapshot.setMemoryUsageMB(estimatePluginMemoryUsage(pluginId));
        
        // 更新线程使用情况
        //TODO: 实现线程使用情况的精确度量
        snapshot.setThreadCount(estimatePluginThreadCount(pluginId));
        
        // 更新文件描述符使用
        snapshot.setFileDescriptorCount(estimatePluginFileDescriptorCount(pluginId));
        
        // 更新网络连接数
        snapshot.setNetworkConnectionCount(estimatePluginNetworkConnections(pluginId));
        
        log.debug("插件[{}]资源使用: CPU={}%, 内存={}MB, 线程数={}, 文件描述符={}, 网络连接={}",
                pluginId, snapshot.getCpuUsagePercent(), snapshot.getMemoryUsageMB(),
                snapshot.getThreadCount(), snapshot.getFileDescriptorCount(),
                snapshot.getNetworkConnectionCount());
        
        // 更新健康快照中的资源使用信息
        monitoringService.getPluginHealthSnapshot(pluginId).ifPresent(healthSnapshot -> {
            healthSnapshot.setMemoryUsageMB(snapshot.getMemoryUsageMB());
            healthSnapshot.setThreadCount(snapshot.getThreadCount());
        });
    }

    /**
     * 估算插件CPU使用率
     */
    private int estimatePluginCpuUsage(String pluginId) {
        // 示例实现，随机生成CPU使用率作为演示
        // 实际应用中应该使用更精确的方法测量插件CPU使用
        return (int) (Math.random() * 50);
    }

    /**
     * 估算插件内存使用
     */
    private long estimatePluginMemoryUsage(String pluginId) {
        // 同样是示例实现
        // 实际应用中应基于ClassLoader隔离区域或通过JMX等方式获取更精确的数据
        return 10 + (int) (Math.random() * 90);
    }

    /**
     * 估算插件线程数
     */
    private int estimatePluginThreadCount(String pluginId) {
        // 示例实现
        // 实际应用中应该通过插件线程组或线程命名规则识别属于插件的线程
        return 1 + (int) (Math.random() * 10);
    }

    /**
     * 估算插件文件描述符使用数
     */
    private int estimatePluginFileDescriptorCount(String pluginId) {
        // 示例实现
        // 实际应用中可通过/proc文件系统或JMX获取
        return (int) (Math.random() * 20);
    }

    /**
     * 估算插件网络连接数
     */
    private int estimatePluginNetworkConnections(String pluginId) {
        // 示例实现
        // 实际应用中可通过/proc/net或JMX获取
        return (int) (Math.random() * 5);
    }

    /**
     * 检查资源异常情况
     */
    private void checkResourceAnomalies() {
        for (ResourceUsageSnapshot snapshot : resourceSnapshots.values()) {
            // 检查CPU使用异常
            if (snapshot.getCpuUsagePercent() > cpuThreshold) {
                log.warn("插件[{}] CPU使用率过高: {}%", snapshot.getPluginId(), snapshot.getCpuUsagePercent());
                // 这里可以触发告警或进一步处理
            }
            
            // 检查内存使用异常
            if (snapshot.getMemoryUsageMB() > memoryThreshold) {
                log.warn("插件[{}] 内存使用过高: {}MB", snapshot.getPluginId(), snapshot.getMemoryUsageMB());
                // 这里可以触发告警或进一步处理
            }
            
            // 检查线程使用异常
            if (snapshot.getThreadCount() > threadThreshold) {
                log.warn("插件[{}] 线程数过多: {}", snapshot.getPluginId(), snapshot.getThreadCount());
                // 这里可以触发告警或进一步处理
            }
            
            // 检查文件描述符使用异常
            if (snapshot.getFileDescriptorCount() > fileDescriptorThreshold) {
                log.warn("插件[{}] 文件描述符过多: {}", snapshot.getPluginId(), snapshot.getFileDescriptorCount());
                // 这里可以触发告警或进一步处理
            }
        }
    }

    /**
     * 获取指定插件的资源使用快照
     */
    public ResourceUsageSnapshot getResourceSnapshot(String pluginId) {
        return resourceSnapshots.get(pluginId);
    }

    /**
     * 获取所有插件的资源使用快照
     */
    public Collection<ResourceUsageSnapshot> getAllResourceSnapshots() {
        return resourceSnapshots.values();
    }
} 