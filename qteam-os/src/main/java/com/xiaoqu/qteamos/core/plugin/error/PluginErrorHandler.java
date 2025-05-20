package com.xiaoqu.qteamos.core.plugin.error;

import com.xiaoqu.qteamos.core.plugin.event.EventBus;
import com.xiaoqu.qteamos.core.plugin.event.PluginEvent;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager;
import com.xiaoqu.qteamos.core.plugin.manager.PluginLifecycleManager.PluginHealthStatus;
import com.xiaoqu.qteamos.core.plugin.running.PluginException;
import com.xiaoqu.qteamos.core.plugin.running.PluginInfo;
import com.xiaoqu.qteamos.core.plugin.running.PluginState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 插件错误处理器
 * 负责管理插件运行时异常、隔离异常影响范围、提供恢复策略
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Component
public class PluginErrorHandler implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PluginErrorHandler.class);

    @Autowired
    private PluginLifecycleManager lifecycleManager;

    @Autowired
    private EventBus eventBus;

    /**
     * 记录插件错误计数
     */
    private final Map<String, ErrorRecord> errorRecords = new ConcurrentHashMap<>();

    /**
     * 错误恢复执行器
     */
    private ScheduledExecutorService recoveryExecutor;

    /**
     * 最大连续错误次数（超过此值触发隔离）
     */
    private static final int MAX_CONSECUTIVE_ERRORS = 3;

    /**
     * 隔离时间（毫秒）
     */
    private static final long ISOLATION_PERIOD_MS = 60000; // 1分钟

    /**
     * 初始化错误处理器
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        recoveryExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "plugin-recovery");
            t.setDaemon(true);
            return t;
        });

        log.info("插件错误处理器初始化完成");
    }

    /**
     * 处理插件异常
     *
     * @param pluginId 插件ID
     * @param exception 异常
     * @param operationType 操作类型
     */
    public void handlePluginError(String pluginId, Throwable exception, OperationType operationType) {
        log.error("插件[{}]发生{}异常: {}", pluginId, operationType, exception.getMessage(), exception);

        // 记录错误
        recordError(pluginId, exception, operationType);

        // 发布错误事件
        eventBus.postEvent(PluginEvent.createErrorEvent(pluginId, 
                getPluginVersion(pluginId), exception));

        // 检查是否需要隔离
        if (shouldIsolatePlugin(pluginId)) {
            isolatePlugin(pluginId);
        }
    }

    /**
     * 记录错误
     */
    private void recordError(String pluginId, Throwable exception, OperationType operationType) {
        ErrorRecord record = errorRecords.computeIfAbsent(pluginId, k -> new ErrorRecord());
        record.addError(exception, operationType);

        // 更新健康状态
        lifecycleManager.getPluginHealthStatus(pluginId).incrementFailCount();
    }

    /**
     * 获取插件版本
     */
    private String getPluginVersion(String pluginId) {
        return lifecycleManager.getPluginInfo(pluginId)
                .map(info -> info.getDescriptor().getVersion())
                .orElse("unknown");
    }

    /**
     * 判断是否应该隔离插件
     */
    private boolean shouldIsolatePlugin(String pluginId) {
        ErrorRecord record = errorRecords.get(pluginId);
        return record != null && record.getConsecutiveErrorCount() >= MAX_CONSECUTIVE_ERRORS;
    }

    /**
     * 隔离插件
     */
    private void isolatePlugin(String pluginId) {
        log.warn("插件[{}]发生连续错误，进行隔离处理", pluginId);

        try {
            // 将插件状态设置为隔离状态
            lifecycleManager.getPluginInfo(pluginId).ifPresent(info -> {
                info.setState(PluginState.ISOLATED);
                info.setErrorMessage("发生连续错误，已隔离");
            });

            // 停止插件
            lifecycleManager.stopPlugin(pluginId);

            // 发布插件隔离事件
            eventBus.postEvent(PluginEvent.createIsolatedEvent(pluginId, getPluginVersion(pluginId)));

            // 安排恢复任务
            schedulePluginRecovery(pluginId);
        } catch (Exception e) {
            log.error("隔离插件[{}]时发生错误", pluginId, e);
        }
    }

    /**
     * 安排插件恢复任务
     */
    private void schedulePluginRecovery(String pluginId) {
        recoveryExecutor.schedule(() -> {
            try {
                log.info("尝试恢复隔离的插件: {}", pluginId);
                
                // 清除错误记录
                errorRecords.remove(pluginId);
                
                // 尝试重启插件
                if (lifecycleManager.startPlugin(pluginId)) {
                    log.info("插件[{}]恢复成功", pluginId);
                    eventBus.postEvent(PluginEvent.createRecoveredEvent(pluginId, getPluginVersion(pluginId)));
                } else {
                    log.warn("插件[{}]恢复失败，将继续保持隔离状态", pluginId);
                }
            } catch (Exception e) {
                log.error("恢复插件[{}]时发生错误", pluginId, e);
            }
        }, ISOLATION_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 手动触发插件恢复
     *
     * @param pluginId 插件ID
     * @return 是否成功恢复
     */
    public boolean recoverPlugin(String pluginId) {
        try {
            log.info("手动触发插件[{}]恢复", pluginId);
            
            // 清除错误记录
            errorRecords.remove(pluginId);
            
            // 尝试重启插件
            if (lifecycleManager.startPlugin(pluginId)) {
                log.info("插件[{}]手动恢复成功", pluginId);
                eventBus.postEvent(PluginEvent.createRecoveredEvent(pluginId, getPluginVersion(pluginId)));
                return true;
            } else {
                log.warn("插件[{}]手动恢复失败", pluginId);
                return false;
            }
        } catch (Exception e) {
            log.error("手动恢复插件[{}]时发生错误", pluginId, e);
            return false;
        }
    }

    /**
     * 获取插件错误记录
     *
     * @param pluginId 插件ID
     * @return 错误记录
     */
    public Optional<ErrorRecord> getErrorRecord(String pluginId) {
        return Optional.ofNullable(errorRecords.get(pluginId));
    }

    /**
     * 清除插件错误记录
     *
     * @param pluginId 插件ID
     */
    public void clearErrorRecord(String pluginId) {
        errorRecords.remove(pluginId);
    }

    /**
     * 操作类型
     */
    public enum OperationType {
        LOAD("加载"),
        INIT("初始化"),
        START("启动"),
        STOP("停止"),
        UNLOAD("卸载"),
        RUNTIME("运行时");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
} 