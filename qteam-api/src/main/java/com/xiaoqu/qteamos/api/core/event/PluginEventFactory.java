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
 * 插件事件工厂类
 * 用于创建各种类型的插件事件，简化事件创建过程
 *
 * @author yangqijun
 * @date 2025-08-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件事件工厂类
 * 提供创建各类插件事件的静态方法
 */
public final class PluginEventFactory {
    
    private PluginEventFactory() {
        // 防止实例化
    }
    
    /**
     * 创建自定义事件
     * 
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param data 事件数据
     * @return 自定义事件
     */
    public static PluginEvent createCustomEvent(String topic, String type, String source, Object data) {
        return new CustomPluginEvent(topic, type, source, data);
    }
    
    /**
     * 创建自定义插件事件
     * 
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param data 事件数据
     * @return 自定义插件事件
     */
    public static PluginEvent createCustomPluginEvent(
            String topic, String type, String source, 
            String pluginId, String version, Object data) {
        return new CustomPluginEvent(topic, type, source, pluginId, version, data);
    }
    
    /**
     * 创建系统事件
     * 
     * @param type 事件类型
     * @param source 事件来源
     * @param data 事件数据
     * @return 系统事件
     */
    public static PluginEvent createSystemEvent(String type, String source, Object data) {
        return new CustomPluginEvent(PluginEventTypes.Topics.SYSTEM, type, source, data);
    }
    
    /**
     * 创建插件加载事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件加载事件
     */
    public static PluginEvent createPluginLoadedEvent(String pluginId, String version) {
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.LOADED,
                "system",
                pluginId,
                version,
                null);
    }
    
    /**
     * 创建插件加载事件（带详细信息）
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param loadedClassCount 加载的类数量
     * @param loadedResourceCount 加载的资源数量
     * @param loadTime 加载时间
     * @return 插件加载事件
     */
    public static PluginEvent createPluginLoadedEvent(
            String pluginId, String version, int loadedClassCount, int loadedResourceCount, long loadTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("loadedClassCount", loadedClassCount);
        data.put("loadedResourceCount", loadedResourceCount);
        data.put("loadTime", loadTime);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.LOADED,
                "system",
                pluginId,
                version,
                data);
    }
    
    /**
     * 创建插件初始化事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件初始化事件
     */
    public static PluginEvent createPluginInitializedEvent(String pluginId, String version) {
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.INITIALIZED,
                "system",
                pluginId,
                version,
                null);
    }
    
    /**
     * 创建插件初始化事件（带详细信息）
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param initData 初始化数据
     * @return 插件初始化事件
     */
    public static PluginEvent createPluginInitializedEvent(
            String pluginId, String version, Map<String, Object> initData) {
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.INITIALIZED,
                "system",
                pluginId,
                version,
                initData);
    }
    
    /**
     * 创建插件启动事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件启动事件
     */
    public static PluginEvent createPluginStartedEvent(String pluginId, String version) {
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.STARTED,
                "system",
                pluginId,
                version,
                null);
    }
    
    /**
     * 创建插件启动事件（带详细信息）
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param startTime 启动时间
     * @return 插件启动事件
     */
    public static PluginEvent createPluginStartedEvent(
            String pluginId, String version, long startTime) {
        Map<String, Object> data = Collections.singletonMap("startTime", startTime);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.STARTED,
                "system",
                pluginId,
                version,
                data);
    }
    
    /**
     * 创建插件停止事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件停止事件
     */
    public static PluginEvent createPluginStoppedEvent(String pluginId, String version) {
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.STOPPED,
                "system",
                pluginId,
                version,
                null);
    }
    
    /**
     * 创建插件停止事件（带详细信息）
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param reason 停止原因
     * @return 插件停止事件
     */
    public static PluginEvent createPluginStoppedEvent(
            String pluginId, String version, String reason) {
        Map<String, Object> data = Collections.singletonMap("reason", reason);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.STOPPED,
                "system",
                pluginId,
                version,
                data);
    }
    
    /**
     * 创建插件卸载事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @return 插件卸载事件
     */
    public static PluginEvent createPluginUnloadedEvent(String pluginId, String version) {
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.UNLOADED,
                "system",
                pluginId,
                version,
                null);
    }
    
    /**
     * 创建插件卸载事件（带详细信息）
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param forced 是否强制卸载
     * @return 插件卸载事件
     */
    public static PluginEvent createPluginUnloadedEvent(
            String pluginId, String version, boolean forced) {
        Map<String, Object> data = Collections.singletonMap("forced", forced);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN,
                PluginEventTypes.Plugin.UNLOADED,
                "system",
                pluginId,
                version,
                data);
    }
    
    /**
     * 创建插件错误事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param error 错误信息
     * @return 插件错误事件
     */
    public static PluginEvent createPluginErrorEvent(String pluginId, String version, Throwable error) {
        Map<String, Object> data = Collections.singletonMap("error", error);
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN, 
                PluginEventTypes.Plugin.ERROR, 
                "system", 
                pluginId, 
                version, 
                data);
    }
    
    /**
     * 创建插件依赖失败事件
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param missingDependencies 缺失依赖列表
     * @return 插件依赖失败事件
     */
    public static PluginEvent createPluginDependencyFailedEvent(
            String pluginId, String version, List<String> missingDependencies) {
        Map<String, Object> data = Collections.singletonMap("missingDependencies", missingDependencies);
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN, 
                PluginEventTypes.Plugin.DEPENDENCY_FAILED, 
                "system", 
                pluginId, 
                version, 
                data);
    }
    
    /**
     * 创建插件健康检查事件
     * 
     * @param pluginId 插件ID
     * @param healthy 是否健康
     * @param message 健康消息
     * @return 插件健康检查事件
     */
    public static PluginEvent createPluginHealthCheckEvent(
            String pluginId, boolean healthy, String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("healthy", healthy);
        data.put("message", message);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN_HEALTH,
                PluginEventTypes.Health.HEALTH_CHECK,
                "system",
                pluginId,
                null,
                data);
    }
    
    /**
     * 创建插件健康恢复事件
     * 
     * @param pluginId 插件ID
     * @param message 恢复消息
     * @return 插件健康恢复事件
     */
    public static PluginEvent createPluginHealthRecoveredEvent(
            String pluginId, String message) {
        Map<String, Object> data = Collections.singletonMap("message", message);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN_HEALTH,
                PluginEventTypes.Health.HEALTH_RECOVERED,
                "system",
                pluginId,
                null,
                data);
    }
    
    /**
     * 创建插件健康失败事件
     * 
     * @param pluginId 插件ID
     * @param message 失败消息
     * @return 插件健康失败事件
     */
    public static PluginEvent createPluginHealthFailedEvent(
            String pluginId, String message) {
        Map<String, Object> data = Collections.singletonMap("message", message);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN_HEALTH,
                PluginEventTypes.Health.HEALTH_FAILED,
                "system",
                pluginId,
                null,
                data);
    }
    
    /**
     * 创建插件恢复尝试事件
     * 
     * @param pluginId 插件ID
     * @param message 恢复消息
     * @return 插件恢复尝试事件
     */
    public static PluginEvent createPluginRecoveryAttemptEvent(
            String pluginId, String message) {
        Map<String, Object> data = Collections.singletonMap("message", message);
        
        return new CustomPluginEvent(
                PluginEventTypes.Topics.PLUGIN_HEALTH,
                PluginEventTypes.Health.RECOVERY_ATTEMPT,
                "system",
                pluginId,
                null,
                data);
    }
    
    /**
     * 自定义插件事件内部类
     */
    private static class CustomPluginEvent extends PluginEvent {
        
        private static final long serialVersionUID = 1L;
        
        /**
         * 构造函数
         * 
         * @param topic 事件主题
         * @param type 事件类型
         * @param source 事件来源
         * @param data 事件数据
         */
        public CustomPluginEvent(String topic, String type, String source, Object data) {
            super(topic, type, source, null, null, data, false);
        }
        
        /**
         * 构造函数
         * 
         * @param topic 事件主题
         * @param type 事件类型
         * @param source 事件来源
         * @param pluginId 插件ID
         * @param version 插件版本
         * @param data 事件数据
         */
        public CustomPluginEvent(String topic, String type, String source, 
                String pluginId, String version, Object data) {
            super(topic, type, source, pluginId, version, data, false);
        }
        
        /**
         * 构造函数
         * 
         * @param topic 事件主题
         * @param type 事件类型
         * @param source 事件来源
         * @param pluginId 插件ID
         * @param version 插件版本
         * @param data 事件数据
         * @param cancellable 是否可取消
         */
        public CustomPluginEvent(String topic, String type, String source, 
                String pluginId, String version, Object data, boolean cancellable) {
            super(topic, type, source, pluginId, version, data, cancellable);
        }
    }
} 