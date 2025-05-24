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

package com.xiaoqu.qteamos.core.plugin;

import com.xiaoqu.qteamos.api.core.plugin.Plugin;
import com.xiaoqu.qteamos.api.core.plugin.exception.PluginLifecycleException;
import com.xiaoqu.qteamos.core.plugin.coordinator.PluginSystemCoordinator;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * 插件系统入口（已弃用）
 * 此类已重构为委托模式，实际功能由PluginSystemCoordinator提供
 * 保留此类仅为向后兼容性考虑
 *
 * @author yangqijun
 * @date 2024-07-02
 * @deprecated 请使用 {@link PluginSystemCoordinator} 替代
 */
@Component
@Deprecated
public class PluginSystem {
    private static final Logger log = LoggerFactory.getLogger(PluginSystem.class);
    
    private final PluginSystemCoordinator coordinator;
    
    @Autowired
    public PluginSystem(PluginSystemCoordinator coordinator) {
        this.coordinator = coordinator;
        log.warn("PluginSystem类已弃用，请使用PluginSystemCoordinator。此类将在未来版本中移除。");
    }
    
    /**
     * 初始化插件系统
     * @deprecated 使用 {@link PluginSystemCoordinator#initExistingPlugins()} 和 
     *             {@link PluginSystemCoordinator#startMonitoring()} 替代
     */
    @Deprecated
    public void init() {
        log.warn("PluginSystem.init()已弃用，委托给PluginSystemCoordinator");
        coordinator.init();
    }
    
    /**
     * 定期扫描插件
     * @deprecated 由PluginSystemCoordinator自动处理
     */
    @Deprecated
    @Scheduled(fixedDelayString = "${plugin.scan-interval:300000}")
    public void scheduledScan() {
        // 现在由PluginSystemCoordinator的PluginScanner组件自动处理
        log.debug("定期扫描现在由PluginSystemCoordinator的PluginScanner组件处理");
    }
    
    /**
     * 扫描新插件
     * @deprecated 由PluginSystemCoordinator自动处理
     */
    @Deprecated
    public void scanForNewPlugins() {
        log.warn("scanForNewPlugins()已弃用，现在由PluginSystemCoordinator自动处理");
    }
    
    /**
     * 扫描并加载插件
     * @deprecated 使用 {@link PluginSystemCoordinator#initExistingPlugins()} 替代
     */
    @Deprecated
    public void scanAndLoadPlugins() {
        log.warn("scanAndLoadPlugins()已弃用，委托给PluginSystemCoordinator.initExistingPlugins()");
        coordinator.initExistingPlugins();
    }
    
    /**
     * 加载插件
     * @param jarPath 插件JAR文件路径
     * @return 插件ID
     * @throws Exception 加载异常
     * @deprecated 使用 {@link PluginSystemCoordinator#loadPlugin(Path)} 替代
     */
    @Deprecated
    public String loadPlugin(Path jarPath) throws Exception {
        log.warn("loadPlugin()已弃用，委托给PluginSystemCoordinator");
        try {
            return coordinator.loadPlugin(jarPath);
        } catch (PluginLifecycleException e) {
            throw new Exception(e);
        }
    }
    
    /**
     * 卸载插件
     * @param pluginId 插件ID
     * @return 是否成功卸载
     * @deprecated 使用 {@link PluginSystemCoordinator#unloadPlugin(String)} 替代
     */
    @Deprecated
    public boolean unloadPlugin(String pluginId) {
        log.warn("unloadPlugin()已弃用，委托给PluginSystemCoordinator");
        return coordinator.unloadPlugin(pluginId);
    }
    
    /**
     * 重新加载插件
     * @param pluginId 插件ID
     * @return 是否成功重新加载
     * @deprecated 使用 {@link PluginSystemCoordinator#reloadPlugin(String)} 替代
     */
    @Deprecated
    public boolean reloadPlugin(String pluginId) {
        log.warn("reloadPlugin()已弃用，委托给PluginSystemCoordinator");
        return coordinator.reloadPlugin(pluginId);
    }
    
    /**
     * 安装插件
     * @param jarFile 插件JAR文件
     * @return 是否成功安装
     * @deprecated PluginInstaller组件自动处理插件安装
     */
    @Deprecated
    public boolean installPlugin(File jarFile) {
        log.warn("installPlugin()已弃用，现在由PluginInstaller组件自动处理");
        // 现在由PluginInstaller组件通过文件监控自动处理
        return true;
    }
    
    /**
     * 卸载插件
     * @param pluginId 插件ID
     * @return 是否成功卸载
     * @deprecated 使用 {@link PluginSystemCoordinator#unloadPlugin(String)} 替代
     */
    @Deprecated
    public boolean uninstallPlugin(String pluginId) {
        log.warn("uninstallPlugin()已弃用，委托给PluginSystemCoordinator.unloadPlugin()");
        return coordinator.unloadPlugin(pluginId);
    }
    
    /**
     * 启用插件
     * @param pluginId 插件ID
     * @return 是否成功启用
     * @deprecated 使用 {@link PluginSystemCoordinator#enablePlugin(String)} 替代
     */
    @Deprecated
    public boolean enablePlugin(String pluginId) {
        log.warn("enablePlugin()已弃用，委托给PluginSystemCoordinator");
        return coordinator.enablePlugin(pluginId);
    }
    
    /**
     * 禁用插件
     * @param pluginId 插件ID
     * @return 是否成功禁用
     * @deprecated 使用 {@link PluginSystemCoordinator#disablePlugin(String)} 替代
     */
    @Deprecated
    public boolean disablePlugin(String pluginId) {
        log.warn("disablePlugin()已弃用，委托给PluginSystemCoordinator");
        return coordinator.disablePlugin(pluginId);
    }
    
    /**
     * 获取所有插件
     * @return 所有插件信息
     * @deprecated 使用 {@link PluginSystemCoordinator#getAllPlugins()} 替代
     */
    @Deprecated
    public Collection<PluginInfo> getAllPlugins() {
        return coordinator.getAllPlugins();
    }
    
    /**
     * 获取已启用的插件
     * @return 已启用的插件信息
     * @deprecated 使用 {@link PluginSystemCoordinator#getEnabledPlugins()} 替代
     */
    @Deprecated
    public Collection<PluginInfo> getEnabledPlugins() {
        return coordinator.getEnabledPlugins();
    }
    
    /**
     * 获取已禁用的插件
     * @return 已禁用的插件信息
     * @deprecated 使用 {@link PluginSystemCoordinator#getDisabledPlugins()} 替代
     */
    @Deprecated
    public Collection<PluginInfo> getDisabledPlugins() {
        return coordinator.getDisabledPlugins();
    }
    
    /**
     * 获取插件信息
     * @param pluginId 插件ID
     * @return 插件信息
     * @deprecated 使用 {@link PluginSystemCoordinator#getPlugin(String)} 替代
     */
    @Deprecated
    public Optional<PluginInfo> getPlugin(String pluginId) {
        return coordinator.getPlugin(pluginId);
    }
    
    /**
     * 获取插件实例
     * @param pluginId 插件ID
     * @return 插件实例
     * @deprecated 使用 {@link PluginSystemCoordinator#getPluginInstance(String)} 替代
     */
    @Deprecated
    public Optional<Plugin> getPluginInstance(String pluginId) {
        return coordinator.getPluginInstance(pluginId);
    }
    
    /**
     * 关闭插件系统
     * @deprecated 由PluginSystemCoordinator自动处理
     */
    @Deprecated
    @PreDestroy
    public void shutdown() {
        log.warn("PluginSystem.shutdown()已弃用，委托给PluginSystemCoordinator");
        coordinator.shutdown();
    }
} 