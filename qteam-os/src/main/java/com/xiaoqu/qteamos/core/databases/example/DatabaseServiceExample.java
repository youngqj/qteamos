package com.xiaoqu.qteamos.core.databases.example;

import com.xiaoqu.qteamos.core.databases.annotation.DataSource;
import com.xiaoqu.qteamos.core.databases.config.DataSourceProperties;
import com.xiaoqu.qteamos.core.databases.core.DatabaseService;
import com.xiaoqu.qteamos.core.databases.dialect.DbDialect;
import com.xiaoqu.qteamos.core.databases.dialect.MongoDbDialect;
import com.xiaoqu.qteamos.core.databases.dialect.MySqlDialect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库服务使用示例
 * 展示如何在插件中使用数据库服务
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
@Component
public class DatabaseServiceExample {

    @Autowired
    private DatabaseService databaseService;
    
    @Autowired
    private MySqlDialect mySqlDialect;
    
    @Autowired
    private MongoDbDialect mongoDbDialect;
    
    /**
     * 通过编程方式使用MySQL数据源
     */
    public void mysqlExample() {
        log.info("=== MySQL示例 ===");
        
        // 基本查询示例
        List<Map> users = databaseService.executeQuery(
                "SELECT id, username, email FROM sys_user WHERE status = ?", 
                Map.class, 
                1);
        log.info("查询到用户数: {}", users.size());
        
        // 带分页的查询示例
        String sql = "SELECT id, username, email FROM sys_user WHERE create_time > ?";
        String pageSql = mySqlDialect.getPageSql(sql, 0, 10);
        List<Map> pagedUsers = databaseService.executeQuery(pageSql, Map.class, "2025-01-01");
        log.info("分页查询用户数: {}", pagedUsers.size());
        
        // 执行插入示例
        int rows = databaseService.executeUpdate(
                "INSERT INTO sys_log(user_id, operation, ip, create_time) VALUES(?, ?, ?, NOW())",
                1, "登录", "127.0.0.1");
        log.info("插入日志记录: {}", rows);
        
        // 使用事务示例
        try {
            databaseService.executeTransaction(connection -> {
                // 在事务中执行多个操作
                try (var stmt = connection.prepareStatement(
                        "UPDATE sys_user SET login_count = login_count + 1 WHERE id = ?")) {
                    stmt.setInt(1, 1);
                    stmt.executeUpdate();
                }
                
                try (var stmt = connection.prepareStatement(
                        "INSERT INTO sys_login_history(user_id, login_time) VALUES(?, NOW())")) {
                    stmt.setInt(1, 1);
                    stmt.executeUpdate();
                }
                
                return true; // 事务成功
            });
            log.info("事务执行成功");
        } catch (Exception e) {
            log.error("事务执行失败", e);
        }
    }
    
    /**
     * 使用注解方式切换到MySQL数据源
     */
    @DataSource("systemDataSource") // 默认数据源
    public void annotationMySqlExample() {
        log.info("=== 注解方式MySQL示例 ===");
        
        // 执行查询，使用注解指定的数据源
        List<String> deptNames = databaseService.executeQuery(
                "SELECT name FROM sys_dept", 
                String.class);
        log.info("部门列表: {}", deptNames);
    }
    
    /**
     * 通过编程方式使用MongoDB数据源
     * 注意：需要先注册MongoDB数据源
     */
    public void mongoDbExample() {
        log.info("=== MongoDB示例 ===");
        
        // 假设已经注册了名为"mongoDb"的MongoDB数据源
        final String MONGO_DS = "mongoDb";
        
        try {
            // 构建MongoDB查询条件
            String condition = mongoDbDialect.buildQueryCondition("status", "active", "$eq");
            String sortCondition = mongoDbDialect.buildSortCondition("createdAt", false);
            
            // 假设使用JSON格式查询字符串（简化示例）
            // 实际使用时可能需要特定的MongoDB驱动适配
            String query = "{ " +
                    "\"find\": \"users\", " +
                    "\"filter\": " + condition + ", " +
                    "\"sort\": " + sortCondition + " " +
                    "}";
            
            // 添加分页
            String pagedQuery = mongoDbDialect.getPageSql(query, 0, 10);
            log.info("MongoDB查询: {}", pagedQuery);
            
            // 执行MongoDB查询(示例)
            // 注意：这里仅展示接口用法，实际操作MongoDB通常需要专用驱动
            List<Map> results = databaseService.executeQuery(MONGO_DS, pagedQuery, Map.class);
            log.info("MongoDB查询结果数: {}", results != null ? results.size() : 0);
            
        } catch (Exception e) {
            log.error("MongoDB操作失败", e);
        }
    }
    
    /**
     * 注册MongoDB数据源示例
     */
    public void registerMongoDbDataSource() {
        log.info("=== 注册MongoDB数据源示例 ===");
        
        try {
            // 创建MongoDB数据源配置
            DataSourceProperties properties = new DataSourceProperties();
            properties.setDriverClassName("mongodb.jdbc.MongoDriver"); // 示例驱动类
            properties.setUrl("mongodb://localhost:27017/testdb");
            properties.setUsername("mongoUser");
            properties.setPassword("mongoPass");
            properties.setDbType(DataSourceProperties.DbType.MONGODB);
            
            // 注册数据源
            databaseService.registerDataSource("mongoDb", properties);
            log.info("MongoDB数据源注册成功");
            
        } catch (Exception e) {
            log.error("注册MongoDB数据源失败", e);
        }
    }
    
    /**
     * 演示如何直接使用数据库连接
     */
    public void directConnectionExample() {
        log.info("=== 直接使用数据库连接示例 ===");
        
        Connection conn = null;
        try {
            // 获取默认数据源连接
            conn = databaseService.getConnection();
            log.info("数据库连接获取成功，自动提交模式: {}", conn.getAutoCommit());
            
            // 使用连接执行操作...
            
        } catch (SQLException e) {
            log.error("数据库连接操作失败", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.error("关闭连接失败", e);
                }
            }
        }
    }
    
    /**
     * 批量插入示例
     */
    public void batchInsertExample() {
        log.info("=== 批量插入示例 ===");
        
        // 准备批量插入的数据
        List<Object[]> batchData = new ArrayList<>();
        batchData.add(new Object[]{ "user1", "user1@example.com", "123456" });
        batchData.add(new Object[]{ "user2", "user2@example.com", "123456" });
        batchData.add(new Object[]{ "user3", "user3@example.com", "123456" });
        
        // 执行批量插入
        String sql = "INSERT INTO sys_user(username, email, password) VALUES(?, ?, ?)";
        int[] results = databaseService.executeBatch(sql, batchData);
        
        int total = 0;
        for (int count : results) {
            total += count;
        }
        log.info("批量插入结果: 总共{}条记录", total);
    }
    
    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        // 注册MongoDB数据源
        registerMongoDbDataSource();
        
        // MySQL示例
        mysqlExample();
        annotationMySqlExample();
        batchInsertExample();
        directConnectionExample();
        
        // MongoDB示例
        mongoDbExample();
    }
} 