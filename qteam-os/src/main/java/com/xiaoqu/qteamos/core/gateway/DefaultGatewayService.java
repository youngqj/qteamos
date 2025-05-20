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
 * 默认网关服务实现
 * 提供基础的API网关功能实现
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认网关服务实现
 * 提供API路由管理和请求转发功能
 */
@Service
public class DefaultGatewayService implements GatewayService {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultGatewayService.class);
    
    /**
     * 是否启用网关功能
     */
    @Value("${qteamos.gateway.enabled:true}")
    private boolean enabled;
    
    /**
     * API请求前缀
     */
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    /**
     * 是否启用请求限流
     */
    @Value("${qteamos.gateway.enable-rate-limit:false}")
    private boolean enableRateLimit;
    
    /**
     * 是否已初始化
     */
    private boolean initialized = false;
    
    /**
     * 注册的API路由数量
     */
    private final AtomicInteger registeredRouteCount = new AtomicInteger(0);
    
    /**
     * 初始化网关服务
     */
    @Override
    public void initialize() {
        if (initialized) {
            log.info("网关服务已经初始化，跳过重复初始化");
            return;
        }
        
        log.info("初始化网关服务...");
        
        if (!enabled) {
            log.info("网关功能已禁用，服务将以有限功能模式运行");
        } else {
            log.info("网关功能已启用，API前缀: {}", apiPrefix);
            
            if (enableRateLimit) {
                log.info("API请求限流已启用");
            } else {
                log.info("API请求限流已禁用");
            }
        }
        
        initialized = true;
        log.info("网关服务初始化完成");
    }
    
    /**
     * 关闭网关服务
     */
    @Override
    public void shutdown() {
        log.info("关闭网关服务...");
        registeredRouteCount.set(0);
        initialized = false;
        log.info("网关服务已关闭");
    }
    
    /**
     * 检查网关服务健康状态
     *
     * @return 如果网关服务运行正常则返回true
     */
    @Override
    public boolean isHealthy() {
        return initialized;
    }
    
    /**
     * 获取网关服务状态信息
     *
     * @return 网关服务状态信息
     */
    @Override
    public String getStatus() {
        if (!initialized) {
            return "未初始化";
        }
        
        if (!enabled) {
            return "有限功能模式（网关功能已禁用）";
        }
        
        StringBuilder status = new StringBuilder("正常运行");
        status.append("，API前缀: ").append(apiPrefix);
        status.append("，已注册路由: ").append(registeredRouteCount.get()).append(" 个");
        status.append("，请求限流: ").append(enableRateLimit ? "已启用" : "已禁用");
        
        return status.toString();
    }
    
    /**
     * 注册一个API路由
     *
     * @param path API路径
     * @param method HTTP方法
     * @return 是否注册成功
     */
    public boolean registerRoute(String path, String method) {
        if (!initialized || !enabled) {
            return false;
        }
        
        log.info("注册API路由: {} {}", method, path);
        registeredRouteCount.incrementAndGet();
        return true;
    }
    
    /**
     * 注销一个API路由
     *
     * @param path API路径
     * @param method HTTP方法
     * @return 是否注销成功
     */
    public boolean unregisterRoute(String path, String method) {
        if (!initialized || !enabled) {
            return false;
        }
        
        log.info("注销API路由: {} {}", method, path);
        registeredRouteCount.decrementAndGet();
        return true;
    }
} 