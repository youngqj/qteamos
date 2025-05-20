package com.xiaoqu.qteamos.core.plugin.monitoring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 插件统计数据实体类（按天）
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Data
@TableName("sys_plugin_stats_daily")
public class PluginStatsDaily {

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
     * 统计日期
     */
    private LocalDate statsDate;

    /**
     * 平均CPU使用率(%)
     */
    private Float avgCpuUsage;

    /**
     * 最大CPU使用率(%)
     */
    private Float maxCpuUsage;

    /**
     * 平均内存使用量(MB)
     */
    private Float avgMemoryUsage;

    /**
     * 最大内存使用量(MB)
     */
    private Float maxMemoryUsage;

    /**
     * 平均线程数
     */
    private Float avgThreadCount;

    /**
     * 最大线程数
     */
    private Integer maxThreadCount;

    /**
     * 错误次数
     */
    private Integer errorCount;

    /**
     * 资源违规次数
     */
    private Integer violationCount;

    /**
     * 停机时间(分钟)
     */
    private Integer downtimeMinutes;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 