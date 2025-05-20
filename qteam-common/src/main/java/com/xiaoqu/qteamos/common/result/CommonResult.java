/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-10 18:05:28
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-10 18:06:59
 * @FilePath: /qelebase/qelebase-common/src/main/java/com/xiaoqu/qelebase/common/result/CommonResult.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.common.result;

import lombok.Data;

/**
 * 通用API返回结果
 *
 * @author yangqijun@xiaoquio.com
 * @version 1.0.0
 * @copyright 浙江小趣信息技术有限公司
 */
@Data
public class CommonResult<T> {
    /**
     * 状态码
     */
    private long code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 数据
     */
    private T data;

    protected CommonResult() {
    }

    protected CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回结果
     *
     * @param data 获取的数据
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回结果
     *
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> success() {
        return new CommonResult<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /**
     * 成功返回结果
     *
     * @param data 获取的数据
     * @param message 提示信息
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<T>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回结果
     *
     * @param message 提示信息
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<T>(ResultCode.FAILED.getCode(), message, null);
    }

    /**
     * 失败返回结果
     *
     * @param errorCode 错误码
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> failed(ResultCode errorCode) {
        return new CommonResult<T>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败返回结果
     *
     * @param errorCode 错误码
     * @param message 提示信息
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> failed(ResultCode errorCode, String message) {
        return new CommonResult<T>(errorCode.getCode(), message, null);
    }

    /**
     * 失败返回结果
     *
     * @param errorCode 错误码
     * @param message 提示信息
     * @param data 数据
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> failed(ResultCode errorCode, String message, T data) {
        return new CommonResult<T>(errorCode.getCode(), message, data);
    }

    /**
     * 参数验证失败返回结果
     *
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> validateFailed() {
        return failed(ResultCode.VALIDATE_FAILED);
    }

    /**
     * 参数验证失败返回结果
     *
     * @param message 提示信息
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<T>(ResultCode.VALIDATE_FAILED.getCode(), message, null);
    }

    /**
     * 未登录返回结果
     *
     * @param data 获取的数据
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> unauthorized(T data) {
        return new CommonResult<T>(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage(), data);
    }

    /**
     * 未授权返回结果
     *
     * @param data 获取的数据
     * @param <T> 数据类型
     * @return 通用返回结果
     */
    public static <T> CommonResult<T> forbidden(T data) {
        return new CommonResult<T>(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(), data);
    }
} 