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

package com.xiaoqu.qteamos.core.databases;

import com.xiaoqu.qteamos.core.databases.core.DatabaseService;
import com.xiaoqu.qteamos.api.core.datasource.DataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.File;

/**
 * 数据库服务连接器
 * 将系统内部的DatabaseService实现连接到面向插件的DataSourceService接口
 *
 * @author yangqijun
 * @date 2025-05-18
 */
@Slf4j
@Component
public class DatabaseServiceConnector implements DataSourceService, InitializingBean {

    @Autowired
    private DatabaseService databaseService;
    
    @Autowired
    private DataSource mainDataSource;
    
    private final Map<String, DataSource> dataSourceMap = new HashMap<>();

    @Override
    public DataSource getMainDataSource() {
        return mainDataSource;
    }

    @Override
    public DataSource getDataSource(String name) {
        if (dataSourceMap.containsKey(name)) {
            return dataSourceMap.get(name);
        }
        log.warn("数据源 {} 不存在", name);
        return null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return databaseService.getConnection();
    }

    @Override
    public Connection getConnection(String name) throws SQLException {
        return databaseService.getConnection(name);
    }

    @Override
    public DataSource getPluginDataSource(String pluginId) throws SQLException {
        // 这里可以根据实际需求实现插件私有数据源的创建逻辑
        // 或者使用命名规则将插件ID映射到一个预先配置的数据源
        String dataSourceName = "plugin_" + pluginId;
        DataSource dataSource = getDataSource(dataSourceName);
        if (dataSource == null) {
            log.warn("获取插件数据源失败: {}，尝试使用主数据源作为备选", pluginId);
            return getMainDataSource();
        }
        return dataSource;
    }

    @Override
    public void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                log.warn("关闭连接失败", e);
            }
        }
    }

    @Override
    public boolean isTableExists(String tableName) {
        try {
            return databaseService.isTableExists(tableName);
        } catch (Exception e) {
            log.error("检查表是否存在时出错: {}", tableName, e);
            return false;
        }
    }
    
    @Override
    public boolean initPluginDatabase(String pluginId) {
        try {
            return databaseService.initPluginDatabase(pluginId);
        } catch (Exception e) {
            log.error("初始化插件数据库失败: {}", pluginId, e);
            return false;
        }
    }
    
    @Override
    public boolean executeSql(String pluginId, String sqlPath) {
        try {
            return databaseService.executeSql(pluginId, sqlPath);
        } catch (Exception e) {
            log.error("执行SQL脚本失败: {}, {}", pluginId, sqlPath, e);
            return false;
        }
    }
    
    @Override
    public boolean executeSqlFile(File sqlFile) {
        try {
            return databaseService.executeSqlFile(sqlFile);
        } catch (Exception e) {
            log.error("执行SQL文件失败: {}", sqlFile.getPath(), e);
            return false;
        }
    }

    @Override
    public List<String> getAvailableDataSourceNames() {
        return databaseService.getAvailableDataSourceNames();
    }

    public Optional<DataSource> getOptionalDataSource(String name) {
        return Optional.ofNullable(getDataSource(name));
    }
    
    /**
     * 注册数据源
     * 
     * @param name 数据源名称
     * @param dataSource 数据源对象
     */
    public void registerDataSource(String name, DataSource dataSource) {
        dataSourceMap.put(name, dataSource);
    }
    
    /**
     * 初始化数据源
     * 在系统启动时调用，从数据库服务中获取所有可用的数据源
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            log.info("开始初始化数据源服务...");
            // 确保主数据源已经被注入
            if (mainDataSource == null) {
                log.error("主数据源未注入，数据源服务初始化失败");
                return;
            }
            
            // 注册所有可用的数据源
            List<String> dataSourceNames = databaseService.getAvailableDataSourceNames();
            log.info("发现 {} 个可用数据源: {}", dataSourceNames.size(), dataSourceNames);
            
            // 在这里可以实现数据源的动态获取和注册
            // 这可能需要数据库服务提供额外的API来获取DataSource对象
            
            log.info("数据源服务初始化完成");
        } catch (Exception e) {
            log.error("初始化数据源服务失败", e);
            throw e;
        }
    }
} 