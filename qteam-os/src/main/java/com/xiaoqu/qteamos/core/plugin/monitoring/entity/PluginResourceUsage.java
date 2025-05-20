/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 10:37:05
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 10:37:33
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/monitoring/entity/PluginResourceUsage.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.monitoring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 插件资源使用历史记录实体类
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Data
@TableName("sys_plugin_resource_usage")
public class PluginResourceUsage {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 插件ID
     */
    private String pluginId;

    /**
     * CPU使用率(%)
     */
    private Integer cpuUsagePercent;

    /**
     * 内存使用量(MB)
     */
    private Integer memoryUsageMb;

    /**
     * 线程数量
     */
    private Integer threadCount;

    /**
     * 文件描述符数量
     */
    private Integer fileDescriptorCount;

    /**
     * 网络连接数
     */
    private Integer networkConnectionCount;

    /**
     * 采集时间
     */
    private LocalDateTime collectTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
} 