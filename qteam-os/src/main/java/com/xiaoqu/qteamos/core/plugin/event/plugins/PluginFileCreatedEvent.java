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

import java.io.File;

import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;

/**
 * 插件文件创建事件
 * 当插件目录中检测到新文件创建时触发
 *
 * @author yangqijun
 * @date 2025-05-27
 * @since 1.0.0
 */
public class PluginFileCreatedEvent extends PluginEvent {
    
    /**
     * 文件创建事件类型
     */
    public static final String TYPE_FILE_CREATED = "file_created";
    
    private final File file;
    
    public PluginFileCreatedEvent(File file) {
        super(TYPE_FILE_CREATED, "unknown", null, file);
        this.file = file;
    }
    
    /**
     * 获取被创建的文件
     *
     * @return 创建的文件对象
     */
    public File getFile() {
        return file;
    }
} 