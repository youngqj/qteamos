/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 17:03:17
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 18:05:30
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/gateway/config/GatewayConfiguration.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.core.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 网关配置类
 * 配置网关服务的核心组件
 *
 * @author yangqijun
 * @date 2025-05-03
 * @since 1.0.0
 */
@Configuration
@ComponentScan("com.xiaoqu.qteamos.core.gateway")
public class GatewayConfiguration {
    
    /**
     * 网关配置项说明：
     * qteamos.gateway.api-prefix=/api  # API前缀路径 包含pub 是公共的
     * qteamos.gateway.html-path-prefix=/html  # 视图文件前缀
     * qteamos.gateway.static-path-prefix=/static  # 静态文件前缀s
     * qteamos.gateway.default-rate-limit=100  # 默认限流速率(每分钟请求数)
     * qteamos.gateway.enable-request-logging=true  # 是否启用请求日志
     * qteamos.gateway.enable-rate-limit=true  # 是否启用限流
     */
    
    /**
     * 提供ObjectMapper用于JSON序列化
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
} 