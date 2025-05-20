package com.xiaoqu.qteamos.core.plugin.manager.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoqu.qteamos.core.plugin.event.EventListener;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.event.Event;


import com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager.PluginStateChangeEvent;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginInfo;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStatus;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginDependency;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginUpdateHistory;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginInfoMapper;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginStatusMapper;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginDependencyMapper;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginUpdateHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 插件状态持久化管理器
 * 用于将插件状态同步到数据库
 *
 * @author yangqijun
 * @date 2024-07-03
 */
@Component
public class PluginStatePersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PluginStatePersistenceManager.class);
    
    @Autowired
    private SysPluginInfoMapper sysPluginInfoMapper;
    
    @Autowired
    private SysPluginStatusMapper sysPluginStatusMapper;
    
    @Autowired
    private SysPluginDependencyMapper sysPluginDependencyMapper;
    
    @Autowired
    private SysPluginUpdateHistoryMapper sysPluginUpdateHistoryMapper;
    
    /**
     * 保存插件信息
     *
     * @param pluginInfo 插件信息
     */
    @Transactional
    public void savePluginInfo(PluginInfo pluginInfo) {
        try {
            String pluginId = pluginInfo.getDescriptor().getPluginId();
            String version = pluginInfo.getDescriptor().getVersion();
            
            // 查询数据库是否已存在此插件
            SysPluginInfo existingInfo = sysPluginInfoMapper.selectOne(
                    new LambdaQueryWrapper<SysPluginInfo>()
                            .eq(SysPluginInfo::getPluginId, pluginId)
                            .eq(SysPluginInfo::getVersion, version));
            
            if (existingInfo == null) {
                // 插入新记录
                insertPluginInfo(pluginInfo);
                
                // 记录安装历史
                SysPluginUpdateHistory history = new SysPluginUpdateHistory()
                        .setPluginId(pluginId)
                        .setPreviousVersion(version)
                        .setTargetVersion(version)
                        .setStatus("SUCCESS")
                        .setUpdateLog("插件初始安装")
                        .setCreateTime(LocalDateTime.now())
                        .setUpdateModifyTime(LocalDateTime.now());
                sysPluginUpdateHistoryMapper.insert(history);
                
                log.info("插件信息已保存至数据库: {}", pluginId);
            } else {
                // 更新现有记录
                updatePluginInfo(pluginInfo);
                log.info("插件信息已更新: {}", pluginId);
            }
            
            // 保存或更新状态
            savePluginStatus(pluginInfo);
            
            // 保存依赖关系
            savePluginDependencies(pluginInfo);
        } catch (Exception e) {
            log.error("保存插件信息失败", e);
            throw new RuntimeException("保存插件信息失败", e);
        }
    }
    
    /**
     * 插入新的插件信息
     */
    private void insertPluginInfo(PluginInfo pluginInfo) {
        log.info("开始插入新的插件信息: {}", pluginInfo.getDescriptor().getPluginId());
        SysPluginInfo entity = new SysPluginInfo()
                .setPluginId(pluginInfo.getDescriptor().getPluginId())
                .setName(pluginInfo.getDescriptor().getName())
                .setVersion(pluginInfo.getDescriptor().getVersion())
                .setDescription(pluginInfo.getDescriptor().getDescription())
                .setAuthor(pluginInfo.getDescriptor().getAuthor())
                .setMainClass(pluginInfo.getDescriptor().getMainClass())
                .setType(pluginInfo.getDescriptor().getType())
                .setTrust(pluginInfo.getDescriptor().getTrust())
                .setRequiredSystemVersion(pluginInfo.getDescriptor().getRequiredSystemVersion())
                .setPriority(pluginInfo.getDescriptor().getPriority())
                .setWebsite(pluginInfo.getJarPath() != null ? pluginInfo.getJarPath().toString() : null);
        
        // 手动设置创建时间和更新时间，防止自动填充失效
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        entity.setVersionNum(0);

        // 使用MyBatis Plus插入
        try {
            int rows = sysPluginInfoMapper.insert(entity);
            log.info("插入插件信息结果: pluginId={}, 影响行数={}", pluginInfo.getDescriptor().getPluginId(), rows);
        } catch (Exception e) {
            log.error("插入插件信息到数据库失败: {}", pluginInfo.getDescriptor().getPluginId(), e);
            throw e; // 重新抛出异常，确保事务回滚
        }
    }
    
    /**
     * 更新现有插件信息
     */
    private void updatePluginInfo(PluginInfo pluginInfo) {
        // 创建更新对象
        SysPluginInfo entity = new SysPluginInfo()
                .setName(pluginInfo.getDescriptor().getName())
                .setDescription(pluginInfo.getDescriptor().getDescription())
                .setAuthor(pluginInfo.getDescriptor().getAuthor())
                .setMainClass(pluginInfo.getDescriptor().getMainClass())
                .setType(pluginInfo.getDescriptor().getType())
                .setTrust(pluginInfo.getDescriptor().getTrust())
                .setRequiredSystemVersion(pluginInfo.getDescriptor().getRequiredSystemVersion())
                .setPriority(pluginInfo.getDescriptor().getPriority())
                .setWebsite(pluginInfo.getJarPath() != null ? pluginInfo.getJarPath().toString() : null);

        // 使用条件构造器更新
        sysPluginInfoMapper.update(entity, 
                new LambdaQueryWrapper<SysPluginInfo>()
                        .eq(SysPluginInfo::getPluginId, pluginInfo.getDescriptor().getPluginId())
                        .eq(SysPluginInfo::getVersion, pluginInfo.getDescriptor().getVersion()));
    }
    
    /**
     * 保存插件状态
     */
    private void savePluginStatus(PluginInfo pluginInfo) {
        String pluginId = pluginInfo.getDescriptor().getPluginId();
        String version = pluginInfo.getDescriptor().getVersion();
        
        // 查询是否存在
        SysPluginStatus existingStatus = sysPluginStatusMapper.selectOne(
                new LambdaQueryWrapper<SysPluginStatus>()
                        .eq(SysPluginStatus::getPluginId, pluginId)
                        .eq(SysPluginStatus::getVersion, version));
        
        if (existingStatus == null) {
            // 插入新记录
            SysPluginStatus status = new SysPluginStatus()
                    .setPluginId(pluginId)
                    .setVersion(version)
                    .setEnabled(pluginInfo.isEnabled())
                    .setStatus(pluginInfo.getState().name());
            
            sysPluginStatusMapper.insert(status);
        } else {
            // 更新现有记录
            SysPluginStatus status = new SysPluginStatus();
            status.setEnabled(pluginInfo.isEnabled());
            status.setStatus(pluginInfo.getState().name());
            
            // 设置时间和错误信息
            if (pluginInfo.getState() == PluginState.STARTED) {
                status.setLastStartTime(LocalDateTime.now());
            } else if (pluginInfo.getState() == PluginState.STOPPED) {
                status.setLastStopTime(LocalDateTime.now());
            } else if (pluginInfo.getState() == PluginState.ERROR) {
                status.setErrorMessage(pluginInfo.getErrorMessage());
            }
            
            sysPluginStatusMapper.update(status, 
                    new LambdaQueryWrapper<SysPluginStatus>()
                            .eq(SysPluginStatus::getPluginId, pluginId)
                            .eq(SysPluginStatus::getVersion, version));
        }
    }
    
    /**
     * 保存插件依赖关系
     */
    private void savePluginDependencies(PluginInfo pluginInfo) {
        String pluginId = pluginInfo.getDescriptor().getPluginId();
        String version = pluginInfo.getDescriptor().getVersion();
        
        // 删除现有依赖
        sysPluginDependencyMapper.delete(
                new LambdaQueryWrapper<SysPluginDependency>()
                        .eq(SysPluginDependency::getPluginId, pluginId)
                        .eq(SysPluginDependency::getPluginVersion, version));
        
        // 重新插入依赖关系
        if (pluginInfo.getDescriptor().getDependencies() != null) {
            pluginInfo.getDescriptor().getDependencies().forEach(dependency -> {
                SysPluginDependency entity = new SysPluginDependency()
                        .setPluginId(pluginId)
                        .setPluginVersion(version)
                        .setDependencyPluginId(dependency.getPluginId())
                        .setVersionRequirement(dependency.getVersionRequirement())
                        .setOptional(dependency.isOptional());
                
                sysPluginDependencyMapper.insert(entity);
            });
        }
    }
    
    /**
     * 监听插件事件
     */
    @EventListener(topics = "plugin")
    public boolean onPluginEvent(Event event) {
        // 各种插件事件都可能导致状态变化，需要更新数据库
        // 后续可以优化为只有状态变化时才更新
        try {
            log.debug("接收到插件事件: 类型={}, 来源={}", event.getClass().getName(), event.getSource());
            
            // 处理PluginStateChangeEvent事件
            if (event instanceof com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager.PluginStateChangeEvent) {
                com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager.PluginStateChangeEvent stateEvent = 
                    (com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager.PluginStateChangeEvent) event;
                
                String pluginId = stateEvent.getPluginId();
                com.xiaoqu.qteamos.core.plugin.running.PluginState newState = stateEvent.getNewState();
                
                // 更新插件状态
                if (newState == com.xiaoqu.qteamos.core.plugin.running.PluginState.STARTED) {
                    updatePluginState(pluginId, null, newState, LocalDateTime.now(), null);
                } else if (newState == com.xiaoqu.qteamos.core.plugin.running.PluginState.STOPPED) {
                    updatePluginState(pluginId, null, newState, null, LocalDateTime.now());
                } else {
                    updatePluginState(pluginId, null, newState, null, null);
                }
                
                log.debug("处理状态变更事件成功: pluginId={}, newState={}", pluginId, newState);
                return true;
            }
            
            // 处理普通的PluginEvent事件
            if (event instanceof PluginEvent) {
                PluginEvent pluginEvent = (PluginEvent) event;
                
                // 根据事件类型更新相关状态
                switch (pluginEvent.getType()) {
                    case PluginEvent.TYPE_LOADED:
                        updatePluginState(pluginEvent.getPluginId(), pluginEvent.getVersion(), PluginState.LOADED, null, null);
                        break;
                    case PluginEvent.TYPE_INITIALIZED:
                        updatePluginState(pluginEvent.getPluginId(), pluginEvent.getVersion(), PluginState.INITIALIZED, null, null);
                        break;
                    case PluginEvent.TYPE_STARTED:
                        updatePluginState(pluginEvent.getPluginId(), pluginEvent.getVersion(), PluginState.STARTED, LocalDateTime.now(), null);
                        break;
                    case PluginEvent.TYPE_STOPPED:
                        updatePluginState(pluginEvent.getPluginId(), pluginEvent.getVersion(), PluginState.STOPPED, null, LocalDateTime.now());
                        break;
                    case PluginEvent.TYPE_UNLOADED:
                        updatePluginState(pluginEvent.getPluginId(), pluginEvent.getVersion(), PluginState.UNLOADED, null, null);
                        break;
                    case PluginEvent.TYPE_ERROR:
                        String errorMessage = null;
                        if (pluginEvent.getData() instanceof Throwable) {
                            errorMessage = ((Throwable) pluginEvent.getData()).getMessage();
                        }
                        updatePluginState(pluginEvent.getPluginId(), pluginEvent.getVersion(), PluginState.ERROR, null, null, errorMessage);
                        break;
                    case PluginEvent.TYPE_ENABLED:
                        updatePluginEnabled(pluginEvent.getPluginId(), pluginEvent.getVersion(), true);
                        break;
                    case PluginEvent.TYPE_DISABLED:
                        updatePluginEnabled(pluginEvent.getPluginId(), pluginEvent.getVersion(), false);
                        break;
                }
                
                log.debug("处理插件事件成功: pluginId={}, 事件类型={}", 
                    ((PluginEvent)event).getPluginId(), ((PluginEvent)event).getType());
                return true;
            }
            
            return true;
        } catch (Exception e) {
            log.error("处理插件事件异常: 事件类型={}", event.getClass().getName(), e);
            return true; // 继续传播事件
        }
    }
    
    /**
     * 监听插件状态变化事件，此方法已废弃，所有事件统一由onPluginEvent处理
     * 保留此方法以防止历史代码调用出错
     */
    @Deprecated
    @EventListener(topics = "plugin", types = "state_change")
    public boolean onPluginStateChangeEvent(com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager.PluginStateChangeEvent stateEvent) {
        log.debug("已废弃的方法被调用，事件将转发到onPluginEvent处理");
        return onPluginEvent(stateEvent);
    }
    
    /**
     * 更新插件状态
     */
    private void updatePluginState(String pluginId, String version, PluginState state, 
                                  LocalDateTime startTime, LocalDateTime stopTime) {
        updatePluginState(pluginId, version, state, startTime, stopTime, null);
    }
    
    /**
     * 更新插件状态（带错误信息）
     */
    private void updatePluginState(String pluginId, String version, PluginState state, 
                                  LocalDateTime startTime, LocalDateTime stopTime, String errorMessage) {
        SysPluginStatus status = new SysPluginStatus();
        status.setStatus(state.name());
        
        if (startTime != null) {
            status.setLastStartTime(startTime);
        }
        
        if (stopTime != null) {
            status.setLastStopTime(stopTime);
        }
        
        if (errorMessage != null) {
            status.setErrorMessage(errorMessage);
        }
        
        LambdaQueryWrapper<SysPluginStatus> wrapper = new LambdaQueryWrapper<SysPluginStatus>()
                .eq(SysPluginStatus::getPluginId, pluginId);
        
        if (version != null) {
            // 如果版本号不为空，精确匹配
            wrapper.eq(SysPluginStatus::getVersion, version);
        } else {
            // 如果版本号为空，查询最新版本
            List<SysPluginStatus> statusList = sysPluginStatusMapper.selectList(
                    new LambdaQueryWrapper<SysPluginStatus>()
                            .eq(SysPluginStatus::getPluginId, pluginId)
                            .orderByDesc(SysPluginStatus::getVersion)
                            .last("LIMIT 1"));
            
            if (statusList.isEmpty()) {
                log.warn("未找到插件状态记录: {}", pluginId);
                return;
            }
            
            wrapper.eq(SysPluginStatus::getVersion, statusList.get(0).getVersion());
        }
        
        sysPluginStatusMapper.update(status, wrapper);
    }
    
    /**
     * 更新插件启用状态
     */
    private void updatePluginEnabled(String pluginId, String version, boolean enabled) {
        SysPluginStatus status = new SysPluginStatus();
        status.setEnabled(enabled);
        
        sysPluginStatusMapper.update(status, 
                new LambdaQueryWrapper<SysPluginStatus>()
                        .eq(SysPluginStatus::getPluginId, pluginId)
                        .eq(SysPluginStatus::getVersion, version));
    }
    
    /**
     * 删除插件记录
     */
    @Transactional
    public void deletePluginRecord(String pluginId, String version) {
        try {
            // 删除依赖关系
            sysPluginDependencyMapper.delete(
                    new LambdaQueryWrapper<SysPluginDependency>()
                            .eq(SysPluginDependency::getPluginId, pluginId)
                            .eq(SysPluginDependency::getPluginVersion, version));
            
            // 删除状态记录
            sysPluginStatusMapper.delete(
                    new LambdaQueryWrapper<SysPluginStatus>()
                            .eq(SysPluginStatus::getPluginId, pluginId)
                            .eq(SysPluginStatus::getVersion, version));
            
            // 删除插件信息
            sysPluginInfoMapper.delete(
                    new LambdaQueryWrapper<SysPluginInfo>()
                            .eq(SysPluginInfo::getPluginId, pluginId)
                            .eq(SysPluginInfo::getVersion, version));
            
            log.info("插件记录已从数据库中删除: {} {}", pluginId, version);
        } catch (Exception e) {
            log.error("删除插件记录失败: " + pluginId + " " + version, e);
            throw new RuntimeException("删除插件记录失败", e);
        }
    }
} 