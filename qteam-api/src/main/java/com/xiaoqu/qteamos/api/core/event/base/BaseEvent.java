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
 * 系统事件基类
 * 所有事件的顶层基类，提供基本事件属性和方法
 *
 * @author yangqijun
 * @date 2025-08-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event.base;

import java.io.Serializable;
import java.util.UUID;

public abstract class BaseEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 事件ID
     */
    private final String eventId;
    
    /**
     * 事件时间戳
     */
    private final long timestamp;
    
    /**
     * 是否可取消
     */
    private final boolean cancellable;
    
    /**
     * 是否已取消
     */
    private boolean cancelled;
    
    /**
     * 事件来源
     */
    private final String source;
    
    /**
     * 事件附加数据
     */
    private final Object data;
    
    /**
     * 构造函数
     *
     * @param source 事件来源
     */
    protected BaseEvent(String source) {
        this(source, null, false);
    }
    
    /**
     * 构造函数
     *
     * @param source 事件来源
     * @param data 事件附加数据
     */
    protected BaseEvent(String source, Object data) {
        this(source, data, false);
    }
    
    /**
     * 构造函数
     *
     * @param source 事件来源
     * @param data 事件附加数据
     * @param cancellable 是否可取消
     */
    protected BaseEvent(String source, Object data, boolean cancellable) {
        this.eventId = UUID.randomUUID().toString();
        this.source = source;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.cancellable = cancellable;
        this.cancelled = false;
    }
    
    /**
     * 获取事件ID
     *
     * @return 事件ID
     */
    public String getEventId() {
        return eventId;
    }
    
    /**
     * 获取事件来源
     *
     * @return 事件来源
     */
    public String getSource() {
        return source;
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
     * 取消事件
     *
     * @return 是否成功取消
     */
    public boolean cancel() {
        if (isCancellable()) {
            cancelled = true;
            return true;
        }
        return false;
    }
    
    /**
     * 获取事件附加数据
     *
     * @return 事件数据
     */
    public Object getData() {
        return data;
    }
    
    /**
     * 获取事件数据并转换为指定类型
     *
     * @param <T> 目标类型
     * @param clazz 目标类型class
     * @return 转换后的数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> clazz) {
        if (data != null && clazz.isInstance(data)) {
            return (T) data;
        }
        return null;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", cancellable=" + cancellable +
                ", cancelled=" + cancelled +
                '}';
    }
} 