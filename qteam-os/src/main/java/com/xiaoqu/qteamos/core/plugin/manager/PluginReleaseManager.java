/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * 插件发布管理器
 * 负责管理插件的发布状态，支持灰度发布和正式发布
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.manager;

/**
 * 插件发布管理器接口
 * 负责管理插件的发布状态，支持灰度发布策略
 */
public interface PluginReleaseManager {
    
    /**
     * 发布状态枚举
     */
    public enum ReleaseStatus {
        CREATED,       // 新创建
        GRAY_TESTING,  // 灰度测试中
        CONFIRMED,     // 已确认为正式版本
        REJECTED,      // 已拒绝并回滚
        DEPRECATED     // 已弃用
    }
    
    /**
     * 设置插件发布状态
     * 
     * @param pluginId 插件ID
     * @param version 版本号
     * @param status 发布状态
     */
    void setReleaseStatus(String pluginId, String version, ReleaseStatus status);
    
    /**
     * 获取插件发布状态
     * 
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 发布状态
     */
    ReleaseStatus getReleaseStatus(String pluginId, String version);
    
    /**
     * 获取插件的最新正式版本
     * 
     * @param pluginId 插件ID
     * @return 正式版本号，如果没有则返回null
     */
    String getStableVersion(String pluginId);
    
    /**
     * 获取插件的上一个稳定版本
     * 
     * @param pluginId 插件ID
     * @return 上一个正式版本号，如果没有则返回null
     */
    String getLastStableVersion(String pluginId);
    
    /**
     * 确认灰度发布为正式版本
     * 
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 确认是否成功
     */
    boolean confirmRelease(String pluginId, String version);
    
    /**
     * 拒绝灰度发布，回滚到上一个稳定版本
     * 
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 拒绝是否成功
     */
    boolean rejectRelease(String pluginId, String version);
    
    /**
     * 将插件版本设置为已弃用
     * 
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 操作是否成功
     */
    boolean deprecateVersion(String pluginId, String version);
} 