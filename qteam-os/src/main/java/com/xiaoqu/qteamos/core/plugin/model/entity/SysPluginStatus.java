/*
 * @Author: yangqijun youngqj@126.com
 * @Date: 2025-04-28 13:57:16
 * @LastEditors: yangqijun youngqj@126.com
 * @LastEditTime: 2025-04-28 14:02:46
 * @FilePath: /qteamos/src/main/java/com/xiaoqu/qteamos/model/entity/SysPluginStatus.java
 * @Description: 
 * 
 * Copyright © Zhejiang Xiaoqu Information Technology Co., Ltd, All Rights Reserved. 
 */
package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 插件状态表实体类
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_plugin_status")
public class SysPluginStatus extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 插件唯一标识符
     */
    @TableField("plugin_id")
    private String pluginId;

    /**
     * 插件版本
     */
    @TableField("version")
    private String version;

    /**
     * 是否启用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 插件状态：INSTALLED-已安装，RUNNING-运行中，STOPPED-已停止，ERROR-错误
     */
    @TableField("status")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 安装时间
     */
    @TableField("installed_time")
    private LocalDateTime installedTime;

    /**
     * 最后启动时间
     */
    @TableField("last_start_time")
    private LocalDateTime lastStartTime;

    /**
     * 最后停止时间
     */
    @TableField("last_stop_time")
    private LocalDateTime lastStopTime;
} 