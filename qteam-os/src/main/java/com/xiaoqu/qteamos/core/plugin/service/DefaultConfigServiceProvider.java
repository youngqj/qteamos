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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.xiaoqu.qteamos.core.plugin.model.entity.SysPluginConfig;
import com.xiaoqu.qteamos.core.plugin.model.mapper.SysPluginConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认插件配置服务提供者实现
 * 负责插件配置的持久化、读取和管理
 *
 * @author yangqijun
 * @date 2025-06-10
 * @since 1.0.0
 */
@Component
public class DefaultConfigServiceProvider implements ConfigServiceProvider {
    private static final Logger log = LoggerFactory.getLogger(DefaultConfigServiceProvider.class);
    
    @Autowired
    private SysPluginConfigMapper configMapper;
    
    // 配置缓存，避免频繁数据库访问
    private final Map<String, Map<String, String>> configCache = new ConcurrentHashMap<>();
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePluginConfig(String pluginId, String key, String value) {
        if (pluginId == null || pluginId.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        
        try {
            // 查询配置是否已存在
            SysPluginConfig existingConfig = configMapper.selectOne(
                    new LambdaQueryWrapper<SysPluginConfig>()
                            .eq(SysPluginConfig::getPluginId, pluginId)
                            .eq(SysPluginConfig::getConfigKey, key));
            
            if (existingConfig != null) {
                // 更新现有配置
                existingConfig.setConfigValue(value);
                existingConfig.setUpdateTime(LocalDateTime.now());
                configMapper.updateById(existingConfig);
            } else {
                // 新增配置
                SysPluginConfig config = new SysPluginConfig();
                config.setPluginId(pluginId);
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                configMapper.insert(config);
            }
            
            // 更新缓存
            updateCache(pluginId, key, value);
            
            log.debug("保存插件配置: {}:{} = {}", pluginId, key, value);
            return true;
        } catch (Exception e) {
            log.error("保存插件配置失败: {}:{} = {}, 错误: {}", pluginId, key, value, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean savePluginConfig(String pluginId, Map<String, String> configs) {
        if (pluginId == null || pluginId.isEmpty() || configs == null || configs.isEmpty()) {
            return false;
        }
        
        try {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                savePluginConfig(pluginId, entry.getKey(), entry.getValue());
            }
            
            log.debug("保存插件配置(批量): {}, 配置项数量: {}", pluginId, configs.size());
            return true;
        } catch (Exception e) {
            log.error("保存插件配置(批量)失败: {}, 错误: {}", pluginId, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getPluginConfig(String pluginId, String key) {
        if (pluginId == null || pluginId.isEmpty() || key == null || key.isEmpty()) {
            return null;
        }
        
        // 先从缓存中获取
        Map<String, String> pluginConfigs = getPluginConfigsFromCache(pluginId);
        if (pluginConfigs.containsKey(key)) {
            return pluginConfigs.get(key);
        }
        
        // 缓存未命中，从数据库获取
        try {
            SysPluginConfig config = configMapper.selectOne(
                    new LambdaQueryWrapper<SysPluginConfig>()
                            .eq(SysPluginConfig::getPluginId, pluginId)
                            .eq(SysPluginConfig::getConfigKey, key));
            
            if (config != null) {
                // 更新缓存
                updateCache(pluginId, key, config.getConfigValue());
                return config.getConfigValue();
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取插件配置失败: {}:{}, 错误: {}", pluginId, key, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public String getPluginConfig(String pluginId, String key, String defaultValue) {
        String value = getPluginConfig(pluginId, key);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public Map<String, String> getPluginConfigs(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return new HashMap<>();
        }
        
        // 检查缓存
        Map<String, String> cachedConfigs = configCache.get(pluginId);
        if (cachedConfigs != null) {
            return new HashMap<>(cachedConfigs);
        }
        
        // 从数据库加载所有配置
        try {
            List<SysPluginConfig> configs = configMapper.selectList(
                    new LambdaQueryWrapper<SysPluginConfig>()
                            .eq(SysPluginConfig::getPluginId, pluginId));
            
            Map<String, String> result = new HashMap<>();
            for (SysPluginConfig config : configs) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
            
            // 更新缓存
            configCache.put(pluginId, new ConcurrentHashMap<>(result));
            
            log.debug("获取插件所有配置: {}, 配置项数量: {}", pluginId, result.size());
            return result;
        } catch (Exception e) {
            log.error("获取插件所有配置失败: {}, 错误: {}", pluginId, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removePluginConfig(String pluginId, String key) {
        if (pluginId == null || pluginId.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        
        try {
            int deleted = configMapper.delete(
                    new LambdaQueryWrapper<SysPluginConfig>()
                            .eq(SysPluginConfig::getPluginId, pluginId)
                            .eq(SysPluginConfig::getConfigKey, key));
            
            // 更新缓存
            removeFromCache(pluginId, key);
            
            log.debug("删除插件配置: {}:{}, 结果: {}", pluginId, key, deleted > 0);
            return deleted > 0;
        } catch (Exception e) {
            log.error("删除插件配置失败: {}:{}, 错误: {}", pluginId, key, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearPluginConfigs(String pluginId) {
        if (pluginId == null || pluginId.isEmpty()) {
            return false;
        }
        
        try {
            int deleted = configMapper.delete(
                    new LambdaQueryWrapper<SysPluginConfig>()
                            .eq(SysPluginConfig::getPluginId, pluginId));
            
            // 清除缓存
            configCache.remove(pluginId);
            
            log.debug("清除插件所有配置: {}, 删除数量: {}", pluginId, deleted);
            return true;
        } catch (Exception e) {
            log.error("清除插件所有配置失败: {}, 错误: {}", pluginId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 从缓存中获取插件配置
     *
     * @param pluginId 插件ID
     * @return 配置映射
     */
    private Map<String, String> getPluginConfigsFromCache(String pluginId) {
        return configCache.computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * 更新配置缓存
     *
     * @param pluginId 插件ID
     * @param key 配置键
     * @param value 配置值
     */
    private void updateCache(String pluginId, String key, String value) {
        Map<String, String> configs = getPluginConfigsFromCache(pluginId);
        configs.put(key, value);
    }
    
    /**
     * 从缓存中移除配置
     *
     * @param pluginId 插件ID
     * @param key 配置键
     */
    private void removeFromCache(String pluginId, String key) {
        Map<String, String> configs = configCache.get(pluginId);
        if (configs != null) {
            configs.remove(key);
        }
    }
} 