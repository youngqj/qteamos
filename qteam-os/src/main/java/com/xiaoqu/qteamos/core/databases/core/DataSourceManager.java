package com.xiaoqu.qteamos.core.databases.core;

import com.alibaba.druid.pool.DruidDataSource;
import com.xiaoqu.qteamos.core.databases.config.DataSourceProperties;
import com.xiaoqu.qteamos.core.databases.exception.DatabaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源管理器
 * 负责管理系统中的所有数据源，包括创建、获取、移除等操作
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
@Component
public class DataSourceManager implements DisposableBean {

    @Autowired
    private DynamicDataSource dynamicDataSource;
    
    /**
     * 数据源缓存
     */
    private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>(16);
    
    /**
     * 获取数据源
     *
     * @param name 数据源名称
     * @return 数据源
     */
    public DataSource getDataSource(String name) {
        return dataSourceMap.get(name);
    }
    
    /**
     * 获取所有数据源
     * 
     * @return 所有数据源的映射
     */
    public Map<String, DataSource> getDataSourceMap() {
        return new HashMap<>(dataSourceMap);
    }
    
    /**
     * 添加数据源
     *
     * @param name 数据源名称
     * @param dataSource 数据源
     */
    public void addDataSource(String name, DataSource dataSource) {
        dataSourceMap.put(name, dataSource);
        
        // 更新动态数据源
        Map<Object, Object> targetDataSources = new HashMap<>(dataSourceMap.size());
        dataSourceMap.forEach((k, v) -> targetDataSources.put(k, v));
        
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.afterPropertiesSet();
        
        log.info("添加数据源: {}", name);
    }
    
    /**
     * 移除数据源
     *
     * @param name 数据源名称
     */
    public void removeDataSource(String name) {
        DataSource dataSource = dataSourceMap.remove(name);
        if (dataSource != null && dataSource instanceof DruidDataSource) {
            try {
                ((DruidDataSource) dataSource).close();
            } catch (Exception e) {
                log.error("关闭数据源异常: {}", name, e);
            }
        }
        
        // 更新动态数据源
        Map<Object, Object> targetDataSources = new HashMap<>(dataSourceMap.size());
        dataSourceMap.forEach((k, v) -> targetDataSources.put(k, v));
        
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.afterPropertiesSet();
        
        log.info("移除数据源: {}", name);
    }
    
    /**
     * 创建数据源
     *
     * @param properties 数据源属性
     * @return 数据源
     */
    public DataSource createDataSource(DataSourceProperties properties) {
        DruidDataSource dataSource = new DruidDataSource();
        
        // 设置基本连接信息
        dataSource.setDriverClassName(properties.getDriverClassName());
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        
        // 设置连接池参数
        dataSource.setInitialSize(properties.getInitialSize());
        dataSource.setMinIdle(properties.getMinIdle());
        dataSource.setMaxActive(properties.getMaxActive());
        dataSource.setMaxWait(properties.getMaxWait());
        
        // 设置可选参数
        Properties props = properties.getProperties();
        if (props != null && !props.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(';');
                }
                sb.append(entry.getKey()).append('=').append(entry.getValue());
            }
            dataSource.setConnectionProperties(sb.toString());
        }
        
        // 通用配置
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setValidationQuery("SELECT 1");
        
        try {
            dataSource.init();
        } catch (SQLException e) {
            log.error("初始化数据源失败: {}", properties.getName(), e);
            throw new DatabaseException("初始化数据源失败: " + e.getMessage(), e);
        }
        
        return dataSource;
    }
    
    /**
     * 测试数据源连接
     *
     * @param dataSource 数据源
     * @return 是否连接成功
     */
    public boolean testConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            log.error("测试数据源连接失败", e);
            return false;
        }
    }
    
    /**
     * 测试数据源连接
     *
     * @param properties 数据源属性
     * @return 是否连接成功
     */
    public boolean testConnection(DataSourceProperties properties) {
        DataSource dataSource = null;
        try {
            dataSource = createDataSource(properties);
            return testConnection(dataSource);
        } catch (Exception e) {
            log.error("测试数据源连接失败", e);
            return false;
        } finally {
            if (dataSource instanceof DruidDataSource) {
                ((DruidDataSource) dataSource).close();
            }
        }
    }
    
    /**
     * Bean销毁时关闭所有数据源
     */
    @Override
    public void destroy() {
        log.info("正在关闭所有数据源...");
        for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            if (entry.getValue() instanceof DruidDataSource) {
                try {
                    ((DruidDataSource) entry.getValue()).close();
                    log.info("关闭数据源: {}", entry.getKey());
                } catch (Exception e) {
                    log.error("关闭数据源异常: {}", entry.getKey(), e);
                }
            }
        }
        dataSourceMap.clear();
        log.info("所有数据源已关闭");
    }
} 