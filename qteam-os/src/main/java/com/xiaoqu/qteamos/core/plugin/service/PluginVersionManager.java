/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:36:18
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-30 21:38:16
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/service/PluginVersionManager.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.service;

import com.xiaoqu.qteamos.common.utils.VersionUtils;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件版本管理器实现
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Component("pluginVersionManager")
public class PluginVersionManager implements com.xiaoqu.qteamos.api.core.plugin.api.PluginVersionManager {
    
    private static final Logger log = LoggerFactory.getLogger(PluginVersionManager.class);
    
    /**
     * 插件最新版本记录，key=pluginId, value=version
     */
    private final Map<String, String> latestVersions = new ConcurrentHashMap<>();

    @Override
    public boolean isLatestVersion(String pluginId, String version) {
        String latestVersion = latestVersions.get(pluginId);
        if (latestVersion == null) {
            return true; // 没有记录，认为是最新版本
        }
        return VersionUtils.compare(version, latestVersion) >= 0;
    }

    @Override
    public void recordVersion(com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo pluginInfo) {
        String pluginId = pluginInfo.getPluginId();
        String version = pluginInfo.getVersion();
        
        String currentLatest = latestVersions.get(pluginId);
        if (currentLatest == null || VersionUtils.compare(version, currentLatest) > 0) {
            latestVersions.put(pluginId, version);
            log.info("记录API插件最新版本: {}, 版本: {}", pluginId, version);
        }
    }
    
    /**
     * 记录核心插件版本
     *
     * @param pluginInfo 核心插件信息
     */
    public void recordVersion(PluginInfo pluginInfo) {
        String pluginId = pluginInfo.getPluginId();
        String version = pluginInfo.getVersion();
        
        String currentLatest = latestVersions.get(pluginId);
        if (currentLatest == null || VersionUtils.compare(version, currentLatest) > 0) {
            latestVersions.put(pluginId, version);
            log.info("记录核心插件最新版本: {}, 版本: {}", pluginId, version);
        }
    }

    @Override
    public String getLatestVersion(String pluginId) {
        return latestVersions.getOrDefault(pluginId, "未知");
    }

    @Override
    public List<String> getUpgradePath(String pluginId, String fromVersion, String toVersion) {
        List<String> path = new ArrayList<>();
        
        // 如果是降级，直接返回目标版本
        if (VersionUtils.compare(fromVersion, toVersion) > 0) {
            path.add(toVersion);
            return path;
        }
        
        // 从起始版本开始，添加所有中间版本直到目标版本
        path.add(fromVersion);
        
        // 简单实现：直接添加目标版本
        // 在实际系统中，这里应该查询数据库获取真实的升级路径
        path.add(toVersion);
        
        return path;
    }
    
    @Override
    public List<String> getAllAvailableVersions() {
        // 这里实现一个简单版本，返回一些常见的版本
        // 在实际系统中，这里应该从数据库或其他存储中获取所有可用版本
        return Arrays.asList("1.0.0", "1.5.0", "2.0.0", "2.1.0", "3.0.0");
    }
} 