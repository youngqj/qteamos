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
 * 插件发布管理器实现类
 * 负责管理插件的发布状态，支持灰度发布和正式发布
 *
 * @author yangqijun
 * @date 2025-07-01
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginVersion;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginVersionMapper;
import com.xiaoqu.qteamos.core.plugin.service.PluginDeploymentHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PluginReleaseManagerImpl implements PluginReleaseManager {
    
    private static final Logger log = LoggerFactory.getLogger(PluginReleaseManagerImpl.class);
    
    // 发布状态变更事件类型
    private static final String TYPE_STATUS_CHANGE = "release.status.change";
    
    /**
     * 插件版本发布状态缓存
     * Key: pluginId:version, Value: 发布状态
     */
    private final Map<String, ReleaseStatus> releaseStatusCache = new ConcurrentHashMap<>();
    
    /**
     * 插件最新稳定版本缓存
     * Key: pluginId, Value: 稳定版本号
     */
    private final Map<String, String> stableVersionCache = new ConcurrentHashMap<>();
    
    @Autowired
    private SysPluginVersionMapper versionMapper;
    
    @Autowired
    private PluginDeploymentHistoryService deploymentHistoryService;
    
    @Autowired
    private EventBus eventBus;
    
    @Autowired
    private PluginRolloutManager rolloutManager;
    
    @Override
    public void setReleaseStatus(String pluginId, String version, ReleaseStatus status) {
        log.info("设置插件[{}]版本[{}]的发布状态为: {}", pluginId, version, status);
        String cacheKey = buildCacheKey(pluginId, version);
        releaseStatusCache.put(cacheKey, status);
        
        // 更新数据库记录
        updateVersionStatus(pluginId, version, status);
        
        // 如果设置为确认状态，更新最新稳定版本缓存
        if (status == ReleaseStatus.CONFIRMED) {
            stableVersionCache.put(pluginId, version);
        }
        
        // 记录部署历史
        deploymentHistoryService.recordStatusChange(pluginId, version, status);
        
        // 发布事件
        publishReleaseStatusChangeEvent(pluginId, version, status);
    }
    
    @Override
    public ReleaseStatus getReleaseStatus(String pluginId, String version) {
        String cacheKey = buildCacheKey(pluginId, version);
        
        // 先从缓存获取
        ReleaseStatus cachedStatus = releaseStatusCache.get(cacheKey);
        if (cachedStatus != null) {
            return cachedStatus;
        }
        
        // 缓存未命中，从数据库获取
        SysPluginVersion versionEntity = getPluginVersion(pluginId, version);
        if (versionEntity == null) {
            // 如果记录不存在，默认为新创建状态
            return ReleaseStatus.CREATED;
        }
        
        // 解析版本状态 
        ReleaseStatus status = parseReleaseStatus(versionEntity);
        
        // 更新缓存
        releaseStatusCache.put(cacheKey, status);
        
        return status;
    }
    
    @Override
    public String getStableVersion(String pluginId) {
        // 先从缓存获取
        String cachedVersion = stableVersionCache.get(pluginId);
        if (cachedVersion != null) {
            return cachedVersion;
        }
        
        // 缓存未命中，从数据库获取最新已确认的版本
        LambdaQueryWrapper<SysPluginVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginVersion::getPluginId, pluginId)
                    .eq(SysPluginVersion::getDeployed, true)
                    .ne(SysPluginVersion::getChangeType, "deprecated")
                    .orderByDesc(SysPluginVersion::getDeployTime)
                    .last("LIMIT 1");
        
        SysPluginVersion version = versionMapper.selectOne(queryWrapper);
        if (version == null) {
            return null;
        }
        
        // 更新缓存
        stableVersionCache.put(pluginId, version.getVersion());
        
        return version.getVersion();
    }
    
    @Override
    public String getLastStableVersion(String pluginId) {
        // 获取当前稳定版本
        String currentStable = getStableVersion(pluginId);
        if (currentStable == null) {
            return null;
        }
        
        // 查询前一个稳定版本
        SysPluginVersion currentVersion = getPluginVersion(pluginId, currentStable);
        if (currentVersion == null || currentVersion.getPreviousVersion() == null) {
            return null;
        }
        
        // 获取前一个版本
        String previousVersion = currentVersion.getPreviousVersion();
        
        // 验证前一个版本是否为稳定版本
        ReleaseStatus previousStatus = getReleaseStatus(pluginId, previousVersion);
        if (previousStatus == ReleaseStatus.CONFIRMED) {
            return previousVersion;
        }
        
        return null;
    }
    
    @Override
    @Transactional
    public boolean confirmRelease(String pluginId, String version) {
        log.info("确认插件[{}]版本[{}]为正式版本", pluginId, version);
        
        // 检查当前状态
        ReleaseStatus currentStatus = getReleaseStatus(pluginId, version);
        if (currentStatus != ReleaseStatus.GRAY_TESTING) {
            log.warn("只有灰度测试中的版本才能被确认为正式版本，当前状态: {}", currentStatus);
            return false;
        }
        
        // 检查灰度发布进度
        if (!isRolloutCompleted(pluginId, version)) {
            log.warn("灰度发布未完成，不能确认为正式版本");
            return false;
        }
        
        // 更新版本状态为已确认
        setReleaseStatus(pluginId, version, ReleaseStatus.CONFIRMED);
        
        // 记录部署历史
        deploymentHistoryService.recordConfirmation(pluginId, version);
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean rejectRelease(String pluginId, String version) {
        log.info("拒绝插件[{}]版本[{}]的灰度发布", pluginId, version);
        
        // 检查当前状态
        ReleaseStatus currentStatus = getReleaseStatus(pluginId, version);
        if (currentStatus != ReleaseStatus.GRAY_TESTING) {
            log.warn("只有灰度测试中的版本才能被拒绝，当前状态: {}", currentStatus);
            return false;
        }
        
        // 执行版本回滚
        String lastStableVersion = getLastStableVersion(pluginId);
        if (lastStableVersion == null) {
            log.warn("没有可回滚的稳定版本");
            return false;
        }
        
        boolean rollbackSuccess = rolloutManager.rollbackToVersion(pluginId, lastStableVersion);
        if (!rollbackSuccess) {
            log.error("回滚到版本[{}]失败", lastStableVersion);
            return false;
        }
        
        // 更新版本状态为已拒绝
        setReleaseStatus(pluginId, version, ReleaseStatus.REJECTED);
        
        // 记录部署历史
        deploymentHistoryService.recordRejection(pluginId, version, lastStableVersion);
        
        return true;
    }
    
    @Override
    public boolean deprecateVersion(String pluginId, String version) {
        log.info("将插件[{}]版本[{}]设置为已弃用", pluginId, version);
        
        // 只有已确认的版本才能被设置为已弃用
        ReleaseStatus currentStatus = getReleaseStatus(pluginId, version);
        if (currentStatus != ReleaseStatus.CONFIRMED) {
            log.warn("只有正式版本才能被设为已弃用，当前状态: {}", currentStatus);
            return false;
        }
        
        // 更新版本状态为已弃用
        setReleaseStatus(pluginId, version, ReleaseStatus.DEPRECATED);
        
        // 记录部署历史
        deploymentHistoryService.recordDeprecation(pluginId, version);
        
        return true;
    }
    
    /**
     * 更新版本状态到数据库
     */
    private void updateVersionStatus(String pluginId, String version, ReleaseStatus status) {
        SysPluginVersion versionEntity = getPluginVersion(pluginId, version);
        if (versionEntity == null) {
            // 如果记录不存在，创建新记录
            versionEntity = new SysPluginVersion()
                    .setPluginId(pluginId)
                    .setVersion(version)
                    .setRecordTime(LocalDateTime.now());
        }
        
        // 根据状态设置对应字段
        switch (status) {
            case GRAY_TESTING:
                // 不改变deployed状态，灰度发布中
                versionEntity.setChangeType("beta");
                break;
            case CONFIRMED:
                versionEntity.setDeployed(true);
                versionEntity.setDeployTime(LocalDateTime.now());
                // 根据版本号自动判断变更类型
                if (version.endsWith("-SNAPSHOT") || version.contains("alpha")) {
                    versionEntity.setChangeType("alpha");
                } else if (version.contains("beta")) {
                    versionEntity.setChangeType("beta");
                } else {
                    versionEntity.setChangeType("patch"); // 默认为补丁版本
                }
                break;
            case REJECTED:
                versionEntity.setDeployed(false);
                versionEntity.setChangeType("rejected");
                break;
            case DEPRECATED:
                // 保持deployed=true，但标记为已弃用
                versionEntity.setChangeType("deprecated");
                break;
            default:
                versionEntity.setDeployed(false);
                versionEntity.setChangeType("created");
                break;
        }
        
        // 保存到数据库
        if (versionEntity.getId() == null) {
            versionMapper.insert(versionEntity);
        } else {
            versionMapper.updateById(versionEntity);
        }
    }
    
    /**
     * 从数据库获取插件版本信息
     */
    private SysPluginVersion getPluginVersion(String pluginId, String version) {
        LambdaQueryWrapper<SysPluginVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginVersion::getPluginId, pluginId)
                    .eq(SysPluginVersion::getVersion, version);
        return versionMapper.selectOne(queryWrapper);
    }
    
    /**
     * 解析数据库中的版本记录为发布状态
     */
    private ReleaseStatus parseReleaseStatus(SysPluginVersion versionEntity) {
        if (versionEntity.getDeployed() != null && versionEntity.getDeployed()) {
            // 已部署的版本
            if ("deprecated".equals(versionEntity.getChangeType())) {
                return ReleaseStatus.DEPRECATED;
            } else {
                return ReleaseStatus.CONFIRMED;
            }
        } else {
            // 未部署的版本
            if ("rejected".equals(versionEntity.getChangeType())) {
                return ReleaseStatus.REJECTED;
            } else if ("beta".equals(versionEntity.getChangeType())) {
                return ReleaseStatus.GRAY_TESTING;
            } else {
                return ReleaseStatus.CREATED;
            }
        }
    }
    
    /**
     * 检查灰度发布是否已完成
     */
    private boolean isRolloutCompleted(String pluginId, String version) {
        return rolloutManager.getRolloutStatus(pluginId)
                .map(status -> status.getTargetVersion().equals(version) && 
                              status.getState() == PluginRolloutManager.RolloutState.COMPLETED)
                .orElse(false);
    }
    
    /**
     * 生成缓存key
     */
    private String buildCacheKey(String pluginId, String version) {
        return pluginId + ":" + version;
    }
    
    /**
     * 发布状态变更事件
     */
    private void publishReleaseStatusChangeEvent(String pluginId, String version, ReleaseStatus status) {
        // 创建事件数据
        Map<String, Object> data = new HashMap<>();
        data.put("status", status.name());
        data.put("timestamp", System.currentTimeMillis());
        
        // 创建插件事件
        PluginEvent event = new PluginEvent(TYPE_STATUS_CHANGE, pluginId, version, data);
        
        // 发布事件
        eventBus.postEvent(event);
    }
} 