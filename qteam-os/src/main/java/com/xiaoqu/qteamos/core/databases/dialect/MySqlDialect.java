/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 11:30:05
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 11:34:04
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/databases/dialect/MySqlDialect.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.databases.dialect;

import org.springframework.stereotype.Component;

/**
 * MySQL数据库方言实现
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Component
public class MySqlDialect implements DbDialect {

    /**
     * 方言类型
     */
    private static final String DIALECT_TYPE = "mysql";

    @Override
    public String getPageSql(String sql, int offset, int limit) {
        StringBuilder pageSql = new StringBuilder(sql);
        pageSql.append(" LIMIT ").append(offset).append(", ").append(limit);
        return pageSql.toString();
    }

    @Override
    public String getCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") AS t";
    }

    @Override
    public String getDialectType() {
        return DIALECT_TYPE;
    }
} 