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
 * 数据库服务接口
 * 提供统一的数据库操作和管理功能
 *
 * @author yangqijun
 * @date 2024-08-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.databases;

/**
 * 数据库服务接口
 * 定义数据库服务的标准功能集合
 * 注：此接口已被合并到com.xiaoqu.qteamos.core.databases.core.DatabaseService
 * 为保持兼容性而保留
 */
public interface DatabaseService extends com.xiaoqu.qteamos.core.databases.core.DatabaseService {
    // 所有方法继承自核心接口

    /**
     * 初始化数据库服务
     */
    void initialize();
    
    /**
     * 检查数据库连接
     *
     * @return 连接是否正常
     */
    boolean checkConnection();
    
    /**
     * 获取数据库类型
     *
     * @return 数据库类型
     */
    String getDatabaseType();
    
    /**
     * 获取数据库版本
     *
     * @return 数据库版本
     */
    String getDatabaseVersion();
    
    /**
     * 检查数据库服务健康状态
     *
     * @return 服务是否健康
     */
    boolean isHealthy();
    
    /**
     * 获取数据库服务状态信息
     *
     * @return 状态信息
     */
    String getStatus();
    
    /**
     * 关闭数据库服务
     */
    void shutdown();
} 