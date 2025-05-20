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
 * 插件事件监听器接口
 * 用于定义插件监听各类事件的回调
 *
 * @author yangqijun
 * @date 2025-05-06
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event;

/**
 * 插件事件监听器接口
 * 用于定义插件监听各类事件的回调
 *
 * @param <T> 事件类型
 */
@FunctionalInterface
public interface PluginEventListener<T> {
    
    /**
     * 事件处理方法
     * 
     * @param event 事件对象
     */
    void onEvent(T event);
} 