/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-02 08:50:29
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 17:09:10
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/cache/DefaultCacheService.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
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
 * 默认缓存服务实现
 * 提供基础的缓存服务功能实现
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认缓存服务实现
 * 提供基于内存和Redis的混合缓存实现
 */
@Service
public class DefaultCacheService implements CacheService {
    
    private static final Logger log = LoggerFactory.getLogger(DefaultCacheService.class);
    
    /**
     * Redis连接工厂
     */
    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;
    
    /**
     * Redis模板
     */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 是否启用Redis
     */
    @Value("${qteamos.data.redis-enabled:false}")
    private boolean redisEnabled;
    
    /**
     * 内存缓存
     */
    private final Map<String, Object> localCache = new ConcurrentHashMap<>();
    
    /**
     * 是否已初始化
     */
    private boolean initialized = false;
    
    /**
     * 初始化缓存服务
     */
    @Override
    public void initialize() {
        if (initialized) {
            log.info("缓存服务已经初始化，跳过重复初始化");
            return;
        }
        
        log.info("初始化缓存服务...");
        
        // 检查Redis是否可用
        if (redisEnabled && redisConnectionFactory != null) {
            try {
                log.info("检查Redis连接...");
                redisConnectionFactory.getConnection().ping();
                log.info("Redis连接成功");
            } catch (Exception e) {
                log.error("Redis连接失败，将仅使用本地缓存", e);
                redisEnabled = false;
            }
        } else {
            log.info("未启用Redis或未配置Redis连接，将仅使用本地缓存");
            redisEnabled = false;
        }
        
        initialized = true;
        log.info("缓存服务初始化完成，使用模式: {}", redisEnabled ? "Redis + 本地缓存" : "仅本地缓存");
    }
    
    /**
     * 关闭缓存服务
     */
    @Override
    public void shutdown() {
        log.info("关闭缓存服务...");
        localCache.clear();
        initialized = false;
        log.info("缓存服务已关闭");
    }
    
    /**
     * 检查缓存服务健康状态
     *
     * @return 如果缓存服务运行正常则返回true
     */
    @Override
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }
        
        if (redisEnabled) {
            try {
                redisConnectionFactory.getConnection().ping();
                return true;
            } catch (Exception e) {
                log.error("Redis健康检查失败", e);
                return false;
            }
        }
        
        return true; // 本地缓存总是健康的
    }
    
    /**
     * 获取缓存服务状态信息
     *
     * @return 缓存服务状态信息
     */
    @Override
    public String getStatus() {
        if (!initialized) {
            return "未初始化";
        }
        
        StringBuilder status = new StringBuilder();
        
        status.append("本地缓存: 正常，当前大小: ").append(localCache.size()).append(" 项");
        
        if (redisEnabled) {
            try {
                long dbSize = redisTemplate.getConnectionFactory().getConnection().dbSize();
                status.append("，Redis: 正常，当前大小: ").append(dbSize).append(" 项");
            } catch (Exception e) {
                status.append("，Redis: 连接异常 - ").append(e.getMessage());
            }
        } else {
            status.append("，Redis: 未启用");
        }
        
        return status.toString();
    }
} 