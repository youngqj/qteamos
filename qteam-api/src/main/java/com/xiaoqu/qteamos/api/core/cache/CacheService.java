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
 * 缓存服务接口
 * 提供缓存数据能力，支持本地缓存和分布式缓存
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.cache;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口
 * 为插件提供缓存数据的能力
 */
public interface CacheService {
    
    /**
     * 获取字符串缓存
     * 
     * @param key 缓存键
     * @return 缓存的字符串值，不存在返回null
     */
    String getString(String key);
    
    /**
     * 设置字符串缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    void setString(String key, String value, long timeout, TimeUnit unit);
    
    /**
     * 获取对象缓存
     * 
     * @param key 缓存键
     * @param clazz 对象类型
     * @param <T> 对象泛型
     * @return 缓存的对象，不存在返回null
     */
    <T> T getObject(String key, Class<T> clazz);
    
    /**
     * 设置对象缓存
     * 
     * @param key 缓存键
     * @param value 缓存对象
     * @param timeout 过期时间
     * @param unit 时间单位
     * @param <T> 对象泛型
     */
    <T> void setObject(String key, T value, long timeout, TimeUnit unit);
    
    /**
     * 删除缓存
     * 
     * @param key 缓存键
     * @return 是否成功删除
     */
    boolean delete(String key);
    
    /**
     * 判断缓存是否存在
     * 
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 获取缓存过期时间
     * 
     * @param key 缓存键
     * @param unit 时间单位
     * @return 过期时间，-1表示永不过期，-2表示键不存在
     */
    long getExpire(String key, TimeUnit unit);
    
    /**
     * 设置缓存过期时间
     * 
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);
    
    // ===================== 以下为从SDK合并的方法 =====================
    
    /**
     * 获取缓存区域名称
     * 
     * @return 缓存区域名称
     */
    String getRegion();
    
    /**
     * 设置缓存
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    boolean set(String key, Object value);
    
    /**
     * 设置缓存，带过期时间
     * 
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否成功
     */
    boolean set(String key, Object value, long timeout, TimeUnit unit);
    
    /**
     * 获取缓存
     * 
     * @param key 缓存键
     * @param <T> 返回类型
     * @return 缓存值，如果不存在返回null
     */
    <T> T get(String key);
    
    /**
     * 获取缓存并转换为指定类型
     * 
     * @param key 缓存键
     * @param clazz 目标类型
     * @param <T> 返回类型
     * @return 缓存值，如果不存在返回null
     */
    <T> T get(String key, Class<T> clazz);
    
    /**
     * 批量删除缓存
     * 
     * @param pattern 匹配模式
     * @return 删除的缓存数量
     */
    long deleteByPattern(String pattern);
    
    /**
     * 按模式获取键列表
     * 
     * @param pattern 匹配模式
     * @return 键列表
     */
    Set<String> keys(String pattern);
    
    /**
     * 递增
     * 
     * @param key 缓存键
     * @param delta 增量
     * @return 增加后的值
     */
    long increment(String key, long delta);
    
    /**
     * 递减
     * 
     * @param key 缓存键
     * @param delta 减量
     * @return 减少后的值
     */
    long decrement(String key, long delta);
    
    /**
     * 哈希获取
     * 
     * @param key 缓存键
     * @param hashKey 哈希键
     * @param <T> 返回类型
     * @return 值
     */
    <T> T hashGet(String key, String hashKey);
    
    /**
     * 哈希设置
     * 
     * @param key 缓存键
     * @param hashKey 哈希键
     * @param value 值
     * @return 是否成功
     */
    boolean hashSet(String key, String hashKey, Object value);
    
    /**
     * 获取哈希所有键值
     * 
     * @param key 缓存键
     * @return 键值对
     */
    Map<String, Object> hashGetAll(String key);
    
    /**
     * 设置哈希所有键值
     * 
     * @param key 缓存键
     * @param map 键值对
     * @return 是否成功
     */
    boolean hashSetAll(String key, Map<String, Object> map);
    
    /**
     * 列表左侧推入
     * 
     * @param key 缓存键
     * @param value 值
     * @return 列表长度
     */
    long listLeftPush(String key, Object value);
    
    /**
     * 列表右侧推入
     * 
     * @param key 缓存键
     * @param value 值
     * @return 列表长度
     */
    long listRightPush(String key, Object value);
    
    /**
     * 列表左侧弹出
     * 
     * @param key 缓存键
     * @param <T> 返回类型
     * @return 值
     */
    <T> T listLeftPop(String key);
    
    /**
     * 列表右侧弹出
     * 
     * @param key 缓存键
     * @param <T> 返回类型
     * @return 值
     */
    <T> T listRightPop(String key);
    
    /**
     * 获取列表范围
     * 
     * @param key 缓存键
     * @param start 开始位置
     * @param end 结束位置
     * @param <T> 返回类型
     * @return 值列表
     */
    <T> List<T> listRange(String key, long start, long end);
    
    /**
     * 获取列表长度
     * 
     * @param key 缓存键
     * @return 列表长度
     */
    long listSize(String key);
    
    /**
     * 清空当前区域所有缓存
     * 
     * @return 是否成功
     */
    boolean clear();
} 