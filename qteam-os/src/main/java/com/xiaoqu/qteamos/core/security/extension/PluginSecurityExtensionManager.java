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

/**
 * 插件安全扩展管理器
 * 负责收集和应用各插件提供的安全配置扩展
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Component;

import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.security.plugin.PluginSecurityExtension;

@Component
public class PluginSecurityExtensionManager {
    private static final Logger log = LoggerFactory.getLogger(PluginSecurityExtensionManager.class);
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    // 存储已注册的插件安全扩展
    private final Map<String, PluginSecurityExtension> registeredExtensions = new ConcurrentHashMap<>();
    
    /**
     * 向安全管理器应用所有已注册的插件安全扩展
     */
    public void applyExtensions(HttpSecurity http) {
        for (PluginSecurityExtension extension : registeredExtensions.values()) {
            try {
                log.info("应用插件安全扩展: {}", extension.getPluginId());
                extension.configure(http);
            } catch (Exception e) {
                log.error("应用插件安全扩展失败: " + extension.getPluginId(), e);
            }
        }
    }
    
    /**
     * 获取所有已注册的安全扩展
     */
    public List<PluginSecurityExtension> getAllExtensions() {
        return new ArrayList<>(registeredExtensions.values());
    }
    
    /**
     * 注册插件安全扩展
     */
    public void registerExtension(PluginSecurityExtension extension) {
        if (extension == null || extension.getPluginId() == null) {
            return;
        }
        
        registeredExtensions.put(extension.getPluginId(), extension);
        log.info("注册插件安全扩展: {}", extension.getPluginId());
    }
    
    /**
     * 注销插件安全扩展
     */
    public void unregisterExtension(String pluginId) {
        if (pluginId == null) {
            return;
        }
        
        registeredExtensions.remove(pluginId);
        log.info("注销插件安全扩展: {}", pluginId);
    }
    
    /**
     * 插件事件监听
     * 自动检测和注册插件的安全扩展
     */
    @EventListener
    public void onPluginEvent(PluginEvent event) {
        // 只处理插件加载事件
        if (!PluginEvent.TYPE_LOADED.equals(event.getType()) && 
            !PluginEvent.TYPE_STARTED.equals(event.getType())) {
            return;
        }
        
        String pluginId = event.getPluginId();
        
        // 从插件注册表中获取插件信息
        Optional<PluginInfo> pluginOpt = pluginRegistry.getPlugin(pluginId);
        if (!pluginOpt.isPresent() || pluginOpt.get().getState() != PluginState.RUNNING) {
            return;
        }
        
        PluginInfo plugin = pluginOpt.get();
        
        try {
            // 尝试从插件中获取安全扩展
            Object pluginInstance = plugin.getPluginInstance();
            if (pluginInstance instanceof PluginSecurityExtension) {
                registerExtension((PluginSecurityExtension) pluginInstance);
            } else {
                // 尝试通过反射获取安全扩展
                try {
                    java.lang.reflect.Method getSecurityExtensionMethod = 
                            pluginInstance.getClass().getDeclaredMethod("getSecurityExtension");
                    if (getSecurityExtensionMethod != null) {
                        getSecurityExtensionMethod.setAccessible(true);
                        Object result = getSecurityExtensionMethod.invoke(pluginInstance);
                        if (result instanceof PluginSecurityExtension) {
                            registerExtension((PluginSecurityExtension) result);
                        }
                    }
                } catch (NoSuchMethodException e) {
                    // 插件没有提供此方法，忽略
                } catch (Exception e) {
                    log.warn("获取插件安全扩展失败: {}", plugin.getPluginId(), e);
                }
            }
        } catch (Exception e) {
            log.error("处理插件安全扩展失败: " + plugin.getPluginId(), e);
        }
    }
    
    /**
     * 插件卸载事件监听
     */
    @EventListener
    public void onPluginUnloaded(PluginEvent event) {
        // 只处理插件卸载事件
        if (PluginEvent.TYPE_UNLOADED.equals(event.getType())) {
            unregisterExtension(event.getPluginId());
        }
    }
} 