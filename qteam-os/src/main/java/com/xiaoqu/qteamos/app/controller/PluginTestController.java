package com.xiaoqu.qteamos.app.controller;

import com.esotericsoftware.minlog.Log;
import com.xiaoqu.qteamos.common.result.Result;
import com.xiaoqu.qteamos.core.plugin.PluginSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件测试控制器
 * 提供插件管理相关的API接口，用于测试
 * 
 * @author yangqijun
 * @since 2025-05-05
 */
@RestController
@RequestMapping("/api/test/plugins")
public class PluginTestController {

    @Autowired
    private PluginSystem pluginSystem;
    
    /**
     * 启用插件
     * 
     * @param pluginId 插件ID
     * @return 操作结果
     */
    @GetMapping("/{pluginId}/enable")
    public ResponseEntity<Map<String, Object>> enablePlugin(@PathVariable String pluginId) {
        Map<String, Object> result = new HashMap<>();

        Log.debug("enablePlugin: " + pluginId);
        
        try {
            boolean success = pluginSystem.enablePlugin(pluginId);
            result.put("success", success);
            result.put("message", success ? "插件启用成功" : "插件启用失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "插件启用异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 禁用插件
     * 
     * @param pluginId 插件ID
     * @return 操作结果
     */
    @PostMapping("/{pluginId}/disable")
    public ResponseEntity<Map<String, Object>> disablePlugin(@PathVariable String pluginId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean success = pluginSystem.disablePlugin(pluginId);
            result.put("success", success);
            result.put("message", success ? "插件禁用成功" : "插件禁用失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "插件禁用异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("test");
    }
    /**
     * 获取插件状态
     * 
     * @param pluginId 插件ID
     * @return 插件状态
     */
    @GetMapping("/{pluginId}")
    public ResponseEntity<Map<String, Object>> getPluginStatus(@PathVariable String pluginId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            var pluginInfo = pluginSystem.getPlugin(pluginId);
            if (pluginInfo.isPresent()) {
                var info = pluginInfo.get();
                result.put("pluginId", info.getPluginId());
                result.put("version", info.getVersion());
                result.put("enabled", info.isEnabled());
                result.put("state", info.getState().name());
                
                return ResponseEntity.ok(result);
            } else {
                result.put("success", false);
                result.put("message", "插件不存在: " + pluginId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取插件状态异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
} 