/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:32:16
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 13:55:33
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/sdk/plugin/api/UiServiceApi.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.api.core.plugin.api;

import java.util.Map;

/**
 * UI服务API接口
 * 提供插件UI交互能力
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface UiServiceApi {

    /**
     * 注册UI组件
     *
     * @param componentId 组件ID
     * @param componentType 组件类型
     * @param config 组件配置
     * @return 是否注册成功
     */
    boolean registerComponent(String componentId, String componentType, Map<String, Object> config);

    /**
     * 注销UI组件
     *
     * @param componentId 组件ID
     * @return 是否注销成功
     */
    boolean unregisterComponent(String componentId);

    /**
     * 更新UI组件配置
     *
     * @param componentId 组件ID
     * @param config 组件配置
     * @return 是否更新成功
     */
    boolean updateComponentConfig(String componentId, Map<String, Object> config);

    /**
     * 显示通知消息
     *
     * @param title 标题
     * @param message 消息内容
     * @param type 通知类型 (info, warning, error, success)
     * @return 通知ID
     */
    String showNotification(String title, String message, String type);

    /**
     * 显示确认对话框
     *
     * @param title 标题
     * @param message 消息内容
     * @param callback 回调函数名称
     * @return 对话框ID
     */
    String showConfirmDialog(String title, String message, String callback);

    /**
     * 注册前端回调处理器
     *
     * @param callbackName 回调名称
     * @param handler 处理器
     * @return 是否注册成功
     */
    boolean registerCallbackHandler(String callbackName, CallbackHandler handler);

    /**
     * 回调处理器接口
     */
    interface CallbackHandler {
        /**
         * 处理回调
         *
         * @param params 参数
         * @return 处理结果
         */
        Object handle(Map<String, Object> params);
    }
} 