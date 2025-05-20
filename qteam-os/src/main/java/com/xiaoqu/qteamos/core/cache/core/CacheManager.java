package com.xiaoqu.qteamos.core.cache.core;

import com.xiaoqu.qteamos.core.cache.api.CacheService;
import com.xiaoqu.qteamos.core.cache.exception.CacheException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器
 * 管理系统中的所有缓存实例
 *
 * @author yangqijun
 * @date 2025-05-04
 */
@Slf4j
public class CacheManager {

    /**
     * 缓存容器
     */
    private final Map<String, CacheService> cacheMap = new ConcurrentHashMap<>();

    /**
     * 默认缓存名称
     */
    private String defaultCacheName = "primaryCache";

    /**
     * 注册缓存
     *
     * @param name 缓存名称
     * @param cacheService 缓存服务
     */
    public void registerCache(String name, CacheService cacheService) {
        cacheMap.put(name, cacheService);
        log.info("注册缓存: {}, 类型: {}", name, cacheService.getType());
    }

    /**
     * 移除缓存
     *
     * @param name 缓存名称
     * @return 移除的缓存服务
     */
    public CacheService removeCache(String name) {
        CacheService cacheService = cacheMap.remove(name);
        if (cacheService != null) {
            log.info("移除缓存: {}, 类型: {}", name, cacheService.getType());
        }
        return cacheService;
    }

    /**
     * 获取缓存服务
     *
     * @param name 缓存名称
     * @return 缓存服务
     */
    public CacheService getCache(String name) {
        CacheService cacheService = cacheMap.get(name);
        if (cacheService == null) {
            throw new CacheException("缓存不存在: " + name);
        }
        return cacheService;
    }

    /**
     * 获取默认缓存服务
     *
     * @return 默认缓存服务
     */
    public CacheService getDefaultCache() {
        return getCache(defaultCacheName);
    }

    /**
     * 设置默认缓存名称
     *
     * @param defaultCacheName 默认缓存名称
     */
    public void setDefaultCacheName(String defaultCacheName) {
        this.defaultCacheName = defaultCacheName;
    }

    /**
     * 获取所有缓存服务
     *
     * @return 所有缓存服务
     */
    public Map<String, CacheService> getAllCaches() {
        return new ConcurrentHashMap<>(cacheMap);
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        cacheMap.values().forEach(CacheService::clear);
        log.info("清空所有缓存");
    }
} 