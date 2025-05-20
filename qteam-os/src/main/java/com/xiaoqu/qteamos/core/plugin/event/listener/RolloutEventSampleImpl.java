package com.xiaoqu.qteamos.core.plugin.event.listener;

import com.xiaoqu.qteamos.core.plugin.event.Event;
import com.xiaoqu.qteamos.core.plugin.event.EventListener;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 灰度发布事件处理示例
 * 演示如何集成灰度发布事件到网关模块
 * 
 * @author yangqijun
 * @date 2024-07-20
 */
@Component
public class RolloutEventSampleImpl {
    private static final Logger log = LoggerFactory.getLogger(RolloutEventSampleImpl.class);
    
    /**
     * 处理灰度发布启动事件
     * 当有新的灰度发布开始时，可以通知网关更新路由配置
     */
    @EventListener(topics = "plugin.rollout", types = "started", priority = 10)
    public void onRolloutStarted(Event event) {
        if (!(event instanceof PluginRolloutEvent)) {
            return;
        }
        
        PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
        log.info("网关收到灰度发布启动事件: 插件[{}], 版本[{} -> {}]",
                rolloutEvent.getPluginId(),
                rolloutEvent.getFromVersion(),
                rolloutEvent.getToVersion());
        
        // 模拟网关操作：准备路由配置
        prepareGatewayConfig(
            rolloutEvent.getPluginId(), 
            rolloutEvent.getFromVersion(), 
            rolloutEvent.getToVersion()
        );
    }
    
    /**
     * 处理灰度发布完成事件
     * 当灰度发布完成后，更新网关路由配置
     */
    @EventListener(topics = "plugin.rollout", types = "completed", priority = 10)
    public void onRolloutCompleted(Event event) {
        if (!(event instanceof PluginRolloutEvent)) {
            return;
        }
        
        PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
        log.info("网关收到灰度发布完成事件: 插件[{}], 版本[{}]全量发布完成",
                rolloutEvent.getPluginId(),
                rolloutEvent.getToVersion());
        
        // 模拟网关操作：更新路由配置为全量路由
        updateToFullTrafficRouting(
            rolloutEvent.getPluginId(), 
            rolloutEvent.getToVersion()
        );
    }
    
    /**
     * 处理灰度发布批次完成事件
     * 根据当前百分比更新流量分配
     */
    @EventListener(topics = "plugin.rollout", types = "batch_completed", priority = 5)
    public void onBatchCompleted(Event event) {
        if (!(event instanceof PluginRolloutEvent)) {
            return;
        }
        
        PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
        int percentage = rolloutEvent.getPercentage();
        
        log.info("网关收到批次完成事件: 插件[{}], 当前进度[{}%]", 
                rolloutEvent.getPluginId(),
                percentage);
        
        // 模拟网关操作：根据百分比调整流量分配
        adjustTrafficDistribution(
            rolloutEvent.getPluginId(),
            rolloutEvent.getFromVersion(),
            rolloutEvent.getToVersion(),
            percentage
        );
    }
    
    /**
     * 处理灰度发布失败事件
     * 将流量全部路由回旧版本
     */
    @EventListener(topics = "plugin.rollout", types = {"failed", "cancelled"}, priority = 20)
    public void onRolloutFailed(Event event) {
        if (!(event instanceof PluginRolloutEvent)) {
            return;
        }
        
        PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
        log.info("网关收到灰度发布失败事件: 插件[{}], 原因[{}]",
                rolloutEvent.getPluginId(),
                rolloutEvent.getMessage());
        
        // 模拟网关操作：回滚路由配置
        rollbackGatewayConfig(
            rolloutEvent.getPluginId(),
            rolloutEvent.getFromVersion()
        );
    }
    
    /**
     * 模拟方法：准备网关配置
     */
    private void prepareGatewayConfig(String pluginId, String oldVersion, String newVersion) {
        // 在实际实现中，这里可能会：
        // 1. 创建新版本的路由配置
        // 2. 初始化灰度规则
        // 3. 准备监控和告警
        log.debug("准备插件[{}]的网关配置: {} -> {}", pluginId, oldVersion, newVersion);
    }
    
    /**
     * 模拟方法：调整流量分配
     */
    private void adjustTrafficDistribution(String pluginId, String oldVersion, 
                                         String newVersion, int percentage) {
        // 在实际实现中，这里可能会：
        // 1. 更新网关的路由权重
        // 2. 调整负载均衡策略
        // 3. 更新流量规则
        log.debug("调整插件[{}]的流量分配: {}%流量路由到版本[{}]", 
                pluginId, percentage, newVersion);
    }
    
    /**
     * 模拟方法：更新为全量路由
     */
    private void updateToFullTrafficRouting(String pluginId, String version) {
        // 在实际实现中，这里可能会：
        // 1. 将所有流量路由到新版本
        // 2. 清理旧版本的路由规则
        // 3. 更新API文档
        log.debug("更新插件[{}]为全量路由到版本[{}]", pluginId, version);
    }
    
    /**
     * 模拟方法：回滚网关配置
     */
    private void rollbackGatewayConfig(String pluginId, String version) {
        // 在实际实现中，这里可能会：
        // 1. 将所有流量路由回旧版本
        // 2. 清理失败版本的路由配置
        // 3. 发送告警通知
        log.debug("回滚插件[{}]的网关配置到版本[{}]", pluginId, version);
    }
} 