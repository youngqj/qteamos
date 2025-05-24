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

package com.xiaoqu.qteamos.sdk.plugin;

import com.xiaoqu.qteamos.sdk.cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件上下文适配器
 * 将API层的PluginContext适配为SDK层的PluginContext
 *
 * @author yangqijun
 * @date 2025-01-20
 * @since 1.0.0
 */
public class PluginContextAdapter implements PluginContext {
    
    private static final Logger log = LoggerFactory.getLogger(PluginContextAdapter.class);
    
    private final com.xiaoqu.qteamos.api.core.plugin.PluginContext apiContext;
    private final Map<String, Object> pluginStatus = new HashMap<>();
    
    public PluginContextAdapter(com.xiaoqu.qteamos.api.core.plugin.PluginContext apiContext) {
        this.apiContext = apiContext;
    }
    
    @Override
    public String getPluginId() {
        return apiContext.getPluginId();
    }
    
    @Override
    public String getPluginName() {
        // 如果API层没有，返回插件ID作为名称
        return apiContext.getPluginId();
    }
    
    @Override
    public String getPluginVersion() {
        return apiContext.getPluginVersion();
    }
    
    @Override
    public String getDataFolderPath() {
        return apiContext.getDataFolderPath();
    }
    
    @Override
    public String getPluginConfig(String key) {
        return apiContext.getConfig(key);
    }
    
    @Override
    public String getPluginConfig(String key, String defaultValue) {
        return apiContext.getConfig(key, defaultValue);
    }
    
    @Override
    public void setPluginConfig(String key, String value) {
        apiContext.setConfig(key, value);
    }
    
    @Override
    public <T> T getService(Class<T> serviceClass) {
        return apiContext.getService(serviceClass);
    }
    
    @Override
    public CacheService getCacheService() {
        return getService(CacheService.class);
    }
    
    @Override
    public void publishEvent(Object event) {
        apiContext.publishEvent(event);
    }
    
    @Override
    public Map<String, Object> getPluginStatus() {
        return new HashMap<>(pluginStatus);
    }
    
    @Override
    public void setPluginStatus(String key, Object value) {
        pluginStatus.put(key, value);
    }
    
    @Override
    public Object getPluginStatus(String key) {
        return pluginStatus.get(key);
    }
} 