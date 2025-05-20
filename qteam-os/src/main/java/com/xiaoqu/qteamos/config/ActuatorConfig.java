/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-02 16:53:31
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 16:54:20
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/config/ActuatorConfig.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
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
 * Actuator配置类
 * 提供对Spring Boot Actuator的自定义配置
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.config;

import org.springframework.boot.actuate.autoconfigure.data.mongo.MongoHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator配置类
 * 用于禁用不需要的健康检查器
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    // 禁用Redis健康检查器
    RedisHealthContributorAutoConfiguration.class,
    RedisReactiveHealthContributorAutoConfiguration.class,
    // 禁用MongoDB健康检查器
    MongoHealthContributorAutoConfiguration.class
})
public class ActuatorConfig {
    // 当前仅使用排除自动配置的方式禁用不需要的健康检查器
} 