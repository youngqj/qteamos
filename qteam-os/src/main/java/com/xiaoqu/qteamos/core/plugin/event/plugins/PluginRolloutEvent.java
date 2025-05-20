package com.xiaoqu.qteamos.core.plugin.event.plugins;

import com.xiaoqu.qteamos.core.plugin.event.AbstractEvent;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRolloutManager.RolloutState;

/**
 * 插件灰度发布事件
 * 在插件灰度发布生命周期的各个阶段触发，用于通知相关组件灰度发布状态变化
 *
 * @author yangqijun
 * @date 2024-07-20
 */
public class PluginRolloutEvent extends AbstractEvent {
    
    /**
     * 灰度发布事件主题
     */
    public static final String TOPIC = "plugin.rollout";
    
    /**
     * 灰度发布开始事件类型
     */
    public static final String TYPE_STARTED = "started";
    
    /**
     * 灰度发布批次开始事件类型
     */
    public static final String TYPE_BATCH_STARTED = "batch_started";
    
    /**
     * 灰度发布批次完成事件类型
     */
    public static final String TYPE_BATCH_COMPLETED = "batch_completed";
    
    /**
     * 灰度发布暂停事件类型
     */
    public static final String TYPE_PAUSED = "paused";
    
    /**
     * 灰度发布恢复事件类型
     */
    public static final String TYPE_RESUMED = "resumed";
    
    /**
     * 灰度发布完成事件类型
     */
    public static final String TYPE_COMPLETED = "completed";
    
    /**
     * 灰度发布失败事件类型
     */
    public static final String TYPE_FAILED = "failed";
    
    /**
     * 灰度发布取消事件类型
     */
    public static final String TYPE_CANCELLED = "cancelled";
    
    private final String pluginId;
    private final String fromVersion;
    private final String toVersion;
    private final int percentage;
    private final RolloutState state;
    private final String message;
    
    /**
     * 构造函数
     *
     * @param type 事件类型
     * @param pluginId 插件ID
     * @param fromVersion 源版本
     * @param toVersion 目标版本
     * @param percentage 当前百分比
     * @param state 灰度状态
     */
    public PluginRolloutEvent(String type, String pluginId, String fromVersion, 
                            String toVersion, int percentage, RolloutState state) {
        this(type, pluginId, fromVersion, toVersion, percentage, state, null);
    }
    
    /**
     * 构造函数
     *
     * @param type 事件类型
     * @param pluginId 插件ID
     * @param fromVersion 源版本
     * @param toVersion 目标版本
     * @param percentage 当前百分比
     * @param state 灰度状态
     * @param message 附加消息
     */
    public PluginRolloutEvent(String type, String pluginId, String fromVersion, 
                            String toVersion, int percentage, RolloutState state, String message) {
        super(TOPIC, type, "plugin.rollout");
        this.pluginId = pluginId;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.percentage = percentage;
        this.state = state;
        this.message = message;
    }
    
    /**
     * 获取插件ID
     *
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 获取源版本
     *
     * @return 源版本
     */
    public String getFromVersion() {
        return fromVersion;
    }
    
    /**
     * 获取目标版本
     *
     * @return 目标版本
     */
    public String getToVersion() {
        return toVersion;
    }
    
    /**
     * 获取当前百分比
     *
     * @return 当前百分比
     */
    public int getPercentage() {
        return percentage;
    }
    
    /**
     * 获取灰度状态
     *
     * @return 灰度状态
     */
    public RolloutState getState() {
        return state;
    }
    
    /**
     * 获取附加消息
     *
     * @return 附加消息
     */
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return "PluginRolloutEvent{" +
                "topic='" + getTopic() + '\'' +
                ", type='" + getType() + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", fromVersion='" + fromVersion + '\'' +
                ", toVersion='" + toVersion + '\'' +
                ", percentage=" + percentage +
                ", state=" + state +
                ", message='" + message + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
    
    /**
     * 创建灰度发布开始事件
     */
    public static PluginRolloutEvent createStartedEvent(String pluginId, String fromVersion, 
                                                     String toVersion) {
        return new PluginRolloutEvent(TYPE_STARTED, pluginId, fromVersion, toVersion, 0, 
                                    RolloutState.INITIALIZED);
    }
    
    /**
     * 创建批次开始事件
     */
    public static PluginRolloutEvent createBatchStartedEvent(String pluginId, String fromVersion, 
                                                          String toVersion, int percentage) {
        return new PluginRolloutEvent(TYPE_BATCH_STARTED, pluginId, fromVersion, toVersion, 
                                    percentage, RolloutState.IN_PROGRESS);
    }
    
    /**
     * 创建批次完成事件
     */
    public static PluginRolloutEvent createBatchCompletedEvent(String pluginId, String fromVersion, 
                                                            String toVersion, int percentage, int batchNumber) {
        return new PluginRolloutEvent(TYPE_BATCH_COMPLETED, pluginId, fromVersion, toVersion, 
                                    percentage, RolloutState.IN_PROGRESS, 
                                    "批次" + batchNumber + "完成");
    }
    
    /**
     * 创建灰度发布暂停事件
     */
    public static PluginRolloutEvent createPausedEvent(String pluginId, String fromVersion, 
                                                    String toVersion, int percentage, String reason) {
        return new PluginRolloutEvent(TYPE_PAUSED, pluginId, fromVersion, toVersion, 
                                    percentage, RolloutState.PAUSED, reason);
    }
    
    /**
     * 创建灰度发布恢复事件
     */
    public static PluginRolloutEvent createResumedEvent(String pluginId, String fromVersion, 
                                                     String toVersion, int percentage) {
        return new PluginRolloutEvent(TYPE_RESUMED, pluginId, fromVersion, toVersion, 
                                    percentage, RolloutState.IN_PROGRESS);
    }
    
    /**
     * 创建灰度发布完成事件
     */
    public static PluginRolloutEvent createCompletedEvent(String pluginId, String fromVersion, 
                                                       String toVersion) {
        return new PluginRolloutEvent(TYPE_COMPLETED, pluginId, fromVersion, toVersion, 
                                    100, RolloutState.COMPLETED);
    }
    
    /**
     * 创建灰度发布失败事件
     */
    public static PluginRolloutEvent createFailedEvent(String pluginId, String fromVersion, 
                                                    String toVersion, int percentage, String reason) {
        return new PluginRolloutEvent(TYPE_FAILED, pluginId, fromVersion, toVersion, 
                                    percentage, RolloutState.FAILED, reason);
    }
    
    /**
     * 创建灰度发布取消事件
     */
    public static PluginRolloutEvent createCancelledEvent(String pluginId, String fromVersion, 
                                                       String toVersion, int percentage, String reason) {
        return new PluginRolloutEvent(TYPE_CANCELLED, pluginId, fromVersion, toVersion, 
                                    percentage, RolloutState.FAILED, reason);
    }
} 