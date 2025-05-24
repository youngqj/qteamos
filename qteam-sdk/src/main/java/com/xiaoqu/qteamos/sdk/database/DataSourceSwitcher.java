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

package com.xiaoqu.qteamos.sdk.database;

import java.util.List;
import java.util.function.Supplier;

/**
 * 数据源切换工具
 * 插件的唯一数据库相关API - 提供多数据源切换能力
 * 
 * @author yangqijun
 * @date 2025-01-20
 * @since 1.0.0
 */
public interface DataSourceSwitcher {
    
    /**
     * 获取可用的数据源列表
     * 
     * @return 数据源名称列表
     */
    List<String> getAvailableDataSources();
    
    /**
     * 在指定数据源上执行操作
     * 
     * @param dataSourceName 数据源名称 
     * @param action 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    <T> T executeWith(String dataSourceName, Supplier<T> action);
    
    /**
     * 在指定数据源上执行操作（无返回值）
     * 
     * @param dataSourceName 数据源名称
     * @param action 要执行的操作
     */
    void executeWith(String dataSourceName, Runnable action);
} 