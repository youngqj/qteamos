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

package com.xiaoqu.qteamos.core.plugin.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStatus;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginStatusMapper;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 插件状态服务
 *
 * @author yangqijun
 * @since 2025-05-22
 */
@Service
public class SysPluginStatusService extends ServiceImpl<SysPluginStatusMapper, SysPluginStatus> {
    private static final Logger log = LoggerFactory.getLogger(SysPluginStatusService.class);

    /**
     * 更新插件状态
     *
     * @param pluginInfo 插件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePluginStatus(PluginInfo pluginInfo) {
        String pluginId = pluginInfo.getPluginId();
        String version = pluginInfo.getVersion();
        PluginState state = pluginInfo.getState();
        
        // 使用 saveOrUpdate 方法
        SysPluginStatus dbStatus = getOne(
                Wrappers.<SysPluginStatus>lambdaQuery()
                        .eq(SysPluginStatus::getPluginId, pluginId)
                        .eq(SysPluginStatus::getVersion, version));
        
        if (dbStatus == null) {
            dbStatus = new SysPluginStatus();
        }
        
        // 设置状态信息
        dbStatus.setPluginId(pluginId)
                .setVersion(version)
                .setEnabled(pluginInfo.isEnabled())
                .setStatus(state.name())
                .setErrorMessage(pluginInfo.getErrorMessage());
        
        // 设置时间信息
        if (pluginInfo.getLoadTime() != null) {
            dbStatus.setInstalledTime(convertToLocalDateTime(pluginInfo.getLoadTime().getTime()));
        }
        if (pluginInfo.getStartTime() != null) {
            dbStatus.setLastStartTime(convertToLocalDateTime(pluginInfo.getStartTime().getTime()));
        }
        if (pluginInfo.getStopTime() != null) {
            dbStatus.setLastStopTime(convertToLocalDateTime(pluginInfo.getStopTime().getTime()));
        }
        
        saveOrUpdate(dbStatus);
        log.info("保存/更新插件状态: {} {}", pluginId, state);
    }

    /**
     * 恢复插件状态
     * 系统重启后调用此方法恢复插件状态
     *
     * @return 需要启动的插件列表
     */
    public List<String> restorePluginStatus() {
        // 使用 lambdaQuery 方法
        List<SysPluginStatus> enabledPlugins = lambdaQuery()
                .eq(SysPluginStatus::getEnabled, true)
                .eq(SysPluginStatus::getDeleted, false)
                .list();
        
        log.info("从数据库恢复插件状态，找到{}个启用的插件", enabledPlugins.size());
        
        // 返回需要加载的插件ID列表
        return enabledPlugins.stream()
                .map(status -> {
                    log.info("插件[{}] 版本[{}] 状态[{}]将被恢复",
                            status.getPluginId(), 
                            status.getVersion(), 
                            status.getStatus());
                    return status.getPluginId();
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取插件的当前状态
     *
     * @param pluginId 插件ID
     * @return 插件状态
     */
    public SysPluginStatus getPluginStatus(String pluginId) {
        return getOne(
                Wrappers.<SysPluginStatus>lambdaQuery()
                        .eq(SysPluginStatus::getPluginId, pluginId)
                        .orderByDesc(SysPluginStatus::getLastStartTime)
                        .last("LIMIT 1"));
    }

    /**
     * 将时间戳转换为LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }
} 