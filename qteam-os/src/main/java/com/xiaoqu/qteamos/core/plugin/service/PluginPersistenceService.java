package com.xiaoqu.qteamos.core.plugin.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginInfo;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStatus;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginVersion;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginInfoMapper;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginStatusMapper;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginVersionMapper;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRolloutManager.RolloutStatus;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginRolloutStatus;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginUpdateHistory;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginRolloutStatusMapper;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginUpdateHistoryMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginConfig;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginConfigMapper;
import com.xiaoqu.qteamos.core.config.PluginConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 插件持久化服务
 * 负责将插件的状态和版本信息持久化到数据库
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Service
public class PluginPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(PluginPersistenceService.class);

    @Autowired
    private SysPluginInfoService pluginInfoService;

    @Autowired
    private SysPluginStatusService pluginStatusService;

    @Autowired
    private SysPluginVersionMapper pluginVersionMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysPluginUpdateHistoryMapper updateHistoryMapper;
    
    @Autowired
    private SysPluginRolloutStatusMapper rolloutStatusMapper;
    
    @Autowired
    private SysPluginConfigMapper configMapper;
    
    @Autowired
    private PluginConfig pluginConfig;

    /**
     * 保存插件基本信息
     *
     * @param pluginInfo 插件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void savePluginInfo(PluginInfo pluginInfo) {
        pluginInfoService.savePluginInfo(pluginInfo);
    }

    /**
     * 更新插件状态
     *
     * @param pluginInfo 插件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePluginStatus(PluginInfo pluginInfo) {
        pluginStatusService.updatePluginStatus(pluginInfo);
    }

    /**
     * 恢复插件状态
     * 系统重启后调用此方法恢复插件状态
     *
     * @return 需要启动的插件列表
     */
    public List<String> restorePluginStatus() {
        return pluginStatusService.restorePluginStatus();
    }
    
    /**
     * 保存插件版本信息
     *
     * @param pluginInfo 插件信息
     * @param versionManager 版本管理器
     */
    @Transactional(rollbackFor = Exception.class)
    public void savePluginVersion(PluginInfo pluginInfo, PluginVersionManager versionManager) {
        PluginDescriptor descriptor = pluginInfo.getDescriptor();
        String pluginId = descriptor.getPluginId();
        String version = descriptor.getVersion();
        String previousVersion = descriptor.getPreviousVersion();
        
        // 检查版本记录是否存在
        SysPluginVersion existingVersion = pluginVersionMapper.selectOne(
                new LambdaQueryWrapper<SysPluginVersion>()
                        .eq(SysPluginVersion::getPluginId, pluginId)
                        .eq(SysPluginVersion::getVersion, version));
        
        if (existingVersion != null) {
            log.info("插件版本记录已存在: {} {}", pluginId, version);
            return;
        }
        
        // 创建新的版本记录
        SysPluginVersion dbVersion = new SysPluginVersion();
        dbVersion.setPluginId(pluginId)
                .setVersion(version)
                .setPreviousVersion(previousVersion)
                .setDeployed(true)
                .setRecordTime(LocalDateTime.now())
                .setDeployTime(LocalDateTime.now());
        
        // 获取升级路径
        if (previousVersion != null) {
            try {
                List<String> upgradePath = versionManager.getUpgradePath(pluginId, previousVersion, version);
                dbVersion.setUpgradePath(objectMapper.writeValueAsString(upgradePath));
            } catch (Exception e) {
                log.error("保存升级路径失败: {}", e.getMessage());
            }
        }
        
        // 获取版本变更类型
        dbVersion.setChangeType(getVersionChangeType(version, previousVersion));
        
        // 保存版本记录
        pluginVersionMapper.insert(dbVersion);
        log.info("保存插件版本信息: {} {}", pluginId, version);
    }
    
    /**
     * 保存插件版本信息(直接保存SysPluginVersion实体)
     *
     * @param versionRecord 版本记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void savePluginVersion(SysPluginVersion versionRecord) {
        try {
            // 检查版本记录是否存在
            SysPluginVersion existingVersion = pluginVersionMapper.selectOne(
                    new LambdaQueryWrapper<SysPluginVersion>()
                            .eq(SysPluginVersion::getPluginId, versionRecord.getPluginId())
                            .eq(SysPluginVersion::getVersion, versionRecord.getVersion()));
            
            if (existingVersion != null) {
                // 更新现有记录
                versionRecord.setId(existingVersion.getId());
                pluginVersionMapper.updateById(versionRecord);
                log.info("更新插件版本记录: {} {}", versionRecord.getPluginId(), versionRecord.getVersion());
            } else {
                // 插入新记录
                pluginVersionMapper.insert(versionRecord);
                log.info("新增插件版本记录: {} {}", versionRecord.getPluginId(), versionRecord.getVersion());
            }
        } catch (Exception e) {
            log.error("保存插件版本记录失败", e);
            throw new RuntimeException("保存插件版本记录失败", e);
        }
    }
    
    /**
     * 获取所有已安装的插件信息
     *
     * @return 插件信息列表
     */
    public List<SysPluginInfo> getAllInstalledPlugins() {
        return pluginInfoService.getAllInstalledPlugins();
    }
    
    /**
     * 获取插件的当前状态
     *
     * @param pluginId 插件ID
     * @return 插件状态
     */
    public SysPluginStatus getPluginStatus(String pluginId) {
        return pluginStatusService.getPluginStatus(pluginId);
    }
    
    /**
     * 获取插件的所有版本历史
     *
     * @return 所有插件的版本历史列表
     */
    public List<SysPluginVersion> getPluginVersionHistory() {
        return pluginVersionMapper.selectList(null);
    }
    
    /**
     * 获取指定插件的所有版本历史
     *
     * @param pluginId 插件ID
     * @return 指定插件的版本历史列表
     */
    public List<SysPluginVersion> getPluginVersionHistory(String pluginId) {
        return pluginVersionMapper.selectList(
                new LambdaQueryWrapper<SysPluginVersion>()
                        .eq(SysPluginVersion::getPluginId, pluginId)
                        .orderByDesc(SysPluginVersion::getRecordTime));
    }
    
    /**
     * 获取指定插件的指定版本
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 版本信息
     */
    public Optional<SysPluginVersion> getPluginVersion(String pluginId, String version) {
        SysPluginVersion versionRecord = pluginVersionMapper.selectOne(
                new LambdaQueryWrapper<SysPluginVersion>()
                        .eq(SysPluginVersion::getPluginId, pluginId)
                        .eq(SysPluginVersion::getVersion, version));
        return Optional.ofNullable(versionRecord);
    }
    
    /**
     * 删除插件版本记录
     *
     * @param pluginId 插件ID
     * @param version 版本号
     * @return 是否删除成功
     */
    public boolean deletePluginVersion(String pluginId, String version) {
        int result = pluginVersionMapper.delete(
                new LambdaQueryWrapper<SysPluginVersion>()
                        .eq(SysPluginVersion::getPluginId, pluginId)
                        .eq(SysPluginVersion::getVersion, version));
        
        if (result > 0) {
            log.info("删除插件版本记录成功: {} {}", pluginId, version);
            return true;
        } else {
            log.warn("删除插件版本记录失败: {} {}", pluginId, version);
            return false;
        }
    }
    
    /**
     * 将时间戳转换为LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
    
    /**
     * 获取版本变更类型
     */
    private String getVersionChangeType(String version, String previousVersion) {
        if (previousVersion == null) {
            return "major";
        }
        
        String[] current = version.split("\\.");
        String[] previous = previousVersion.split("\\.");
        
        if (current.length > 0 && previous.length > 0) {
            if (!current[0].equals(previous[0])) {
                return "major";
            } else if (current.length > 1 && previous.length > 1 && !current[1].equals(previous[1])) {
                return "minor";
            } else {
                return "patch";
            }
        }
        
        return "patch";
    }
    
    /**
     * 记录插件更新历史
     *
     * @param pluginId 插件ID
     * @param fromVersion 原版本
     * @param toVersion 目标版本
     * @param status 状态
     * @param message 消息
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean recordPluginUpdate(String pluginId, String fromVersion, String toVersion, 
                                    String status, String message) {
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setPreviousVersion(fromVersion);
        history.setTargetVersion(toVersion);
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus(status);
        history.setUpdateLog("插件从版本" + fromVersion + "更新到" + toVersion);
        if (!"SUCCESS".equals(status)) {
            history.setErrorMessage(message);
        }
        
        return updateHistoryMapper.insert(history) > 0;
    }
    
    /**
     * 记录插件回滚历史
     *
     * @param pluginId 插件ID
     * @param fromVersion 回滚前版本
     * @param toVersion 回滚后版本
     * @param reason 回滚原因
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean recordPluginRollback(String pluginId, String fromVersion, String toVersion, String reason) {
        // 创建更新历史记录
        SysPluginUpdateHistory history = new SysPluginUpdateHistory();
        history.setPluginId(pluginId);
        history.setPreviousVersion(fromVersion);
        history.setTargetVersion(toVersion);
        history.setUpdateModifyTime(LocalDateTime.now());
        history.setStatus("ROLLBACK");
        history.setUpdateLog("插件从版本" + fromVersion + "回滚到" + toVersion);
        history.setExecutedBy("系统");
        history.setErrorMessage(reason);
        
        // 更新版本记录
        try {
            // 查询版本记录
            Optional<SysPluginVersion> targetVersionOpt = getPluginVersion(pluginId, toVersion);
            if (targetVersionOpt.isPresent()) {
                SysPluginVersion targetVersion = targetVersionOpt.get();
                targetVersion.setDeployed(true);
                targetVersion.setDeployTime(LocalDateTime.now());
                savePluginVersion(targetVersion);
            }
            
            // 更新之前的版本记录
            Optional<SysPluginVersion> previousVersionOpt = getPluginVersion(pluginId, fromVersion);
            if (previousVersionOpt.isPresent()) {
                SysPluginVersion previousVersion = previousVersionOpt.get();
                previousVersion.setDeployed(false);
                savePluginVersion(previousVersion);
            }
        } catch (Exception e) {
            log.error("更新版本记录失败", e);
        }
        
        return updateHistoryMapper.insert(history) > 0;
    }
    
    /**
     * 保存灰度发布状态
     *
     * @param status 灰度发布状态
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveRolloutStatus(RolloutStatus status) {
        try {
            // 转换为实体对象
            SysPluginRolloutStatus entity = SysPluginRolloutStatus.fromRolloutStatus(status);
            
            // 检查是否已存在
            LambdaQueryWrapper<SysPluginRolloutStatus> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysPluginRolloutStatus::getPluginId, status.getPluginId())
                    .eq(SysPluginRolloutStatus::getCurrentVersion, status.getCurrentVersion())
                    .eq(SysPluginRolloutStatus::getTargetVersion, status.getTargetVersion())
                    .orderByDesc(SysPluginRolloutStatus::getCreateTime)
                    .last("LIMIT 1");
            
            SysPluginRolloutStatus existingStatus = rolloutStatusMapper.selectOne(queryWrapper);
            
            if (existingStatus != null) {
                // 更新现有记录
                entity.setId(existingStatus.getId());
                return rolloutStatusMapper.updateById(entity) > 0;
            } else {
                // 插入新记录
                return rolloutStatusMapper.insert(entity) > 0;
            }
        } catch (Exception e) {
            log.error("保存灰度发布状态失败", e);
            return false;
        }
    }
    
    /**
     * 获取灰度发布状态
     *
     * @param pluginId 插件ID
     * @return 灰度发布状态
     */
    public RolloutStatus getRolloutStatus(String pluginId) {
        try {
            // 查询最新的灰度发布记录
            LambdaQueryWrapper<SysPluginRolloutStatus> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysPluginRolloutStatus::getPluginId, pluginId)
                    .orderByDesc(SysPluginRolloutStatus::getCreateTime)
                    .last("LIMIT 1");
            
            SysPluginRolloutStatus entity = rolloutStatusMapper.selectOne(queryWrapper);
            
            if (entity != null) {
                return entity.toRolloutStatus();
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取灰度发布状态失败", e);
            return null;
        }
    }
    
    /**
     * 获取插件的所有灰度发布记录
     *
     * @param pluginId 插件ID
     * @return 灰度发布记录列表
     */
    public List<SysPluginRolloutStatus> getPluginRolloutHistory(String pluginId) {
        LambdaQueryWrapper<SysPluginRolloutStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginRolloutStatus::getPluginId, pluginId)
                .orderByDesc(SysPluginRolloutStatus::getCreateTime);
        
        return rolloutStatusMapper.selectList(queryWrapper);
    }
    
    /**
     * 删除历史灰度发布记录
     *
     * @param pluginId 插件ID
     * @param days 保留天数
     * @return 删除记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupRolloutHistory(String pluginId, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        LambdaQueryWrapper<SysPluginRolloutStatus> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginRolloutStatus::getPluginId, pluginId)
                .lt(SysPluginRolloutStatus::getCreateTime, cutoffDate);
        
        return rolloutStatusMapper.delete(queryWrapper);
    }
    
    /**
     * 获取插件配置
     *
     * @param pluginId 插件ID
     * @return 插件配置
     */
    public Map<String, String> getPluginConfig(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return Collections.emptyMap();
        }
        
        try {
            // 查询插件配置
            List<SysPluginConfig> configs = configMapper.selectList(
                    new LambdaQueryWrapper<SysPluginConfig>()
                            .eq(SysPluginConfig::getPluginId, pluginId));
            
            Map<String, String> result = new HashMap<>();
            for (SysPluginConfig config : configs) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取插件配置失败: {}, 错误: {}", pluginId, e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 获取插件数据目录
     *
     * @param pluginId 插件ID
     * @return 插件数据目录
     */
    public File getPluginDataDir(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            throw new IllegalArgumentException("插件ID不能为空");
        }
        
        // 获取插件数据根目录
        File dataRootDir = new File(pluginConfig.getPluginDataDir());
        if (!dataRootDir.exists() && !dataRootDir.mkdirs()) {
            log.error("创建插件数据根目录失败: {}", dataRootDir.getAbsolutePath());
        }
        
        // 创建插件特定的数据目录
        File pluginDataDir = new File(dataRootDir, pluginId);
        if (!pluginDataDir.exists() && !pluginDataDir.mkdirs()) {
            log.error("创建插件数据目录失败: {}", pluginDataDir.getAbsolutePath());
        }
        
        return pluginDataDir;
    }
} 