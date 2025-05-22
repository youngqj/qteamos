/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.core.plugin.event;

/**
 * 通用事件包装器
 * 用于包装非Event类型的对象，使其能够被EventBus处理
 *
 * @author yangqijun
 * @date 2025-06-11
 * @since 1.0.0
 */
public class GenericEvent extends Event {
    
    private final Object payload;
    
    /**
     * 构造函数
     *
     * @param source 事件来源
     * @param type 事件类型
     * @param payload 事件负载
     */
    public GenericEvent(String source, String type, Object payload) {
        super("plugin.event", type, payload);
        this.payload = payload;
    }
    
    /**
     * 获取事件负载
     *
     * @return 事件负载
     */
    public Object getPayload() {
        return payload;
    }
    
    /**
     * 获取事件负载并转换为指定类型
     *
     * @param <T> 目标类型
     * @param clazz 类型Class
     * @return 转换后的负载
     * @throws ClassCastException 如果类型转换失败
     */
    @SuppressWarnings("unchecked")
    public <T> T getPayload(Class<T> clazz) {
        if (payload == null) {
            return null;
        }
        if (clazz.isInstance(payload)) {
            return (T) payload;
        }
        throw new ClassCastException("Cannot cast " + payload.getClass().getName() + " to " + clazz.getName());
    }
    
    @Override
    public String toString() {
        return "GenericEvent{" +
                "source='" + getSource() + '\'' +
                ", type='" + getType() + '\'' +
                ", payload=" + payload +
                ", timestamp=" + getTimestamp() +
                ", cancelled=" + isCancelled() +
                '}';
    }
} 