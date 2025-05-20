/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-07 11:14:46
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-07 12:08:03
 * @FilePath: /qelebase/qelebase-common/src/main/java/com/xiaoqu/qelebase/common/result/PageResult.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页结果
 *
 * @author yangqijun@xiaoquio.com
 * @version 1.0.0
 * @copyright 浙江小趣信息技术有限公司
 */
@Data
public class PageResult<T> {

    /**
     * 记录列表
     */
    private List<T> records;

    /**
     * 页码
     */
    private long page;

    /**
     * 每页大小
     */
    private long pageSize;

    /**
     * 总记录数
     */
    private long total;

    public PageResult() {
    }

    public PageResult(List<T> records, long page, long pageSize, long total) {
        this.records = records;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }
} 