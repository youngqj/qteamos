package com.xiaoqu.qteamos.core.plugin.monitoring;

import com.xiaoqu.qteamos.core.plugin.error.PluginErrorHandler;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager;
import com.xiaoqu.qteamos.core.plugin.manager.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件资源限制服务
 * 负责监控和限制插件资源使用，防止资源滥用
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
@Service
public class ResourceThrottleService {

    @Autowired
    private PluginResourceMonitor resourceMonitor;
    
    @Autowired
    private PluginLifecycleManager lifecycleManager;
    
    @Autowired
    private PluginErrorHandler errorHandler;

    /**
     * 资源限制阈值（高级别）
     */
    @Value("${plugin.resource.critical-memory-threshold:300}")
    private long criticalMemoryThresholdMB;
    
    @Value("${plugin.resource.critical-cpu-threshold:90}")
    private int criticalCpuThresholdPercent;
    
    @Value("${plugin.resource.critical-thread-threshold:50}")
    private int criticalThreadThreshold;

    /**
     * 资源超限次数记录
     */
    private final Map<String, ResourceViolationRecord> resourceViolations = new ConcurrentHashMap<>();
    
    /**
     * 资源限制状态
     */
    private final Map<String, ResourceRestriction> resourceRestrictions = new ConcurrentHashMap<>();

    /**
     * 检查插件资源使用并进行限制
     *
     * @param pluginId 插件ID
     * @return 是否应用了资源限制
     */
    public boolean checkAndApplyResourceLimits(String pluginId) {
        ResourceUsageSnapshot usage = resourceMonitor.getResourceSnapshot(pluginId);
        if (usage == null) {
            return false;
        }
        
        boolean appliedRestriction = false;
        
        // 检查内存使用是否超过临界阈值
        if (usage.getMemoryUsageMB() > criticalMemoryThresholdMB) {
            recordResourceViolation(pluginId, ResourceType.MEMORY);
            appliedRestriction = true;
        }
        
        // 检查CPU使用是否超过临界阈值
        if (usage.getCpuUsagePercent() > criticalCpuThresholdPercent) {
            recordResourceViolation(pluginId, ResourceType.CPU);
            appliedRestriction = true;
        }
        
        // 检查线程使用是否超过临界阈值
        if (usage.getThreadCount() > criticalThreadThreshold) {
            recordResourceViolation(pluginId, ResourceType.THREAD);
            appliedRestriction = true;
        }
        
        // 如果有资源超限，应用资源限制措施
        if (appliedRestriction) {
            applyResourceRestriction(pluginId);
        }
        
        return appliedRestriction;
    }

    /**
     * 记录资源违规
     *
     * @param pluginId    插件ID
     * @param resourceType 资源类型
     */
    private void recordResourceViolation(String pluginId, ResourceType resourceType) {
        ResourceViolationRecord record = resourceViolations.computeIfAbsent(
                pluginId, id -> new ResourceViolationRecord(pluginId));
        
        switch (resourceType) {
            case MEMORY:
                record.incrementMemoryViolationCount();
                break;
            case CPU:
                record.incrementCpuViolationCount();
                break;
            case THREAD:
                record.incrementThreadViolationCount();
                break;
        }
        
        log.warn("插件[{}]资源超限: {}, 当前违规次数: {}", 
                pluginId, resourceType, record.getTotalViolationCount());
    }

    /**
     * 应用资源限制
     *
     * @param pluginId 插件ID
     */
    private void applyResourceRestriction(String pluginId) {
        ResourceViolationRecord violationRecord = resourceViolations.get(pluginId);
        if (violationRecord == null) {
            return;
        }
        
        ResourceRestriction restriction = resourceRestrictions.computeIfAbsent(
                pluginId, id -> new ResourceRestriction(pluginId));
        
        // 根据违规次数升级限制级别
        int totalViolations = violationRecord.getTotalViolationCount();
        
        if (totalViolations >= 10) {
            // 严重违规，暂停插件
            restrictPluginSuspend(pluginId, restriction);
        } else if (totalViolations >= 5) {
            // 多次违规，降低资源限额
            restrictPluginSevere(pluginId, restriction);
        } else if (totalViolations >= 3) {
            // 初次违规，警告并轻微限制
            restrictPluginMild(pluginId, restriction);
        }
    }

    /**
     * 轻微限制（警告并轻微降低资源限额）
     */
    private void restrictPluginMild(String pluginId, ResourceRestriction restriction) {
        if (restriction.getRestrictionLevel() < RestrictionLevel.MILD.level) {
            log.warn("对插件[{}]应用轻微资源限制", pluginId);
            restriction.setRestrictionLevel(RestrictionLevel.MILD.level);
            restriction.setRestrictionReason("资源使用超过阈值");
            
            // 这里可以实现轻微的资源限制
            // 例如，通过插件API降低资源配额
            //TODO: 实现轻微的资源限制
        }
    }

    /**
     * 严重限制（大幅降低资源限额）
     */
    private void restrictPluginSevere(String pluginId, ResourceRestriction restriction) {
        if (restriction.getRestrictionLevel() < RestrictionLevel.SEVERE.level) {
            log.warn("对插件[{}]应用严重资源限制", pluginId);
            restriction.setRestrictionLevel(RestrictionLevel.SEVERE.level);
            restriction.setRestrictionReason("多次资源使用超过阈值");
            
            // 这里可以实现严重的资源限制
            // 例如，强制停止部分线程，限制后台任务等
            //TODO: 实现严重的资源限制
        }
    }

    /**
     * 暂停插件运行
     */
    private void restrictPluginSuspend(String pluginId, ResourceRestriction restriction) {
        if (restriction.getRestrictionLevel() < RestrictionLevel.SUSPEND.level) {
            log.error("由于严重资源滥用，暂停插件[{}]运行", pluginId);
            restriction.setRestrictionLevel(RestrictionLevel.SUSPEND.level);
            restriction.setRestrictionReason("严重资源滥用，自动暂停");
            
            try {
                // 获取插件信息
                lifecycleManager.getPluginInfo(pluginId).ifPresent(info -> {
                    // 只有插件处于运行状态才暂停
                    if (info.getState() == PluginState.STARTED) {
                        try {
                            // 停止插件
                            lifecycleManager.stopPlugin(pluginId);
                            // 更新插件状态
                            info.setState(PluginState.RESOURCE_LIMITED);
                            info.setErrorMessage("由于资源滥用，插件已被自动暂停");
                        } catch (PluginLifecycleException e) {
                            log.error("停止资源滥用插件[{}]时发生异常", pluginId, e);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("暂停资源滥用插件[{}]时发生错误", pluginId, e);
            }
        }
    }

    /**
     * 重置插件资源限制
     *
     * @param pluginId 插件ID
     */
    public void resetResourceRestriction(String pluginId) {
        resourceViolations.remove(pluginId);
        resourceRestrictions.remove(pluginId);
        
        // 恢复插件状态
        lifecycleManager.getPluginInfo(pluginId).ifPresent(info -> {
            if (info.getState() == PluginState.RESOURCE_LIMITED) {
                try {
                    // 尝试重启插件
                    if (lifecycleManager.startPlugin(pluginId)) {
                        log.info("插件[{}]资源限制已重置，已成功重启", pluginId);
                    } else {
                        log.warn("插件[{}]资源限制已重置，但重启失败", pluginId);
                    }
                } catch (Exception e) {
                    log.error("重启插件[{}]时发生错误", pluginId, e);
                }
            }
        });
        
        log.info("插件[{}]资源限制已重置", pluginId);
    }

    /**
     * 获取插件的资源违规记录
     *
     * @param pluginId 插件ID
     * @return 资源违规记录
     */
    public ResourceViolationRecord getViolationRecord(String pluginId) {
        return resourceViolations.get(pluginId);
    }

    /**
     * 获取插件的资源限制状态
     *
     * @param pluginId 插件ID
     * @return 资源限制状态
     */
    public ResourceRestriction getResourceRestriction(String pluginId) {
        return resourceRestrictions.get(pluginId);
    }

    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        MEMORY,
        CPU,
        THREAD,
        FILE,
        NETWORK
    }

    /**
     * 限制级别枚举
     */
    public enum RestrictionLevel {
        NONE(0),
        MILD(1),
        SEVERE(2),
        SUSPEND(3);

        private final int level;

        RestrictionLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * 资源违规记录
     */
    public static class ResourceViolationRecord {
        private final String pluginId;
        private int memoryViolationCount;
        private int cpuViolationCount;
        private int threadViolationCount;
        private int fileViolationCount;
        private int networkViolationCount;

        public ResourceViolationRecord(String pluginId) {
            this.pluginId = pluginId;
        }

        public void incrementMemoryViolationCount() {
            memoryViolationCount++;
        }

        public void incrementCpuViolationCount() {
            cpuViolationCount++;
        }

        public void incrementThreadViolationCount() {
            threadViolationCount++;
        }

        public void incrementFileViolationCount() {
            fileViolationCount++;
        }

        public void incrementNetworkViolationCount() {
            networkViolationCount++;
        }

        public int getTotalViolationCount() {
            return memoryViolationCount + cpuViolationCount + threadViolationCount 
                    + fileViolationCount + networkViolationCount;
        }

        public String getPluginId() {
            return pluginId;
        }

        public int getMemoryViolationCount() {
            return memoryViolationCount;
        }

        public int getCpuViolationCount() {
            return cpuViolationCount;
        }

        public int getThreadViolationCount() {
            return threadViolationCount;
        }

        public int getFileViolationCount() {
            return fileViolationCount;
        }

        public int getNetworkViolationCount() {
            return networkViolationCount;
        }
    }

    /**
     * 资源限制状态
     */
    public static class ResourceRestriction {
        private final String pluginId;
        private int restrictionLevel;
        private String restrictionReason;

        public ResourceRestriction(String pluginId) {
            this.pluginId = pluginId;
            this.restrictionLevel = RestrictionLevel.NONE.level;
        }

        public String getPluginId() {
            return pluginId;
        }

        public int getRestrictionLevel() {
            return restrictionLevel;
        }

        public void setRestrictionLevel(int restrictionLevel) {
            this.restrictionLevel = restrictionLevel;
        }

        public String getRestrictionReason() {
            return restrictionReason;
        }

        public void setRestrictionReason(String restrictionReason) {
            this.restrictionReason = restrictionReason;
        }
    }
} 