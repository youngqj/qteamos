/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 10:23:52
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 10:31:51
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/monitoring/PluginMonitoringController.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.monitoring;

import com.xiaoqu.qteamos.common.result.Result;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 插件监控Controller
 * 提供插件健康状态查询和管理接口
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@RestController
@RequestMapping("/api/plugins/monitoring")
public class PluginMonitoringController {

    @Autowired
    private PluginMonitoringService monitoringService;
    
    @Autowired
    private PluginLifecycleManager lifecycleManager;

    /**
     * 获取所有插件健康状态
     */
    @GetMapping("/health")
    public Result<Collection<PluginHealthSnapshot>> getAllPluginsHealth() {
        return Result.success(monitoringService.getAllHealthSnapshots());
    }

    /**
     * 获取指定插件健康状态
     */
    @GetMapping("/health/{pluginId}")
    public Result<PluginHealthSnapshot> getPluginHealth(@PathVariable String pluginId) {
        Optional<PluginHealthSnapshot> snapshot = monitoringService.getPluginHealthSnapshot(pluginId);
        return snapshot.map(Result::success).orElseGet(() -> Result.failed("插件不存在或未加载"));
    }

    /**
     * 获取所有不健康的插件
     */
    @GetMapping("/unhealthy")
    public Result<Collection<PluginHealthSnapshot>> getUnhealthyPlugins() {
        return Result.success(monitoringService.getUnhealthySnapshots());
    }

    /**
     * 重置插件错误计数
     */
    @PostMapping("/reset-errors/{pluginId}")
    public Result<Boolean> resetPluginErrors(@PathVariable String pluginId) {
        boolean success = monitoringService.resetPluginErrorCount(pluginId);
        return success ? Result.success(true) : Result.failed("插件不存在或未加载");
    }

    /**
     * 获取插件版本历史
     */
    @GetMapping("/versions/{pluginId}")
    public Result<Map<String, String>> getPluginVersionHistory(@PathVariable String pluginId) {
        Map<String, String> versionHistory = lifecycleManager.getPluginVersionHistory(pluginId);
        if (versionHistory.isEmpty()) {
            return Result.failed("未找到插件版本历史或插件不存在");
        }
        return Result.success(versionHistory);
    }

    /**
     * 触发插件健康检查
     */
    @PostMapping("/check-health")
    public Result<Map<String, Boolean>> triggerHealthCheck() {
        lifecycleManager.performHealthCheck();
        
        // 返回健康检查结果
        Map<String, Boolean> healthStatus = monitoringService.getAllHealthSnapshots().stream()
                .collect(Collectors.toMap(
                        PluginHealthSnapshot::getPluginId,
                        PluginHealthSnapshot::isHealthy
                ));
        
        return Result.success(healthStatus);
    }
} 