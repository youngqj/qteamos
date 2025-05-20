package com.xiaoqu.qteamos.core.databases.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.xiaoqu.qteamos.core.databases.core.DynamicDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据源配置类
 * 负责创建和管理系统数据源
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.primary-name:systemDataSource}")
    private String primaryDataSourceName;

    private final Environment environment;

    public DataSourceConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * 创建主数据源
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DataSource primaryDataSource() {
        log.info("创建主数据源: {}", primaryDataSourceName);
        
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(environment.getProperty("spring.datasource.driver-class-name"));
        dataSource.setUrl(environment.getProperty("spring.datasource.url"));
        dataSource.setUsername(environment.getProperty("spring.datasource.username"));
        dataSource.setPassword(environment.getProperty("spring.datasource.password"));
        
        return dataSource;
    }

    /**
     * 创建动态数据源
     */
    @Primary
    @Bean
    public DataSource dynamicDataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        
        // 设置默认数据源
        dynamicDataSource.setDefaultTargetDataSource(primaryDataSource());
        
        // 设置数据源映射
        Map<Object, Object> dataSourceMap = new HashMap<>(8);
        dataSourceMap.put(primaryDataSourceName, primaryDataSource());
        
        // 加载多数据源配置
        loadMultiDataSources(dataSourceMap);
        
        dynamicDataSource.setTargetDataSources(dataSourceMap);
        
        return dynamicDataSource;
    }

    /**
     * 加载多数据源配置
     */
    private void loadMultiDataSources(Map<Object, Object> dataSourceMap) {
        boolean enabled = Boolean.TRUE.equals(environment.getProperty("spring.datasource-multi.enabled", Boolean.class));
        if (!enabled) {
            log.info("多数据源功能未启用");
            return;
        }
        
        log.info("开始加载多数据源配置");
        
        // TODO: 在这里加载多数据源配置
        // 根据配置文件的结构遍历加载多个数据源
        // 这里需要根据实际的配置格式修改
        
        log.info("多数据源加载完成");
    }
} 