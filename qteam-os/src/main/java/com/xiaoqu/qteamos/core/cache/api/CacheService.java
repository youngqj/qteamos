package com.xiaoqu.qteamos.core.cache.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口
 * 提供统一的缓存操作方法
 *
 * @author yangqijun
 * @date 2025-05-04
 */
public interface CacheService {

    /**
     * 缓存类型
     */
    enum CacheType {
        /**
         * 文件缓存
         */
        FILE,
        
        /**
         * Redis缓存
         */
        REDIS,
        
        /**
         * Caffeine本地缓存
         */
        CAFFEINE
    }
    
    /**
     * 获取缓存类型
     *
     * @return 缓存类型
     */
    CacheType getType();
    
    /**
     * 获取配置的缓存名称
     *
     * @return 缓存名称
     */
    String getName();
    
    /**
     * 设置缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     * @return 是否成功
     */
    <T> boolean set(String key, T value);
    
    /**
     * 设置缓存，并指定过期时间
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否成功
     */
    <T> boolean set(String key, T value, long timeout, TimeUnit unit);
    
    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @param clazz 值类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> clazz);
    
    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否成功
     */
    boolean delete(String key);
    
    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 成功删除的个数
     */
    long deleteAll(Collection<String> keys);
    
    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);
    
    /**
     * 设置过期时间
     *
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return 是否成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);
    
    /**
     * 获取过期时间
     *
     * @param key 缓存键
     * @param unit 时间单位
     * @return 过期时间，-1表示永不过期，-2表示键不存在
     */
    long getExpire(String key, TimeUnit unit);
    
    /**
     * 递增
     *
     * @param key 缓存键
     * @param delta 递增因子
     * @return 递增后的值
     */
    long increment(String key, long delta);
    
    /**
     * 递减
     *
     * @param key 缓存键
     * @param delta 递减因子
     * @return 递减后的值
     */
    long decrement(String key, long delta);
    
    /**
     * 获取Hash结构中的属性
     *
     * @param key 缓存键
     * @param hashKey Hash键
     * @param clazz 值类型
     * @return Hash值
     */
    <T> T getHashValue(String key, String hashKey, Class<T> clazz);
    
    /**
     * 设置Hash结构中的属性
     *
     * @param key 缓存键
     * @param hashKey Hash键
     * @param value Hash值
     * @return 是否成功
     */
    <T> boolean setHashValue(String key, String hashKey, T value);
    
    /**
     * 获取整个Hash结构
     *
     * @param key 缓存键
     * @return Hash结构
     */
    Map<String, Object> getEntireHash(String key);
    
    /**
     * 删除Hash结构中的属性
     *
     * @param key 缓存键
     * @param hashKeys Hash键集合
     * @return 成功删除的个数
     */
    long deleteHashValue(String key, Object... hashKeys);
    
    /**
     * 判断Hash结构中是否存在属性
     *
     * @param key 缓存键
     * @param hashKey Hash键
     * @return 是否存在
     */
    boolean existsHashKey(String key, String hashKey);
    
    /**
     * 向Set结构中添加属性
     *
     * @param key 缓存键
     * @param values 值集合
     * @return 添加的个数
     */
    <T> long addToSet(String key, T... values);
    
    /**
     * 获取Set结构
     *
     * @param key 缓存键
     * @param clazz 值类型
     * @return 值集合
     */
    <T> Set<T> getSet(String key, Class<T> clazz);
    
    /**
     * 向List结构中添加属性（左侧）
     *
     * @param key 缓存键
     * @param value 值
     * @return 添加后的长度
     */
    <T> long leftPush(String key, T value);
    
    /**
     * 向List结构中添加属性（右侧）
     *
     * @param key 缓存键
     * @param value 值
     * @return 添加后的长度
     */
    <T> long rightPush(String key, T value);
    
    /**
     * 获取List结构
     *
     * @param key 缓存键
     * @param start 开始索引
     * @param end 结束索引
     * @param clazz 值类型
     * @return 值列表
     */
    <T> List<T> getList(String key, long start, long end, Class<T> clazz);
    
    /**
     * 获取List结构长度
     *
     * @param key 缓存键
     * @return 长度
     */
    long getListSize(String key);
    
    /**
     * 清空缓存
     *
     * @return 是否成功
     */
    boolean clear();
} 