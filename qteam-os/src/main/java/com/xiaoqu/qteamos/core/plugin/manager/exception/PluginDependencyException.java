package com.xiaoqu.qteamos.core.plugin.manager.exception;

/**
 * 插件依赖异常
 * 在处理插件依赖关系时可能抛出，如循环依赖、缺失依赖等
 *
 * @author yangqijun
 * @date 2024-07-02
 */
public class PluginDependencyException extends Exception {
    
    public PluginDependencyException(String message) {
        super(message);
    }
    
    public PluginDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PluginDependencyException(Throwable cause) {
        super(cause);
    }
} 