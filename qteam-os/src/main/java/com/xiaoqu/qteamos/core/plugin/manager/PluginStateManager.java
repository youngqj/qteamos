package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.core.plugin.event.Event;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map;
import java.util.HashMap;

/**
 * 插件状态管理器
 * 负责监控和管理插件状态变化，触发相应的事件
 *
 * @author yangqijun
 * @date 2024-07-02
 */
@Component
public class PluginStateManager {
    private static final Logger log = LoggerFactory.getLogger(PluginStateManager.class);
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private EventBus eventBus;
    
    // 缓存上一次的插件状态
    private final Map<String, PluginState> lastStates = new HashMap<>();
    
    /**
     * 记录插件状态变化
     *
     * @param pluginId 插件ID
     * @param newState 新状态
     */
    public void recordStateChange(String pluginId, PluginState newState) {
        Optional<PluginInfo> optPluginInfo = pluginRegistry.getPlugin(pluginId);
        if (optPluginInfo.isEmpty()) {
            log.warn("无法记录状态变化，插件不存在: {}", pluginId);
            return;
        }
        
        PluginInfo pluginInfo = optPluginInfo.get();
        PluginState oldState = lastStates.getOrDefault(pluginId, null);
        
        // 更新插件状态
        pluginInfo.setState(newState);
        pluginRegistry.updatePlugin(pluginInfo);
        
        // 缓存新状态
        lastStates.put(pluginId, newState);
        
        // 发布状态变化事件
        publishStateChangeEvent(pluginInfo, oldState, newState);
        
        log.info("插件状态已变更: {}, {} -> {}", pluginId, oldState, newState);
    }
    
    /**
     * 发布状态变化事件
     *
     * @param pluginInfo 插件信息
     * @param oldState 旧状态
     * @param newState 新状态
     */
    private void publishStateChangeEvent(PluginInfo pluginInfo, PluginState oldState, PluginState newState) {
        PluginStateChangeEvent event = new PluginStateChangeEvent(
                pluginInfo.getDescriptor().getPluginId(),
                oldState,
                newState
        );
        
        // 添加详细的调试日志
        log.debug("创建事件: {}", event);
        log.debug("事件详情 - 类名: {}, 插件ID: {}, 旧状态: {}, 新状态: {}", 
                event.getClass().getName(),
                event.getPluginId(), 
                event.getOldState(), 
                event.getNewState());
        log.debug("事件类加载器: {}", event.getClass().getClassLoader());
        
        eventBus.postEvent(event);
    }
    
    /**
     * 获取插件当前状态
     *
     * @param pluginId 插件ID
     * @return 插件状态
     */
    public Optional<PluginState> getPluginState(String pluginId) {
        return pluginRegistry.getPlugin(pluginId)
                .map(PluginInfo::getState);
    }
    
    /**
     * 检查插件是否处于特定状态
     *
     * @param pluginId 插件ID
     * @param state 状态
     * @return 是否处于该状态
     */
    public boolean isInState(String pluginId, PluginState state) {
        return getPluginState(pluginId)
                .map(currentState -> currentState == state)
                .orElse(false);
    }
    
    /**
     * 获取处于特定状态的插件列表
     *
     * @param state 状态
     * @return 插件ID列表
     */
    public List<String> getPluginsInState(PluginState state) {
        List<String> result = new ArrayList<>();
        
        for (PluginInfo pluginInfo : pluginRegistry.getAllPlugins()) {
            if (pluginInfo.getState() == state) {
                result.add(pluginInfo.getDescriptor().getPluginId());
            }
        }
        
        return result;
    }
    
    /**
     * 获取失败的插件列表
     *
     * @return 失败的插件ID列表
     */
    public List<String> getFailedPlugins() {
        List<String> result = new ArrayList<>();
        
        for (PluginInfo pluginInfo : pluginRegistry.getAllPlugins()) {
            PluginState state = pluginInfo.getState();
            if (state == PluginState.FAILED || state == PluginState.DEPENDENCY_FAILED) {
                result.add(pluginInfo.getDescriptor().getPluginId());
            }
        }
        
        return result;
    }

    /**
 * 记录插件失败状态和错误信息
 *
 * @param pluginId 插件ID
 * @param errorMessage 错误信息
 */
public void recordFailure(String pluginId, String errorMessage) {
    // 记录状态变化
    recordStateChange(pluginId, PluginState.ERROR);
    
    // 获取插件信息并设置错误信息
    pluginRegistry.getPlugin(pluginId).ifPresent(pluginInfo -> {
        pluginInfo.setErrorMessage(errorMessage);
        pluginRegistry.updatePlugin(pluginInfo);
    });
    
    // 记录到日志
    log.error("插件[{}]失败: {}", pluginId, errorMessage);
}
    
    /**
     * 清除插件状态记录
     *
     * @param pluginId 插件ID
     */
    public void clearStateRecord(String pluginId) {
        lastStates.remove(pluginId);
    }
    
    /**
     * 清除所有状态记录
     */
    public void clearAllStateRecords() {
        lastStates.clear();
    }
    
    /**
     * 插件状态变更事件
     */
    public static class PluginStateChangeEvent extends com.xiaoqu.qteamos.core.plugin.event.Event {
        private final String pluginId;
        private final PluginState oldState;
        private final PluginState newState;
        
        public PluginStateChangeEvent(String pluginId, PluginState oldState, PluginState newState) {
            super("plugin", "state_change");
            this.pluginId = pluginId;
            this.oldState = oldState;
            this.newState = newState;
            // 将状态信息放入data字段
            Map<String, Object> data = new HashMap<>();
            data.put("pluginId", pluginId);
            data.put("oldState", oldState);
            data.put("newState", newState);
            setData(data);
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public PluginState getOldState() {
            return oldState;
        }
        
        public PluginState getNewState() {
            return newState;
        }
    }
} 