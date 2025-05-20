package com.xiaoqu.qteamos.core.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.xiaoqu.qteamos.core.cache.api.CacheService;
import com.xiaoqu.qteamos.core.cache.config.CacheProperties;
import com.xiaoqu.qteamos.core.cache.exception.CacheException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Caffeine缓存服务实现
 * 基于Caffeine的高性能本地缓存
 *
 * @author yangqijun
 * @date 2025-05-04
 */
@Slf4j
public class CaffeineCacheService implements CacheService {

    /**
     * 缓存名称
     */
    private String name = "caffeineCacheService";

    /**
     * 缓存属性配置
     */
    private CacheProperties properties;

    /**
     * 主缓存实例
     */
    private Cache<String, CacheValue<?>> cache;

    /**
     * Hash缓存实例映射
     */
    private final Map<String, Cache<String, Object>> hashCaches = new ConcurrentHashMap<>();

    /**
     * Set缓存实例映射
     */
    private final Map<String, Cache<Object, Boolean>> setCaches = new ConcurrentHashMap<>();

    /**
     * List缓存实例映射
     */
    private final Map<String, List<Object>> listCaches = new ConcurrentHashMap<>();

    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 初始化方法
     */
    public void init() {
        // 创建Caffeine缓存构建器
        Caffeine<String, CacheValue<?>> builder = Caffeine.newBuilder()
                .initialCapacity(properties.getCaffeine().getInitialCapacity())
                .maximumSize(properties.getCaffeine().getMaximumSize())
                .expireAfter(new CaffeineExpiry());
        
        // 根据配置启用统计功能
        if (properties.getCaffeine().isRecordStats()) {
            builder.recordStats();
        }
        
        // 构建缓存
        cache = builder.build();

        log.info("初始化Caffeine缓存服务，最大容量: {}", properties.getCaffeine().getMaximumSize());
    }

    @Override
    public CacheType getType() {
        return CacheType.CAFFEINE;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * 设置缓存名称
     *
     * @param name 缓存名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 设置缓存属性
     *
     * @param properties 缓存属性
     */
    public void setProperties(CacheProperties properties) {
        this.properties = properties;
        init();
    }

    @Override
    public <T> boolean set(String key, T value) {
        return set(key, value, properties.getDefaultExpiration(), TimeUnit.SECONDS);
    }

    @Override
    public <T> boolean set(String key, T value, long timeout, TimeUnit unit) {
        try {
            String cacheKey = buildKey(key);
            
            // 计算过期时间
            long expireTime = timeout < 0 ? -1 : System.currentTimeMillis() + unit.toMillis(timeout);
            
            // 创建缓存值对象
            CacheValue<T> cacheValue = new CacheValue<>(value, expireTime);
            
            // 设置缓存
            cache.put(cacheKey, cacheValue);
            
            return true;
        } catch (Exception e) {
            log.error("设置缓存失败: {}", key, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key, Class<T> clazz) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取缓存值
            CacheValue<?> cacheValue = cache.getIfPresent(cacheKey);
            if (cacheValue == null) {
                return null;
            }
            
            // 检查是否过期
            if (isExpired(cacheValue)) {
                delete(key);
                return null;
            }
            
            // 获取实际值并尝试转换
            Object value = cacheValue.getValue();
            if (value == null) {
                return null;
            }
            
            if (clazz.isInstance(value)) {
                return (T) value;
            } else {
                // 尝试类型转换
                return objectMapper.convertValue(value, clazz);
            }
        } catch (Exception e) {
            log.error("获取缓存失败: {}", key, e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            String cacheKey = buildKey(key);
            cache.invalidate(cacheKey);
            return true;
        } catch (Exception e) {
            log.error("删除缓存失败: {}", key, e);
            return false;
        }
    }

    @Override
    public long deleteAll(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        long count = 0;
        for (String key : keys) {
            if (delete(key)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean exists(String key) {
        String cacheKey = buildKey(key);
        CacheValue<?> cacheValue = cache.getIfPresent(cacheKey);
        
        if (cacheValue == null) {
            return false;
        }
        
        if (isExpired(cacheValue)) {
            delete(key);
            return false;
        }
        
        return true;
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        String cacheKey = buildKey(key);
        
        // 获取缓存值
        CacheValue<?> cacheValue = cache.getIfPresent(cacheKey);
        if (cacheValue == null) {
            return false;
        }
        
        if (isExpired(cacheValue)) {
            delete(key);
            return false;
        }
        
        // 设置新的过期时间
        long expireTime = timeout < 0 ? -1 : System.currentTimeMillis() + unit.toMillis(timeout);
        cacheValue.setExpireTime(expireTime);
        
        // 更新缓存
        cache.put(cacheKey, cacheValue);
        
        return true;
    }

    @Override
    public long getExpire(String key, TimeUnit unit) {
        String cacheKey = buildKey(key);
        
        // 获取缓存值
        CacheValue<?> cacheValue = cache.getIfPresent(cacheKey);
        if (cacheValue == null) {
            return -2; // 键不存在
        }
        
        if (isExpired(cacheValue)) {
            delete(key);
            return -2; // 键不存在（已过期）
        }
        
        long expireTime = cacheValue.getExpireTime();
        if (expireTime < 0) {
            return -1; // 永不过期
        }
        
        long remaining = expireTime - System.currentTimeMillis();
        return unit.convert(remaining > 0 ? remaining : 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public long increment(String key, long delta) {
        String cacheKey = buildKey(key);
        
        synchronized (this) {
            // 获取当前值
            Long current = get(key, Long.class);
            if (current == null) {
                current = 0L;
            }
            
            // 递增
            long newValue = current + delta;
            
            // 保存新值
            set(key, newValue);
            
            return newValue;
        }
    }

    @Override
    public long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    @Override
    public <T> T getHashValue(String key, String hashKey, Class<T> clazz) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取或创建Hash缓存
            Cache<String, Object> hashCache = hashCaches.computeIfAbsent(cacheKey, k -> createHashCache());
            
            // 获取值
            Object value = hashCache.getIfPresent(hashKey);
            if (value == null) {
                return null;
            }
            
            if (clazz.isInstance(value)) {
                return clazz.cast(value);
            } else {
                // 尝试类型转换
                return objectMapper.convertValue(value, clazz);
            }
        } catch (Exception e) {
            log.error("获取Hash值失败: {}, {}", key, hashKey, e);
            return null;
        }
    }

    @Override
    public <T> boolean setHashValue(String key, String hashKey, T value) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取或创建Hash缓存
            Cache<String, Object> hashCache = hashCaches.computeIfAbsent(cacheKey, k -> createHashCache());
            
            // 设置值
            hashCache.put(hashKey, value);
            
            return true;
        } catch (Exception e) {
            log.error("设置Hash值失败: {}, {}", key, hashKey, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getEntireHash(String key) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取Hash缓存
            Cache<String, Object> hashCache = hashCaches.get(cacheKey);
            if (hashCache == null) {
                return new HashMap<>();
            }
            
            // 转换为Map
            Map<String, Object> result = new HashMap<>();
            hashCache.asMap().forEach(result::put);
            
            return result;
        } catch (Exception e) {
            log.error("获取整个Hash失败: {}", key, e);
            return new HashMap<>();
        }
    }

    @Override
    public long deleteHashValue(String key, Object... hashKeys) {
        if (hashKeys == null || hashKeys.length == 0) {
            return 0;
        }
        
        try {
            String cacheKey = buildKey(key);
            
            // 获取Hash缓存
            Cache<String, Object> hashCache = hashCaches.get(cacheKey);
            if (hashCache == null) {
                return 0;
            }
            
            // 删除值
            long count = 0;
            for (Object hashKey : hashKeys) {
                if (hashCache.asMap().remove(hashKey.toString()) != null) {
                    count++;
                }
            }
            
            return count;
        } catch (Exception e) {
            log.error("删除Hash值失败: {}", key, e);
            return 0;
        }
    }

    @Override
    public boolean existsHashKey(String key, String hashKey) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取Hash缓存
            Cache<String, Object> hashCache = hashCaches.get(cacheKey);
            if (hashCache == null) {
                return false;
            }
            
            // 检查键
            return hashCache.getIfPresent(hashKey) != null;
        } catch (Exception e) {
            log.error("检查Hash键存在失败: {}, {}", key, hashKey, e);
            return false;
        }
    }

    @Override
    public <T> long addToSet(String key, T... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        
        try {
            String cacheKey = buildKey(key);
            
            // 获取或创建Set缓存
            Cache<Object, Boolean> setCache = setCaches.computeIfAbsent(cacheKey, k -> createSetCache());
            
            // 添加值
            long count = 0;
            for (T value : values) {
                if (setCache.getIfPresent(value) == null) {
                    setCache.put(value, Boolean.TRUE);
                    count++;
                }
            }
            
            return count;
        } catch (Exception e) {
            log.error("添加到Set失败: {}", key, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Set<T> getSet(String key, Class<T> clazz) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取Set缓存
            Cache<Object, Boolean> setCache = setCaches.get(cacheKey);
            if (setCache == null) {
                return new HashSet<>();
            }
            
            // 转换为Set
            Set<T> result = new HashSet<>();
            for (Object value : setCache.asMap().keySet()) {
                if (clazz.isInstance(value)) {
                    result.add((T) value);
                } else {
                    // 尝试类型转换
                    result.add(objectMapper.convertValue(value, clazz));
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取Set失败: {}", key, e);
            return new HashSet<>();
        }
    }

    @Override
    public <T> long leftPush(String key, T value) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取或创建List
            List<Object> list = listCaches.computeIfAbsent(cacheKey, k -> new ArrayList<>());
            
            // 添加到列表开头
            synchronized (list) {
                list.add(0, value);
                return list.size();
            }
        } catch (Exception e) {
            log.error("左推到List失败: {}", key, e);
            return 0;
        }
    }

    @Override
    public <T> long rightPush(String key, T value) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取或创建List
            List<Object> list = listCaches.computeIfAbsent(cacheKey, k -> new ArrayList<>());
            
            // 添加到列表末尾
            synchronized (list) {
                list.add(value);
                return list.size();
            }
        } catch (Exception e) {
            log.error("右推到List失败: {}", key, e);
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getList(String key, long start, long end, Class<T> clazz) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取List
            List<Object> list = listCaches.get(cacheKey);
            if (list == null) {
                return new ArrayList<>();
            }
            
            synchronized (list) {
                // 处理范围，-1表示到末尾
                int size = list.size();
                int startIndex = (int) Math.max(start, 0);
                int endIndex = (int) (end < 0 ? size - 1 : Math.min(end, size - 1));
                
                if (startIndex > endIndex || startIndex >= size) {
                    return new ArrayList<>();
                }
                
                // 提取子列表
                List<Object> subList = list.subList(startIndex, endIndex + 1);
                
                // 转换为结果类型
                List<T> result = new ArrayList<>();
                for (Object value : subList) {
                    if (clazz.isInstance(value)) {
                        result.add((T) value);
                    } else {
                        // 尝试类型转换
                        result.add(objectMapper.convertValue(value, clazz));
                    }
                }
                
                return result;
            }
        } catch (Exception e) {
            log.error("获取List失败: {}", key, e);
            return new ArrayList<>();
        }
    }

    @Override
    public long getListSize(String key) {
        try {
            String cacheKey = buildKey(key);
            
            // 获取List
            List<Object> list = listCaches.get(cacheKey);
            return list != null ? list.size() : 0;
        } catch (Exception e) {
            log.error("获取List大小失败: {}", key, e);
            return 0;
        }
    }

    @Override
    public boolean clear() {
        try {
            // 清空所有缓存
            cache.invalidateAll();
            hashCaches.values().forEach(Cache::invalidateAll);
            setCaches.values().forEach(Cache::invalidateAll);
            listCaches.clear();
            
            log.info("清空Caffeine缓存");
            return true;
        } catch (Exception e) {
            log.error("清空Caffeine缓存失败", e);
            return false;
        }
    }

    /**
     * 构建缓存键
     *
     * @param key 原始键
     * @return 加上前缀的缓存键
     */
    private String buildKey(String key) {
        return properties.getKeyPrefix() + key;
    }

    /**
     * 判断缓存是否过期
     *
     * @param cacheValue 缓存值
     * @return 是否过期
     */
    private boolean isExpired(CacheValue<?> cacheValue) {
        long expireTime = cacheValue.getExpireTime();
        return expireTime > 0 && expireTime < System.currentTimeMillis();
    }

    /**
     * 创建Hash缓存
     *
     * @return Hash缓存实例
     */
    private Cache<String, Object> createHashCache() {
        return Caffeine.newBuilder()
                .initialCapacity(properties.getCaffeine().getInitialCapacity())
                .maximumSize(properties.getCaffeine().getMaximumSize())
                .build();
    }

    /**
     * 创建Set缓存
     *
     * @return Set缓存实例
     */
    private Cache<Object, Boolean> createSetCache() {
        return Caffeine.newBuilder()
                .initialCapacity(properties.getCaffeine().getInitialCapacity())
                .maximumSize(properties.getCaffeine().getMaximumSize())
                .build();
    }

    /**
     * Caffeine过期策略
     */
    private class CaffeineExpiry implements Expiry<String, CacheValue<?>> {
        @Override
        public long expireAfterCreate(String key, CacheValue<?> value, long currentTime) {
            long expireTime = value.getExpireTime();
            return expireTime < 0 ? Long.MAX_VALUE : expireTime * 1000000 - currentTime;
        }

        @Override
        public long expireAfterUpdate(String key, CacheValue<?> value, long currentTime, long currentDuration) {
            return expireAfterCreate(key, value, currentTime);
        }

        @Override
        public long expireAfterRead(String key, CacheValue<?> value, long currentTime, long currentDuration) {
            return currentDuration;
        }
    }

    /**
     * 缓存值包装类
     *
     * @param <T> 值类型
     */
    @Data
    private static class CacheValue<T> {
        /**
         * 缓存值
         */
        private final T value;
        
        /**
         * 过期时间（毫秒时间戳），-1表示永不过期
         */
        private long expireTime;
        
        /**
         * 构造函数
         *
         * @param value 缓存值
         * @param expireTime 过期时间
         */
        public CacheValue(T value, long expireTime) {
            this.value = value;
            this.expireTime = expireTime;
        }
    }
} 