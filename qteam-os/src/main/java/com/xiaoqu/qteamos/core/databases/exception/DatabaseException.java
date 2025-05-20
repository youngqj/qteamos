package com.xiaoqu.qteamos.core.databases.exception;

/**
 * 数据库异常
 * 封装所有数据库操作相关的异常
 *
 * @author yangqijun
 * @date 2025-05-02
 */
public class DatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 构造方法
     *
     * @param message 异常信息
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * 构造方法
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造方法
     *
     * @param cause 原始异常
     */
    public DatabaseException(Throwable cause) {
        super(cause);
    }
} 