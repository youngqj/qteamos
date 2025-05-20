/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * 日志工具类
 * 提供统一的日志记录功能
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.sdk.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 * 封装SLF4J，提供统一的日志记录接口
 */
public class LogUtils {
    
    private LogUtils() {
        // 私有构造方法，防止实例化
    }
    
    /**
     * 获取Logger实例
     * 
     * @param clazz 类
     * @return Logger实例
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
    
    /**
     * 获取Logger实例
     * 
     * @param name 名称
     * @return Logger实例
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger(name);
    }
    
    /**
     * 获取插件Logger实例
     * 使用插件ID作为Logger名称前缀
     * 
     * @param pluginId 插件ID
     * @param clazz 类
     * @return Logger实例
     */
    public static Logger getPluginLogger(String pluginId, Class<?> clazz) {
        return LoggerFactory.getLogger("plugin." + pluginId + "." + clazz.getName());
    }
    
    /**
     * 记录调试日志
     * 
     * @param logger Logger实例
     * @param message 日志消息
     * @param args 参数
     */
    public static void debug(Logger logger, String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, args);
        }
    }
    
    /**
     * 记录信息日志
     * 
     * @param logger Logger实例
     * @param message 日志消息
     * @param args 参数
     */
    public static void info(Logger logger, String message, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(message, args);
        }
    }
    
    /**
     * 记录警告日志
     * 
     * @param logger Logger实例
     * @param message 日志消息
     * @param args 参数
     */
    public static void warn(Logger logger, String message, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(message, args);
        }
    }
    
    /**
     * 记录错误日志
     * 
     * @param logger Logger实例
     * @param message 日志消息
     * @param args 参数
     */
    public static void error(Logger logger, String message, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(message, args);
        }
    }
    
    /**
     * 记录错误日志
     * 
     * @param logger Logger实例
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void error(Logger logger, String message, Throwable throwable) {
        if (logger.isErrorEnabled()) {
            logger.error(message, throwable);
        }
    }
} 