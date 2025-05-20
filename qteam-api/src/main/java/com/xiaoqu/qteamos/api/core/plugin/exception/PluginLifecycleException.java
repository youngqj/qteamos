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

package com.xiaoqu.qteamos.api.core.plugin.exception;

/**
 * 插件生命周期异常
 * 在插件的加载、初始化、启动、停止和卸载过程中可能抛出的异常
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
public class PluginLifecycleException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 异常阶段
     */
    private final LifecyclePhase phase;
    
    /**
     * 插件ID
     */
    private final String pluginId;
    
    /**
     * 创建一个插件生命周期异常
     *
     * @param message 异常信息
     */
    public PluginLifecycleException(String message) {
        super(message);
        this.phase = LifecyclePhase.UNKNOWN;
        this.pluginId = null;
    }
    
    /**
     * 创建一个插件生命周期异常
     *
     * @param message 异常信息
     * @param cause 原因
     */
    public PluginLifecycleException(String message, Throwable cause) {
        super(message, cause);
        this.phase = LifecyclePhase.UNKNOWN;
        this.pluginId = null;
    }
    
    /**
     * 创建一个插件生命周期异常
     *
     * @param message 异常信息
     * @param pluginId 插件ID
     */
    public PluginLifecycleException(String message, String pluginId) {
        super(message);
        this.phase = LifecyclePhase.UNKNOWN;
        this.pluginId = pluginId;
    }
    
    /**
     * 创建一个插件生命周期异常
     *
     * @param message 异常信息
     * @param pluginId 插件ID
     * @param cause 原因
     */
    public PluginLifecycleException(String message, String pluginId, Throwable cause) {
        super(message, cause);
        this.phase = LifecyclePhase.UNKNOWN;
        this.pluginId = pluginId;
    }
    
    /**
     * 创建一个插件生命周期异常
     *
     * @param message 异常信息
     * @param phase 生命周期阶段
     * @param pluginId 插件ID
     */
    public PluginLifecycleException(String message, LifecyclePhase phase, String pluginId) {
        super(message);
        this.phase = phase;
        this.pluginId = pluginId;
    }
    
    /**
     * 创建一个插件生命周期异常
     *
     * @param message 异常信息
     * @param phase 生命周期阶段
     * @param pluginId 插件ID
     * @param cause 原因
     */
    public PluginLifecycleException(String message, LifecyclePhase phase, String pluginId, Throwable cause) {
        super(message, cause);
        this.phase = phase;
        this.pluginId = pluginId;
    }
    
    /**
     * 获取异常阶段
     *
     * @return 异常阶段
     */
    public LifecyclePhase getPhase() {
        return phase;
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
     * 插件生命周期阶段
     */
    public enum LifecyclePhase {
        /** 加载 */
        LOAD,
        /** 初始化 */
        INITIALIZE,
        /** 启动 */
        START,
        /** 停止 */
        STOP,
        /** 卸载 */
        UNLOAD,
        /** 安装 */
        INSTALL,
        /** 更新 */
        UPDATE,
        /** 未知 */
        UNKNOWN
    }
} 