/*
 * Copyright (c) 2023-2025 XiaoQuTeam. All rights reserved.
 * QTeamOS is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

/**
 * 插件版本历史表实体类
 * 用于记录插件的版本历史和部署状态
 *
 * @author yangqijun
 * @date 2025-04-28
 * @since 1.0.0
 */
package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 插件版本历史表实体类
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_plugin_version")
public class SysPluginVersion extends BaseEntity {

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
     * 前置版本
     */
    @TableField("previous_version")
    private String previousVersion;
    
    /**
     * 是否已部署
     */
    @TableField("deployed")
    private Boolean deployed;
    
    /**
     * 记录时间
     */
    @TableField("record_time")
    private LocalDateTime recordTime;
    
    /**
     * 部署时间
     */
    @TableField("deploy_time")
    private LocalDateTime deployTime;
    
    /**
     * 升级路径，JSON格式
     */
    @TableField("upgrade_path")
    private String upgradePath;
    
    /**
     * 版本说明
     */
    @TableField("release_notes")
    private String releaseNotes;
    
    /**
     * 版本变更类型：major-主版本，minor-次版本，patch-补丁，alpha-测试版，beta-预览版
     */
    @TableField("change_type")
    private String changeType;
} 