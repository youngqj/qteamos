/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 11:26:16
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 11:53:34
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/databases/annotation/DataSource.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.databases.annotation;

import java.lang.annotation.*;

/**
 * 数据源注解
 * 用于指定方法或类使用的数据源
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {
    
    /**
     * 数据源名称
     * 默认使用系统主数据源
     */
    String value() default "systemDataSource";
} 