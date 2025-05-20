package com.xiaoqu.qteamos.core.plugin.event;

import java.util.List;

/**
 * 插件事件基类
 * 提供插件相关事件的基本实现
 *
 * @author yangqijun
 * @date 2024-07-03
 */
public class PluginEvent extends AbstractEvent {
    
    /**
     * 插件事件主题
     */
    public static final String TOPIC = "plugin";
    
    /**
     * 插件加载事件类型
     */
    public static final String TYPE_LOADED = "loaded";
    
    /**
     * 插件初始化事件类型
     */
    public static final String TYPE_INITIALIZED = "initialized";
    
    /**
     * 插件启动事件类型
     */
    public static final String TYPE_STARTED = "started";
    
    /**
     * 插件停止事件类型
     */
    public static final String TYPE_STOPPED = "stopped";
    
    /**
     * 插件卸载事件类型
     */
    public static final String TYPE_UNLOADED = "unloaded";
    
    /**
     * 插件启用事件类型
     */
    public static final String TYPE_ENABLED = "enabled";
    
    /**
     * 插件禁用事件类型
     */
    public static final String TYPE_DISABLED = "disabled";
    
    /**
     * 插件错误事件类型
     */
    public static final String TYPE_ERROR = "error";
    
    /**
     * 插件依赖检查失败事件类型
     */
    public static final String TYPE_DEPENDENCY_FAILED = "dependency_failed";
    
    /**
     * 插件隔离事件类型
     */
    public static final String TYPE_ISOLATED = "plugin.isolated";
    
    /**
     * 插件恢复事件类型
     */
    public static final String TYPE_RECOVERED = "plugin.recovered";
    
    private final String pluginId;
    private final String version;
    private final Object data;
    
    /**
     * 构造函数
     *
     * @param type 事件类型
     * @param pluginId 插件ID
     */
    public PluginEvent(String type, String pluginId) {
        this(type, pluginId, null, null);
    }
    
    /**
     * 构造函数
     *
     * @param type 事件类型
     * @param pluginId 插件ID
     * @param version 插件版本
     */
    public PluginEvent(String type, String pluginId, String version) {
        this(type, pluginId, version, null);
    }
    
    /**
     * 构造函数
     *
     * @param type 事件类型
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param data 事件附加数据
     */
    public PluginEvent(String type, String pluginId, String version, Object data) {
        super(TOPIC, type, "system", false);
        this.pluginId = pluginId;
        this.version = version;
        this.data = data;
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
     * 获取插件版本
     *
     * @return 插件版本
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * 获取事件附加数据
     *
     * @return 事件数据
     */
    public Object getData() {
        return data;
    }
    
    /**
     * 获取事件数据并转换为指定类型
     *
     * @param <T> 目标类型
     * @param clazz 目标类型class
     * @return 转换后的数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        if (data != null && clazz.isInstance(data)) {
            return (T) data;
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "PluginEvent{" +
                "topic='" + getTopic() + '\'' +
                ", type='" + getType() + '\'' +
                ", source='" + getSource() + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", version='" + version + '\'' +
                ", timestamp=" + getTimestamp() +
                '}';
    }
    
    /**
     * 创建插件加载事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createLoadedEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_LOADED, pluginId, version);
    }
    
    /**
     * 创建插件初始化事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createInitializedEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_INITIALIZED, pluginId, version);
    }
    
    /**
     * 创建插件启动事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createStartedEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_STARTED, pluginId, version);
    }
    
    /**
     * 创建插件停止事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createStoppedEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_STOPPED, pluginId, version);
    }
    
    /**
     * 创建插件卸载事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createUnloadedEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_UNLOADED, pluginId, version);
    }
    
    /**
     * 创建插件启用事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createEnabledEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_ENABLED, pluginId, version);
    }
    
    /**
     * 创建插件禁用事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createDisabledEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_DISABLED, pluginId, version);
    }
    
    /**
     * 创建插件错误事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param error 错误信息
     * @return 插件事件
     */
    public static PluginEvent createErrorEvent(String pluginId, String version, Throwable error) {
        return new PluginEvent(TYPE_ERROR, pluginId, version, error);
    }
    
    /**
     * 创建插件依赖检查失败事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param missingDependencies 缺失的依赖
     * @return 插件事件
     */
    public static PluginEvent createDependencyFailedEvent(String pluginId, String version, List<String> missingDependencies) {
        return new PluginEvent(TYPE_DEPENDENCY_FAILED, pluginId, version, missingDependencies);
    }
    
    /**
     * 创建插件隔离事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createIsolatedEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_ISOLATED, pluginId, version);
    }
    
    /**
     * 创建插件恢复事件
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件事件
     */
    public static PluginEvent createRecoveredEvent(String pluginId, String version) {
        return new PluginEvent(TYPE_RECOVERED, pluginId, version);
    }
} 