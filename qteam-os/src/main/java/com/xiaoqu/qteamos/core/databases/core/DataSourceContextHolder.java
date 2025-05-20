package com.xiaoqu.qteamos.core.databases.core;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据源上下文持有者
 * 基于ThreadLocal实现数据源切换的线程隔离
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Slf4j
public class DataSourceContextHolder {
    
    /**
     * 默认数据源名称
     */
    public static final String DEFAULT_DATASOURCE = "systemDataSource";
    
    /**
     * 线程本地存储当前数据源名称
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前线程数据源
     *
     * @param dataSourceName 数据源名称
     */
    public static void setDataSource(String dataSourceName) {
        log.debug("切换数据源到: {}", dataSourceName);
        CONTEXT_HOLDER.set(dataSourceName);
    }
    
    /**
     * 获取当前线程数据源
     *
     * @return 当前数据源名称
     */
    public static String getDataSource() {
        String dataSource = CONTEXT_HOLDER.get();
        return dataSource == null ? DEFAULT_DATASOURCE : dataSource;
    }
    
    /**
     * 清除当前线程数据源
     * 将使用默认数据源
     */
    public static void clearDataSource() {
        log.debug("清除数据源设置，切换为默认数据源");
        CONTEXT_HOLDER.remove();
    }
} 