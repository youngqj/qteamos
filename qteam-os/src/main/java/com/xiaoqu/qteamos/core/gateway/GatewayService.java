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

/**
 * 网关服务接口
 * 提供统一的网关管理功能，包括路由、限流和负载均衡等
 *
 * @author yangqijun
 * @date 2024-08-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.gateway;

/**
 * 网关服务接口
 * 提供统一的网关管理功能，包括路由、限流和负载均衡等
 */
public interface GatewayService {
    
    /**
     * 初始化网关服务
     */
    void initialize();
    
    /**
     * 检查网关服务健康状态
     *
     * @return 服务是否健康
     */
    boolean isHealthy();
    
    /**
     * 获取网关服务状态信息
     *
     * @return 状态信息
     */
    String getStatus();
    
    /**
     * 关闭网关服务
     */
    void shutdown();
} 