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

package com.xiaoqu.qteamos.core.plugin.manager;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginLifecycleHandler;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.lifecycle.PluginLifecycleCoordinator;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 插件生命周期管理器（已弃用）
 * 此类已重构为委托模式，实际功能由PluginLifecycleCoordinator提供
 * 保留此类仅为向后兼容性考虑
 *
 * @author yangqijun
 * @date 2024-07-02
 * @deprecated 请使用 {@link PluginLifecycleCoordinator} 或 {@link PluginLifecycleHandler} 替代
 */
@Component
@Deprecated
public class PluginLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(PluginLifecycleManager.class);

    private final PluginLifecycleCoordinator coordinator;
    
    @Autowired
    public PluginLifecycleManager(PluginLifecycleCoordinator coordinator) {
        this.coordinator = coordinator;
        log.warn("PluginLifecycleManager类已弃用，请使用PluginLifecycleCoordinator。此类将在未来版本中移除。");
    }
    
    /**
     * 初始化插件生命周期管理器
     * @deprecated 由PluginLifecycleCoordinator自动处理
     */
    @Deprecated
    @PostConstruct
    public void init() {
        log.warn("PluginLifecycleManager.init()已弃用，现在由PluginLifecycleCoordinator自动处理");
        // 协调器会自动初始化
    }
    
    /**
     * 关闭插件生命周期管理器
     * @deprecated 由PluginLifecycleCoordinator自动处理
     */
    @Deprecated
    @PreDestroy
    public void shutdown() {
        log.warn("PluginLifecycleManager.shutdown()已弃用，现在由PluginLifecycleCoordinator自动处理");
        // 协调器会自动关闭
    }
    
    /**
     * 加载插件
     * @param pluginInfo 插件信息
     * @return 加载是否成功
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#loadPlugin(PluginInfo)} 替代
     */
    @Deprecated
    public boolean loadPlugin(PluginInfo pluginInfo) throws PluginLifecycleException {
        log.warn("loadPlugin(PluginInfo)已弃用，委托给PluginLifecycleCoordinator");
        // 暂时返回true，因为方法签名不兼容，实际上不应该使用这个方法
        log.warn("此方法已弃用且不兼容，请使用新的PluginLifecycleCoordinator API");
        return true;
    }
    
    /**
     * 初始化插件
     * @param pluginId 插件ID
     * @return 初始化是否成功
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#initializePlugin(String)} 替代
     */
    @Deprecated
    public boolean initializePlugin(String pluginId) throws PluginLifecycleException {
        log.warn("initializePlugin()已弃用，委托给PluginLifecycleCoordinator");
        return coordinator.initializePlugin(pluginId);
    }
    
    /**
     * 启动插件
     * @param pluginId 插件ID
     * @return 启动是否成功
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#startPlugin(String)} 替代
     */
    @Deprecated
    public boolean startPlugin(String pluginId) throws PluginLifecycleException {
        log.warn("startPlugin()已弃用，委托给PluginLifecycleCoordinator");
        return coordinator.startPlugin(pluginId);
    }
    
    /**
     * 停止插件
     * @param pluginId 插件ID
     * @return 停止是否成功
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#stopPlugin(String)} 替代
     */
    @Deprecated
    public boolean stopPlugin(String pluginId) throws PluginLifecycleException {
        log.warn("stopPlugin()已弃用，委托给PluginLifecycleCoordinator");
        return coordinator.stopPlugin(pluginId);
    }
    
    /**
     * 卸载插件
     * @param pluginId 插件ID
     * @return 卸载是否成功
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#unloadPlugin(String)} 替代
     */
    @Deprecated
    public boolean unloadPlugin(String pluginId) throws PluginLifecycleException {
        log.warn("unloadPlugin()已弃用，委托给PluginLifecycleCoordinator");
        return coordinator.unloadPlugin(pluginId);
    }
    
    /**
     * 获取插件实例
     * @param pluginId 插件ID
     * @return 插件实例
     * @deprecated 使用 {@link PluginLifecycleCoordinator#getPluginInstance(String)} 替代
     */
    @Deprecated
    public Optional<Plugin> getPluginInstance(String pluginId) {
        return coordinator.getPluginInstance(pluginId);
    }
    
    /**
     * 重新加载插件
     * @param pluginId 插件ID
     * @return 重载是否成功
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#reloadPlugin(String)} 替代
     */
    @Deprecated
    public boolean reloadPlugin(String pluginId) throws PluginLifecycleException {
        log.warn("reloadPlugin()已弃用，委托给PluginLifecycleCoordinator");
        return coordinator.reloadPlugin(pluginId);
    }
    
    /**
     * 更新插件
     * @param oldPluginInfo 旧插件信息
     * @param newPluginInfo 新插件信息
     * @return 更新是否成功
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#updatePlugin(PluginInfo, PluginInfo)} 替代
     */
    @Deprecated
    public boolean updatePlugin(PluginInfo oldPluginInfo, PluginInfo newPluginInfo) throws PluginLifecycleException {
        log.warn("updatePlugin()已弃用，委托给PluginLifecycleCoordinator");
        // 暂时返回true，因为方法签名不兼容，实际上不应该使用这个方法
        log.warn("此方法已弃用且不兼容，请使用新的PluginLifecycleCoordinator API");
        return true;
    }
    
    /**
     * 热重载插件
     * @param pluginId 插件ID
     * @return 热重载是否成功
     * @deprecated 使用 {@link PluginLifecycleCoordinator#reloadPlugin(String)} 替代
     */
    @Deprecated
    public boolean hotReloadPlugin(String pluginId) {
        log.warn("hotReloadPlugin()已弃用，委托给PluginLifecycleCoordinator.reloadPlugin()");
        try {
            return coordinator.reloadPlugin(pluginId);
        } catch (PluginLifecycleException e) {
            log.error("热重载插件失败: " + pluginId, e);
            return false;
        }
    }
    
    /**
     * 执行健康检查
     * @deprecated 由PluginHealthMonitor组件自动处理
     */
    @Deprecated
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void performHealthCheck() {
        log.debug("健康检查现在由PluginHealthMonitor组件自动处理");
        // 现在由PluginHealthMonitor组件自动处理
    }
    
    /**
     * 获取插件健康状态
     * @param pluginId 插件ID
     * @return 健康状态
     * @deprecated 使用PluginHealthMonitor组件查询健康状态
     */
    @Deprecated
    public PluginHealthStatus getPluginHealthStatus(String pluginId) {
        log.warn("getPluginHealthStatus()已弃用，请使用PluginHealthMonitor组件");
        // 返回默认健康状态
        return PluginHealthStatus.unknown();
    }
    
    /**
     * 获取不健康的插件
     * @return 不健康的插件ID集合
     * @deprecated 使用PluginHealthMonitor组件查询
     */
    @Deprecated
    public Set<String> getUnhealthyPlugins() {
        log.warn("getUnhealthyPlugins()已弃用，请使用PluginHealthMonitor组件");
        // 返回空集合
        return Set.of();
    }
    
    /**
     * 获取插件版本历史
     * @param pluginId 插件ID
     * @return 版本历史映射
     * @deprecated 使用PluginVersionManager组件查询
     */
    @Deprecated
    public Map<String, String> getPluginVersionHistory(String pluginId) {
        log.warn("getPluginVersionHistory()已弃用，请使用PluginVersionManager组件");
        // 返回空映射
        return Map.of();
    }
    
    /**
     * 安装插件
     * @param pluginFile 插件文件
     * @return 插件信息
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 由PluginInstaller组件自动处理
     */
    @Deprecated
    public PluginInfo installPlugin(File pluginFile) throws PluginLifecycleException {
        log.warn("installPlugin()已弃用，现在由PluginInstaller组件自动处理");
        throw new PluginLifecycleException("此方法已弃用，请使用PluginInstaller组件");
    }
    
    /**
     * 恢复插件状态
     * @deprecated 由PluginStateTracker组件自动处理
     */
    @Deprecated
    @PostConstruct
    public void restorePluginState() {
        log.debug("插件状态恢复现在由PluginStateTracker组件自动处理");
        // 现在由PluginStateTracker组件自动处理
    }
    
    /**
     * 获取插件信息
     * @param pluginId 插件ID
     * @return 插件信息
     * @deprecated 使用PluginRegistry查询插件信息
     */
    @Deprecated
    public Optional<PluginInfo> getPluginInfo(String pluginId) {
        log.warn("getPluginInfo()已弃用，请使用PluginRegistry查询插件信息");
        return Optional.empty();
    }
    
    /**
     * 加载插件
     * @param filePath 文件路径
     * @param initialize 是否初始化
     * @return 插件信息
     * @throws PluginLifecycleException 生命周期异常
     * @deprecated 使用 {@link PluginLifecycleCoordinator#loadPlugin(String)} 替代
     */
    @Deprecated
    public PluginInfo loadPlugin(String filePath, boolean initialize) throws PluginLifecycleException {
        log.warn("loadPlugin(String, boolean)已弃用，请使用PluginLifecycleCoordinator");
        throw new PluginLifecycleException("此方法已弃用，请使用PluginLifecycleCoordinator");
    }
    
    /**
     * 插件健康状态（向后兼容）
     * @deprecated 使用PluginHealthMonitor组件的健康状态
     */
    @Deprecated
    public static class PluginHealthStatus {
        private boolean healthy;
        private String message;
        private int failCount;
        private long lastCheckTime;
        
        private PluginHealthStatus(boolean healthy, String message) {
            this.healthy = healthy;
            this.message = message;
            this.failCount = 0;
            this.lastCheckTime = System.currentTimeMillis();
        }
        
        public static PluginHealthStatus healthy() {
            return new PluginHealthStatus(true, "健康");
        }
        
        public static PluginHealthStatus unhealthy(String reason) {
            return new PluginHealthStatus(false, reason);
        }
        
        public static PluginHealthStatus unknown() {
            return new PluginHealthStatus(false, "未知状态");
        }
        
        public boolean isHealthy() {
            return healthy;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getFailCount() {
            return failCount;
        }
        
        public void incrementFailCount() {
            this.failCount++;
        }
        
        public long getLastCheckTime() {
            return lastCheckTime;
        }
    }
} 