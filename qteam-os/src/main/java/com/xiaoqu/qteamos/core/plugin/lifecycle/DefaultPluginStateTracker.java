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

package com.xiaoqu.qteamos.core.plugin.lifecycle;

import com.xiaoqu.qteamos.api.core.plugin.api.PluginStateTracker;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginStateChangeEvent;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginStateHistory;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.service.SysPluginStateHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 插件状态跟踪器默认实现
 * 负责跟踪和管理插件状态变化，记录状态转换历史，并支持状态持久化
 *
 * @author yangqijun
 * @date 2024-08-10
 * @since 1.0.0
 */
@Component
public class DefaultPluginStateTracker implements PluginStateTracker {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginStateTracker.class);

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private SysPluginStateHistoryService stateHistoryService;

    // 缓存上一次的插件状态
    private final Map<String, String> lastStates = new HashMap<>();

    @Override
    public void recordStateChange(String pluginId, String newState) {
        recordStateChange(pluginId, newState, null);
    }

    @Override
    public void recordStateChange(String pluginId, String newState, String message) {
        Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.warn("无法记录状态变化，插件不存在: {}", pluginId);
            return;
        }

        PluginInfo pluginInfo = optPluginInfo.get();
        String version = pluginInfo.getDescriptor().getVersion();
        String oldState = lastStates.getOrDefault(pluginId, null);

        // 转换enum类型为字符串
        PluginState newStateEnum = PluginState.valueOf(newState);

        // 更新插件状态
        pluginInfo.setState(newStateEnum);
        pluginRegistry.updatePlugin(pluginInfo);

        // 缓存新状态
        lastStates.put(pluginId, newState);

        // 保存状态历史记录
        stateHistoryService.addStateHistory(pluginId, version, oldState, newState, message);

        // 发布状态变化事件
        publishStateChangeEvent(pluginId, version, oldState, newState, message);

        log.info("插件状态已变更: {}, {} -> {}", pluginId, oldState, newState);
    }

    @Override
    public void recordFailure(String pluginId, String errorMessage) {
        // 记录状态变化为ERROR
        recordStateChange(pluginId, PluginState.ERROR.name(), errorMessage);

        // 获取插件信息并设置错误信息
        pluginRegistry.getPlugin(pluginId).ifPresent(pluginInfo -> {
            pluginInfo.setErrorMessage(errorMessage);
            pluginRegistry.updatePlugin(pluginInfo);
        });

        // 记录到日志
        log.error("插件[{}]失败: {}", pluginId, errorMessage);
    }

    @Override
    public Optional<String> getPluginState(String pluginId) {
        return pluginRegistry.getPlugin(pluginId)
                .map(PluginInfo::getState)
                .map(PluginState::name);
    }

    @Override
    public boolean isInState(String pluginId, String state) {
        return getPluginState(pluginId)
                .map(currentState -> currentState.equals(state))
                .orElse(false);
    }

    @Override
    public List<String> getPluginsInState(String state) {
        return stateHistoryService.getPluginsInState(state);
    }

    @Override
    public List<String> getFailedPlugins() {
        return stateHistoryService.getFailedPlugins();
    }

    @Override
    public List<StateChangeRecord> getStateHistory(String pluginId, int limit) {
        List<SysPluginStateHistory> historyList = stateHistoryService.getStateHistory(pluginId, limit);
        return historyList.stream().map(this::convertToStateChangeRecord).collect(Collectors.toList());
    }

    @Override
    public Optional<StateChangeRecord> getLastStateChange(String pluginId) {
        return stateHistoryService.getLastStateChange(pluginId)
                .map(this::convertToStateChangeRecord);
    }

    @Override
    public void clearStateRecord(String pluginId) {
        lastStates.remove(pluginId);
        stateHistoryService.deletePluginStateHistory(pluginId);
    }

    @Override
    public void clearAllStateRecords() {
        lastStates.clear();
        stateHistoryService.clearAllStateRecords();
    }

    /**
     * 发布状态变化事件
     */
    private void publishStateChangeEvent(String pluginId, String version, String oldState, String newState, String message) {
        PluginStateChangeEvent event = new PluginStateChangeEvent(pluginId, version, oldState, newState, message);

        // 添加详细的调试日志
        log.debug("创建状态变更事件: {}", event);
        log.debug("事件详情 - 插件ID: {}, 旧状态: {}, 新状态: {}", 
                event.getPluginId(), 
                event.getOldState(), 
                event.getNewState());

        eventBus.postEvent(event);
    }

    /**
     * 将SysPluginStateHistory转换为StateChangeRecord
     */
    private StateChangeRecord convertToStateChangeRecord(SysPluginStateHistory history) {
        return new DefaultStateChangeRecord(
                history.getPluginId(),
                history.getVersion(),
                history.getOldState(),
                history.getNewState(),
                history.getChangeTime(),
                history.getMessage()
        );
    }

    /**
     * StateChangeRecord默认实现
     */
    private static class DefaultStateChangeRecord implements StateChangeRecord {
        private final String pluginId;
        private final String version;
        private final String oldState;
        private final String newState;
        private final LocalDateTime changeTime;
        private final String message;

        public DefaultStateChangeRecord(String pluginId, String version, String oldState, String newState, 
                                       LocalDateTime changeTime, String message) {
            this.pluginId = pluginId;
            this.version = version;
            this.oldState = oldState;
            this.newState = newState;
            this.changeTime = changeTime;
            this.message = message;
        }

        @Override
        public String getPluginId() {
            return pluginId;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getOldState() {
            return oldState;
        }

        @Override
        public String getNewState() {
            return newState;
        }

        @Override
        public LocalDateTime getChangeTime() {
            return changeTime;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
} 