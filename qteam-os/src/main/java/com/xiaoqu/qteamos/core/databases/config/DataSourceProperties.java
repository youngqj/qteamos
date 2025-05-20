package com.xiaoqu.qteamos.core.databases.config;

import lombok.Data;

import java.util.Properties;

/**
 * 数据源属性配置类
 * 用于定义数据源的连接属性
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Data
public class DataSourceProperties {
    
    /**
     * 数据源名称
     */
    private String name;
    
    /**
     * 数据库驱动类名
     */
    private String driverClassName;
    
    /**
     * 数据库连接URL
     */
    private String url;
    
    /**
     * 数据库用户名
     */
    private String username;
    
    /**
     * 数据库密码
     */
    private String password;
    
    /**
     * 数据库类型
     */
    private DbType dbType;
    
    /**
     * 连接池初始大小
     */
    private int initialSize = 5;
    
    /**
     * 最小空闲连接数
     */
    private int minIdle = 5;
    
    /**
     * 最大活跃连接数
     */
    private int maxActive = 20;
    
    /**
     * 获取连接等待超时时间(毫秒)
     */
    private int maxWait = 60000;
    
    /**
     * 额外属性
     */
    private Properties properties = new Properties();
    
    /**
     * 数据库类型枚举
     */
    public enum DbType {
        MYSQL("mysql", "com.mysql.cj.jdbc.Driver"),
        POSTGRESQL("postgresql", "org.postgresql.Driver"),
        SQLSERVER("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
        ORACLE("oracle", "oracle.jdbc.OracleDriver"),
        MONGODB("mongodb", "mongodb.jdbc.MongoDriver"),
        H2("h2", "org.h2.Driver");
        
        private final String name;
        private final String driverClass;
        
        DbType(String name, String driverClass) {
            this.name = name;
            this.driverClass = driverClass;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDriverClass() {
            return driverClass;
        }
        
        /**
         * 根据数据库URL判断数据库类型
         *
         * @param jdbcUrl 数据库连接URL
         * @return 数据库类型
         */
        public static DbType fromUrl(String jdbcUrl) {
            if (jdbcUrl == null) {
                return null;
            }
            jdbcUrl = jdbcUrl.toLowerCase();
            
            if (jdbcUrl.contains(":mysql:") || jdbcUrl.contains(":mariadb:")) {
                return MYSQL;
            } else if (jdbcUrl.contains(":postgresql:")) {
                return POSTGRESQL;
            } else if (jdbcUrl.contains(":sqlserver:")) {
                return SQLSERVER;
            } else if (jdbcUrl.contains(":oracle:")) {
                return ORACLE;
            } else if (jdbcUrl.contains(":mongodb:")) {
                return MONGODB;
            } else if (jdbcUrl.contains(":h2:")) {
                return H2;
            }
            
            return null;
        }
    }
} 