/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-02 22:17:27
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-02 22:19:56
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/sdk/plugin/PluginEventListener.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
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
 * 插件事件监听器
 * 用于监听系统或其他插件发布的事件
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.plugin;

/**
 * 插件事件监听器接口
 * 
 * @param <T> 事件类型
 */
public interface PluginEventListener<T> {
    
    /**
     * 处理事件
     * 
     * @param event 事件对象
     */
    void onEvent(T event);
} 