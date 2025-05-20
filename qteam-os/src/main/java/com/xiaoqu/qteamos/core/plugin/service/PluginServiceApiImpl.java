/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:18:12
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-06 04:10:35
 * @FilePath: /QTeam/qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/service/PluginServiceApiImpl.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.service;

import com.xiaoqu.qteamos.core.plugin.error.PluginErrorHandler;
import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.DataServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.ConfigServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.StorageServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.LogServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.EventServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.UiServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.ExtensionServiceApi;
import com.xiaoqu.qteamos.api.core.plugin.api.SystemInfo;
import com.xiaoqu.qteamos.api.core.plugin.api.PluginApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 插件服务API实现类
 * 整合错误处理和API服务
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Component
public class PluginServiceApiImpl implements PluginServiceApi, InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PluginServiceApiImpl.class);

    @Autowired
    private PluginLifecycleManager lifecycleManager;

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private PluginErrorHandler errorHandler;

    @Autowired
    private DataServiceApiImpl dataService;

    @Autowired
    private ConfigServiceApiImpl configService;

    @Autowired
    private StorageServiceApiImpl storageService;

    /**
     * 当前插件ID
     */
    private final ThreadLocal<String> currentPluginId = new ThreadLocal<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        // 注册API实例
        PluginApi.init(this);
        log.info("插件服务API初始化完成");
    }

    /**
     * 设置当前线程的插件ID
     *
     * @param pluginId 插件ID
     */
    public void setCurrentPluginId(String pluginId) {
        currentPluginId.set(pluginId);
    }

    /**
     * 清除当前线程的插件ID
     */
    public void clearCurrentPluginId() {
        currentPluginId.remove();
    }

    @Override
    public String getPluginId() {
        return getCurrentPluginId();
    }

    @Override
    public String getPluginVersion() {
        return lifecycleManager.getPluginInfo(getCurrentPluginId())
                .map(info -> info.getDescriptor().getVersion())
                .orElse("unknown");
    }

    @Override
    public DataServiceApi getDataService() {
        return dataService;
    }

    @Override
    public ConfigServiceApi getConfigService() {
        return configService;
    }

    @Override
    public StorageServiceApi getStorageService() {
        return storageService;
    }

    @Override
    public LogServiceApi getLogService() {
        throw new UnsupportedOperationException("日志服务API尚未实现");
    }

    @Override
    public EventServiceApi getEventService() {
        throw new UnsupportedOperationException("事件服务API尚未实现");
    }

    @Override
    public UiServiceApi getUiService() {
        throw new UnsupportedOperationException("UI服务API尚未实现");
    }

    @Override
    public ExtensionServiceApi getExtensionService() {
        throw new UnsupportedOperationException("扩展点服务API尚未实现");
    }

    @Override
    public SystemInfo getSystemInfo() {
        // 创建系统信息对象
        return SystemInfo.builder()
                .name("QTeamOS")
                .version("1.0.0")
                .buildTime("2025-05-01")
                .environment(System.getProperty("spring.profiles.active", "prod"))
                .deployMode("Standard")
                .uptime(ManagementFactory.getRuntimeMXBean().getUptime())
                .memoryInfo(SystemInfo.MemoryInfo.builder()
                        .allocatedMemory(Runtime.getRuntime().totalMemory() / (1024 * 1024))
                        .usedMemory((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024))
                        .maxMemory(Runtime.getRuntime().maxMemory() / (1024 * 1024))
                        .build())
                .build();
    }

    @Override
    public List<com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo> getAvailablePlugins() {
        List<com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo> result = new ArrayList<>();
        
        // 直接使用注入的pluginRegistry获取插件列表
        for (PluginInfo corePluginInfo : pluginRegistry.getAllPlugins()) {
            result.add(convertToApiPluginInfo(corePluginInfo));
        }
        
        return result;
    }

    @Override
    public <T> Optional<T> invokePluginService(String pluginId, String serviceName, Map<String, Object> params) {
        try {
            // 这里需要实现跨插件服务调用逻辑
            log.info("插件[{}]调用插件[{}]的服务[{}]", getCurrentPluginId(), pluginId, serviceName);
            
            // 目前返回空实现
            return Optional.empty();
        } catch (Exception e) {
            log.error("调用插件服务异常: {}", e.getMessage(), e);
            errorHandler.handlePluginError(getCurrentPluginId(), e, PluginErrorHandler.OperationType.RUNTIME);
            return Optional.empty();
        }
    }

    /**
     * 获取当前插件ID
     *
     * @return 当前插件ID
     */
    private String getCurrentPluginId() {
        String pluginId = currentPluginId.get();
        if (pluginId == null) {
            throw new IllegalStateException("未设置当前插件ID，请确保在插件上下文中调用此方法");
        }
        return pluginId;
    }

    /**
     * 转换插件信息为API格式
     *
     * @param corePluginInfo 核心插件信息
     * @return API插件信息
     */
    private com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo convertToApiPluginInfo(PluginInfo corePluginInfo) {
        return com.xiaoqu.qteamos.api.core.plugin.api.PluginInfo.builder()
                .pluginId(corePluginInfo.getPluginId())
                .name(corePluginInfo.getName())
                .version(corePluginInfo.getVersion())
                .description(corePluginInfo.getDescriptor().getDescription())
                .author(corePluginInfo.getDescriptor().getAuthor())
                .type(corePluginInfo.getDescriptor().getType())
                .enabled(corePluginInfo.isEnabled())
                .state(corePluginInfo.getState().name())
                .loadTime(corePluginInfo.getLoadTime())
                .build();
    }
} 