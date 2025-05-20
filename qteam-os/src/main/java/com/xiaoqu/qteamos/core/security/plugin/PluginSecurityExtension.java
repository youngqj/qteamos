/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-02 00:06:37
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 00:07:53
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/security/plugin/PluginSecurityExtension.java
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
 * 插件安全扩展接口
 * 允许插件自定义安全规则和权限
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.plugin;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * 插件安全扩展接口
 * 插件可以实现此接口提供自定义的安全配置
 */
public interface PluginSecurityExtension {
    
    /**
     * 获取插件ID
     * 用于唯一标识此安全扩展
     */
    String getPluginId();
    
    /**
     * 配置HttpSecurity
     * 插件可以在此添加自定义的安全规则
     * 
     * @param http Spring Security的HttpSecurity配置对象
     * @throws Exception 配置过程中可能抛出的异常
     */
    void configure(HttpSecurity http) throws Exception;
    
    /**
     * 获取插件定义的权限列表
     * 可选实现，用于系统整合插件权限
     * 
     * @return 权限列表
     */
    default String[] getPermissions() {
        return new String[0];
    }
    
    /**
     * 获取插件定义的角色列表
     * 可选实现，用于系统整合插件角色
     * 
     * @return 角色列表
     */
    default String[] getRoles() {
        return new String[0];
    }
} 