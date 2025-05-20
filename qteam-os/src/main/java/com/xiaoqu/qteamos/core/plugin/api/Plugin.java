/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-02 22:06:27
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-06 00:42:47
 * @FilePath: /QTeam/qteam-os/src/main/java/com/xiaoqu/qteamos/core/plugin/api/Plugin.java
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

package com.xiaoqu.qteamos.core.plugin.api;

public interface Plugin {
    
    /**
     * 获取插件ID
     * 
     * @return 插件唯一标识
     */
    String getId();
    
    /**
     * 获取插件名称
     * 
     * @return 插件名称
     */
    String getName();
    
    /**
     * 获取插件版本
     * 
     * @return 插件版本
     */
    String getVersion();
    
    /**
     * 插件初始化
     * 在插件加载后调用，用于初始化资源
     */
    void init();
    
    /**
     * 插件启动
     * 在系统启动或插件被激活时调用
     */
    void start();
    
    /**
     * 插件停止
     * 在系统关闭或插件被禁用时调用
     */
    void stop();
    
    /**
     * 插件卸载
     * 在插件被删除前调用，用于清理资源
     */
    void uninstall();
    
    /**
     * 获取插件描述信息
     * 
     * @return 插件描述
     */
    String getDescription();
    
    /**
     * 获取插件作者
     * 
     * @return 作者信息
     */
    String getAuthor();
} 