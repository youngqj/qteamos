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
 * 所有插件必须实现此接口
 * 
 * @author yangqijun
 * @date 2024-07-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core;

/**
 * 插件接口
 * 所有QTeamOS插件必须实现此接口
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

    /**
     * 初始化插件
     * 在插件启动前调用，用于初始化资源
     *
     * @param context 插件上下文
     * @throws Exception 初始化异常
     */
    void init(PluginContext context) throws Exception;
    
    /**
     * 简化的初始化方法（兼容旧接口）
     * 在插件加载后调用，用于初始化资源
     */
    default void init() throws Exception {
        // 默认空实现，兼容旧接口
    }

    /**
     * 启动插件
     * 在插件初始化后调用，开始提供功能
     *
     * @throws Exception 启动异常
     */
    void start() throws Exception;

    /**
     * 停止插件
     * 在插件需要暂停时调用
     *
     * @throws Exception 停止异常
     */
    void stop() throws Exception;

    /**
     * 销毁插件
     * 在插件卸载前调用，释放资源
     *
     * @throws Exception 销毁异常
     */
    void destroy() throws Exception;
    
    /**
     * 卸载插件（兼容旧接口）
     * 在插件被删除前调用，用于清理资源
     * 
     * @throws Exception 卸载异常
     */
    default void uninstall() throws Exception {
        destroy();
    }
} 