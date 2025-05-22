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

package com.xiaoqu.qteamos.api.core.event.health;

import com.xiaoqu.qteamos.api.core.event.PluginEvent;
import com.xiaoqu.qteamos.api.core.event.PluginEventTypes;

/**
 * 插件恢复尝试事件
 * 用于通知系统正在尝试恢复插件
 *
 * @author yangqijun
 * @date 2024-08-20
 * @since 1.0.0
 */
public class PluginRecoveryAttemptEvent extends PluginEvent {
    
    private final String message;
    private final int attemptCount;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param message 恢复尝试描述消息
     * @param attemptCount 尝试次数
     */
    public PluginRecoveryAttemptEvent(String pluginId, String version, String message, int attemptCount) {
        super(PluginEventTypes.Topics.PLUGIN,
              PluginEventTypes.Health.RECOVERY_ATTEMPT,
              "system",
              pluginId,
              version,
              null,
              false);
        
        this.message = message;
        this.attemptCount = attemptCount;
    }
    
    /**
     * 获取恢复尝试描述消息
     *
     * @return 恢复尝试描述消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取尝试次数
     *
     * @return 尝试次数
     */
    public int getAttemptCount() {
        return attemptCount;
    }
} 