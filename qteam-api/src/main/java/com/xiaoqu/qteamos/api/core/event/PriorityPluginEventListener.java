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
 * 带优先级的插件事件监听器接口
 * 支持指定事件处理的优先级，高优先级的监听器先执行
 *
 * @author yangqijun
 * @date 2025-08-15
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.event;

/**
 * 带优先级的插件事件监听器接口
 * 
 * @param <T> 事件类型
 */
public interface PriorityPluginEventListener<T> extends PluginEventListener<T> {
    
    /**
     * 默认优先级
     */
    int DEFAULT_PRIORITY = 0;
    
    /**
     * 最高优先级
     */
    int HIGHEST_PRIORITY = 100;
    
    /**
     * 高优先级
     */
    int HIGH_PRIORITY = 75;
    
    /**
     * 中等优先级
     */
    int MEDIUM_PRIORITY = 50;
    
    /**
     * 低优先级
     */
    int LOW_PRIORITY = 25;
    
    /**
     * 最低优先级
     */
    int LOWEST_PRIORITY = 0;
    
    /**
     * 获取监听器的优先级
     * 优先级范围为0-100，数值越大优先级越高
     * 
     * @return 优先级数值
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }
    
    /**
     * 是否同步执行
     * 默认为异步执行，可以重写此方法以要求同步执行
     * 
     * @return true表示同步执行，false表示异步执行
     */
    default boolean isSynchronous() {
        return false;
    }
} 