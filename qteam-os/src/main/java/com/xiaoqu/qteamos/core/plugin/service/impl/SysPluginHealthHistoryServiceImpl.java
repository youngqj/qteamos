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

package com.xiaoqu.qteamos.core.plugin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginHealthHistoryMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginHealthHistory;
import com.xiaoqu.qteamos.core.plugin.service.SysPluginHealthHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 插件健康检查历史服务实现类
 *
 * @author yangqijun
 * @date 2024-08-15
 * @since 1.0.0
 */
@Service
public class SysPluginHealthHistoryServiceImpl extends ServiceImpl<SysPluginHealthHistoryMapper, SysPluginHealthHistory> implements SysPluginHealthHistoryService {
    private static final Logger log = LoggerFactory.getLogger(SysPluginHealthHistoryServiceImpl.class);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addHealthCheck(String pluginId, String version, String state, 
            boolean healthy, String message, int failCount, String checkType) {
        SysPluginHealthHistory history = SysPluginHealthHistory.create(
                pluginId, version, state, healthy, message, failCount, checkType);
        boolean success = save(history);
        if (success) {
            log.debug("记录插件[{}]健康检查: 健康状态={}, 消息={}", pluginId, healthy, message);
            return history.getId();
        } else {
            log.error("记录插件[{}]健康检查失败", pluginId);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addHealthCheck(String pluginId, String version, String state, 
            boolean healthy, String message, int failCount, 
            int memoryUsageMb, int threadCount, String checkType) {
        SysPluginHealthHistory history = SysPluginHealthHistory.create(
                pluginId, version, state, healthy, message, failCount, 
                memoryUsageMb, threadCount, checkType);
        boolean success = save(history);
        if (success) {
            log.debug("记录插件[{}]健康检查: 健康状态={}, 消息={}, 内存={}MB, 线程数={}", 
                    pluginId, healthy, message, memoryUsageMb, threadCount);
            return history.getId();
        } else {
            log.error("记录插件[{}]健康检查失败", pluginId);
            return null;
        }
    }

    @Override
    public List<SysPluginHealthHistory> getHealthHistory(String pluginId, int limit) {
        return baseMapper.getHealthHistory(pluginId, limit);
    }

    @Override
    public Optional<SysPluginHealthHistory> getLastHealthCheck(String pluginId) {
        return Optional.ofNullable(baseMapper.getLastHealthCheck(pluginId));
    }

    @Override
    public List<String> getHealthyPlugins() {
        return baseMapper.getPluginsByHealthStatus(true);
    }

    @Override
    public List<String> getUnhealthyPlugins() {
        return baseMapper.getPluginsByHealthStatus(false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePluginHealthHistory(String pluginId) {
        LambdaQueryWrapper<SysPluginHealthHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginHealthHistory::getPluginId, pluginId);
        
        boolean success = remove(queryWrapper);
        if (success) {
            log.debug("清除插件[{}]健康检查历史记录成功", pluginId);
        } else {
            log.warn("清除插件[{}]健康检查历史记录失败", pluginId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllHealthRecords() {
        boolean success = remove(null);  // 传入null表示删除所有记录
        if (success) {
            log.debug("清除所有插件健康检查历史记录成功");
        } else {
            log.warn("清除所有插件健康检查历史记录失败");
        }
    }
} 