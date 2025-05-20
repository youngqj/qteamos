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
 * 安全层核心配置
 * 提供Spring Security的基础配置和插件权限集成支持
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.http.SessionCreationPolicy;
 
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

import com.xiaoqu.qteamos.core.security.extension.PluginSecurityExtensionManager;
import com.xiaoqu.qteamos.core.security.filter.JwtAuthenticationFilter;
import com.xiaoqu.qteamos.core.security.properties.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private PluginSecurityExtensionManager pluginSecurityExtensionManager;
    
    @Autowired
    private SecurityProperties securityProperties;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${qteamos.gateway.api-prefix:/api}")
    private String apiPrefix;
    
    @Value("${qteamos.gateway.public-path-prefix:/pub}")
    private String publicApiPrefix;
    
    @Value("${qteamos.gateway.admin-path-prefix:/admin}")
    private String adminApiPrefix;
    
    // 添加新的路径前缀配置
    private static final String PLUGIN_PATH_PREFIX = "/p-";
    private static final String PLUGIN_PUBLIC_PATH = "/pub/";
    private static final String PLUGIN_PROTECTED_PATH = "/protected/";
    
    @Value("${qteamos.security.enabled:false}")
    private boolean securityEnabled;
    
    /**
     * 配置安全过滤链
     * 支持插件扩展和网关集成
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("安全功能已禁用，所有请求允许通过");
        
        return http
            .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF保护
            .cors(AbstractHttpConfigurer::disable)  // 禁用CORS保护
            .httpBasic(AbstractHttpConfigurer::disable)  // 禁用HTTP基本认证
            .formLogin(AbstractHttpConfigurer::disable)  // 禁用表单登录
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 无状态会话
            .authorizeHttpRequests(authorize -> {
                // 允许插件公共API匿名访问
                authorize.requestMatchers(apiPrefix + PLUGIN_PATH_PREFIX + "*" + PLUGIN_PUBLIC_PATH + "**").permitAll();
                // 允许插件公共API匿名访问 - 另一种模式
                authorize.requestMatchers(apiPrefix + PLUGIN_PATH_PREFIX + "*/pub/**").permitAll();
                // 所有其他请求也允许，用于测试
                authorize.anyRequest().permitAll();
            })
            .build();
    }
} 