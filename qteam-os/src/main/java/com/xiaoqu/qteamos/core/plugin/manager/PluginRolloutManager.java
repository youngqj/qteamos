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

package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 插件灰度发布和回滚管理器（已弃用）
 * 此类已重构为委托模式，实际功能由新的插件管理组件提供
 * 保留此类仅为向后兼容性考虑
 *
 * @author yangqijun
 * @date 2024-07-19
 * @deprecated 请使用 PluginVersionManager 和 PluginDeploymentService 等新组件替代
 */
@Component
@Deprecated
public class PluginRolloutManager {
    private static final Logger log = LoggerFactory.getLogger(PluginRolloutManager.class);

    // 保留旧组件的依赖，但实际功能已委托给新组件
    @Autowired(required = false)
    private EnhancedPluginVersionManager versionManager;

    public PluginRolloutManager() {
        log.warn("PluginRolloutManager类已弃用，请使用新的插件管理组件。此类将在未来版本中移除。");
    }

    /**
     * 启动灰度发布过程
     * @param pluginId 插件ID
     * @param targetVersion 目标版本
     * @param batchSize 每批百分比
     * @param validateTime 每批验证时间（分钟）
     * @return 灰度发布状态
     * @deprecated 请使用新的PluginDeploymentService组件
     */
    @Deprecated
    public RolloutStatus startGradualRollout(String pluginId, String targetVersion, int batchSize, int validateTime) {
        log.warn("startGradualRollout()已弃用，请使用新的PluginDeploymentService组件");
        return RolloutStatus.failed(pluginId, "功能已弃用");
    }

    /**
     * 进行下一批次的灰度发布
     * @param pluginId 插件ID
     * @return 更新后的灰度发布状态
     * @deprecated 请使用新的PluginDeploymentService组件
     */
    @Deprecated
    public RolloutStatus proceedToNextBatch(String pluginId) {
        log.warn("proceedToNextBatch()已弃用，请使用新的PluginDeploymentService组件");
        return RolloutStatus.failed(pluginId, "功能已弃用");
    }

    /**
     * 暂停灰度发布
     * @param pluginId 插件ID
     * @param reason 暂停原因
     * @return 灰度发布状态
     * @deprecated 请使用新的PluginDeploymentService组件
     */
    @Deprecated
    public RolloutStatus pauseRollout(String pluginId, String reason) {
        log.warn("pauseRollout()已弃用，请使用新的PluginDeploymentService组件");
        return RolloutStatus.failed(pluginId, "功能已弃用");
    }

    /**
     * 恢复灰度发布
     * @param pluginId 插件ID
     * @return 灰度发布状态
     * @deprecated 请使用新的PluginDeploymentService组件
     */
    @Deprecated
    public RolloutStatus resumeRollout(String pluginId) {
        log.warn("resumeRollout()已弃用，请使用新的PluginDeploymentService组件");
        return RolloutStatus.failed(pluginId, "功能已弃用");
    }

    /**
     * 取消灰度发布
     * @param pluginId 插件ID
     * @param reason 取消原因
     * @return 灰度发布状态
     * @deprecated 请使用新的PluginDeploymentService组件
     */
    @Deprecated
    public RolloutStatus cancelRollout(String pluginId, String reason) {
        log.warn("cancelRollout()已弃用，请使用新的PluginDeploymentService组件");
        return RolloutStatus.failed(pluginId, "功能已弃用");
    }

    /**
     * 回滚到指定版本
     * @param pluginId 插件ID
     * @param targetVersion 目标版本
     * @return 是否成功
     * @deprecated 请使用PluginVersionManager组件
     */
    @Deprecated
    public boolean rollbackToVersion(String pluginId, String targetVersion) {
        log.warn("rollbackToVersion()已弃用，请使用PluginVersionManager组件");
        return false;
    }

    /**
     * 检查待处理的灰度发布
     * @deprecated 功能已自动化，无需手动检查
     */
    @Deprecated
    @Scheduled(fixedDelayString = "${plugin.rollout.check-interval:60000}")
    public void checkPendingRollouts() {
        log.debug("checkPendingRollouts()已弃用，功能已自动化");
    }

    /**
     * 获取灰度发布状态
     * @param pluginId 插件ID
     * @return 灰度发布状态
     * @deprecated 请使用新的PluginDeploymentService组件
     */
    @Deprecated
    public Optional<RolloutStatus> getRolloutStatus(String pluginId) {
        log.warn("getRolloutStatus()已弃用，请使用新的PluginDeploymentService组件");
        return Optional.empty();
    }

    /**
     * 获取所有灰度发布状态
     * @return 所有灰度发布状态
     * @deprecated 请使用新的PluginDeploymentService组件
     */
    @Deprecated
    public Collection<RolloutStatus> getAllRolloutStatuses() {
        log.warn("getAllRolloutStatuses()已弃用，请使用新的PluginDeploymentService组件");
        return java.util.Collections.emptyList();
    }

    /**
     * 判断插件是否应该自动激活
     * @param descriptor 插件描述符
     * @return 是否应该自动激活
     * @deprecated 请使用PluginActivationService组件
     */
    @Deprecated
    public boolean shouldAutoActivate(PluginDescriptor descriptor) {
        log.warn("shouldAutoActivate()已弃用，请使用PluginActivationService组件");
        return false;
    }

    /**
     * 灰度发布状态（向后兼容）
     * @deprecated 请使用新的状态管理组件
     */
    @Deprecated
    public static class RolloutStatus {
        private String pluginId;
        private String currentVersion;
        private String targetVersion;
        private int batchSize;
        private int validateTimeMinutes;
        private int currentBatch;
        private int currentPercentage;
        private RolloutState state;
        private String message;
        private LocalDateTime startTime;
        private LocalDateTime lastBatchTime;
        private LocalDateTime completionTime;
        private Map<String, Object> metadata;

        public RolloutStatus(String pluginId, String currentVersion, String targetVersion, 
                           int batchSize, int validateTimeMinutes) {
            this.pluginId = pluginId;
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
            this.batchSize = batchSize;
            this.validateTimeMinutes = validateTimeMinutes;
            this.currentBatch = 0;
            this.currentPercentage = 0;
            this.state = RolloutState.INITIALIZED;
            this.startTime = LocalDateTime.now();
            this.metadata = new java.util.HashMap<>();
        }

        public static RolloutStatus failed(String pluginId, String message) {
            RolloutStatus status = new RolloutStatus(pluginId, "unknown", "unknown", 0, 0);
            status.setState(RolloutState.FAILED);
            status.setMessage(message);
            return status;
        }

        public String getPluginId() { return pluginId; }
        public String getCurrentVersion() { return currentVersion; }
        public String getTargetVersion() { return targetVersion; }
        public int getBatchSize() { return batchSize; }
        public int getValidateTimeMinutes() { return validateTimeMinutes; }
        public int getCurrentBatch() { return currentBatch; }
        public void setCurrentBatch(int currentBatch) { this.currentBatch = currentBatch; }
        public int getCurrentPercentage() { return currentPercentage; }
        public void setCurrentPercentage(int currentPercentage) { this.currentPercentage = currentPercentage; }
        public RolloutState getState() { return state; }
        public void setState(RolloutState state) { this.state = state; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getLastBatchTime() { return lastBatchTime; }
        public void setLastBatchTime(LocalDateTime lastBatchTime) { this.lastBatchTime = lastBatchTime; }
        public LocalDateTime getCompletionTime() { return completionTime; }
        public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

        @Override
        public String toString() {
            return String.format("RolloutStatus{pluginId='%s', state=%s, currentBatch=%d, currentPercentage=%d}", 
                    pluginId, state, currentBatch, currentPercentage);
        }
    }

    /**
     * 灰度发布状态枚举（向后兼容）
     */
    @Deprecated
    public enum RolloutState {
        INITIALIZED,    // 初始化
        IN_PROGRESS,    // 进行中
        PAUSED,         // 暂停
        COMPLETED,      // 完成
        FAILED          // 失败
    }

    /**
     * 灰度发布事件类型枚举（向后兼容）
     */
    @Deprecated
    public enum RolloutEventType {
        STARTED,          // 灰度发布开始
        BATCH_STARTED,    // 批次开始
        BATCH_COMPLETED,  // 批次完成
        PAUSED,           // 暂停
        RESUMED,          // 恢复
        COMPLETED,        // 完成
        FAILED,           // 失败
        CANCELLED         // 取消
    }

    /**
     * 插件灰度发布事件（向后兼容）
     */
    @Deprecated
    public static class PluginRolloutEvent {
        private final Object source;
        private final String pluginId;
        private final String fromVersion;
        private final String toVersion;
        private final int percentage;
        private final RolloutEventType eventType;

        public PluginRolloutEvent(Object source, String pluginId, String fromVersion, 
                                String toVersion, int percentage, RolloutEventType eventType) {
            this.source = source;
            this.pluginId = pluginId;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.percentage = percentage;
            this.eventType = eventType;
        }

        public Object getSource() { return source; }
        public String getPluginId() { return pluginId; }
        public String getFromVersion() { return fromVersion; }
        public String getToVersion() { return toVersion; }
        public int getPercentage() { return percentage; }
        public RolloutEventType getEventType() { return eventType; }
    }
} 