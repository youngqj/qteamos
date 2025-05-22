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
 * 插件文件修改事件
 * 当插件目录中检测到文件修改时触发
 *
 * @author yangqijun
 * @date 2025-05-27
 * @since 1.0.0
 */
public class PluginFileModifiedEvent extends PluginEvent {
    
    /**
     * 文件修改事件类型
     */
    public static final String TYPE_FILE_MODIFIED = "file_modified";
    
    private final File file;
    
    public PluginFileModifiedEvent(File file) {
        super(TYPE_FILE_MODIFIED, "unknown", null, file);
        this.file = file;
    }
    
    /**
     * 获取被修改的文件
     *
     * @return 修改的文件对象
     */
    public File getFile() {
        return file;
    }
} 