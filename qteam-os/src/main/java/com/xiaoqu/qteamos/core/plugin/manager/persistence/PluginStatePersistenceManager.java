package com.xiaoqu.qteamos.core.plugin.manager.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoqu.qteamos.core.plugin.event.EventListener;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.event.Event;

import com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager.PluginStateChangeEvent;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.running.PluginDescriptor;
import com.xiaoqu.qteamos.core.plugin.running.PluginDependency;

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
import java.util.Collection;
import java.util.Optional;
import java.util.ArrayList;
import java.nio.file.Path;
import java.util.Map;

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
                .setWebsite(null); // 网站地址暂时为空
        
        // 设置JAR文件路径
        if (pluginInfo.getJarPath() != null) {
            log.info("保存插件JAR路径: {}", pluginInfo.getJarPath());
            entity.setJarPath(pluginInfo.getJarPath().toString());
        } else {
            log.warn("插件没有JAR路径信息");
        }
        
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
                .setWebsite(null)
                .setJarPath(pluginInfo.getJarPath() != null ? pluginInfo.getJarPath().toString() : null);

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
        try {
            log.debug("接收到插件事件: 类型={}, 来源={}", event.getClass().getName(), event.getSource());
            
            // 使用instanceof检查是否是PluginStateManager.PluginStateChangeEvent类型
            if (event.getClass().getName().equals("com.xiaoqu.qteamos.core.plugin.manager.PluginStateManager$PluginStateChangeEvent")) {
                // 使用反射获取pluginId和newState，避免直接类型转换
                try {
                    String pluginId = (String) event.getClass().getMethod("getPluginId").invoke(event);
                    PluginState newState = (PluginState) event.getClass().getMethod("getNewState").invoke(event);
                    
                    // 更新插件状态
                    if (newState == PluginState.STARTED) {
                        updatePluginState(pluginId, null, newState, LocalDateTime.now(), null);
                    } else if (newState == PluginState.STOPPED) {
                        updatePluginState(pluginId, null, newState, null, LocalDateTime.now());
                    } else {
                        updatePluginState(pluginId, null, newState, null, null);
                    }
                    
                    log.debug("处理PluginStateManager状态变更事件成功: pluginId={}, newState={}", pluginId, newState);
                    return true;
                } catch (Exception e) {
                    log.error("处理PluginStateManager状态变更事件失败", e);
                }
            }
            
            // 使用instanceof检查是否是PluginStateChangeEvent类型(从DefaultPluginStateTracker)
            if (event.getClass().getName().equals("com.xiaoqu.qteamos.core.plugin.event.plugins.PluginStateChangeEvent")) {
                try {
                    // 使用反射获取pluginId、version和newState，避免直接类型转换
                    String pluginId = (String) event.getClass().getMethod("getPluginId").invoke(event);
                    String version = (String) event.getClass().getMethod("getVersion").invoke(event);
                    String newStateStr = (String) event.getClass().getMethod("getNewState").invoke(event);
                    
                    // 将字符串状态转换为枚举
                    PluginState newState = null;
                    try {
                        if (newStateStr != null && !newStateStr.isEmpty()) {
                            newState = PluginState.valueOf(newStateStr);
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("无效的状态值: {}", newStateStr);
                    }
                    
                    if (newState != null) {
                        // 更新插件状态
                        if (newState == PluginState.STARTED) {
                            updatePluginState(pluginId, version, newState, LocalDateTime.now(), null);
                        } else if (newState == PluginState.STOPPED) {
                            updatePluginState(pluginId, version, newState, null, LocalDateTime.now());
                        } else {
                            updatePluginState(pluginId, version, newState, null, null);
                        }
                        
                        log.debug("处理PluginStateChangeEvent状态变更事件成功: pluginId={}, version={}, newState={}", 
                                 pluginId, version, newState);
                    }
                    return true;
                } catch (Exception e) {
                    log.error("处理PluginStateChangeEvent事件失败", e);
                }
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
    public boolean onPluginStateChangeEvent(Event stateEvent) {
        try {
            log.debug("已废弃的方法被调用: onPluginStateChangeEvent，事件类型: {}", stateEvent.getClass().getName());
            return onPluginEvent(stateEvent);
        } catch (Exception e) {
            log.error("转发事件异常: 事件类型={}", stateEvent.getClass().getName(), e);
            return true; // 继续传播事件
        }
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

    /**
     * 获取插件信息
     * 
     * @param pluginId 插件ID
     * @return 插件信息
     */
    public Optional<PluginInfo> getPluginInfo(String pluginId) {
        // Implementation needed
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * 删除插件信息
     * 
     * @param pluginId 插件ID
     */
    public void deletePluginInfo(String pluginId) {
        // Implementation needed
        throw new UnsupportedOperationException("Method not implemented");
    }

    /**
     * 获取所有已注册的插件信息
     * 
     * @return 所有插件信息集合
     */
    public Collection<PluginInfo> getAllPlugins() {
        try {
            log.info("开始从数据库获取所有已注册插件信息");
            
            // 使用联合查询获取所有插件信息和状态
            List<Map<String, Object>> pluginDataList = sysPluginInfoMapper.selectPluginsWithStatus();
            
            if (pluginDataList.isEmpty()) {
                log.info("数据库中没有已注册的插件信息");
                return new ArrayList<>();
            }
            
            log.info("从数据库获取到{}个插件信息", pluginDataList.size());
            List<PluginInfo> result = new ArrayList<>();
            
            // 遍历每个插件信息，构建完整的PluginInfo对象
            for (Map<String, Object> pluginData : pluginDataList) {
                try {
                    // 从联合查询结果中提取插件基本信息
                    SysPluginInfo sysPluginInfo = new SysPluginInfo();
                    sysPluginInfo.setId((Long) pluginData.get("id"));
                    sysPluginInfo.setPluginId((String) pluginData.get("plugin_id"));
                    sysPluginInfo.setName((String) pluginData.get("name"));
                    sysPluginInfo.setVersion((String) pluginData.get("version"));
                    sysPluginInfo.setDescription((String) pluginData.get("description"));
                    sysPluginInfo.setAuthor((String) pluginData.get("author"));
                    sysPluginInfo.setMainClass((String) pluginData.get("main_class"));
                    sysPluginInfo.setType((String) pluginData.get("type"));
                    sysPluginInfo.setTrust((String) pluginData.get("trust"));
                    sysPluginInfo.setRequiredSystemVersion((String) pluginData.get("required_system_version"));
                    sysPluginInfo.setPriority((Integer) pluginData.get("priority"));
                    sysPluginInfo.setProvider((String) pluginData.get("provider"));
                    sysPluginInfo.setLicense((String) pluginData.get("license"));
                    sysPluginInfo.setCategory((String) pluginData.get("category"));
                    sysPluginInfo.setWebsite((String) pluginData.get("website"));
                    sysPluginInfo.setJarPath((String) pluginData.get("jar_file"));
                    sysPluginInfo.setHaveDependency((Integer) pluginData.get("have_dependency"));
                    
                    // 创建插件描述符
                    PluginDescriptor descriptor = createPluginDescriptor(sysPluginInfo);
                    
                    // 创建PluginInfo对象
                    PluginInfo pluginInfo = PluginInfo.builder()
                        .descriptor(descriptor)
                        .build();
                    
                    // 从联合查询结果中提取插件状态信息
                    if (pluginData.get("status") != null) {
                        try {
                            pluginInfo.setState(PluginState.valueOf((String) pluginData.get("status")));
                        } catch (IllegalArgumentException e) {
                            log.warn("未知的插件状态: {}, 设置为CREATED", pluginData.get("status"));
                            pluginInfo.setState(PluginState.CREATED);
                        }
                    }
                    
                    // 设置插件启用状态
                    Boolean enabled = (Boolean) pluginData.get("enabled");
                    pluginInfo.setEnabled(enabled != null ? enabled : false);
                    
                    // 设置错误信息
                    String errorMessage = (String) pluginData.get("error_message");
                    if (errorMessage != null) {
                        pluginInfo.setErrorMessage(errorMessage);
                    }
                    
                    // 设置JAR路径
                    String jarPath = (String) pluginData.get("jar_file");
                    if (jarPath != null && !jarPath.isEmpty()) {
                        log.info("从数据库读取到插件[{}]的JAR路径: {}", pluginInfo.getDescriptor().getPluginId(), jarPath);
                        pluginInfo.setJarPath(Path.of(jarPath));
                    } else if (sysPluginInfo.getJarPath() != null && !sysPluginInfo.getJarPath().isEmpty()) {
                        log.info("从实体类读取到插件[{}]的JAR路径: {}", pluginInfo.getDescriptor().getPluginId(), sysPluginInfo.getJarPath());
                        pluginInfo.setJarPath(Path.of(sysPluginInfo.getJarPath()));
                    } else {
                        log.warn("插件[{}]的JAR路径为空", pluginInfo.getDescriptor().getPluginId());
                    }
                    
                    result.add(pluginInfo);
                    
                } catch (Exception e) {
                    log.error("处理插件信息时发生错误: {}", pluginData.get("plugin_id"), e);
                }
            }
            
            log.info("成功加载{}个插件信息", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("获取插件信息时发生错误", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 创建插件描述符
     */
    private PluginDescriptor createPluginDescriptor(SysPluginInfo sysPluginInfo) {
        // 构建插件描述符
        PluginDescriptor descriptor = new PluginDescriptor();
            
        descriptor.setPluginId(sysPluginInfo.getPluginId());
        descriptor.setName(sysPluginInfo.getName());
        descriptor.setVersion(sysPluginInfo.getVersion());
        descriptor.setDescription(sysPluginInfo.getDescription());
        descriptor.setAuthor(sysPluginInfo.getAuthor());
        descriptor.setMainClass(sysPluginInfo.getMainClass());
        descriptor.setType(sysPluginInfo.getType());
        
        // 安全地设置trust值
        if (sysPluginInfo.getTrust() != null) {
            descriptor.setTrust(sysPluginInfo.getTrust());
        } else {
            descriptor.setTrust(null);
        }
        
        descriptor.setRequiredSystemVersion(sysPluginInfo.getRequiredSystemVersion());
        descriptor.setPriority(sysPluginInfo.getPriority());
        
        // 查询插件依赖
        List<SysPluginDependency> dependencies = sysPluginDependencyMapper.selectList(
                new LambdaQueryWrapper<SysPluginDependency>()
                        .eq(SysPluginDependency::getPluginId, sysPluginInfo.getPluginId())
                        .eq(SysPluginDependency::getPluginVersion, sysPluginInfo.getVersion()));
        
        // 添加依赖到描述符
        if (!dependencies.isEmpty()) {
            List<PluginDependency> pluginDependencies = 
                    new ArrayList<>();
            
            for (SysPluginDependency dependency : dependencies) {
                PluginDependency pluginDependency = new PluginDependency();
                pluginDependency.setPluginId(dependency.getDependencyPluginId());
                pluginDependency.setVersionRequirement(dependency.getVersionRequirement());
                pluginDependency.setOptional(dependency.getOptional());
                pluginDependencies.add(pluginDependency);
            }
            
            descriptor.setDependencies(pluginDependencies);
        }
        
        return descriptor;
    }
} 