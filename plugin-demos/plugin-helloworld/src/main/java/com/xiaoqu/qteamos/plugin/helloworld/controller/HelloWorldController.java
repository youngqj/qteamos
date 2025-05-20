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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.xiaoqu.qteamos.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * HelloWorld插件控制器
 * 演示插件API接口实现
 */
@RestController
@RequestMapping("/pub")
public class HelloWorldController {
    
    private static final Logger log = LoggerFactory.getLogger(HelloWorldController.class);
    
    /**
     * 公共接口，无需授权可访问
     * 
     * @return 问候信息
     */
    @GetMapping(value = "/hello")
    public Result<Map<String, Object>> hello() {
        try {
            log.error("HelloWorldController.hello方法被调用 - 使用ERROR级别");
            System.err.println("HelloWorldController.hello方法被调用 - 使用stderr");
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Hello from HelloWorld Plugin!");
            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());
            
            log.error("返回响应数据: {}", response);
      
            return Result.success(response);
        } catch (Exception e) {
            log.error("执行hello方法时出错", e);
            return Result.failed(e.getMessage());
        }
    }
    
    /**
     * 保护接口，需要普通用户权限
     * 
     * @return 状态信息
     */
    @GetMapping(value = "/protected/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> status() {
        System.out.println("HelloWorldController.status方法被调用");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "running");
        response.put("success", true);
        response.put("uptime", "1h 23m");
        return Result.success(response);
    }
    
    /**
     * 管理接口，需要管理员权限
     * 
     * @return 配置信息
     */
    @GetMapping(value = "/admin/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> config() {
        System.out.println("HelloWorldController.config方法被调用");
        
        Map<String, Object> response = new HashMap<>();
        response.put("mode", "development");
        response.put("version", "1.0.0");
        response.put("debug", true);
        return Result.success(response);
    }
} 