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

import com.xiaoqu.qteamos.api.core.plugin.model.PluginCandidate;

import java.nio.file.Path;
import java.util.List;

/**
 * 插件扫描器接口
 * 负责扫描指定目录，发现可能的插件文件或目录
 *
 * @author yangqijun
 * @date 2025-05-25
 * @since 1.0.0
 */
public interface PluginScanner {
    
    /**
     * 扫描指定目录，查找插件候选者
     *
     * @param directory 要扫描的目录
     * @return 发现的插件候选者列表
     */
    List<PluginCandidate> scanPlugins(Path directory);
    
    /**
     * 启动定期扫描
     * 根据配置的时间间隔，定期扫描插件目录
     */
    void startScheduledScanning();
    
    /**
     * 停止定期扫描
     */
    void stopScheduledScanning();
    
    /**
     * 执行一次扫描
     * 扫描配置的插件目录，查找新插件
     * 
     * @return 发现的新插件候选者数量
     */
    int scanOnce();
} 