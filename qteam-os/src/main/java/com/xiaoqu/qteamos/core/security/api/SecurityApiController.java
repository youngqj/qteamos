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
 * 安全API控制器
 * 提供权限和用户管理相关接口
 *
 * @author yangqijun
 * @date 2025-07-21
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.security.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.xiaoqu.qteamos.core.security.service.PermissionService;
import com.xiaoqu.qteamos.core.security.extension.PluginSecurityExtensionManager;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 安全API控制器
 * 提供给前端和插件使用的安全相关接口
 */
@RestController
@RequestMapping("/api/security")
public class SecurityApiController {

    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private PluginSecurityExtensionManager extensionManager;
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/current-user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("authenticated", true);
        result.put("username", principal.getName());
        result.put("roles", permissionService.getAllRoles().stream()
                .filter(role -> permissionService.hasRole(role))
                .toList());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查当前用户是否有指定权限
     */
    @GetMapping("/has-permission")
    public ResponseEntity<Map<String, Object>> hasPermission(@RequestParam String permission) {
        boolean hasPermission = permissionService.hasPermission(permission);
        return ResponseEntity.ok(Map.of(
                "permission", permission,
                "hasPermission", hasPermission
        ));
    }
    
    /**
     * 获取所有可用权限
     * 需要权限管理权限
     */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('core:role:manage')")
    public ResponseEntity<List<String>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }
    
    /**
     * 获取所有角色
     * 需要权限管理权限
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('core:role:manage')")
    public ResponseEntity<List<String>> getAllRoles() {
        return ResponseEntity.ok(permissionService.getAllRoles());
    }
    
    /**
     * 获取插件安全扩展信息
     * 需要插件管理权限
     */
    @GetMapping("/plugin-extensions")
    @PreAuthorize("hasAuthority('core:plugin:manage')")
    public ResponseEntity<List<Map<String, Object>>> getPluginExtensions() {
        List<Map<String, Object>> result = extensionManager.getAllExtensions().stream()
                .map(ext -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("pluginId", ext.getPluginId());
                    info.put("permissions", ext.getPermissions());
                    info.put("roles", ext.getRoles());
                    return info;
                })
                .toList();
        
        return ResponseEntity.ok(result);
    }
} 