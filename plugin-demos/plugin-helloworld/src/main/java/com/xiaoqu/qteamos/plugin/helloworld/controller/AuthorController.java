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
 * HelloWorld插件控制器
 * 提供基本的API示例
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.plugin.helloworld.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.xiaoqu.qteamos.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HelloWorld插件控制器
 * 演示插件API接口实现
 */
@RestController
@RequestMapping("/pub/auth")
public class AuthorController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthorController.class);
    
    /**
     * 公共接口，无需授权可访问
     * 
     * @return 注册接口
     */
    @PostMapping(value = "/reg", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> hello() {
        try {
            

            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Thanks Register Our QTeam ! ");
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
      
            return Result.success(response);
        } catch (Exception e) {
            log.error("执行hello方法时出错", e);
            return Result.failed(e.getMessage());
        }
    }
    
    /**
     * 登录接口，接收用户名和密码
     * 
     * @param loginRequest 包含用户名和密码的请求体
     * @return 登录结果，包含token和用户信息
     */
    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        log.info("收到登录请求，用户名: {}", loginRequest.getUsername());
        
        // 在实际应用中，这里应该验证用户名和密码
        // 这是测试接口，直接返回成功
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", 1001);
        userData.put("username", loginRequest.getUsername());
        userData.put("nickname", "测试用户_" + loginRequest.getUsername());
        userData.put("avatar", "https://qteamos.com/assets/images/avatar.png");
        userData.put("roles", new String[]{"user", "member"});
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", UUID.randomUUID().toString().replace("-", ""));
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400);
        response.put("user", userData);
        response.put("loginTime", System.currentTimeMillis());
        
        return Result.success(response);
    }
    
    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
} 