package com.xiaoqu.qteamos.core.databases.core;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源
 * 基于Spring的AbstractRoutingDataSource实现数据源动态切换
 *
 * @author yangqijun
 * @date 2025-05-02
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    /**
     * 获取当前数据源的key
     * 
     * @return 当前数据源的key
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSource();
    }
} 