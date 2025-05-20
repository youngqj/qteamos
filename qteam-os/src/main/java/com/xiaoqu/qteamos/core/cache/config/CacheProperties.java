package com.xiaoqu.qteamos.core.cache.config;

import com.xiaoqu.qteamos.core.cache.api.CacheService.CacheType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置属性
 *
 * @author yangqijun
 * @date 2025-05-04
 */
@Data
@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

    /**
     * 缓存类型，默认文件缓存
     */
    private CacheType type = CacheType.FILE;

    /**
     * 是否启用，默认开启
     */
    private boolean enabled = true;

    /**
     * 全局过期时间（秒），默认24小时，-1表示永不过期
     */
    private long defaultExpiration = 24 * 60 * 60;

    /**
     * 缓存键前缀
     */
    private String keyPrefix = "qteamos:";

    /**
     * 文件缓存配置
     */
    private FileConfig file = new FileConfig();

    /**
     * Redis缓存配置
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * Caffeine缓存配置
     */
    private CaffeineConfig caffeine = new CaffeineConfig();

    /**
     * 文件缓存配置
     */
    @Data
    public static class FileConfig {
        /**
         * 文件缓存目录，默认系统临时目录下的qteamos-cache
         */
        private String directory = System.getProperty("java.io.tmpdir") + "/qteamos-cache";

        /**
         * 是否使用序列化存储
         */
        private boolean serialized = true;

        /**
         * 缓存清理周期（秒）
         */
        private long cleanInterval = 3600;
    }

    /**
     * Redis缓存配置
     */
    @Data
    public static class RedisConfig {
        /**
         * Redis主机
         */
        private String host = "localhost";

        /**
         * Redis端口
         */
        private int port = 6379;

        /**
         * Redis密码
         */
        private String password;

        /**
         * Redis数据库索引
         */
        private int database = 0;

        /**
         * 连接超时时间（毫秒）
         */
        private int timeout = 2000;

        /**
         * 是否使用SSL
         */
        private boolean ssl = false;
    }

    /**
     * Caffeine缓存配置
     */
    @Data
    public static class CaffeineConfig {
        /**
         * 初始容量
         */
        private int initialCapacity = 100;

        /**
         * 最大容量
         */
        private long maximumSize = 10000;

        /**
         * 是否记录统计信息
         */
        private boolean recordStats = false;
    }
} 