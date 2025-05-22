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
 * 统一插件事件基类
 * 作为所有插件相关事件的基础类，提供共同的属性和方法
 *
 * @author yangqijun
 * @date 2024-07-22
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event;

import com.xiaoqu.qteamos.api.core.event.base.TopicEvent;

import java.io.Serializable;
import java.util.UUID;

public abstract class PluginEvent extends TopicEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 插件ID
     */
    private final String pluginId;
    
    /**
     * 插件版本
     */
    private final String version;
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param pluginId 插件ID
     */
    protected PluginEvent(String topic, String type, String source, String pluginId) {
        this(topic, type, source, pluginId, null);
    }
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param pluginId 插件ID
     * @param version 插件版本
     */
    protected PluginEvent(String topic, String type, String source, String pluginId, String version) {
        this(topic, type, source, pluginId, version, null, false);
    }
    
    /**
     * 构造函数
     *
     * @param topic 事件主题
     * @param type 事件类型
     * @param source 事件来源
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param data 事件附加数据
     * @param cancellable 是否可取消
     */
    protected PluginEvent(String topic, String type, String source, String pluginId, String version, Object data, boolean cancellable) {
        super(topic, type, source, data, cancellable);
        this.pluginId = pluginId;
        this.version = version;
    }
    
    /**
     * 获取插件ID
     *
     * @return 插件ID
     */
    public String getPluginId() {
        return pluginId;
    }
    
    /**
     * 获取插件版本
     *
     * @return 插件版本
     */
    public String getVersion() {
        return version;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "eventId='" + getEventId() + '\'' +
                ", topic='" + getTopic() + '\'' +
                ", type='" + getType() + '\'' +
                ", pluginId='" + pluginId + '\'' +
                ", version='" + version + '\'' +
                ", timestamp=" + getTimestamp() +
                ", source='" + getSource() + '\'' +
                ", cancellable=" + isCancellable() +
                ", cancelled=" + isCancelled() +
                '}';
    }
} 