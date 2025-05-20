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

package com.xiaoqu.qteamos.core.gateway.service.impl;

import com.xiaoqu.qteamos.core.gateway.service.GatewayService;
import com.xiaoqu.qteamos.core.plugin.event.Event;
import com.xiaoqu.qteamos.core.plugin.event.EventHandler;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRegistry;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import com.xiaoqu.qteamos.core.plugin.web.PluginRequestMappingHandlerMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关服务实现类
 * 实现网关服务的核心功能
 *
 * @author yangqijun
 * @date 2025-05-03
 * @since 1.0.0
 */
@Service
public class GatewayServiceImpl implements GatewayService, EventHandler {
    private static final Logger log = LoggerFactory.getLogger(GatewayServiceImpl.class);
    
    @Autowired
    private PluginRegistry pluginRegistry;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private PluginRequestMappingHandlerMapping pluginRequestMapping;
    
    @Value("${qteamos.gateway.default-rate-limit:100}")
    private int defaultRateLimit;
    
    // 插件限流配置
    private final Map<String, Integer> pluginRateLimits = new ConcurrentHashMap<>();
    
    // API调用统计
    private final Map<String, Map<String, Integer>> apiCallStatistics = new ConcurrentHashMap<>();
    
    // API响应时间统计
    private final Map<String, List<Long>> apiResponseTimes = new ConcurrentHashMap<>();
    
    @Override
    public void initialize() {
        log.info("初始化网关服务...");
        
        // 注册所有插件的路由 - 底层由PluginRequestMappingHandlerMapping处理
        // 这里只做必要的初始化
        log.info("网关服务初始化完成");
    }
    
    @Override
    public int registerPluginRoutes(String pluginId) {
        log.info("触发插件[{}]的API路由注册", pluginId);
        
        Optional<PluginInfo> pluginOpt = pluginRegistry.getPlugin(pluginId);
        if (pluginOpt.isPresent()) {
            PluginInfo plugin = pluginOpt.get();
            return registerPluginRoutes(plugin);
        } else {
            log.warn("插件[{}]不存在，无法触发路由注册", pluginId);
            return 0;
        }
    }
    
    @Override
    public int registerPluginRoutes(PluginInfo plugin) {
        if (plugin.getState() != PluginState.RUNNING) {
            log.warn("插件[{}]未运行，状态为{}，跳过路由注册", plugin.getPluginId(), plugin.getState());
            return 0;
        }
        
        log.info("触发插件[{}]的API路由注册", plugin.getPluginId());
        
        // 实际注册工作由PluginRequestMappingHandlerMapping和GatewayPathMappingListener处理
        // 这里只返回一个标志值
        return 1;
    }
    
    @Override
    public int unregisterPluginRoutes(String pluginId) {
        log.info("触发插件[{}]的API路由注销", pluginId);
        
        // 实际注销工作由PluginRequestMappingHandlerMapping和GatewayPathMappingListener处理
        // 这里只返回一个标志值
        return 1;
    }
    
    @Override
    public void refreshRoutes() {
        log.info("刷新网关路由配置");
        
        // 获取所有运行中的插件
        Collection<PluginInfo> plugins = pluginRegistry.getAllPlugins().stream()
                .filter(p -> p.getState() == PluginState.RUNNING)
                .toList();
        
        // 实际刷新工作由PluginRequestMappingHandlerMapping处理
        // 这里只记录日志
        log.info("触发网关路由刷新，共{}个插件", plugins.size());
    }
    
    @Override
    public Object getApiStatistics(String pluginId) {
        // 组合返回API调用统计和响应时间统计
        Map<String, Object> stats = new HashMap<>();
        
        // API调用次数统计
        stats.put("calls", apiCallStatistics.getOrDefault(pluginId, new HashMap<>()));
        
        // API响应时间统计
        stats.put("responseTimes", getApiResponseTimeStats(pluginId));
        
        return stats;
    }
    
    @Override
    public void setApiRateLimit(String pluginId, int limitRate) {
        log.info("设置插件[{}]API限流规则为{}次/分钟", pluginId, limitRate);
        pluginRateLimits.put(pluginId, limitRate);
    }
    
    /**
     * 获取插件的限流配置
     *
     * @param pluginId 插件ID
     * @return 限流配置，如果没有配置则返回空
     */
    public Optional<Integer> getPluginRateLimit(String pluginId) {
        return Optional.ofNullable(pluginRateLimits.get(pluginId));
    }
    
    /**
     * 实现EventHandler接口，处理插件版本变更事件
     */
    @Override
    public boolean handle(Event event) {
        if (event instanceof PluginRolloutEvent) {
            PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
            String pluginId = rolloutEvent.getPluginId();
            
            // 处理不同类型的灰度发布事件
            switch (rolloutEvent.getType()) {
                case PluginRolloutEvent.TYPE_COMPLETED:
                    // 灰度发布完成，记录日志
                    log.info("插件[{}]版本从{}更新到{}，可能需要刷新路由配置", 
                           pluginId, rolloutEvent.getFromVersion(), rolloutEvent.getToVersion());
                    break;
                    
                case PluginRolloutEvent.TYPE_FAILED:
                case PluginRolloutEvent.TYPE_CANCELLED:
                    // 灰度发布失败或取消，记录日志
                    log.warn("插件[{}]版本从{}到{}的更新{}，可能需要检查路由", 
                           pluginId, 
                           rolloutEvent.getFromVersion(), 
                           rolloutEvent.getToVersion(),
                           rolloutEvent.getType().equals(PluginRolloutEvent.TYPE_FAILED) ? "失败" : "取消");
                    break;
            }
        }
        return true;
    }
    
    /**
     * 获取关注的事件主题
     */
    @Override
    public String[] getTopics() {
        return new String[]{PluginRolloutEvent.TOPIC};
    }
    
    /**
     * 记录API调用统计
     *
     * @param pluginId 插件ID
     * @param apiPath API路径
     */
    public void recordApiCall(String pluginId, String apiPath) {
        // 获取或创建插件的API统计Map
        Map<String, Integer> pluginStats = apiCallStatistics.computeIfAbsent(pluginId, k -> new ConcurrentHashMap<>());
        
        // 增加API调用计数
        pluginStats.compute(apiPath, (k, v) -> (v == null) ? 1 : v + 1);
    }
    
    /**
     * 记录API响应时间
     *
     * @param pluginId 插件ID
     * @param apiPath API路径
     * @param responseTimeMs 响应时间(毫秒)
     */
    public void recordApiResponseTime(String pluginId, String apiPath, long responseTimeMs) {
        List<Long> times = apiResponseTimes.computeIfAbsent(pluginId, k -> new ArrayList<>());
        synchronized (times) {
            times.add(responseTimeMs);
            // 保留最近100条记录
            if (times.size() > 100) {
                times.remove(0);
            }
        }
    }
    
    /**
     * 获取API响应时间统计
     *
     * @param pluginId 插件ID
     * @return 响应时间统计
     */
    public Map<String, Object> getApiResponseTimeStats(String pluginId) {
        Map<String, Object> stats = new HashMap<>();
        List<Long> times = apiResponseTimes.get(pluginId);
        
        if (times != null && !times.isEmpty()) {
            synchronized (times) {
                long sum = 0;
                long max = Long.MIN_VALUE;
                long min = Long.MAX_VALUE;
                
                for (Long time : times) {
                    sum += time;
                    max = Math.max(max, time);
                    min = Math.min(min, time);
                }
                
                stats.put("avg", sum / times.size());
                stats.put("max", max);
                stats.put("min", min);
                stats.put("count", times.size());
            }
        }
        
        return stats;
    }
    
    /**
     * 网关健康检查
     *
     * @return 健康状态信息
     */
    public Map<String, Object> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        
        // 活跃插件数
        long activePlugins = pluginRegistry.getAllPlugins().stream()
                .filter(p -> p.getState() == PluginState.RUNNING)
                .count();
        health.put("activePlugins", activePlugins);
        
        // 限流规则数
        health.put("rateLimitRules", pluginRateLimits.size());
        
        // API调用总数
        long totalCalls = apiCallStatistics.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToInt(Integer::intValue)
                .sum();
        health.put("totalApiCalls", totalCalls);
        
        return health;
    }
} 