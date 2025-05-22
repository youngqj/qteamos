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

package com.xiaoqu.qteamos.core.plugin.event.file;

import java.nio.file.Path;
import com.xiaoqu.qteamos.core.plugin.event.Event;

/**
 * 插件文件删除事件
 * 当插件目录中的文件被删除时触发此事件
 *
 * @author yangqijun
 * @date 2024-07-15
 * @since 1.0.0
 */
public class PluginFileDeletedEvent extends Event {
    private final Path path;
    
    public PluginFileDeletedEvent(Path path) {
        super("file", "deleted");
        this.path = path;
    }
    
    public Path getPath() {
        return path;
    }
    
    @Override
    public String toString() {
        return "PluginFileDeletedEvent{path=" + path + '}';
    }
} 