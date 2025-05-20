package com.xiaoqu.qteamos.core.plugin.running;

/**
 * 插件异常
 * 用于处理插件相关的异常情况
 * 
 * @author yangqijun
 * @version 1.0.0
 */
public class PluginException extends RuntimeException {
    
    /**
     * 异常类型
     */
    private final PluginExceptionType type;
    
    /**
     * 插件ID
     */
    private final String pluginId;
    
    /**
     * 构造函数
     * 
     * @param message 异常信息
     */
    public PluginException(String message) {
        this(message, null);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常信息
     * @param cause 原因
     */
    public PluginException(String message, Throwable cause) {
        this(PluginExceptionType.GENERAL, null, message, cause);
    }
    
    /**
     * 构造函数
     * 
     * @param type 异常类型
     * @param message 异常信息
     */
    public PluginException(PluginExceptionType type, String message) {
        this(type, null, message, null);
    }
    
    /**
     * 构造函数
     * 
     * @param type 异常类型
     * @param pluginId 插件ID
     * @param message 异常信息
     */
    public PluginException(PluginExceptionType type, String pluginId, String message) {
        this(type, pluginId, message, null);
    }
    
    /**
     * 构造函数
     * 
     * @param type 异常类型
     * @param pluginId 插件ID
     * @param message 异常信息
     * @param cause 原因
     */
    public PluginException(PluginExceptionType type, String pluginId, String message, Throwable cause) {
        super(message, cause);
        this.type = type;
        this.pluginId = pluginId;
    }
    
    /**
     * 获取异常类型
     * 
     * @return 异常类型
     */
    public PluginExceptionType getType() {
        return type;
    }
    
    /**
     * 获取插件ID
     * 
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    @Override
    public String getMessage() {
        if (pluginId != null && !pluginId.isEmpty()) {
            return "[" + type + "] [Plugin: " + pluginId + "] " + super.getMessage();
        }
        return "[" + type + "] " + super.getMessage();
    }
    
    /**
     * 插件异常类型
     */
    public enum PluginExceptionType {
        
        /**
         * 通用异常
         */
        GENERAL,
        
        /**
         * 插件不存在
         */
        NOT_FOUND,
        
        /**
         * 插件加载失败
         */
        LOAD_FAILURE,
        
        /**
         * 插件初始化失败
         */
        INIT_FAILURE,
        
        /**
         * 插件启动失败
         */
        START_FAILURE,
        
        /**
         * 插件停止失败
         */
        STOP_FAILURE,
        
        /**
         * 插件卸载失败
         */
        UNLOAD_FAILURE,
        
        /**
         * 插件依赖错误
         */
        DEPENDENCY_ERROR,
        
        /**
         * 插件版本冲突
         */
        VERSION_CONFLICT,
        
        /**
         * 插件描述符错误
         */
        DESCRIPTOR_ERROR,
        
        /**
         * 插件权限错误
         */
        PERMISSION_ERROR,
        
        /**
         * 插件配置错误
         */
        CONFIG_ERROR
    }
} 