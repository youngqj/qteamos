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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 插件配置表实体类
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_plugin_config")
public class SysPluginConfig extends BaseEntity {

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
    @TableField("plugin_version")
    private String pluginVersion;

    /**
     * 配置键
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 配置值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 配置类型：STRING-字符串，NUMBER-数字，BOOLEAN-布尔值，JSON-JSON对象
     */
    @TableField("config_type")
    private String configType;

    /**
     * 配置描述
     */
    @TableField("description")
    private String description;

    /**
     * 默认值
     */
    @TableField("default_value")
    private String defaultValue;

    /**
     * 是否系统配置
     */
    @TableField("is_system")
    private Boolean isSystem;
} 