package com.xiaoqu.qteamos.core.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoqu.qteamos.core.cache.api.CacheService;
import com.xiaoqu.qteamos.core.cache.config.CacheProperties;
import com.xiaoqu.qteamos.core.cache.exception.CacheException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis缓存服务实现
 *
 * @author yangqijun
 * @date 2025-05-04
 */
@Slf4j
public class RedisCacheService implements CacheService {

    /**
     * 缓存名称
     */
    private String name = "redisCacheService";

    /**
     * 缓存属性配置
     */
    private CacheProperties properties;

    /**
     * Redis模板
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构造函数
     *
     * @param redisTemplate Redis模板
     */
    public RedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CacheType getType() {
        return CacheType.REDIS;
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
    }

    @Override
    public <T> boolean set(String key, T value) {
        return set(key, value, properties.getDefaultExpiration(), TimeUnit.SECONDS);
    }

    @Override
    public <T> boolean set(String key, T value, long timeout, TimeUnit unit) {
        try {
            String cacheKey = buildKey(key);
            
            if (timeout <= 0) {
                redisTemplate.opsForValue().set(cacheKey, value);
            } else {
                redisTemplate.opsForValue().set(cacheKey, value, timeout, unit);
            }
            
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
            Object value = redisTemplate.opsForValue().get(cacheKey);
            
            if (value == null) {
                return null;
            }
            
            if (clazz.isInstance(value)) {
                return (T) value;
            } else {
                // 尝试转换
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
            return Boolean.TRUE.equals(redisTemplate.delete(cacheKey));
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
        
        try {
            List<String> cacheKeys = keys.stream()
                    .map(this::buildKey)
                    .collect(Collectors.toList());
            
            Long count = redisTemplate.delete(cacheKeys);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("批量删除缓存失败", e);
            return 0;
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            String cacheKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey));
        } catch (Exception e) {
            log.error("检查缓存存在失败: {}", key, e);
            return false;
        }
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            String cacheKey = buildKey(key);
            if (timeout < 0) {
                return Boolean.TRUE.equals(redisTemplate.persist(cacheKey));
            } else {
                return Boolean.TRUE.equals(redisTemplate.expire(cacheKey, timeout, unit));
            }
        } catch (Exception e) {
            log.error("设置过期时间失败: {}", key, e);
            return false;
        }
    }

    @Override
    public long getExpire(String key, TimeUnit unit) {
        try {
            String cacheKey = buildKey(key);
            Long expireTime = redisTemplate.getExpire(cacheKey, unit);
            return expireTime != null ? expireTime : -2;
        } catch (Exception e) {
            log.error("获取过期时间失败: {}", key, e);
            return -2;
        }
    }

    @Override
    public long increment(String key, long delta) {
        try {
            String cacheKey = buildKey(key);
            Long result = redisTemplate.opsForValue().increment(cacheKey, delta);
            
            // 如果是新增加的键，设置默认过期时间
            if (result != null && result.equals(delta)) {
                long defaultExpiration = properties.getDefaultExpiration();
                if (defaultExpiration > 0) {
                    redisTemplate.expire(cacheKey, defaultExpiration, TimeUnit.SECONDS);
                }
            }
            
            return result != null ? result : 0;
        } catch (Exception e) {
            log.error("递增失败: {}", key, e);
            throw new CacheException("递增失败: " + e.getMessage(), e);
        }
    }

    @Override
    public long decrement(String key, long delta) {
        return increment(key, -delta);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getHashValue(String key, String hashKey, Class<T> clazz) {
        try {
            String cacheKey = buildKey(key);
            Object value = redisTemplate.opsForHash().get(cacheKey, hashKey);
            
            if (value == null) {
                return null;
            }
            
            if (clazz.isInstance(value)) {
                return (T) value;
            } else {
                // 尝试转换
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
            redisTemplate.opsForHash().put(cacheKey, hashKey, value);
            
            // 如果是新增加的键，设置默认过期时间
            long defaultExpiration = properties.getDefaultExpiration();
            if (defaultExpiration > 0 && !Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                redisTemplate.expire(cacheKey, defaultExpiration, TimeUnit.SECONDS);
            }
            
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
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(cacheKey);
            
            if (entries == null || entries.isEmpty()) {
                return new HashMap<>();
            }
            
            // 转换键类型为String
            Map<String, Object> result = new HashMap<>(entries.size());
            entries.forEach((k, v) -> result.put(k.toString(), v));
            
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
            Long count = redisTemplate.opsForHash().delete(cacheKey, hashKeys);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("删除Hash值失败: {}", key, e);
            return 0;
        }
    }

    @Override
    public boolean existsHashKey(String key, String hashKey) {
        try {
            String cacheKey = buildKey(key);
            return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(cacheKey, hashKey));
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
            Long added = redisTemplate.opsForSet().add(cacheKey, values);
            
            // 如果是新增加的键，设置默认过期时间
            if (added != null && added > 0) {
                long defaultExpiration = properties.getDefaultExpiration();
                if (defaultExpiration > 0 && !Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                    redisTemplate.expire(cacheKey, defaultExpiration, TimeUnit.SECONDS);
                }
            }
            
            return added != null ? added : 0;
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
            Set<Object> members = redisTemplate.opsForSet().members(cacheKey);
            
            if (members == null || members.isEmpty()) {
                return new HashSet<>();
            }
            
            // 如果元素类型匹配，直接转换
            if (clazz.isInstance(members.iterator().next())) {
                return (Set<T>) members;
            }
            
            // 否则尝试转换每个元素
            return members.stream()
                    .map(item -> objectMapper.convertValue(item, clazz))
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("获取Set失败: {}", key, e);
            return new HashSet<>();
        }
    }

    @Override
    public <T> long leftPush(String key, T value) {
        try {
            String cacheKey = buildKey(key);
            Long size = redisTemplate.opsForList().leftPush(cacheKey, value);
            
            // 如果是新增加的键，设置默认过期时间
            if (size != null && size == 1) {
                long defaultExpiration = properties.getDefaultExpiration();
                if (defaultExpiration > 0) {
                    redisTemplate.expire(cacheKey, defaultExpiration, TimeUnit.SECONDS);
                }
            }
            
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("左推到List失败: {}", key, e);
            return 0;
        }
    }

    @Override
    public <T> long rightPush(String key, T value) {
        try {
            String cacheKey = buildKey(key);
            Long size = redisTemplate.opsForList().rightPush(cacheKey, value);
            
            // 如果是新增加的键，设置默认过期时间
            if (size != null && size == 1) {
                long defaultExpiration = properties.getDefaultExpiration();
                if (defaultExpiration > 0) {
                    redisTemplate.expire(cacheKey, defaultExpiration, TimeUnit.SECONDS);
                }
            }
            
            return size != null ? size : 0;
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
            List<Object> range = redisTemplate.opsForList().range(cacheKey, start, end);
            
            if (range == null || range.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 如果元素类型匹配，直接转换
            if (clazz.isInstance(range.get(0))) {
                return (List<T>) range;
            }
            
            // 否则尝试转换每个元素
            return range.stream()
                    .map(item -> objectMapper.convertValue(item, clazz))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取List失败: {}", key, e);
            return new ArrayList<>();
        }
    }

    @Override
    public long getListSize(String key) {
        try {
            String cacheKey = buildKey(key);
            Long size = redisTemplate.opsForList().size(cacheKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取List大小失败: {}", key, e);
            return 0;
        }
    }

    @Override
    public boolean clear() {
        try {
            // 获取所有匹配的键
            Set<String> keys = redisTemplate.keys(properties.getKeyPrefix() + "*");
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            
            log.info("清空Redis缓存，前缀: {}", properties.getKeyPrefix());
            return true;
        } catch (Exception e) {
            log.error("清空Redis缓存失败", e);
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
} 