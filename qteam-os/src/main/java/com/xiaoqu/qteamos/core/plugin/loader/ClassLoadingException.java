package com.xiaoqu.qteamos.core.plugin.loader;

/**
 * 类加载异常
 * 用于表示插件类加载过程中发生的异常
 * 
 * @author yangqijun
 * @version 1.0.0
 */
public class ClassLoadingException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 异常类型枚举
     */
    public enum ErrorType {
        /**
         * 类未找到
         */
        CLASS_NOT_FOUND,
        
        /**
         * 类加载失败
         */
        CLASS_LOADING_FAILED,
        
        /**
         * 类被禁止加载
         */
        CLASS_BLOCKED,
        
        /**
         * 类加载器已关闭
         */
        CLASSLOADER_CLOSED,
        
        /**
         * 资源未找到
         */
        RESOURCE_NOT_FOUND,
        
        /**
         * 未知错误
         */
        UNKNOWN
    }
    
    /**
     * 错误类型
     */
    private final ErrorType errorType;
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     * @param errorType 错误类型
     */
    public ClassLoadingException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }
    
    /**
     * 构造函数
     * 
     * @param message 错误信息
     * @param cause 原始异常
     * @param errorType 错误类型
     */
    public ClassLoadingException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    /**
     * 获取错误类型
     * 
     * @return 错误类型
     */
    public ErrorType getErrorType() {
        return errorType;
    }
} 