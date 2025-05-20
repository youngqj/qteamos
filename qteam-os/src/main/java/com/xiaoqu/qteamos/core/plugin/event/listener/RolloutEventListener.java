package com.xiaoqu.qteamos.core.plugin.event.listener;

import com.xiaoqu.qteamos.core.plugin.event.Event;
import com.xiaoqu.qteamos.core.plugin.event.EventListener;
import com.xiaoqu.qteamos.core.plugin.event.plugins.PluginRolloutEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 灰度发布事件监听器
 * 用于监听和处理灰度发布事件
 *
 * @author yangqijun
 * @date 2024-07-20
 */
@Component
public class RolloutEventListener {
    private static final Logger log = LoggerFactory.getLogger(RolloutEventListener.class);
    
    /**
     * 处理灰度发布开始事件
     */
    @EventListener(topics = "plugin.rollout", types = "started")
    public void onRolloutStarted(Event event) {
        if (event instanceof PluginRolloutEvent) {
            PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
            log.info("插件[{}]开始灰度发布: {} -> {}", 
                    rolloutEvent.getPluginId(),
                    rolloutEvent.getFromVersion(),
                    rolloutEvent.getToVersion());
            
            // 这里可以添加额外的处理逻辑
            // 例如: 发送通知、更新监控系统等
        }
    }
    
    /**
     * 处理批次完成事件
     * 异步处理，不阻塞主流程
     */
    @EventListener(topics = "plugin.rollout", types = "batch_completed", synchronous = false)
    public void onBatchCompleted(Event event) {
        if (event instanceof PluginRolloutEvent) {
            PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
            log.info("插件[{}]灰度发布批次完成: 进度 {}%", 
                    rolloutEvent.getPluginId(), 
                    rolloutEvent.getPercentage());
            
            // 这里可以添加批次完成后的处理逻辑
            // 例如: 执行健康检查、发送进度报告等
        }
    }
    
    /**
     * 处理灰度发布完成事件
     */
    @EventListener(topics = "plugin.rollout", types = "completed", priority = 10)
    public void onRolloutCompleted(Event event) {
        if (event instanceof PluginRolloutEvent) {
            PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
            log.info("插件[{}]灰度发布完成: {} -> {}", 
                    rolloutEvent.getPluginId(),
                    rolloutEvent.getFromVersion(),
                    rolloutEvent.getToVersion());
            
            // 这里可以添加灰度发布完成后的处理逻辑
            // 例如: 更新插件状态、清理临时文件、通知相关组件等
        }
    }
    
    /**
     * 处理灰度发布失败事件
     */
    @EventListener(topics = "plugin.rollout", types = {"failed", "cancelled"}, priority = 20)
    public void onRolloutFailed(Event event) {
        if (event instanceof PluginRolloutEvent) {
            PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
            log.warn("插件[{}]灰度发布失败: 进度 {}%, 原因: {}", 
                    rolloutEvent.getPluginId(),
                    rolloutEvent.getPercentage(),
                    rolloutEvent.getMessage());
            
            // 这里可以添加灰度发布失败后的处理逻辑
            // 例如: 执行回滚操作、发送告警通知、记录日志等
        }
    }
    
    /**
     * 监控全部灰度发布事件
     * 优先级低，用于记录所有事件
     */
    @EventListener(topics = "plugin.rollout", types = "*", priority = -10)
    public void onAnyRolloutEvent(Event event) {
        if (event instanceof PluginRolloutEvent) {
            PluginRolloutEvent rolloutEvent = (PluginRolloutEvent) event;
            
            // 这里可以添加通用的事件处理逻辑
            // 例如: 记录事件日志、发送到消息队列、更新统计信息等
            log.debug("灰度发布事件: 插件[{}], 类型[{}], 进度[{}%]", 
                    rolloutEvent.getPluginId(),
                    rolloutEvent.getType(),
                    rolloutEvent.getPercentage());
        }
    }
} 