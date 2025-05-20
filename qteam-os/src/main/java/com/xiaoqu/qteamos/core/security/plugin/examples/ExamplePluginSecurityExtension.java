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
 * 示例插件安全扩展
 * 展示插件如何扩展系统安全配置
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.plugin.examples;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import com.xiaoqu.qteamos.core.security.plugin.PluginSecurityExtension;

/**
 * 示例插件安全扩展
 * 展示插件如何实现PluginSecurityExtension接口
 */
public class ExamplePluginSecurityExtension implements PluginSecurityExtension {

    private final String pluginId;
    
    public ExamplePluginSecurityExtension(String pluginId) {
        this.pluginId = pluginId;
    }
    
    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        // 配置插件特定的安全规则
        http.authorizeHttpRequests(authorize -> {
            // 公共资源
            authorize.requestMatchers("/api/" + pluginId + "/public/**").permitAll();
            
            // 需要特定权限的资源
            authorize.requestMatchers("/api/" + pluginId + "/admin/**")
                     .hasAuthority(pluginId + ":admin");
                     
            authorize.requestMatchers("/api/" + pluginId + "/reports/**")
                     .hasAnyAuthority(pluginId + ":reports:view", pluginId + ":admin");
                     
            // 使用自定义权限验证器
            // authorize.requestMatchers("/api/" + pluginId + "/custom/**")
            //          .access((authentication, object) -> 
            //              new AuthorizationDecision(authentication.get().getName().equals("admin")));
        });
    }

    @Override
    public String[] getPermissions() {
        // 定义插件的权限
        return new String[] {
            pluginId + ":view",        // 查看基本资源
            pluginId + ":edit",        // 编辑资源
            pluginId + ":admin",       // 管理员权限
            pluginId + ":reports:view",// 查看报表
            pluginId + ":reports:export"// 导出报表
        };
    }

    @Override
    public String[] getRoles() {
        // 定义插件特有的角色
        return new String[] {
            "ROLE_" + pluginId.toUpperCase() + "_ADMIN",     // 插件管理员
            "ROLE_" + pluginId.toUpperCase() + "_USER",      // 插件普通用户
            "ROLE_" + pluginId.toUpperCase() + "_VIEWER"     // 插件只读用户
        };
    }
} 