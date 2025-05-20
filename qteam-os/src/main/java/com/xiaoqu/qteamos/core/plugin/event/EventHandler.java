/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-27 19:59:05
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-27 20:02:49
 * @FilePath: /QEleBase/qelebase-core/src/main/java/com/xiaoqu/qelebase/core/pluginSource/event/EventHandler.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.event;

/**
 * 事件处理器接口
 * 定义事件处理的标准方法，所有事件监听器都应实现此接口
 *
 * @author yangqijun
 * @date 2024-07-03
 */
public interface EventHandler {
    
    /**
     * 处理事件
     * 当事件发布时，此方法将被调用
     *
     * @param event 要处理的事件
     * @return 处理结果，如果返回false表示不继续传播事件
     */
    boolean handle(Event event);
    
    /**
     * 获取事件处理器的优先级
     * 数值越大优先级越高，默认为0
     *
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * 获取事件处理器关注的主题
     * 可以使用通配符"*"表示关注所有主题
     *
     * @return 关注的主题
     */
    default String[] getTopics() {
        return new String[]{"*"};
    }
    
    /**
     * 获取事件处理器关注的类型
     * 可以使用通配符"*"表示关注所有类型
     *
     * @return 关注的类型
     */
    default String[] getTypes() {
        return new String[]{"*"};
    }
    
    /**
     * 是否同步处理事件
     * 如果返回true，事件将在发布线程中同步处理
     * 如果返回false，事件将在事件处理线程池中异步处理
     *
     * @return 是否同步处理
     */
    default boolean isSynchronous() {
        return true;
    }
    
    /**
     * 是否在处理事件发生异常时继续传播事件
     * 如果返回true，即使处理过程中发生异常也会继续传播事件
     * 如果返回false，发生异常将终止事件传播
     *
     * @return 是否继续传播
     */
    default boolean isContinueOnError() {
        return false;
    }
} 