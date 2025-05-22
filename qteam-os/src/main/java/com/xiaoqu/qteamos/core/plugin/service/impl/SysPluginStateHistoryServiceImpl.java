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
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginStateHistoryMapper;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStateHistory;
import com.xiaoqu.qteamos.core.plugin.service.SysPluginStateHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 插件状态变更历史服务实现类
 *
 * @author yangqijun
 * @date 2024-08-10
 */
@Service
public class SysPluginStateHistoryServiceImpl extends ServiceImpl<SysPluginStateHistoryMapper, SysPluginStateHistory> implements SysPluginStateHistoryService {
    private static final Logger log = LoggerFactory.getLogger(SysPluginStateHistoryServiceImpl.class);

    // 定义失败状态列表
    private static final List<String> FAILED_STATES = Arrays.asList("ERROR", "FAILED", "DEPENDENCY_FAILED");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addStateHistory(String pluginId, String version, String oldState, String newState, String message) {
        SysPluginStateHistory history = SysPluginStateHistory.create(pluginId, version, oldState, newState, message);
        boolean success = save(history);
        if (success) {
            log.debug("记录插件[{}]状态变更: {} -> {}, 消息: {}", pluginId, oldState, newState, message);
            return history.getId();
        } else {
            log.error("记录插件[{}]状态变更失败", pluginId);
            return null;
        }
    }

    @Override
    public List<SysPluginStateHistory> getStateHistory(String pluginId, int limit) {
        return baseMapper.getStateHistory(pluginId, limit);
    }

    @Override
    public Optional<SysPluginStateHistory> getLastStateChange(String pluginId) {
        return Optional.ofNullable(baseMapper.getLastStateChange(pluginId));
    }

    @Override
    public List<String> getPluginsInState(String state) {
        return baseMapper.getPluginsInState(state);
    }

    @Override
    public List<String> getFailedPlugins() {
        // 使用mybatis-plus的Lambda查询构建器查询处于失败状态的插件
        LambdaQueryWrapper<SysPluginStateHistory> queryWrapper = new LambdaQueryWrapper<>();
        
        // 查询条件：状态为ERROR, FAILED或DEPENDENCY_FAILED，并且是最新的状态记录
        queryWrapper.select(SysPluginStateHistory::getPluginId)
                    .in(SysPluginStateHistory::getNewState, FAILED_STATES)
                    .notExists("SELECT 1 FROM sys_plugin_state_history t2 WHERE t2.plugin_id = sys_plugin_state_history.plugin_id AND t2.change_time > sys_plugin_state_history.change_time")
                    .groupBy(SysPluginStateHistory::getPluginId);
                    
        return listObjs(queryWrapper, Object::toString);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePluginStateHistory(String pluginId) {
        LambdaQueryWrapper<SysPluginStateHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysPluginStateHistory::getPluginId, pluginId);
        
        boolean success = remove(queryWrapper);
        if (success) {
            log.debug("清除插件[{}]状态历史记录成功", pluginId);
        } else {
            log.warn("清除插件[{}]状态历史记录失败", pluginId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearAllStateRecords() {
        boolean success = remove(null);  // 传入null表示删除所有记录
        if (success) {
            log.debug("清除所有插件状态历史记录成功");
        } else {
            log.warn("清除所有插件状态历史记录失败");
        }
    }
} 