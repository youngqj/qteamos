package com.xiaoqu.qteamos.core.plugin.manager.exception;

/**
 * 插件生命周期异常
 * 在插件的加载、初始化、启动、停止和卸载过程中可能抛出
 *
 * @author yangqijun
 * @date 2024-07-02
 */
public class PluginLifecycleException extends Exception {
    
    public PluginLifecycleException(String message) {
        super(message);
    }
    
    public PluginLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PluginLifecycleException(Throwable cause) {
        super(cause);
    }
} 