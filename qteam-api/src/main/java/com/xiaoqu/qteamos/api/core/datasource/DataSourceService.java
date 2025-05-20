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
 * 数据源服务接口
 * 提供插件获取系统数据源的能力
 *
 * @author yangqijun
 * @date 2025-05-02
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.api.core.datasource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * 数据源服务接口
 * 提供插件访问数据库的能力
 */
public interface DataSourceService {
    
    /**
     * 获取主数据源
     * 
     * @return 数据源对象
     */
    DataSource getMainDataSource();
    
    /**
     * 获取指定名称的数据源
     * 
     * @param name 数据源名称
     * @return 数据源对象，不存在返回null
     */
    DataSource getDataSource(String name);
    
    /**
     * 获取数据库连接
     * 
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    Connection getConnection() throws SQLException;
    
    /**
     * 获取指定数据源的连接
     * 
     * @param name 数据源名称
     * @return 数据库连接
     * @throws SQLException SQL异常
     */
    Connection getConnection(String name) throws SQLException;
    
    /**
     * 获取插件私有数据源
     * 插件可以拥有自己的数据源配置
     * 
     * @param pluginId 插件ID
     * @return 数据源对象
     * @throws SQLException SQL异常
     */
    DataSource getPluginDataSource(String pluginId) throws SQLException;
    
    /**
     * 关闭连接
     * 
     * @param connection 数据库连接
     */
    void closeConnection(Connection connection);
    
    // ===================== 以下为从SDK合并的方法 =====================
    
    /**
     * 获取默认数据源（兼容SDK命名）
     * 
     * @return 数据源
     */
    default DataSource getDataSource() {
        return getMainDataSource();
    }
    
    /**
     * 初始化插件数据库
     * 自动查找并执行插件的db/init.sql文件
     * 
     * @param pluginId 插件ID
     * @return 是否成功
     */
    boolean initPluginDatabase(String pluginId);
    
    /**
     * 执行SQL脚本
     * 
     * @param pluginId 插件ID
     * @param sqlPath 插件内SQL文件路径
     * @return 是否成功
     */
    boolean executeSql(String pluginId, String sqlPath);
    
    /**
     * 执行SQL脚本文件
     * 
     * @param sqlFile SQL文件
     * @return 是否成功
     */
    boolean executeSqlFile(File sqlFile);
    
    /**
     * 检查表是否存在
     * 
     * @param tableName 表名
     * @return 是否存在
     */
    boolean isTableExists(String tableName);
    
    /**
     * 获取可用的数据源名称列表
     * 
     * @return 数据源名称列表
     */
    List<String> getAvailableDataSourceNames();
    
    /**
     * 获取可选数据源
     * 
     * @param name 数据源名称
     * @return 数据源对象（可选）
     */
    default Optional<DataSource> getOptionalDataSource(String name) {
        return Optional.ofNullable(getDataSource(name));
    }
} 