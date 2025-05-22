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
 * 插件事件类型常量类
 * 统一管理所有插件相关事件的主题和类型常量
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event;

public final class PluginEventTypes {
    
    private PluginEventTypes() {
        // 防止实例化
    }
    
    /**
     * 插件主题
     */
    public static final class Topics {
        /**
         * 插件基础主题
         */
        public static final String PLUGIN = "plugin";
        
        /**
         * 插件文件主题
         */
        public static final String PLUGIN_FILE = "plugin.file";
        
        /**
         * 插件健康主题
         */
        public static final String PLUGIN_HEALTH = "plugin.health";
        
        /**
         * 插件灰度发布主题
         */
        public static final String PLUGIN_ROLLOUT = "plugin.rollout";
        
        /**
         * 插件安装主题
         */
        public static final String PLUGIN_INSTALL = "plugin.install";
        
        /**
         * 插件状态主题
         */
        public static final String PLUGIN_STATE = "plugin.state";
        
        /**
         * 系统主题
         */
        public static final String SYSTEM = "system";
        
        /**
         * 通用事件主题
         */
        public static final String GENERIC = "plugin.event";
        
        private Topics() {
            // 防止实例化
        }
    }
    
    /**
     * 插件基础事件类型
     */
    public static final class Plugin {
        /**
         * 插件扫描发现事件类型
         */
        public static final String DISCOVERED = "discovered";
        
        /**
         * 插件加载事件类型
         */
        public static final String LOADED = "loaded";
        
        /**
         * 插件初始化事件类型
         */
        public static final String INITIALIZED = "initialized";
        
        /**
         * 插件启动事件类型
         */
        public static final String STARTED = "started";
        
        /**
         * 插件停止事件类型
         */
        public static final String STOPPED = "stopped";
        
        /**
         * 插件卸载事件类型
         */
        public static final String UNLOADED = "unloaded";
        
        /**
         * 插件启用事件类型
         */
        public static final String ENABLED = "enabled";
        
        /**
         * 插件禁用事件类型
         */
        public static final String DISABLED = "disabled";
        
        /**
         * 插件错误事件类型
         */
        public static final String ERROR = "error";
        
        /**
         * 插件依赖检查失败事件类型
         */
        public static final String DEPENDENCY_FAILED = "dependency_failed";
        
        private Plugin() {
            // 防止实例化
        }
    }
    
    /**
     * 插件文件事件类型
     */
    public static final class File {
        /**
         * 文件创建事件类型
         */
        public static final String CREATED = "created";
        
        /**
         * 文件修改事件类型
         */
        public static final String MODIFIED = "modified";
        
        /**
         * 文件删除事件类型
         */
        public static final String DELETED = "deleted";
        
        private File() {
            // 防止实例化
        }
    }
    
    /**
     * 插件健康事件类型
     */
    public static final class Health {
        /**
         * 插件隔离事件类型
         */
        public static final String ISOLATED = "isolated";
        
        /**
         * 插件恢复事件类型
         */
        public static final String RECOVERED = "recovered";
        
        /**
         * 插件健康检查事件类型
         */
        public static final String HEALTH_CHECK = "health_check";
        
        /**
         * 插件恢复尝试事件类型
         */
        public static final String RECOVERY_ATTEMPT = "recovery_attempt";
        
        /**
         * 插件健康恢复事件类型
         */
        public static final String HEALTH_RECOVERED = "health_recovered";
        
        /**
         * 插件健康失败事件类型
         */
        public static final String HEALTH_FAILED = "health_failed";
        
        private Health() {
            // 防止实例化
        }
    }
    
    /**
     * 插件灰度发布事件类型
     */
    public static final class Rollout {
        /**
         * 灰度发布开始事件类型
         */
        public static final String STARTED = "started";
        
        /**
         * 灰度发布批次开始事件类型
         */
        public static final String BATCH_STARTED = "batch_started";
        
        /**
         * 灰度发布批次完成事件类型
         */
        public static final String BATCH_COMPLETED = "batch_completed";
        
        /**
         * 灰度发布暂停事件类型
         */
        public static final String PAUSED = "paused";
        
        /**
         * 灰度发布恢复事件类型
         */
        public static final String RESUMED = "resumed";
        
        /**
         * 灰度发布完成事件类型
         */
        public static final String COMPLETED = "completed";
        
        /**
         * 灰度发布失败事件类型
         */
        public static final String FAILED = "failed";
        
        /**
         * 灰度发布取消事件类型
         */
        public static final String CANCELLED = "cancelled";
        
        private Rollout() {
            // 防止实例化
        }
    }
    
    /**
     * 插件安装事件类型
     */
    public static final class Install {
        /**
         * 插件安装事件类型
         */
        public static final String INSTALLED = "installed";
        
        /**
         * 插件卸载事件类型
         */
        public static final String UNINSTALLED = "uninstalled";
        
        /**
         * 插件升级事件类型
         */
        public static final String UPGRADED = "upgraded";
        
        /**
         * 插件验证事件类型
         */
        public static final String VERIFIED = "verified";
        
        /**
         * 插件验证失败事件类型
         */
        public static final String VERIFY_FAILED = "verify_failed";
        
        /**
         * 插件备份事件类型
         */
        public static final String BACKUP = "backup";
        
        /**
         * 插件恢复事件类型
         */
        public static final String RESTORE = "restore";
        
        private Install() {
            // 防止实例化
        }
    }
    
    /**
     * 插件状态事件类型
     */
    public static final class State {
        /**
         * 状态变更事件类型
         */
        public static final String STATE_CHANGE = "state_change";
        
        /**
         * 状态历史事件类型
         */
        public static final String HISTORY_ADDED = "history_added";
        
        private State() {
            // 防止实例化
        }
    }
    
    /**
     * 系统事件类型
     */
    public static final class System {
        /**
         * 系统初始化事件类型
         */
        public static final String INITIALIZED = "initialized";
        
        /**
         * 系统关闭事件类型
         */
        public static final String SHUTDOWN = "shutdown";
        
        /**
         * 系统就绪事件类型
         */
        public static final String READY = "ready";
        
        /**
         * 系统重启事件类型
         */
        public static final String RESTART = "restart";
        
        /**
         * 系统配置变更事件类型
         */
        public static final String CONFIG_CHANGED = "config_changed";
        
        private System() {
            // 防止实例化
        }
    }
} 