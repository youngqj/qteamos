package com.xiaoqu.qteamos.core.cache.config;

import com.xiaoqu.qteamos.core.cache.api.CacheService;
import com.xiaoqu.qteamos.core.cache.core.CacheManager;
import com.xiaoqu.qteamos.core.cache.impl.CaffeineCacheService;
import com.xiaoqu.qteamos.core.cache.impl.FileCacheService;
import com.xiaoqu.qteamos.core.cache.impl.RedisCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 缓存配置类
 *
 * @author yangqijun
 * @date 2025-05-04
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {

    /**
     * 缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(CacheProperties properties) {
        CacheManager cacheManager = new CacheManager();
        log.info("初始化缓存管理器，默认缓存类型: {}", properties.getType());
        return cacheManager;
    }
    
    /**
     * 主缓存服务
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "cacheService")
    public CacheService cacheService(CacheManager cacheManager, CacheProperties properties) {
        CacheService cacheService = null;
        
        switch (properties.getType()) {
            case FILE:
                cacheService = fileCacheService(properties);
                break;
            case REDIS:
                cacheService = redisCacheService(properties, redisConnectionFactory(properties));
                break;
            case CAFFEINE:
                cacheService = caffeineCacheService(properties);
                break;
            default:
                cacheService = fileCacheService(properties);
        }
        
        cacheManager.registerCache("primaryCache", cacheService);
        log.info("初始化主缓存服务: {}", cacheService.getType());
        return cacheService;
    }
    
    /**
     * 文件缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "cache.type", havingValue = "FILE")
    public FileCacheService fileCacheService(CacheProperties properties) {
        FileCacheService cacheService = new FileCacheService();
        cacheService.setProperties(properties);
        cacheService.setName("fileCacheService");
        log.info("初始化文件缓存服务，缓存目录: {}", properties.getFile().getDirectory());
        return cacheService;
    }
    
    /**
     * Redis连接工厂
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "cache.type", havingValue = "REDIS")
    public RedisConnectionFactory redisConnectionFactory(CacheProperties properties) {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(properties.getRedis().getHost());
        redisConfig.setPort(properties.getRedis().getPort());
        
        // 只有当密码不为空时才设置密码
        if (properties.getRedis().getPassword() != null && !properties.getRedis().getPassword().isEmpty()) {
            redisConfig.setPassword(properties.getRedis().getPassword());
        }
        
        redisConfig.setDatabase(properties.getRedis().getDatabase());
        
        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig);
        connectionFactory.afterPropertiesSet();
        log.info("初始化Redis连接工厂，主机: {}:{}", properties.getRedis().getHost(), properties.getRedis().getPort());
        return connectionFactory;
    }
    
    /**
     * Redis模板
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "cache.type", havingValue = "REDIS")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 设置key的序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        // 设置value的序列化方式
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 设置hash key的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        // 设置hash value的序列化方式
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * Redis缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "cache.type", havingValue = "REDIS")
    public RedisCacheService redisCacheService(CacheProperties properties, RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = redisTemplate(connectionFactory);
        RedisCacheService cacheService = new RedisCacheService(redisTemplate);
        cacheService.setProperties(properties);
        cacheService.setName("redisCacheService");
        log.info("初始化Redis缓存服务");
        return cacheService;
    }
    
    /**
     * Caffeine缓存服务
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "cache.type", havingValue = "CAFFEINE")
    public CaffeineCacheService caffeineCacheService(CacheProperties properties) {
        CaffeineCacheService cacheService = new CaffeineCacheService();
        cacheService.setProperties(properties);
        cacheService.setName("caffeineCacheService");
        log.info("初始化Caffeine缓存服务，最大容量: {}", properties.getCaffeine().getMaximumSize());
        return cacheService;
    }
} 