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

package com.xiaoqu.qteamos.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 插件配置类
 * 负责提供插件系统相关的配置参数
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
@Component
public class PluginConfig {
    
    /**
     * 插件根目录
     */
    @Value("${plugin.dir:plugins}")
    private String pluginDir;
    
    /**
     * 插件数据根目录
     */
    @Value("${plugin.data.dir:plugin-data}")
    private String pluginDataDir;
    
    /**
     * 插件备份根目录
     */
    @Value("${plugin.backup.dir:plugin-backup}")
    private String pluginBackupDir;
    
    /**
     * 插件扫描间隔（毫秒）
     */
    @Value("${plugin.scan.interval:30000}")
    private long scanInterval;
    
    /**
     * 插件自动加载
     */
    @Value("${plugin.auto.load:true}")
    private boolean autoLoad;
    
    /**
     * 插件自动启动
     */
    @Value("${plugin.auto.start:true}")
    private boolean autoStart;
    
    /**
     * 插件版本保留数量
     */
    @Value("${plugin.version.keep:3}")
    private int versionKeep;
    
    /**
     * 获取插件根目录
     *
     * @return 插件根目录
     */
    public String getPluginDir() {
        return pluginDir;
    }
    
    /**
     * 获取插件数据根目录
     *
     * @return 插件数据根目录
     */
    public String getPluginDataDir() {
        return pluginDataDir;
    }
    
    /**
     * 获取插件备份根目录
     *
     * @return 插件备份根目录
     */
    public String getPluginBackupDir() {
        return pluginBackupDir;
    }
    
    /**
     * 获取插件扫描间隔
     *
     * @return 插件扫描间隔（毫秒）
     */
    public long getScanInterval() {
        return scanInterval;
    }
    
    /**
     * 是否自动加载插件
     *
     * @return 是否自动加载
     */
    public boolean isAutoLoad() {
        return autoLoad;
    }
    
    /**
     * 是否自动启动插件
     *
     * @return 是否自动启动
     */
    public boolean isAutoStart() {
        return autoStart;
    }
    
    /**
     * 获取插件版本保留数量
     *
     * @return 版本保留数量
     */
    public int getVersionKeep() {
        return versionKeep;
    }
} 