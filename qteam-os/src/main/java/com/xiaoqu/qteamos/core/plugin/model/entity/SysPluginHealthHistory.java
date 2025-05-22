/*
 * Copyright (c) 2023-2024 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 插件健康检查历史记录实体类
 *
 * @author yangqijun
 * @date 2024-08-15
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_plugin_health_history")
public class SysPluginHealthHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 插件ID
     */
    @TableField("plugin_id")
    private String pluginId;

    /**
     * 插件版本
     */
    @TableField("version")
    private String version;

    /**
     * 插件状态
     */
    @TableField("state")
    private String state;

    /**
     * 是否健康
     */
    @TableField("healthy")
    private Boolean healthy;

    /**
     * 健康状态消息
     */
    @TableField("health_message")
    private String healthMessage;

    /**
     * 失败计数
     */
    @TableField("fail_count")
    private Integer failCount;

    /**
     * 内存使用量(MB)
     */
    @TableField("memory_usage_mb")
    private Integer memoryUsageMb;

    /**
     * 线程数量
     */
    @TableField("thread_count")
    private Integer threadCount;

    /**
     * 检查类型（AUTO-自动, MANUAL-手动）
     */
    @TableField("check_type")
    private String checkType;

    /**
     * 采集时间
     */
    @TableField("collect_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 创建健康历史记录
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param state 插件状态
     * @param healthy 是否健康
     * @param healthMessage 健康状态消息
     * @param failCount 失败计数
     * @param checkType 检查类型
     * @return 健康历史记录
     */
    public static SysPluginHealthHistory create(String pluginId, String version, String state,
            boolean healthy, String healthMessage, int failCount, String checkType) {
        SysPluginHealthHistory history = new SysPluginHealthHistory();
        history.setPluginId(pluginId);
        history.setVersion(version);
        history.setState(state);
        history.setHealthy(healthy);
        history.setHealthMessage(healthMessage);
        history.setFailCount(failCount);
        history.setCheckType(checkType);
        history.setCollectTime(LocalDateTime.now());
        return history;
    }
    
    /**
     * 创建健康历史记录（包含资源使用情况）
     *
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param state 插件状态
     * @param healthy 是否健康
     * @param healthMessage 健康状态消息
     * @param failCount 失败计数
     * @param memoryUsageMb 内存使用量(MB)
     * @param threadCount 线程数量
     * @param checkType 检查类型
     * @return 健康历史记录
     */
    public static SysPluginHealthHistory create(String pluginId, String version, String state,
            boolean healthy, String healthMessage, int failCount,
            int memoryUsageMb, int threadCount, String checkType) {
        SysPluginHealthHistory history = create(pluginId, version, state, healthy, healthMessage, failCount, checkType);
        history.setMemoryUsageMb(memoryUsageMb);
        history.setThreadCount(threadCount);
        return history;
    }
} 