/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:35:54
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 13:51:19
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/sdk/plugin/api/PluginVersionManager.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.api.core.plugin.api;

import java.util.List;

/**
 * 插件版本管理接口
 * 提供插件版本管理功能
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface PluginVersionManager {

    /**
     * 检查插件版本是否最新
     *
     * @param pluginId 插件ID
     * @param version 当前版本
     * @return 是否是最新版本
     */
    boolean isLatestVersion(String pluginId, String version);

    /**
     * 记录插件版本
     *
     * @param pluginInfo 插件信息
     */
    void recordVersion(PluginInfo pluginInfo);

    /**
     * 获取最新版本
     *
     * @param pluginId 插件ID
     * @return 最新版本号
     */
    String getLatestVersion(String pluginId);
    
    /**
     * 获取从一个版本到另一个版本的升级路径
     *
     * @param pluginId 插件ID
     * @param fromVersion 起始版本
     * @param toVersion 目标版本
     * @return 升级路径
     */
    List<String> getUpgradePath(String pluginId, String fromVersion, String toVersion);
    
    /**
     * 获取所有可用的版本列表
     *
     * @return 所有可用版本列表
     */
    List<String> getAllAvailableVersions();
} 