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
 * Redis配置类
 * 提供条件化的Redis配置，可以通过spring.data.redis-enabled属性控制是否启用Redis
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis配置类
 * 只有在spring.data.redis-enabled=true时才启用Redis相关功能
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis-enabled", havingValue = "true", matchIfMissing = false)
@EnableRedisRepositories
@Import({RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
public class RedisConfig {
    // Redis配置在这里可以添加自定义配置，当前仅使用条件注解控制是否启用Redis
} 