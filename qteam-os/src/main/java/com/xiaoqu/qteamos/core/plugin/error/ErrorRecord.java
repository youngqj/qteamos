package com.xiaoqu.qteamos.core.plugin.error;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 插件错误记录
 * 用于跟踪插件错误信息和统计
 *
 * @author yangqijun
 * @date 2025-05-01
 */
@Data
public class ErrorRecord {

    /**
     * 总错误次数
     */
    private final AtomicInteger totalErrorCount = new AtomicInteger(0);
    
    /**
     * 连续错误次数
     */
    private final AtomicInteger consecutiveErrorCount = new AtomicInteger(0);
    
    /**
     * 最近一次错误时间
     */
    private LocalDateTime lastErrorTime;
    
    /**
     * 最近一次错误消息
     */
    private String lastErrorMessage;
    
    /**
     * 最近一次错误操作类型
     */
    private PluginErrorHandler.OperationType lastOperationType;
    
    /**
     * 错误历史记录
     */
    private final List<ErrorItem> errorHistory = new ArrayList<>();
    
    /**
     * 最大历史记录数
     */
    private static final int MAX_HISTORY_SIZE = 10;
    
    /**
     * 添加错误记录
     *
     * @param exception 异常
     * @param operationType 操作类型
     */
    public void addError(Throwable exception, PluginErrorHandler.OperationType operationType) {
        totalErrorCount.incrementAndGet();
        consecutiveErrorCount.incrementAndGet();
        
        lastErrorTime = LocalDateTime.now();
        lastErrorMessage = exception.getMessage();
        lastOperationType = operationType;
        
        // 添加到历史记录
        ErrorItem item = new ErrorItem(
                LocalDateTime.now(),
                exception.getClass().getName(),
                exception.getMessage(),
                operationType
        );
        
        synchronized (errorHistory) {
            errorHistory.add(item);
            // 保持历史记录不超过最大值
            if (errorHistory.size() > MAX_HISTORY_SIZE) {
                errorHistory.remove(0);
            }
        }
    }
    
    /**
     * 重置连续错误计数
     */
    public void resetConsecutiveErrors() {
        consecutiveErrorCount.set(0);
    }
    
    /**
     * 获取连续错误次数
     */
    public int getConsecutiveErrorCount() {
        return consecutiveErrorCount.get();
    }
    
    /**
     * 获取总错误次数
     */
    public int getTotalErrorCount() {
        return totalErrorCount.get();
    }
    
    /**
     * 错误详细信息
     */
    @Data
    public static class ErrorItem {
        private final LocalDateTime timestamp;
        private final String exceptionType;
        private final String message;
        private final PluginErrorHandler.OperationType operationType;
    }
} 