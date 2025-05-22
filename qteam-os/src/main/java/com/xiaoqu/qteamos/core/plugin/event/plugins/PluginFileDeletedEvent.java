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

import java.nio.file.Path;

import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;

/**
 * 插件文件删除事件
 * 当插件目录中检测到文件删除时触发
 *
 * @author yangqijun
 * @date 2025-05-27
 * @since 1.0.0
 */
public class PluginFileDeletedEvent extends PluginEvent {
    
    /**
     * 文件删除事件类型
     */
    public static final String TYPE_FILE_DELETED = "file_deleted";
    
    private final Path path;
    
    public PluginFileDeletedEvent(Path path) {
        super(TYPE_FILE_DELETED, "unknown", null, path.toString());
        this.path = path;
    }
    
    /**
     * 获取被删除的文件路径
     *
     * @return 删除的文件路径
     */
    public Path getPath() {
        return path;
    }
} 