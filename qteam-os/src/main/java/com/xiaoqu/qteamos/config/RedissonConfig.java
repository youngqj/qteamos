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
 * Redisson配置类
 * 提供条件化的Redisson配置，可以通过spring.data.redis-enabled属性控制是否启用Redisson
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson配置类
 * 只有在spring.data.redis-enabled=true时才启用Redisson相关功能
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis-enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {
    // 当前仅使用条件注解控制是否启用Redisson
    // 具体配置由Redisson自动配置类完成
} 