package com.xiaoqu.qteamos.core.cache.exception;

/**
 * 缓存异常
 *
 * @author yangqijun
 * @date 2025-05-04
 */
public class CacheException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     *
     * @param message 异常信息
     */
    public CacheException(String message) {
        super(message);
    }

    /**
     * 构造函数
     *
     * @param message 异常信息
     * @param cause 异常原因
     */
    public CacheException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造函数
     *
     * @param cause 异常原因
     */
    public CacheException(Throwable cause) {
        super(cause);
    }
} 