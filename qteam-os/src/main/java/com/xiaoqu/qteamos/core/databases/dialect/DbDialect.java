/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 11:27:21
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 11:32:24
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/databases/dialect/DbDialect.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.databases.dialect;

/**
 * 数据库方言接口
 * 用于处理不同数据库的SQL差异
 *
 * @author yangqijun
 * @date 2025-05-02
 */
public interface DbDialect {

    /**
     * 获取分页SQL
     *
     * @param sql 原始SQL
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 分页SQL
     */
    String getPageSql(String sql, int offset, int limit);
    
    /**
     * 获取查询总数SQL
     *
     * @param sql 原始SQL
     * @return 查询总数SQL
     */
    String getCountSql(String sql);
    
    /**
     * 获取方言类型
     *
     * @return 方言类型
     */
    String getDialectType();
}