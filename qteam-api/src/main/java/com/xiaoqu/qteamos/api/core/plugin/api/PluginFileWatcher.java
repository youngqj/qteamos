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

package com.xiaoqu.qteamos.api.core.plugin.api;

import java.io.File;
import java.nio.file.Path;

/**
 * 插件文件监控接口
 * 负责监控插件目录的文件变化
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
public interface PluginFileWatcher {
    
    /**
     * 开始监控指定目录
     *
     * @param directory 要监控的目录
     */
    void startWatching(Path directory);
    
    /**
     * 停止目录监控
     */
    void stopWatching();
    
    /**
     * 将目录添加到监控列表
     *
     * @param directory 要添加的目录
     * @return 是否添加成功
     */
    boolean addWatchDirectory(Path directory);
    
    /**
     * 从监控列表移除目录
     *
     * @param directory 要移除的目录
     * @return 是否移除成功
     */
    boolean removeWatchDirectory(Path directory);
    
    /**
     * 处理新文件事件
     *
     * @param file 新增的文件
     */
    void handleFileCreated(File file);
    
    /**
     * 处理文件修改事件
     *
     * @param file 修改的文件
     */
    void handleFileModified(File file);
    
    /**
     * 处理文件删除事件
     *
     * @param path 删除的文件路径
     */
    void handleFileDeleted(Path path);
    
    /**
     * 检查文件是否被监控
     *
     * @param filePath 文件路径
     * @return 是否被监控
     */
    boolean isWatched(Path filePath);
} 