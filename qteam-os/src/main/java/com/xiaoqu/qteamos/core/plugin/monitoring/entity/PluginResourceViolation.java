/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-05-01 10:37:28
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-05-01 10:39:20
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/core/plugin/monitoring/entity/PluginResourceViolation.java
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
 * 插件资源违规记录实体类
 *
 * @author yangqijun
 * @date 2025-05-02
 */
@Data
@TableName("sys_plugin_resource_violation")
public class PluginResourceViolation {

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
     * 资源类型：MEMORY/CPU/THREAD/FILE/NETWORK
     */
    private String resourceType;

    /**
     * 违规次数
     */
    private Integer violationCount;

    /**
     * 阈值
     */
    private Integer thresholdValue;

    /**
     * 实际值
     */
    private Integer actualValue;

    /**
     * 限制级别：0无限制，1轻微，2严重，3暂停
     */
    private Integer restrictionLevel;

    /**
     * 限制原因
     */
    private String restrictionReason;

    /**
     * 违规时间
     */
    private LocalDateTime violationTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
} 