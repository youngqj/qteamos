/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-02 12:15:25
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 12:00:25
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/databases/dialect/MongoDbDialect.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.databases.dialect;

import org.springframework.stereotype.Component;

/**
 * MongoDB数据库方言实现
 * 注意：由于MongoDB使用非SQL语法，本方言主要提供辅助功能
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Component
public class MongoDbDialect implements DbDialect {

    /**
     * 方言类型
     */
    private static final String DIALECT_TYPE = "mongodb";

    /**
     * 获取MongoDB分页查询语句
     * 注意：MongoDB不使用SQL语法，此方法主要用于兼容接口
     * 实际使用时应使用MongoDB原生的limit和skip方法
     *
     * @param sql MongoDB查询JSON字符串
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 带有分页信息的查询字符串
     */
    @Override
    public String getPageSql(String sql, int offset, int limit) {
        // 假设输入是MongoDB的查询JSON字符串
        // 实际使用中，应该使用MongoDB驱动的原生API进行分页
        if (sql.endsWith("}")) {
            // 在查询JSON中添加分页信息
            return sql.substring(0, sql.length() - 1) + 
                   ", \"$skip\": " + offset + 
                   ", \"$limit\": " + limit + "}";
        }
        return sql;
    }

    /**
     * 获取MongoDB计数查询
     * 注意：MongoDB不使用SQL语法，此方法主要用于兼容接口
     * 实际使用时应使用MongoDB的count()方法
     *
     * @param sql MongoDB查询JSON字符串
     * @return 计数查询
     */
    @Override
    public String getCountSql(String sql) {
        // 假设输入是MongoDB的查询JSON字符串
        // 实际使用中，应该使用MongoDB驱动的count()方法
        return "db.collection.count(" + sql + ")";
    }

    /**
     * 获取方言类型
     *
     * @return 方言类型
     */
    @Override
    public String getDialectType() {
        return DIALECT_TYPE;
    }
    
    /**
     * 构建MongoDB查询条件
     * 
     * @param field 字段名
     * @param value 字段值
     * @param operator 操作符（如$eq, $gt, $lt等）
     * @return 查询条件JSON字符串
     */
    public String buildQueryCondition(String field, Object value, String operator) {
        if (operator == null || operator.isEmpty()) {
            operator = "$eq";
        }
        
        String valueStr = (value instanceof String) ? "\"" + value + "\"" : String.valueOf(value);
        return "{ \"" + field + "\": { \"" + operator + "\": " + valueStr + " } }";
    }
    
    /**
     * 构建MongoDB排序条件
     * 
     * @param field 字段名
     * @param ascending 是否升序（true为升序，false为降序）
     * @return 排序条件JSON字符串
     */
    public String buildSortCondition(String field, boolean ascending) {
        int direction = ascending ? 1 : -1;
        return "{ \"" + field + "\": " + direction + " }";
    }
} 