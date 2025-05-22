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
 * 插件停止事件
 * 表示插件已成功停止运行
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.lifecycle;

import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;

public class PluginStoppedEvent extends PluginLifecycleEvent {
    
    /**
     * 运行时长（毫秒）
     */
    private final long runningTime;
    
    /**
     * 停止原因
     */
    private final StopReason reason;
    
    /**
     * 是否正常停止
     */
    private final boolean graceful;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param runningTime 运行时长
     * @param reason 停止原因
     * @param graceful 是否正常停止
     */
    public PluginStoppedEvent(String pluginId, String version, long runningTime, StopReason reason, boolean graceful) {
        super(PluginEventTypes.Plugin.STOPPED, pluginId, version);
        this.runningTime = runningTime;
        this.reason = reason;
        this.graceful = graceful;
    }
    
    /**
     * 获取运行时长
     *
     * @return 运行时长（毫秒）
     */
    public long getRunningTime() {
        return runningTime;
    }
    
    /**
     * 获取停止原因
     *
     * @return 停止原因
     */
    public StopReason getReason() {
        return reason;
    }
    
    /**
     * 是否正常停止
     *
     * @return 是否正常停止
     */
    public boolean isGraceful() {
        return graceful;
    }
    
    /**
     * 停止原因枚举
     */
    public enum StopReason {
        /**
         * 用户请求停止
         */
        USER_REQUEST("用户请求"),
        
        /**
         * 系统关闭
         */
        SYSTEM_SHUTDOWN("系统关闭"),
        
        /**
         * 依赖变更
         */
        DEPENDENCY_CHANGE("依赖变更"),
        
        /**
         * 更新/升级
         */
        UPDATE("更新升级"),
        
        /**
         * 错误
         */
        ERROR("错误"),
        
        /**
         * 资源限制
         */
        RESOURCE_LIMIT("资源限制"),
        
        /**
         * 未知原因
         */
        UNKNOWN("未知原因");
        
        private final String description;
        
        StopReason(String description) {
            this.description = description;
        }
        
        /**
         * 获取描述
         *
         * @return 描述
         */
        public String getDescription() {
            return description;
        }
    }
} 