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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
     * 缓存清理调度器
     */
    private ScheduledExecutorService cleanupScheduler;
    
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
        
        // 启动定期清理过期缓存的调度器
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "cache-cleanup-thread");
            thread.setDaemon(true);
            return thread;
        });
        
        cleanupScheduler.scheduleAtFixedRate(this::cleanupCache, 
                30, 30, TimeUnit.SECONDS);
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
        
        if (cleanupScheduler != null) {
            cleanupScheduler.shutdownNow();
        }
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
    
    /**
     * 清理过期缓存
     */
    private void cleanupCache() {
        try {
            int cleanedCount = 0;
            
            for (Map.Entry<String, Object> entry : localCache.entrySet()) {
                if (entry.getValue() instanceof CacheEntry && ((CacheEntry<?>) entry.getValue()).isExpired()) {
                    localCache.remove(entry.getKey());
                    cleanedCount++;
                }
            }
            
            if (cleanedCount > 0) {
                log.debug("缓存清理完成，移除了{}个过期条目", cleanedCount);
            }
        } catch (Exception e) {
            log.error("缓存清理过程中发生异常", e);
        }
    }
    
    /**
     * 缓存条目
     *
     * @param <T> 值类型
     */
    private static class CacheEntry<T> {
        /**
         * 缓存值
         */
        private final T value;
        
        /**
         * 过期时间（毫秒时间戳），0表示永不过期
         */
        private final long expireTime;
        
        /**
         * 构造函数
         *
         * @param value 缓存值
         * @param expireTime 过期时间戳
         */
        public CacheEntry(T value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
        
        /**
         * 获取缓存值
         *
         * @return 缓存值
         */
        public T getValue() {
            return value;
        }
        
        /**
         * 获取过期时间
         *
         * @return 过期时间戳
         */
        public long getExpireTime() {
            return expireTime;
        }
        
        /**
         * 检查是否已过期
         *
         * @return 是否已过期
         */
        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }
    }
    
    /**
     * 存储缓存数据
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param expireSeconds 过期时间(秒)，0表示永不过期
     * @param <T> 缓存值类型
     * @return 操作是否成功
     */
    @Override
    public <T> boolean set(String key, T value, int expireSeconds) {
        if (key == null || value == null) {
            return false;
        }
        
        long expireTime = 0;
        if (expireSeconds > 0) {
            expireTime = System.currentTimeMillis() + (expireSeconds * 1000L);
        }
        
        // 存储到本地缓存
        localCache.put(key, new CacheEntry<>(value, expireTime));
        
        // 如果启用了Redis，也存储到Redis中
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(key, value);
                if (expireSeconds > 0) {
                    redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
                }
            } catch (Exception e) {
                log.error("Redis存储缓存失败: key={}", key, e);
            }
        }
        
        return true;
    }
    
    /**
     * 获取缓存数据
     *
     * @param key 缓存键
     * @param clazz 返回值类型Class
     * @param <T> 返回值类型
     * @return 缓存值，不存在则返回null
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return null;
        }
        
        // 先从本地缓存中获取
        Object localValue = localCache.get(key);
        if (localValue instanceof CacheEntry) {
            CacheEntry<?> entry = (CacheEntry<?>) localValue;
            
            // 检查是否过期
            if (entry.isExpired()) {
                localCache.remove(key);
                return null;
            }
            
            Object value = entry.getValue();
            if (clazz.isInstance(value)) {
                return (T) value;
            }
        }
        
        // 如果本地没有或类型不匹配，尝试从Redis获取
        if (redisEnabled && redisTemplate != null) {
            try {
                Object redisValue = redisTemplate.opsForValue().get(key);
                if (redisValue != null && clazz.isInstance(redisValue)) {
                    // 更新本地缓存
                    long expireTime = 0;
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (ttl != null && ttl > 0) {
                        expireTime = System.currentTimeMillis() + (ttl * 1000L);
                    }
                    localCache.put(key, new CacheEntry<>(redisValue, expireTime));
                    
                    return (T) redisValue;
                }
            } catch (Exception e) {
                log.error("Redis获取缓存失败: key={}", key, e);
            }
        }
        
        return null;
    }
    
    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 操作是否成功
     */
    @Override
    public boolean delete(String key) {
        if (key == null) {
            return false;
        }
        
        // 从本地缓存删除
        localCache.remove(key);
        
        // 如果启用了Redis，也从Redis中删除
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception e) {
                log.error("Redis删除缓存失败: key={}", key, e);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 清空所有缓存
     *
     * @return 操作是否成功
     */
    @Override
    public boolean clear() {
        // 清空本地缓存
        localCache.clear();
        
        // 如果启用了Redis，尝试清空Redis
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.getConnectionFactory().getConnection().flushDb();
            } catch (Exception e) {
                log.error("Redis清空缓存失败", e);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查键是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }
        
        // 检查本地缓存
        Object localValue = localCache.get(key);
        if (localValue instanceof CacheEntry) {
            CacheEntry<?> entry = (CacheEntry<?>) localValue;
            if (!entry.isExpired()) {
                return true;
            } else {
                localCache.remove(key);
            }
        }
        
        // 如果本地没有，检查Redis
        if (redisEnabled && redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            } catch (Exception e) {
                log.error("Redis检查键存在失败: key={}", key, e);
            }
        }
        
        return false;
    }
    
    /**
     * 获取缓存剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余过期时间(秒)，-1表示不存在，-2表示永不过期
     */
    @Override
    public long ttl(String key) {
        if (key == null) {
            return -1;
        }
        
        // 检查本地缓存
        Object localValue = localCache.get(key);
        if (localValue instanceof CacheEntry) {
            CacheEntry<?> entry = (CacheEntry<?>) localValue;
            
            // 检查是否过期
            if (entry.isExpired()) {
                localCache.remove(key);
                return -1;
            }
            
            // 永不过期
            if (entry.getExpireTime() == 0) {
                return -2;
            }
            
            // 计算剩余时间
            long remainingMs = entry.getExpireTime() - System.currentTimeMillis();
            return remainingMs > 0 ? remainingMs / 1000 : 0;
        }
        
        // 如果本地没有，检查Redis
        if (redisEnabled && redisTemplate != null) {
            try {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                if (ttl != null) {
                    return ttl;
                }
            } catch (Exception e) {
                log.error("Redis获取过期时间失败: key={}", key, e);
            }
        }
        
        return -1;
    }
} 