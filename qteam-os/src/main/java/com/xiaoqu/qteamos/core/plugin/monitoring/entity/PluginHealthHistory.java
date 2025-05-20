package com.xiaoqu.qteamos.core.plugin.monitoring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 插件健康状态历史记录实体类
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Data
@TableName("sys_plugin_health_history")
public class PluginHealthHistory {

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
     * 插件版本
     */
    private String version;

    /**
     * 插件状态
     */
    private String state;

    /**
     * 是否健康：1健康，0不健康
     */
    private Boolean healthy;

    /**
     * 健康状态消息
     */
    private String healthMessage;

    /**
     * 失败计数
     */
    private Integer failCount;

    /**
     * 总错误次数
     */
    private Integer totalErrorCount;

    /**
     * 连续错误次数
     */
    private Integer consecutiveErrorCount;

    /**
     * 最后错误消息
     */
    private String lastErrorMessage;

    /**
     * 内存使用量(MB)
     */
    private Integer memoryUsageMb;

    /**
     * 线程数量
     */
    private Integer threadCount;

    /**
     * 采集时间
     */
    private LocalDateTime collectTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 