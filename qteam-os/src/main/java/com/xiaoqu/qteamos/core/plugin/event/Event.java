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

import java.util.UUID;
import java.io.Serializable;

/**
 * 事件基类
 * 所有插件系统事件的基础类
 *
 * @author yangqijun
 * @date 2024-07-15
 * @since 1.0.0
 */
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String id;
    private final String topic;
    private final String type;
    private final long timestamp;
    private final Object source;
    private Object data;
    private boolean cancelled;
    private final boolean cancellable;
    
    /**
     * 创建事件
     *
     * @param topic 事件主题
     * @param type 事件类型
     */
    public Event(String topic, String type) {
        this(topic, type, null);
    }
    
    /**
     * 创建事件
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param data 事件数据
     */
    public Event(String topic, String type, Object data) {
        this.id = UUID.randomUUID().toString();
        this.topic = topic;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.source = this;
        this.data = data;
        this.cancelled = false;
        this.cancellable = true;
    }
    
    /**
     * 获取事件ID
     *
     * @return 事件ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取事件ID (兼容API)
     *
     * @return 事件ID
     */
    public String getEventId() {
        return id;
    }
    
    /**
     * 获取事件主题
     *
     * @return 事件主题
     */
    public String getTopic() {
        return topic;
    }
    
    /**
     * 获取事件类型
     *
     * @return 事件类型
     */
    public String getType() {
        return type;
    }
    
    /**
     * 获取事件时间戳
     *
     * @return 事件时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 获取事件来源
     *
     * @return 事件来源
     */
    public Object getSource() {
        return source;
    }
    
    /**
     * 获取事件数据
     *
     * @return 事件数据
     */
    public Object getData() {
        return data;
    }
    
    /**
     * 设置事件数据
     *
     * @param data 事件数据
     */
    public void setData(Object data) {
        this.data = data;
    }
    
    /**
     * 是否可取消
     *
     * @return 是否可取消
     */
    public boolean isCancellable() {
        return cancellable;
    }
    
    /**
     * 是否已取消
     *
     * @return 是否已取消
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * 是否已取消 (兼容旧代码)
     *
     * @return 是否已取消
     */
    public boolean isCanceled() {
        return cancelled;
    }
    
    /**
     * 取消事件
     *
     * @return 是否成功取消
     */
    public boolean cancel() {
        if (cancellable) {
            this.cancelled = true;
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", topic='" + topic + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                ", cancelled=" + cancelled +
                '}';
    }
} 