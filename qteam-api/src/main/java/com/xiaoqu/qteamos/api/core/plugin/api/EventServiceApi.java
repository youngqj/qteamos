package com.xiaoqu.qteamos.api.core.plugin.api;

import java.util.function.Consumer;

/**
 * 事件服务API接口
 * 提供插件事件发布和订阅能力
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface EventServiceApi {

    /**
     * 发布事件
     *
     * @param eventType 事件类型
     * @param data 事件数据
     */
    void publishEvent(String eventType, Object data);

    /**
     * 订阅事件
     *
     * @param eventType 事件类型
     * @param listener 事件监听器
     * @return 订阅ID，用于取消订阅
     */
    String subscribeEvent(String eventType, Consumer<Object> listener);

    /**
     * 取消订阅
     *
     * @param subscriptionId 订阅ID
     * @return 是否成功取消
     */
    boolean unsubscribeEvent(String subscriptionId);

    /**
     * 注册全局事件处理器
     *
     * @param listener 事件监听器
     * @return 订阅ID，用于取消注册
     */
    String registerGlobalHandler(Consumer<EventContext> listener);

    /**
     * 取消全局事件处理器
     *
     * @param handlerId 处理器ID
     * @return 是否成功取消
     */
    boolean unregisterGlobalHandler(String handlerId);

    /**
     * 事件上下文
     */
    interface EventContext {
        /**
         * 获取事件类型
         *
         * @return 事件类型
         */
        String getEventType();

        /**
         * 获取事件数据
         *
         * @return 事件数据
         */
        Object getEventData();

        /**
         * 获取事件源（发布者）
         *
         * @return 事件源
         */
        String getSource();

        /**
         * 获取事件发生时间
         *
         * @return 事件发生时间
         */
        long getTimestamp();
    }
} 