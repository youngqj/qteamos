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
 * 主题化事件基类
 * 为事件添加主题和类型概念，用于事件分类和路由
 *
 * @author yangqijun
 * @date 2025-08-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.base;

public abstract class TopicEvent extends BaseEvent {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 事件主题
     */
    private final String topic;
    
    /**
     * 事件类型
     */
    private final String type;
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     */
    protected TopicEvent(String topic, String type, String source) {
        super(source);
        this.topic = topic;
        this.type = type;
    }
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param data 事件附加数据
     */
    protected TopicEvent(String topic, String type, String source, Object data) {
        super(source, data);
        this.topic = topic;
        this.type = type;
    }
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param data 事件附加数据
     * @param cancellable 是否可取消
     */
    protected TopicEvent(String topic, String type, String source, Object data, boolean cancellable) {
        super(source, data, cancellable);
        this.topic = topic;
        this.type = type;
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
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "eventId='" + getEventId() + '\'' +
                ", topic='" + topic + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + getTimestamp() +
                ", source='" + getSource() + '\'' +
                ", cancellable=" + isCancellable() +
                ", cancelled=" + isCancelled() +
                '}';
    }
} 