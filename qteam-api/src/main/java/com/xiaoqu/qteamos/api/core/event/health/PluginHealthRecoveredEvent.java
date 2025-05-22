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
 * 插件健康恢复事件
 * 用于通知插件从不健康状态恢复到健康状态
 *
 * @author yangqijun
 * @date 2024-08-20
 * @since 1.0.0
 */
public class PluginHealthRecoveredEvent extends PluginEvent {
    
    private final String message;
    private final long recoveryDuration;
    
    /**
     * 构造函数
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param message 恢复描述消息
     * @param recoveryDuration 恢复耗时（毫秒）
     */
    public PluginHealthRecoveredEvent(String pluginId, String version, String message, long recoveryDuration) {
        super(PluginEventTypes.Topics.PLUGIN,
              PluginEventTypes.Health.HEALTH_RECOVERED,
              "system",
              pluginId,
              version,
              null,
              false);
        
        this.message = message;
        this.recoveryDuration = recoveryDuration;
    }
    
    /**
     * 获取恢复描述消息
     *
     * @return 恢复描述消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取恢复耗时（毫秒）
     *
     * @return 恢复耗时
     */
    public long getRecoveryDuration() {
        return recoveryDuration;
    }
} 