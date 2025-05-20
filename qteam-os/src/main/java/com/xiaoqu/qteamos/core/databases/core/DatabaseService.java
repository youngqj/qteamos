package com.xiaoqu.qteamos.core.databases.core;

import com.xiaoqu.qteamos.core.databases.config.DataSourceProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.io.File;

/**
 * 数据库服务接口
 * 提供数据库操作的统一入口
 *
 * @author yangqijun
 * @date 2025-05-02
 */
public interface DatabaseService {

    /**
     * 获取默认数据源连接
     *
     * @return 数据库连接
     * @throws SQLException 获取连接异常
     */
    Connection getConnection() throws SQLException;
    
    /**
     * 获取指定数据源连接
     *
     * @param dataSourceName 数据源名称
     * @return 数据库连接
     * @throws SQLException 获取连接异常
     */
    Connection getConnection(String dataSourceName) throws SQLException;
    
    /**
     * 执行SQL查询（使用默认数据源）
     *
     * @param sql SQL语句
     * @param resultType 结果类型
     * @param params 查询参数
     * @param <T> 结果类型
     * @return 查询结果列表
     */
    <T> List<T> executeQuery(String sql, Class<T> resultType, Object... params);
    
    /**
     * 执行SQL查询（指定数据源）
     *
     * @param dataSourceName 数据源名称
     * @param sql SQL语句
     * @param resultType 结果类型
     * @param params 查询参数
     * @param <T> 结果类型
     * @return 查询结果列表
     */
    <T> List<T> executeQuery(String dataSourceName, String sql, Class<T> resultType, Object... params);
    
    /**
     * 执行SQL更新（使用默认数据源）
     *
     * @param sql SQL语句
     * @param params 更新参数
     * @return 影响的行数
     */
    int executeUpdate(String sql, Object... params);
    
    /**
     * 执行SQL更新（指定数据源）
     *
     * @param dataSourceName 数据源名称
     * @param sql SQL语句
     * @param params 更新参数
     * @return 影响的行数
     */
    int executeUpdate(String dataSourceName, String sql, Object... params);
    
    /**
     * 执行批量更新（使用默认数据源）
     *
     * @param sql SQL语句
     * @param paramList 批量参数列表
     * @return 影响的行数数组
     */
    int[] executeBatch(String sql, List<Object[]> paramList);
    
    /**
     * 执行批量更新（指定数据源）
     *
     * @param dataSourceName 数据源名称
     * @param sql SQL语句
     * @param paramList 批量参数列表
     * @return 影响的行数数组
     */
    int[] executeBatch(String dataSourceName, String sql, List<Object[]> paramList);
    
    /**
     * 注册数据源
     *
     * @param name 数据源名称
     * @param properties 数据源属性
     */
    void registerDataSource(String name, DataSourceProperties properties);
    
    /**
     * 移除数据源
     *
     * @param name 数据源名称
     */
    void removeDataSource(String name);
    
    /**
     * 获取所有数据源名称
     *
     * @return 数据源名称列表
     */
    List<String> getDataSourceNames();
    
    /**
     * 获取可用的数据源名称列表
     * 
     * @return 可用的数据源名称列表
     */
    List<String> getAvailableDataSourceNames();
    
    /**
     * 执行事务操作
     *
     * @param action 事务操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    <T> T executeTransaction(TransactionAction<T> action);
    
    /**
     * 执行事务操作（指定数据源）
     *
     * @param dataSourceName 数据源名称
     * @param action 事务操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    <T> T executeTransaction(String dataSourceName, TransactionAction<T> action);
    
    /**
     * 测试数据源连接
     *
     * @param properties 数据源属性
     * @return 是否连接成功
     */
    boolean testConnection(DataSourceProperties properties);
    
    /**
     * 检查表是否存在
     * 
     * @param tableName 表名
     * @return 是否存在
     */
    boolean isTableExists(String tableName);
    
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
     * 初始化数据库服务
     * 在系统启动时调用，完成数据库连接池创建等初始化工作
     */
    default void initialize() {
        // 默认实现为空，子类可以根据需要重写
    }
    
    /**
     * 关闭数据库服务
     * 在系统关闭时调用，释放数据库连接等资源
     */
    default void shutdown() {
        // 默认实现为空，子类可以根据需要重写
    }
    
    /**
     * 检查数据库服务健康状态
     *
     * @return 如果数据库服务运行正常则返回true
     */
    default boolean isHealthy() {
        try {
            // 尝试获取连接并验证
            try (Connection conn = getConnection()) {
                return conn != null && conn.isValid(5);
            }
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * 获取数据库服务状态信息
     *
     * @return 数据库服务状态信息
     */
    default String getStatus() {
        try {
            // 尝试获取连接并返回状态信息
            try (Connection conn = getConnection()) {
                if (conn != null && conn.isValid(5)) {
                    return "正常运行 - " + conn.getMetaData().getURL();
                } else {
                    return "连接异常";
                }
            }
        } catch (SQLException e) {
            return "连接失败: " + e.getMessage();
        }
    }
    
    /**
     * 事务操作接口
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface TransactionAction<T> {
        /**
         * 在事务中执行的操作
         *
         * @param connection 数据库连接
         * @return 操作结果
         * @throws SQLException SQL异常
         */
        T execute(Connection connection) throws SQLException;
    }
}