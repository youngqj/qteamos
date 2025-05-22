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

package com.xiaoqu.qteamos.core.plugin.event.plugins;

import com.xiaoqu.qteamos.core.plugin.event.Event;

/**
 * 系统关闭事件
 * 在系统关闭过程中发布，用于通知插件系统将要关闭
 *
 * @author yangqijun
 * @date 2024-07-15
 * @since 1.0.0
 */
public class SystemShutdownEvent extends Event {
    /**
     * 关闭原因枚举
     */
    public enum ShutdownReason {
        NORMAL("正常关闭"),
        ERROR("错误导致关闭"),
        RESTART("重启"),
        USER_REQUEST("用户请求关闭"),
        UNKNOWN("未知原因");
        
        private final String description;
        
        ShutdownReason(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ShutdownReason reason;
    
    /**
     * 创建系统关闭事件
     *
     * @param reason 关闭原因
     */
    public SystemShutdownEvent(ShutdownReason reason) {
        super("system", "shutdown");
        this.reason = reason;
    }
    
    /**
     * 获取关闭原因
     *
     * @return 关闭原因
     */
    public ShutdownReason getReason() {
        return reason;
    }
} 