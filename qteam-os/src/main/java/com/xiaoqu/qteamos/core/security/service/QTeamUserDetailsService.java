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
 * QTeamOS用户详情服务
 * 用于Spring Security用户认证
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 用户详情服务
 * 用于Spring Security用户认证和授权
 * 集成插件用户系统
 */
@Service
public class QTeamUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(QTeamUserDetailsService.class);
    
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    /**
     * 加载用户详情
     * 按以下顺序查找用户：
     * 1. 系统内置用户
     * 2. 数据库用户（如果有）
     * 3. 插件提供的用户（通过事件机制）
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户详情: {}", username);
        
        // 1. 尝试从系统内置用户加载
        UserDetails systemUser = loadSystemUser(username);
        if (systemUser != null) {
            return systemUser;
        }
        
        // 2. 尝试从数据库加载用户（实际实现时替换为真实的数据库查询）
        // TODO: 实现数据库用户查询
        
        // 3. 发布用户加载事件，让插件有机会提供用户详情
        UserLoadingEvent event = new UserLoadingEvent(username);
        eventPublisher.publishEvent(event);
        
        // 如果事件处理后找到了用户，则返回
        if (event.getUserDetails() != null) {
            return event.getUserDetails();
        }
        
        // 没有找到用户，抛出异常
        throw new UsernameNotFoundException("找不到用户: " + username);
    }
    
    /**
     * 加载系统内置用户
     * 开发环境或演示模式下使用
     */
    private UserDetails loadSystemUser(String username) {
        // 在实际应用中，系统用户应从配置或系统表加载
        // 这里简单实现一个admin用户用于测试
        if ("admin".equals(username)) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("core:system:manage"));
            
            return new User(username, "{noop}admin", true, true, true, true, authorities);
        }
        
        return null;
    }
    
    /**
     * 用户加载事件
     * 插件可以监听此事件提供用户详情
     */
    public static class UserLoadingEvent {
        private final String username;
        private UserDetails userDetails;
        
        public UserLoadingEvent(String username) {
            this.username = username;
        }
        
        public String getUsername() {
            return username;
        }
        
        public UserDetails getUserDetails() {
            return userDetails;
        }
        
        public void setUserDetails(UserDetails userDetails) {
            this.userDetails = userDetails;
        }
    }
} 