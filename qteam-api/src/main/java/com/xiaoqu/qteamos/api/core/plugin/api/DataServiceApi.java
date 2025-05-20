/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-30 21:14:18
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-30 21:15:07
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/sdk/plugin/api/DataServiceApi.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.api.core.plugin.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据服务API接口
 * 提供安全的数据访问能力
 *
 * @author yangqijun
 * @date 2025-05-01
 */
public interface DataServiceApi {

    /**
     * 查询数据
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 查询结果
     */
    List<Map<String, Object>> query(String sql, Object... params);

    /**
     * 更新数据
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 影响的行数
     */
    int update(String sql, Object... params);

    /**
     * 执行插入并返回生成的主键
     *
     * @param sql SQL语句
     * @param params 参数
     * @return 生成的主键
     */
    Optional<Object> insertAndGetKey(String sql, Object... params);

    /**
     * 批量更新
     *
     * @param sql SQL语句
     * @param batchArgs 批处理参数
     * @return 影响的行数数组
     */
    int[] batchUpdate(String sql, List<Object[]> batchArgs);

    /**
     * 执行事务
     *
     * @param action 事务操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    <T> T executeInTransaction(TransactionAction<T> action);

    /**
     * 事务操作接口
     *
     * @param <T> 返回类型
     */
    @FunctionalInterface
    interface TransactionAction<T> {
        /**
         * 执行事务操作
         *
         * @param dataService 数据服务
         * @return 操作结果
         * @throws Exception 操作异常
         */
        T execute(DataServiceApi dataService) throws Exception;
    }
} 