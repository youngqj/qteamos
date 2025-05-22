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
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 系统启动事件
 * 在系统启动过程中发布，用于通知插件系统已启动
 *
 * @author yangqijun
 * @date 2024-07-15
 * @since 1.0.0
 */
public class SystemStartupEvent extends Event {
    private final SystemInfo systemInfo;
    
    /**
     * 默认构造函数
     */
    public SystemStartupEvent() {
        super("system", "startup");
        this.systemInfo = createSystemInfo();
    }
    
    /**
     * 获取系统信息
     *
     * @return 系统信息
     */
    public SystemInfo getSystemInfo() {
        return systemInfo;
    }
    
    /**
     * 创建系统信息
     *
     * @return 系统信息
     */
    private SystemInfo createSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        return new SystemInfo(
                System.getProperty("os.name", "Unknown"),
                System.getProperty("os.version", "Unknown"),
                System.getProperty("java.version", "Unknown"),
                "1.0.0", // 系统版本，可根据实际情况调整
                runtime.maxMemory(),
                runtime.availableProcessors()
        );
    }
    
    /**
     * 系统信息类
     */
    @Data
    @AllArgsConstructor
    public static class SystemInfo {
        private String osName;
        private String osVersion;
        private String javaVersion;
        private String version;
        private long maxMemory;
        private int processorCount;
    }
} 