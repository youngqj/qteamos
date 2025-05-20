/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-02 22:17:55
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-07 13:48:55
 * @FilePath: /QTeam/qteam-api/src/main/java/com/xiaoqu/qteamos/api/core/plugin/Plugin.java
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
 * 插件接口
 * 所有插件必须实现的基础接口，定义插件生命周期
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.plugin;

/**
 * 插件接口
 * 定义插件的生命周期方法和基本操作
 */
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
     * 
     * @param context 插件上下文
     * @throws Exception 初始化异常
     */
    void init(PluginContext context) throws Exception;
    
    /**
     * 插件启动
     * 在系统启动或插件被激活时调用
     * 
     * @throws Exception 启动异常
     */
    void start() throws Exception;
    
    /**
     * 插件停止
     * 在系统关闭或插件被禁用时调用
     * 
     * @throws Exception 停止异常
     */
    void stop() throws Exception;
    
    /**
     * 插件销毁
     * 在插件被移除前调用，用于释放资源
     * 
     * @throws Exception 销毁异常
     */
    void destroy() throws Exception;
    
    /**
     * 插件卸载
     * 在插件被删除前调用，用于清理资源
     * 
     * @throws Exception 卸载异常
     */
    void uninstall() throws Exception;
    
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