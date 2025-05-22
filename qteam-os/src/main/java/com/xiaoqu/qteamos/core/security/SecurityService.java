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
 * 安全服务接口
 * 提供统一的安全功能，包括认证、授权和加密等
 *
 * @author yangqijun
 * @date 2024-08-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security;

/**
 * 安全服务接口
 * 提供统一的安全功能，包括认证、授权和加密等
 */
public interface SecurityService {
    
    /**
     * 初始化安全服务
     */
    void initialize();
    
    /**
     * 检查安全服务健康状态
     *
     * @return 服务是否健康
     */
    boolean isHealthy();
    
    /**
     * 获取安全服务状态信息
     *
     * @return 状态信息
     */
    String getStatus();
    
    /**
     * 关闭安全服务
     */
    void shutdown();
} 