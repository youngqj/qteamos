package com.xiaoqu.qteamos.core.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoqu.qteamos.core.cache.api.CacheService;
import com.xiaoqu.qteamos.core.cache.config.CacheProperties;
import com.xiaoqu.qteamos.core.cache.exception.CacheException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件缓存服务实现
 *
 * @author yangqijun
 * @date 2025-05-04
 */
@Slf4j
public class FileCacheService implements CacheService {

    /**
     * 缓存名称
     */
    private String name = "fileCacheService";

    /**
     * 缓存属性配置
     */
    private CacheProperties properties;

    /**
     * 缓存根目录
     */
    private Path cacheDir;

    /**
     * 内存缓存，用于提高性能
     */
    private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    /**
     * JSON序列化工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 定时任务执行器
     */
    private ScheduledExecutorService scheduler;

    /**
     * 初始化方法
     */
    public void init() {
        try {
            // 创建缓存目录
            cacheDir = Paths.get(properties.getFile().getDirectory());
            Files.createDirectories(cacheDir);
            log.info("文件缓存目录: {}", cacheDir.toAbsolutePath());

            // 加载现有缓存
            loadExistingCache();

            // 启动定时清理任务
            startCleanupTask();
        } catch (IOException e) {
            throw new CacheException("初始化文件缓存失败", e);
        }
    }

    /**
     * 销毁方法
     */
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /**
     * 加载现有缓存
     */
    private void loadExistingCache() {
        try {
            File[] files = cacheDir.toFile().listFiles((dir, name) -> name.endsWith(".cache"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String key = file.getName().replace(".cache", "");
                        CacheEntry entry = loadCacheEntry(key);
                        if (entry != null && !isExpired(entry)) {
                            memoryCache.put(key, entry);
                        } else {
                            file.delete();
                        }
                    } catch (Exception e) {
                        log.warn("加载缓存文件失败: {}", file.getName(), e);
                    }
                }
                log.info("加载现有缓存, 共{}项", memoryCache.size());
            }
        } catch (Exception e) {
            log.error("加载现有缓存失败", e);
        }
    }

    /**
     * 启动定时清理任务
     */
    private void startCleanupTask() {
        long intervalSeconds = properties.getFile().getCleanInterval();
        if (intervalSeconds > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::cleanupExpiredCache, 
                    intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            log.info("启动缓存清理任务, 周期: {}秒", intervalSeconds);
        }
    }

    /**
     * 清理过期缓存
     */
    private void cleanupExpiredCache() {
        try {
            int count = 0;
            
            // 清理内存缓存
            Iterator<Map.Entry<String, CacheEntry>> iterator = memoryCache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, CacheEntry> entry = iterator.next();
                if (isExpired(entry.getValue())) {
                    iterator.remove();
                    deleteCacheFile(entry.getKey());
                    count++;
                }
            }
            
            // 清理文件系统中的缓存
            File[] files = cacheDir.toFile().listFiles((dir, name) -> name.endsWith(".cache"));
            if (files != null) {
                for (File file : files) {
                    String key = file.getName().replace(".cache", "");
                    if (!memoryCache.containsKey(key)) {
                        file.delete();
                        count++;
                    }
                }
            }
            
            if (count > 0) {
                log.info("清理过期缓存, 共{}项", count);
            }
        } catch (Exception e) {
            log.error("清理过期缓存失败", e);
        }
    }

    /**
     * 判断缓存是否过期
     *
     * @param entry 缓存项
     * @return 是否过期
     */
    private boolean isExpired(CacheEntry entry) {
        return entry.getExpireTime() > 0 && entry.getExpireTime() < System.currentTimeMillis();
    }

    /**
     * 加载缓存项
     *
     * @param key 键
     * @return 缓存项
     */
    private CacheEntry loadCacheEntry(String key) {
        Path filePath = cacheDir.resolve(key + ".cache");
        if (!Files.exists(filePath)) {
            return null;
        }

        try {
            if (properties.getFile().isSerialized()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
                    return (CacheEntry) ois.readObject();
                }
            } else {
                String json = new String(Files.readAllBytes(filePath));
                return objectMapper.readValue(json, CacheEntry.class);
            }
        } catch (Exception e) {
            log.warn("加载缓存项失败: {}", key, e);
            return null;
        }
    }

    /**
     * 保存缓存项
     *
     * @param key 键
     * @param entry 缓存项
     */
    private void saveCacheEntry(String key, CacheEntry entry) {
        Path filePath = cacheDir.resolve(key + ".cache");
        try {
            if (properties.getFile().isSerialized()) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
                    oos.writeObject(entry);
                }
            } else {
                String json = objectMapper.writeValueAsString(entry);
                Files.write(filePath, json.getBytes());
            }
        } catch (Exception e) {
            log.warn("保存缓存项失败: {}", key, e);
        }
    }

    /**
     * 删除缓存文件
     *
     * @param key 键
     */
    private void deleteCacheFile(String key) {
        try {
            Path filePath = cacheDir.resolve(key + ".cache");
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            log.warn("删除缓存文件失败: {}", key, e);
        }
    }

    @Override
    public CacheType getType() {
        return CacheType.FILE;
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
            CacheEntry entry = new CacheEntry();
            
            // 设置过期时间
            long expireTime = timeout < 0 ? -1 : System.currentTimeMillis() + unit.toMillis(timeout);
            entry.setExpireTime(expireTime);
            
            // 设置值
            entry.setValue(value);
            
            // 保存到内存缓存
            memoryCache.put(cacheKey, entry);
            
            // 保存到文件
            saveCacheEntry(cacheKey, entry);
            
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
            
            // 从内存缓存获取
            CacheEntry entry = memoryCache.get(cacheKey);
            
            // 如果内存中没有，尝试从文件加载
            if (entry == null) {
                entry = loadCacheEntry(cacheKey);
                if (entry != null) {
                    memoryCache.put(cacheKey, entry);
                }
            }
            
            // 检查是否过期
            if (entry != null) {
                if (isExpired(entry)) {
                    delete(key);
                    return null;
                }
                
                Object value = entry.getValue();
                if (value != null) {
                    if (clazz.isInstance(value)) {
                        return (T) value;
                    } else {
                        // 尝试类型转换
                        if (value instanceof Map && clazz != Map.class) {
                            return objectMapper.convertValue(value, clazz);
                        }
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取缓存失败: {}", key, e);
            return null;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            String cacheKey = buildKey(key);
            
            // 从内存缓存删除
            memoryCache.remove(cacheKey);
            
            // 删除缓存文件
            deleteCacheFile(cacheKey);
            
            return true;
        } catch (Exception e) {
            log.error("删除缓存失败: {}", key, e);
            return false;
        }
    }

    @Override
    public long deleteAll(Collection<String> keys) {
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
        
        // 检查内存缓存
        CacheEntry entry = memoryCache.get(cacheKey);
        if (entry != null) {
            if (isExpired(entry)) {
                delete(key);
                return false;
            }
            return true;
        }
        
        // 检查文件缓存
        entry = loadCacheEntry(cacheKey);
        if (entry != null) {
            if (isExpired(entry)) {
                delete(key);
                return false;
            }
            memoryCache.put(cacheKey, entry);
            return true;
        }
        
        return false;
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        String cacheKey = buildKey(key);
        
        // 获取缓存项
        CacheEntry entry = memoryCache.get(cacheKey);
        if (entry == null) {
            entry = loadCacheEntry(cacheKey);
            if (entry == null) {
                return false;
            }
        }
        
        if (isExpired(entry)) {
            delete(key);
            return false;
        }
        
        // 设置新的过期时间
        long expireTime = timeout < 0 ? -1 : System.currentTimeMillis() + unit.toMillis(timeout);
        entry.setExpireTime(expireTime);
        
        // 更新内存缓存
        memoryCache.put(cacheKey, entry);
        
        // 更新文件缓存
        saveCacheEntry(cacheKey, entry);
        
        return true;
    }

    @Override
    public long getExpire(String key, TimeUnit unit) {
        String cacheKey = buildKey(key);
        
        // 获取缓存项
        CacheEntry entry = memoryCache.get(cacheKey);
        if (entry == null) {
            entry = loadCacheEntry(cacheKey);
            if (entry == null) {
                return -2; // 键不存在
            }
            memoryCache.put(cacheKey, entry);
        }
        
        if (isExpired(entry)) {
            delete(key);
            return -2; // 键不存在（已过期）
        }
        
        if (entry.getExpireTime() < 0) {
            return -1; // 永不过期
        }
        
        long remaining = entry.getExpireTime() - System.currentTimeMillis();
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
        String combinedKey = key + ":" + hashKey;
        return get(combinedKey, clazz);
    }

    @Override
    public <T> boolean setHashValue(String key, String hashKey, T value) {
        String combinedKey = key + ":" + hashKey;
        return set(combinedKey, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getEntireHash(String key) {
        Map<String, Object> result = new HashMap<>();
        String prefix = buildKey(key) + ":";
        
        // 从缓存中筛选所有该hash的内容
        memoryCache.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(prefix) && !isExpired(entry.getValue()))
            .forEach(entry -> {
                String hashKey = entry.getKey().substring(prefix.length());
                result.put(hashKey, entry.getValue().getValue());
            });
        
        return result;
    }

    @Override
    public long deleteHashValue(String key, Object... hashKeys) {
        long count = 0;
        for (Object hashKey : hashKeys) {
            String combinedKey = key + ":" + hashKey;
            if (delete(combinedKey)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean existsHashKey(String key, String hashKey) {
        String combinedKey = key + ":" + hashKey;
        return exists(combinedKey);
    }

    @Override
    public <T> long addToSet(String key, T... values) {
        String cacheKey = buildKey(key);
        
        synchronized (this) {
            // 获取当前集合
            Set<T> currentSet = getSet(key, (Class<T>) values.getClass().getComponentType());
            if (currentSet == null) {
                currentSet = new HashSet<>();
            }
            
            // 添加值
            long added = 0;
            for (T value : values) {
                if (currentSet.add(value)) {
                    added++;
                }
            }
            
            // 保存集合
            if (added > 0) {
                set(key, currentSet);
            }
            
            return added;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Set<T> getSet(String key, Class<T> clazz) {
        Set<T> set = get(key, Set.class);
        if (set == null) {
            return new HashSet<>();
        }
        
        // 如果需要类型转换
        if (set.isEmpty() || clazz.isInstance(set.iterator().next())) {
            return set;
        } else {
            return set.stream()
                .map(item -> objectMapper.convertValue(item, clazz))
                .collect(Collectors.toSet());
        }
    }

    @Override
    public <T> long leftPush(String key, T value) {
        String cacheKey = buildKey(key);
        
        synchronized (this) {
            // 获取当前列表
            List<T> currentList = getList(key, 0, -1, (Class<T>) value.getClass());
            if (currentList == null) {
                currentList = new ArrayList<>();
            }
            
            // 添加到列表开头
            currentList.add(0, value);
            
            // 保存列表
            set(key, currentList);
            
            return currentList.size();
        }
    }

    @Override
    public <T> long rightPush(String key, T value) {
        String cacheKey = buildKey(key);
        
        synchronized (this) {
            // 获取当前列表
            List<T> currentList = getList(key, 0, -1, (Class<T>) value.getClass());
            if (currentList == null) {
                currentList = new ArrayList<>();
            }
            
            // 添加到列表末尾
            currentList.add(value);
            
            // 保存列表
            set(key, currentList);
            
            return currentList.size();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> getList(String key, long start, long end, Class<T> clazz) {
        List<T> list = get(key, List.class);
        if (list == null) {
            return new ArrayList<>();
        }
        
        // 处理范围，-1表示到末尾
        int size = list.size();
        int startIndex = (int) Math.max(start, 0);
        int endIndex = (int) (end < 0 ? size : Math.min(end, size - 1));
        
        if (startIndex > endIndex || startIndex >= size) {
            return new ArrayList<>();
        }
        
        List<T> result = list.subList(startIndex, endIndex + 1);
        
        // 如果需要类型转换
        if (result.isEmpty() || clazz.isInstance(result.get(0))) {
            return result;
        } else {
            return result.stream()
                .map(item -> objectMapper.convertValue(item, clazz))
                .collect(Collectors.toList());
        }
    }

    @Override
    public long getListSize(String key) {
        List<?> list = get(key, List.class);
        return list == null ? 0 : list.size();
    }

    @Override
    public boolean clear() {
        try {
            // 清除内存缓存
            memoryCache.clear();
            
            // 清除文件缓存
            File[] files = cacheDir.toFile().listFiles((dir, name) -> name.endsWith(".cache"));
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            
            log.info("清空缓存");
            return true;
        } catch (Exception e) {
            log.error("清空缓存失败", e);
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
     * 缓存项
     */
    @Data
    public static class CacheEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        
        /**
         * 过期时间（毫秒时间戳），-1表示永不过期
         */
        private long expireTime;
        
        /**
         * 缓存值
         */
        private Object value;
    }
} 