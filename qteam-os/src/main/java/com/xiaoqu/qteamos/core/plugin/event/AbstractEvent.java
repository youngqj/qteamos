package com.xiaoqu.qteamos.core.plugin.event;

/**
 * 抽象事件类
 * 提供事件接口的基本实现，作为所有具体事件的基类
 *
 * @author yangqijun
 * @date 2024-07-03
 */
public abstract class AbstractEvent implements Event {
    
    private final String topic;
    private final String type;
    private final String source;
    private final long timestamp;
    private boolean cancelled;
    private final boolean cancellable;
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     */
    protected AbstractEvent(String topic, String type, String source) {
        this(topic, type, source, false);
    }
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param cancellable 是否可取消
     */
    protected AbstractEvent(String topic, String type, String source, boolean cancellable) {
        this.topic = topic;
        this.type = type;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
        this.cancellable = cancellable;
        this.cancelled = false;
    }
    
    @Override
    public String getTopic() {
        return topic;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public String getSource() {
        return source;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public boolean isCancellable() {
        return cancellable;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public boolean cancel() {
        if (cancellable) {
            cancelled = true;
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "Event{" +
                "topic='" + topic + '\'' +
                ", type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", timestamp=" + timestamp +
                ", cancelled=" + cancelled +
                ", cancellable=" + cancellable +
                '}';
    }
} 