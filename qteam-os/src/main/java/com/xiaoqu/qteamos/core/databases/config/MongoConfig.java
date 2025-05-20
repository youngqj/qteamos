/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 11:52:36
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 16:52:00
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/databases/config/MongoConfig.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.databases.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * MongoDB配置类
 * 支持配置多个MongoDB数据源
 *
 * @author yangqijun
 * @date 2025-05-03
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.data.mongodb", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MongoConfig {

    /**
     * 主MongoDB数据库工厂
     */
    @Primary
    @Bean(name = "primaryMongoDbFactory")
    public MongoDatabaseFactory primaryMongoDbFactory(
            @Value("${spring.data.mongodb.primary.uri:mongodb://localhost:27017/primary}") String connectionString) {
        return new SimpleMongoClientDatabaseFactory(connectionString);
    }

    /**
     * 主MongoDB模板
     */
    @Primary
    @Bean(name = "primaryMongoTemplate")
    public MongoTemplate primaryMongoTemplate(
            @Qualifier("primaryMongoDbFactory") MongoDatabaseFactory mongoDbFactory) {
        return new MongoTemplate(mongoDbFactory);
    }

    /**
     * 次要MongoDB数据库工厂
     */
    @Bean(name = "secondaryMongoDbFactory")
    public MongoDatabaseFactory secondaryMongoDbFactory(
            @Value("${spring.data.mongodb.secondary.uri:#{null}}") String connectionString) {
        if (connectionString == null) {
            return null;
        }
        return new SimpleMongoClientDatabaseFactory(connectionString);
    }

    /**
     * 次要MongoDB模板
     */
    @Bean(name = "secondaryMongoTemplate")
    public MongoTemplate secondaryMongoTemplate(
            @Qualifier("secondaryMongoDbFactory") MongoDatabaseFactory mongoDbFactory) {
        if (mongoDbFactory == null) {
            return null;
        }
        return new MongoTemplate(mongoDbFactory);
    }
} 