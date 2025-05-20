/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:30:41
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-30 21:31:09
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/sdk/plugin/api/LogServiceApi.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.api.core.plugin.api;

/**
 * 日志服务API接口
 * 提供插件日志记录能力
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface LogServiceApi {

    /**
     * 记录调试级别日志
     *
     * @param message 日志消息
     */
    void debug(String message);

    /**
     * 记录调试级别日志（带参数）
     *
     * @param message 日志消息模板
     * @param args 参数
     */
    void debug(String message, Object... args);

    /**
     * 记录信息级别日志
     *
     * @param message 日志消息
     */
    void info(String message);

    /**
     * 记录信息级别日志（带参数）
     *
     * @param message 日志消息模板
     * @param args 参数
     */
    void info(String message, Object... args);

    /**
     * 记录警告级别日志
     *
     * @param message 日志消息
     */
    void warn(String message);

    /**
     * 记录警告级别日志（带参数）
     *
     * @param message 日志消息模板
     * @param args 参数
     */
    void warn(String message, Object... args);

    /**
     * 记录错误级别日志
     *
     * @param message 日志消息
     */
    void error(String message);

    /**
     * 记录错误级别日志（带参数）
     *
     * @param message 日志消息模板
     * @param args 参数
     */
    void error(String message, Object... args);

    /**
     * 记录错误级别日志（带异常）
     *
     * @param message 日志消息
     * @param t 异常
     */
    void error(String message, Throwable t);
} 