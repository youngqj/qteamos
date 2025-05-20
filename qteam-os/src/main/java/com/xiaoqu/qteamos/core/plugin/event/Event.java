/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-27 19:58:21
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-27 20:00:00
 * @FilePath: /QEleBase/qelebase-core/src/main/java/com/xiaoqu/qelebase/core/pluginSource/event/Event.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.event;

/**
 * 事件接口
 * 所有插件系统中的事件都应实现此接口
 *
 * @author yangqijun
 * @date 2024-07-03
 */
public interface Event {
    
    /**
     * 获取事件主题
     * 主题用于对事件进行分类和过滤
     *
     * @return 事件主题
     */
    String getTopic();
    
    /**
     * 获取事件类型
     * 用于更细粒度的事件分类
     *
     * @return 事件类型
     */
    String getType();
    
    /**
     * 获取事件来源
     * 标识事件的产生者，例如插件ID
     *
     * @return 事件来源
     */
    String getSource();
    
    /**
     * 获取事件发生时间戳
     *
     * @return 时间戳（毫秒）
     */
    long getTimestamp();
    
    /**
     * 事件是否可取消
     * 如果返回true，事件处理器可以取消事件的传播
     *
     * @return 是否可取消
     */
    default boolean isCancellable() {
        return false;
    }
    
    /**
     * 事件是否已取消
     * 对于可取消的事件，返回其当前是否已被取消
     *
     * @return 是否已取消
     */
    default boolean isCancelled() {
        return false;
    }
    
    /**
     * 取消事件
     * 对于可取消的事件，调用此方法可以阻止事件继续传播
     *
     * @return 是否成功取消
     */
    default boolean cancel() {
        return false;
    }
} 