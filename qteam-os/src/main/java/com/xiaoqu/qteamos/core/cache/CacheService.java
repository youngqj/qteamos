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

/**
 * 缓存服务接口
 * 提供统一的缓存操作API
 *
 * @author yangqijun
 * @date 2024-08-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.cache;

/**
 * 缓存服务接口
 * 提供统一的缓存操作API
 */
public interface CacheService {
    
    /**
     * 初始化缓存服务
     */
    void initialize();
    
    /**
     * 存储缓存数据
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param expireSeconds 过期时间(秒)，0表示永不过期
     * @param <T> 缓存值类型
     * @return 操作是否成功
     */
    <T> boolean set(String key, T value, int expireSeconds);
    
    /**
     * 获取缓存数据
     *
     * @param key 缓存键
     * @param clazz 返回值类型Class
     * @param <T> 返回值类型
     * @return 缓存值，不存在则返回null
     */
    <T> T get(String key, Class<T> clazz);
    
    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 操作是否成功
     */
    boolean delete(String key);
    
    /**
     * 清空所有缓存
     *
     * @return 操作是否成功
     */
    boolean clear();
    
    /**
     * 检查键是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 获取缓存剩余过期时间
     *
     * @param key 缓存键
     * @return 剩余过期时间(秒)，-1表示不存在，-2表示永不过期
     */
    long ttl(String key);
    
    /**
     * 检查缓存服务健康状态
     *
     * @return 如果缓存服务运行正常则返回true
     */
    boolean isHealthy();
    
    /**
     * 获取缓存服务状态信息
     *
     * @return 缓存服务状态信息
     */
    String getStatus();
    
    /**
     * 关闭缓存服务
     */
    void shutdown();
} 