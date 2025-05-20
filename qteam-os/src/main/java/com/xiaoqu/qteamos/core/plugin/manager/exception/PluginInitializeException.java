package com.xiaoqu.qteamos.core.plugin.manager.exception;

/**
 * 插件初始化异常
 * 在插件初始化过程中可能抛出
 *
 * @author yangqijun
 * @date 2024-07-02
 */
public class PluginInitializeException extends PluginLifecycleException {
    
    public PluginInitializeException(String message) {
        super(message);
    }
    
    public PluginInitializeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PluginInitializeException(Throwable cause) {
        super(cause);
    }
} 