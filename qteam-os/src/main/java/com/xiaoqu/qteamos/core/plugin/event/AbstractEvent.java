package com.xiaoqu.qteamos.core.plugin.event;

/**
 * 抽象事件类
 * 提供事件的基本实现，作为所有具体事件的基类
 *
 * @author yangqijun
 * @date 2024-07-03
 */
public abstract class AbstractEvent extends Event {
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     */
    protected AbstractEvent(String topic, String type, Object source) {
        super(topic, type, source);
    }
    
    @Override
    public String toString() {
        return "AbstractEvent{" +
                "topic='" + getTopic() + '\'' +
                ", type='" + getType() + '\'' +
                ", source='" + getSource() + '\'' +
                ", timestamp=" + getTimestamp() +
                ", cancelled=" + isCancelled() +
                ", cancellable=" + isCancellable() +
                '}';
    }
} 