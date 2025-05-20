package com.xiaoqu.qteamos.core.databases.example;

import com.xiaoqu.qteamos.core.databases.config.DataSourceProperties;
import com.xiaoqu.qteamos.core.databases.config.DataSourceProperties.DbType;
import com.xiaoqu.qteamos.core.databases.core.DatabaseService;
import com.xiaoqu.qteamos.core.databases.dialect.MongoDbDialect;
import com.xiaoqu.qteamos.core.databases.dialect.MySqlDialect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 数据库测试工具
 * 可用于插件开发时测试数据库连接和操作
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
@Component
public class DatabaseTester implements CommandLineRunner {

    @Autowired
    private DatabaseService databaseService;
    
    @Autowired
    private MySqlDialect mySqlDialect;
    
    @Autowired
    private MongoDbDialect mongoDbDialect;
    
    @Override
    public void run(String... args) {
        // 如果启动参数包含--db-test则运行数据库测试
        for (String arg : args) {
            if ("--db-test".equals(arg)) {
                startTest();
                break;
            }
        }
    }
    
    /**
     * 启动交互式数据库测试
     */
    private void startTest() {
        log.info("启动数据库测试工具...");
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            printMenu();
            
            int choice = getIntInput(scanner, "请选择操作: ", 0, 6);
            
            try {
                switch (choice) {
                    case 1:
                        testDefaultConnection();
                        break;
                    case 2:
                        testMySqlQuery(scanner);
                        break;
                    case 3:
                        testMongoDbSetup(scanner);
                        break;
                    case 4:
                        testMongoDbQuery(scanner);
                        break;
                    case 5:
                        testCustomConnection(scanner);
                        break;
                    case 0:
                        running = false;
                        break;
                    default:
                        log.info("无效选项，请重新选择");
                }
            } catch (Exception e) {
                log.error("操作执行失败: {}", e.getMessage(), e);
            }
            
            if (running) {
                log.info("操作完成，按回车键继续...");
                scanner.nextLine();
            }
        }
        
        log.info("数据库测试工具已退出");
    }
    
    /**
     * 打印菜单
     */
    private void printMenu() {
        log.info("\n==== 数据库测试工具 ====");
        log.info("1. 测试默认数据源连接");
        log.info("2. 测试MySQL查询");
        log.info("3. 设置MongoDB连接");
        log.info("4. 测试MongoDB查询");
        log.info("5. 测试自定义数据源");
        log.info("0. 退出");
        log.info("=====================");
    }
    
    /**
     * 测试默认数据源连接
     */
    private void testDefaultConnection() {
        log.info("测试默认数据源连接...");
        
        try {
            // 获取当前数据源列表
            List<String> dataSources = databaseService.getDataSourceNames();
            log.info("当前可用数据源: {}", dataSources);
            
            // 获取当前数据库类型
            DbType dbType = databaseService.getConnection().getMetaData().getURL().contains("mysql") ? 
                    DbType.MYSQL : DbType.POSTGRESQL;
            
            // 创建DataSourceProperties对象
            DataSourceProperties properties = new DataSourceProperties();
            properties.setDbType(dbType);
            properties.setDriverClassName(dbType.getDriverClass());
            properties.setUrl(databaseService.getConnection().getMetaData().getURL());
            
            // 测试连接
            boolean success = databaseService.testConnection(properties);
            
            log.info("默认数据源连接测试: {}", success ? "成功" : "失败");
            
        } catch (Exception e) {
            log.error("测试默认数据源连接失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试MySQL查询
     */
    private void testMySqlQuery(Scanner scanner) {
        log.info("测试MySQL查询...");
        
        try {
            log.info("请输入要执行的SQL查询语句:");
            String sql = scanner.nextLine();
            
            if (sql == null || sql.trim().isEmpty()) {
                sql = "SELECT 1 as test";
                log.info("使用默认查询: {}", sql);
            }
            
            List<Map> results = databaseService.executeQuery(sql, Map.class);
            
            log.info("查询结果: {} 条记录", results.size());
            for (int i = 0; i < Math.min(10, results.size()); i++) {
                log.info("记录 {}: {}", i + 1, results.get(i));
            }
            
            if (results.size() > 10) {
                log.info("... 共 {} 条记录", results.size());
            }
            
        } catch (Exception e) {
            log.error("测试MySQL查询失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 设置MongoDB连接
     */
    private void testMongoDbSetup(Scanner scanner) {
        log.info("设置MongoDB连接...");
        
        try {
            log.info("MongoDB服务器地址 (默认: localhost:27017): ");
            String server = scanner.nextLine();
            if (server == null || server.trim().isEmpty()) {
                server = "localhost:27017";
            }
            
            log.info("数据库名称 (默认: testdb): ");
            String dbName = scanner.nextLine();
            if (dbName == null || dbName.trim().isEmpty()) {
                dbName = "testdb";
            }
            
            log.info("用户名 (可选): ");
            String username = scanner.nextLine();
            
            log.info("密码 (可选): ");
            String password = scanner.nextLine();
            
            // 创建数据源属性
            DataSourceProperties properties = new DataSourceProperties();
            properties.setDriverClassName("mongodb.jdbc.MongoDriver");
            properties.setUrl("mongodb://" + server + "/" + dbName);
            
            if (username != null && !username.trim().isEmpty()) {
                properties.setUsername(username);
                properties.setPassword(password);
            }
            
            properties.setDbType(DbType.MONGODB);
            
            // 注册数据源
            databaseService.registerDataSource("mongoDb", properties);
            log.info("MongoDB数据源设置成功");
            
        } catch (Exception e) {
            log.error("设置MongoDB连接失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试MongoDB查询
     */
    private void testMongoDbQuery(Scanner scanner) {
        log.info("测试MongoDB查询...");
        
        try {
            log.info("请输入集合名称 (默认: users): ");
            String collection = scanner.nextLine();
            if (collection == null || collection.trim().isEmpty()) {
                collection = "users";
            }
            
            log.info("请输入查询条件字段 (可选): ");
            String field = scanner.nextLine();
            
            log.info("请输入查询条件值 (可选): ");
            String value = scanner.nextLine();
            
            // 构建MongoDB查询
            String condition = "";
            if (field != null && !field.trim().isEmpty()) {
                condition = mongoDbDialect.buildQueryCondition(field, value, "$eq");
            } else {
                condition = "{}";
            }
            
            String query = "{ " +
                    "\"find\": \"" + collection + "\", " +
                    "\"filter\": " + condition + " " +
                    "}";
            
            log.info("MongoDB查询: {}", query);
            
            // 执行查询
            List<Map> results = databaseService.executeQuery("mongoDb", query, Map.class);
            
            log.info("查询结果: {} 条记录", results.size());
            for (int i = 0; i < Math.min(10, results.size()); i++) {
                log.info("记录 {}: {}", i + 1, results.get(i));
            }
            
            if (results.size() > 10) {
                log.info("... 共 {} 条记录", results.size());
            }
            
        } catch (Exception e) {
            log.error("测试MongoDB查询失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试自定义数据源
     */
    private void testCustomConnection(Scanner scanner) {
        log.info("测试自定义数据源...");
        
        try {
            log.info("数据源类型 (1:MySQL, 2:PostgreSQL, 3:MongoDB): ");
            int dbType = getIntInput(scanner, "请选择: ", 1, 3);
            
            log.info("JDBC URL: ");
            String url = scanner.nextLine();
            
            log.info("用户名: ");
            String username = scanner.nextLine();
            
            log.info("密码: ");
            String password = scanner.nextLine();
            
            // 创建数据源属性
            DataSourceProperties properties = new DataSourceProperties();
            
            switch (dbType) {
                case 1:
                    properties.setDriverClassName(DbType.MYSQL.getDriverClass());
                    properties.setDbType(DbType.MYSQL);
                    break;
                case 2:
                    properties.setDriverClassName(DbType.POSTGRESQL.getDriverClass());
                    properties.setDbType(DbType.POSTGRESQL);
                    break;
                case 3:
                    properties.setDriverClassName(DbType.MONGODB.getDriverClass());
                    properties.setDbType(DbType.MONGODB);
                    break;
            }
            
            properties.setUrl(url);
            properties.setUsername(username);
            properties.setPassword(password);
            
            // 测试连接
            boolean success = databaseService.testConnection(properties);
            log.info("自定义数据源连接测试: {}", success ? "成功" : "失败");
            
            if (success) {
                log.info("是否注册该数据源? (1:是, 0:否): ");
                int register = getIntInput(scanner, "请选择: ", 0, 1);
                
                if (register == 1) {
                    log.info("请输入数据源名称: ");
                    String name = scanner.nextLine();
                    
                    if (name != null && !name.trim().isEmpty()) {
                        databaseService.registerDataSource(name, properties);
                        log.info("数据源 '{}' 注册成功", name);
                    } else {
                        log.info("数据源名称不能为空，注册取消");
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("测试自定义数据源失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取整数输入
     */
    private int getIntInput(Scanner scanner, String prompt, int min, int max) {
        int result = min - 1;
        
        while (result < min || result > max) {
            log.info(prompt);
            try {
                String input = scanner.nextLine();
                result = Integer.parseInt(input);
                
                if (result < min || result > max) {
                    log.info("输入超出范围，请输入 {} 到 {} 之间的数字", min, max);
                }
            } catch (NumberFormatException e) {
                log.info("无效输入，请输入数字");
            }
        }
        
        return result;
    }
} 