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
 * 网关服务接口
 * 定义网关服务的标准接口方法
 *
 * @author yangqijun
 * @date 2025-05-04
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.gateway;

/**
 * 网关服务接口
 * 定义网关服务的标准功能集合
 */
public interface GatewayService {
    
    /**
     * 初始化网关服务
     * 在系统启动时调用，完成路由规则加载、拦截器配置等工作
     */
    void initialize();
    
    /**
     * 关闭网关服务
     * 在系统关闭时调用，释放网关相关资源
     */
    void shutdown();
    
    /**
     * 检查网关服务健康状态
     *
     * @return 如果网关服务运行正常则返回true
     */
    boolean isHealthy();
    
    /**
     * 获取网关服务状态信息
     *
     * @return 网关服务状态信息
     */
    String getStatus();
} 