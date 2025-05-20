package com.xiaoqu.qteamos.core.databases.core;

import com.xiaoqu.qteamos.core.databases.config.DataSourceProperties;
import com.xiaoqu.qteamos.core.databases.exception.DatabaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 数据库服务默认实现
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
@Service
public class DefaultDatabaseService implements DatabaseService {

    @Value("${spring.datasource.primary-name:systemDataSource}")
    private String primaryDataSourceName;

    @Autowired
    private DataSourceManager dataSourceManager;
    
    /**
     * 是否已初始化
     */
    private boolean initialized = false;
    
    /**
     * 初始化数据库服务
     */
    @Override
    public void initialize() {
        if (initialized) {
            log.info("数据库服务已经初始化，跳过重复初始化");
            return;
        }
        
        log.info("初始化数据库服务...");
        
        try {
            // 测试连接
            try (Connection conn = getConnection()) {
                log.info("数据库连接测试成功: {}", conn.getMetaData().getURL());
            }
        } catch (SQLException e) {
            log.error("数据库连接测试失败", e);
        }
        
        initialized = true;
        log.info("数据库服务初始化完成");
    }
    
    /**
     * 关闭数据库服务
     */
    @Override
    public void shutdown() {
        log.info("关闭数据库服务...");
        // 关闭所有数据源
        for (String dataSourceName : getDataSourceNames()) {
            try {
                removeDataSource(dataSourceName);
            } catch (Exception e) {
                log.error("关闭数据源失败: {}", dataSourceName, e);
            }
        }
        
        initialized = false;
        log.info("数据库服务已关闭");
    }
    
    /**
     * 检查数据库服务健康状态
     *
     * @return 如果数据库服务运行正常则返回true
     */
    @Override
    public boolean isHealthy() {
        if (!initialized) {
            return false;
        }
        
        try {
            // 尝试获取连接并验证
            try (Connection conn = getConnection()) {
                return conn != null && conn.isValid(5);
            }
        } catch (SQLException e) {
            log.error("数据库健康检查失败", e);
            return false;
        }
    }
    
    /**
     * 获取数据库服务状态信息
     *
     * @return 数据库服务状态信息
     */
    @Override
    public String getStatus() {
        if (!initialized) {
            return "未初始化";
        }
        
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

    @Override
    public Connection getConnection() throws SQLException {
        return getConnection(primaryDataSourceName);
    }

    @Override
    public Connection getConnection(String dataSourceName) throws SQLException {
        // 设置当前线程数据源
        DataSourceContextHolder.setDataSource(dataSourceName);
        
        // 获取数据源
        DataSource dataSource = dataSourceManager.getDataSource(dataSourceName);
        if (dataSource == null) {
            throw new SQLException("数据源不存在: " + dataSourceName);
        }
        
        // 获取连接
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("获取数据库连接失败: {}", dataSourceName, e);
            throw e;
        }
    }

    @Override
    public <T> List<T> executeQuery(String sql, Class<T> resultType, Object... params) {
        return executeQuery(primaryDataSourceName, sql, resultType, params);
    }

    @Override
    public <T> List<T> executeQuery(String dataSourceName, String sql, Class<T> resultType, Object... params) {
        try (Connection conn = getConnection(dataSourceName);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 设置参数
            setParameters(stmt, params);
            
            // 执行查询
            try (ResultSet rs = stmt.executeQuery()) {
                return mapResultSet(rs, resultType);
            }
        } catch (SQLException e) {
            log.error("执行查询失败: {}", sql, e);
            throw new DatabaseException("执行查询失败: " + e.getMessage(), e);
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    @Override
    public int executeUpdate(String sql, Object... params) {
        return executeUpdate(primaryDataSourceName, sql, params);
    }

    @Override
    public int executeUpdate(String dataSourceName, String sql, Object... params) {
        try (Connection conn = getConnection(dataSourceName);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 设置参数
            setParameters(stmt, params);
            
            // 执行更新
            return stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("执行更新失败: {}", sql, e);
            throw new DatabaseException("执行更新失败: " + e.getMessage(), e);
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    @Override
    public int[] executeBatch(String sql, List<Object[]> paramList) {
        return executeBatch(primaryDataSourceName, sql, paramList);
    }

    @Override
    public int[] executeBatch(String dataSourceName, String sql, List<Object[]> paramList) {
        try (Connection conn = getConnection(dataSourceName);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // 添加批处理
            for (Object[] params : paramList) {
                setParameters(stmt, params);
                stmt.addBatch();
            }
            
            // 执行批处理
            return stmt.executeBatch();
        } catch (SQLException e) {
            log.error("执行批处理失败: {}", sql, e);
            throw new DatabaseException("执行批处理失败: " + e.getMessage(), e);
        } finally {
            DataSourceContextHolder.clearDataSource();
        }
    }

    @Override
    public void registerDataSource(String name, DataSourceProperties properties) {
        // 设置数据源名称
        properties.setName(name);
        
        // 创建数据源
        DataSource dataSource = dataSourceManager.createDataSource(properties);
        
        // 添加到数据源管理器
        dataSourceManager.addDataSource(name, dataSource);
        
        log.info("注册数据源成功: {}", name);
    }

    @Override
    public void removeDataSource(String name) {
        // 不允许移除主数据源
        if (primaryDataSourceName.equals(name)) {
            throw new DatabaseException("不允许移除主数据源: " + name);
        }
        
        // 移除数据源
        dataSourceManager.removeDataSource(name);
        
        log.info("移除数据源成功: {}", name);
    }

    @Override
    public List<String> getDataSourceNames() {
        // 从数据源管理器获取所有数据源
        Map<String, DataSource> dataSourceMap = dataSourceManager.getDataSourceMap();
        return new ArrayList<>(dataSourceMap.keySet());
    }

    @Override
    public List<String> getAvailableDataSourceNames() {
        List<String> availableDataSources = new ArrayList<>();
        
        // 获取所有数据源
        Map<String, DataSource> dataSourceMap = dataSourceManager.getDataSourceMap();
        
        // 检查每个数据源是否可用
        for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            String name = entry.getKey();
            DataSource dataSource = entry.getValue();
            
            // 测试连接是否可用
            try (Connection conn = dataSource.getConnection()) {
                if (conn != null && conn.isValid(3)) {
                    availableDataSources.add(name);
                }
            } catch (SQLException e) {
                log.warn("数据源连接测试失败: {}", name, e);
                // 连接失败的数据源不添加到可用列表
            }
        }
        
        return availableDataSources;
    }

    @Override
    public <T> T executeTransaction(TransactionAction<T> action) {
        return executeTransaction(primaryDataSourceName, action);
    }

    @Override
    public <T> T executeTransaction(String dataSourceName, TransactionAction<T> action) {
        Connection conn = null;
        boolean autoCommit = true;
        
        try {
            // 获取连接
            conn = getConnection(dataSourceName);
            
            // 保存自动提交设置
            autoCommit = conn.getAutoCommit();
            
            // 开始事务
            conn.setAutoCommit(false);
            
            // 执行事务操作
            T result = action.execute(conn);
            
            // 提交事务
            conn.commit();
            
            return result;
        } catch (SQLException e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    log.error("事务回滚失败", ex);
                }
            }
            
            log.error("事务执行失败", e);
            throw new DatabaseException("事务执行失败: " + e.getMessage(), e);
        } finally {
            // 恢复自动提交设置
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommit);
                    conn.close();
                } catch (SQLException e) {
                    log.error("关闭连接失败", e);
                }
            }
            
            // 清除数据源上下文
            DataSourceContextHolder.clearDataSource();
        }
    }

    @Override
    public boolean testConnection(DataSourceProperties properties) {
        return dataSourceManager.testConnection(properties);
    }

    /**
     * 设置预处理语句参数
     *
     * @param stmt 预处理语句
     * @param params 参数数组
     * @throws SQLException SQL异常
     */
    private void setParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params != null) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
        }
    }

    /**
     * 将结果集映射为指定类型的对象列表
     *
     * @param rs 结果集
     * @param resultType 结果类型
     * @param <T> 结果类型
     * @return 对象列表
     * @throws SQLException SQL异常
     */
    private <T> List<T> mapResultSet(ResultSet rs, Class<T> resultType) throws SQLException {
        List<T> results = new ArrayList<>();
        
        // 获取结果集元数据
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // 处理基本类型
        if (resultType == String.class || resultType == Integer.class || resultType == Long.class ||
            resultType == Double.class || resultType == Boolean.class || resultType == Date.class) {
            while (rs.next()) {
                @SuppressWarnings("unchecked")
                T value = (T) rs.getObject(1, resultType);
                results.add(value);
            }
            return results;
        }
        
        // 处理Map类型
        if (resultType == Map.class) {
            while (rs.next()) {
                Map<String, Object> map = new java.util.HashMap<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    map.put(columnName, value);
                }
                @SuppressWarnings("unchecked")
                T value = (T) map;
                results.add(value);
            }
            return results;
        }
        
        // 处理复杂对象类型
        // 在实际项目中，这里可能需要使用反射来创建对象并设置属性
        // 或者使用ORM框架来处理对象映射
        // 这里仅提供一个简单的实现
        throw new SQLException("不支持的结果类型: " + resultType.getName() + "，请使用Map类型或基本类型");
    }

    /**
     * 检查表是否存在
     *
     * @param tableName 表名
     * @return 是否存在
     */
    @Override
    public boolean isTableExists(String tableName) {
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"});
            return rs.next();
        } catch (SQLException e) {
            log.error("检查表是否存在失败: {}", tableName, e);
            return false;
        }
    }
    
    /**
     * 初始化插件数据库
     *
     * @param pluginId 插件ID
     * @return 是否成功
     */
    @Override
    public boolean initPluginDatabase(String pluginId) {
        log.info("初始化插件数据库: {}", pluginId);
        try {
            // 根据插件ID查找对应的SQL初始化文件
            Path pluginDataPath = Paths.get("plugins", pluginId, "db", "init.sql");
            if (!Files.exists(pluginDataPath)) {
                log.warn("插件数据库初始化文件不存在: {}", pluginDataPath);
                return false;
            }
            
            // 执行初始化SQL文件
            return executeSqlFile(pluginDataPath.toFile());
        } catch (Exception e) {
            log.error("初始化插件数据库失败: {}", pluginId, e);
            return false;
        }
    }
    
    /**
     * 执行SQL脚本
     *
     * @param pluginId 插件ID
     * @param sqlPath 插件内SQL文件路径
     * @return 是否成功
     */
    @Override
    public boolean executeSql(String pluginId, String sqlPath) {
        log.info("执行插件SQL脚本: {}, {}", pluginId, sqlPath);
        try {
            // 根据插件ID和SQL路径构建完整路径
            Path fullSqlPath = Paths.get("plugins", pluginId, sqlPath);
            if (!Files.exists(fullSqlPath)) {
                log.warn("插件SQL文件不存在: {}", fullSqlPath);
                return false;
            }
            
            // 执行SQL文件
            return executeSqlFile(fullSqlPath.toFile());
        } catch (Exception e) {
            log.error("执行插件SQL脚本失败: {}, {}", pluginId, sqlPath, e);
            return false;
        }
    }
    
    /**
     * 执行SQL脚本文件
     *
     * @param sqlFile SQL文件
     * @return 是否成功
     */
    @Override
    public boolean executeSqlFile(File sqlFile) {
        log.info("执行SQL文件: {}", sqlFile.getPath());
        if (!sqlFile.exists() || !sqlFile.isFile()) {
            log.warn("SQL文件不存在或不是文件: {}", sqlFile.getPath());
            return false;
        }
        
        try (Connection conn = getConnection();
             BufferedReader reader = new BufferedReader(new FileReader(sqlFile))) {
            
            // 设置自动提交为false，开启事务
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                StringBuilder sqlStatement = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    // 忽略注释和空行
                    if (line.trim().isEmpty() || line.trim().startsWith("--") || line.trim().startsWith("#")) {
                        continue;
                    }
                    
                    sqlStatement.append(line);
                    
                    // 如果SQL语句结束，执行它
                    if (line.trim().endsWith(";")) {
                        String sql = sqlStatement.toString().trim();
                        sql = sql.substring(0, sql.length() - 1); // 移除末尾的分号
                        
                        try (Statement stmt = conn.createStatement()) {
                            stmt.execute(sql);
                        }
                        
                        // 清空SQL语句缓存
                        sqlStatement.setLength(0);
                    }
                }
                
                // 提交事务
                conn.commit();
                log.info("SQL文件执行成功: {}", sqlFile.getPath());
                return true;
            } catch (Exception e) {
                // 回滚事务
                conn.rollback();
                log.error("执行SQL文件失败，事务回滚: {}", sqlFile.getPath(), e);
                return false;
            } finally {
                // 恢复自动提交设置
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException | IOException e) {
            log.error("执行SQL文件失败: {}", sqlFile.getPath(), e);
            return false;
        }
    }
} 