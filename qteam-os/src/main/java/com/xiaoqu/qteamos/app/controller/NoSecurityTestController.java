package com.xiaoqu.qteamos.app.controller;

import com.xiaoqu.qteamos.core.plugin.PluginSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 无安全限制的测试控制器
 * 完全不受Spring Security保护
 */
@RestController
@RequestMapping("/nosec")
public class NoSecurityTestController {

    @Autowired
    private PluginSystem pluginSystem;
    
    /**
     * 获取插件信息
     */
    @GetMapping("/plugin/{pluginId}")
    public Map<String, Object> getPlugin(@PathVariable String pluginId) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "get");
        result.put("pluginId", pluginId);
        
        var pluginInfo = pluginSystem.getPlugin(pluginId);
        if (pluginInfo.isPresent()) {
            result.put("found", true);
            result.put("enabled", pluginInfo.get().isEnabled());
            result.put("state", pluginInfo.get().getState().name());
        } else {
            result.put("found", false);
        }
        
        return result;
    }
    
    /**
     * 启用插件
     */
    @GetMapping("/plugin/{pluginId}/enable")
    public Map<String, Object> enablePlugin(@PathVariable String pluginId) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "enable");
        result.put("pluginId", pluginId);
        
        try {
            boolean success = pluginSystem.enablePlugin(pluginId);
            result.put("success", success);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 禁用插件
     */
    @GetMapping("/plugin/{pluginId}/disable")
    public Map<String, Object> disablePlugin(@PathVariable String pluginId) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "disable");
        result.put("pluginId", pluginId);
        
        try {
            boolean success = pluginSystem.disablePlugin(pluginId);
            result.put("success", success);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试接口
     */
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "测试成功，无安全限制");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
} 