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
 * 示例安全控制器
 * 展示方法级别权限控制
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.plugin.examples;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.security.RolesAllowed;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * 示例安全控制器
 * 展示在控制器中使用Spring Security的各种方法级别权限控制
 */
@RestController
@RequestMapping("/api/example-plugin")
public class ExampleSecuredController {

    /**
     * 公共访问接口，无需认证
     */
    @GetMapping("/public/info")
    public ResponseEntity<Map<String, String>> getPublicInfo() {
        return ResponseEntity.ok(Map.of(
                "name", "Example Plugin",
                "version", "1.0.0",
                "type", "public"
        ));
    }
    
    /**
     * 需要认证的接口
     * 任何已登录用户都可访问
     */
    @GetMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(Principal principal) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", principal.getName());
        profile.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(profile);
    }
    
    /**
     * 使用@PreAuthorize注解进行权限控制
     * 需要'example-plugin:view'权限
     */
    @GetMapping("/data")
    @PreAuthorize("hasAuthority('example-plugin:view')")
    public ResponseEntity<Map<String, Object>> getData() {
        return ResponseEntity.ok(Map.of(
                "data", "This is protected data",
                "accessType", "requires view permission"
        ));
    }
    
    /**
     * 使用@Secured注解进行角色控制
     * 需要'ROLE_EXAMPLE_PLUGIN_ADMIN'角色
     */
    @PostMapping("/data")
    @Secured("ROLE_EXAMPLE_PLUGIN_ADMIN")
    public ResponseEntity<Map<String, Object>> createData(@RequestBody Map<String, Object> data) {
        // 处理数据创建
        return ResponseEntity.ok(Map.of(
                "status", "created",
                "data", data
        ));
    }
    
    /**
     * 使用@RolesAllowed注解(JSR-250)进行角色控制
     * 需要'ROLE_EXAMPLE_PLUGIN_ADMIN'或'ROLE_ADMIN'角色
     */
    @DeleteMapping("/data/{id}")
    @RolesAllowed({"ROLE_EXAMPLE_PLUGIN_ADMIN", "ROLE_ADMIN"})
    public ResponseEntity<Map<String, Object>> deleteData(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "status", "deleted",
                "id", id
        ));
    }
    
    /**
     * 使用复杂的SpEL表达式进行权限控制
     * 需要同时拥有'example-plugin:reports:view'权限和'ROLE_EXAMPLE_PLUGIN_USER'角色
     */
    @GetMapping("/reports/{id}")
    @PreAuthorize("hasAuthority('example-plugin:reports:view') && hasRole('EXAMPLE_PLUGIN_USER')")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "reportId", id,
                "content", "This is a protected report"
        ));
    }
    
    /**
     * 基于方法参数的动态权限控制
     * 只有当用户名与请求路径中的用户名匹配时才允许访问
     */
    @GetMapping("/users/{username}/settings")
    @PreAuthorize("#username == authentication.principal.username || hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserSettings(@PathVariable String username) {
        return ResponseEntity.ok(Map.of(
                "username", username,
                "settings", Map.of(
                        "theme", "dark",
                        "notifications", true
                )
        ));
    }
} 