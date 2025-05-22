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
 * 插件状态变更历史实体类
 * 对应表 sys_plugin_state_history
 *
 * @author yangqijun
 * @date 2024-08-10
 */
@Data
@Accessors(chain = true)
@TableName("sys_plugin_state_history")
public class SysPluginStateHistory implements Serializable {

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
     * 旧状态
     */
    @TableField("old_state")
    private String oldState;

    /**
     * 新状态
     */
    @TableField("new_state")
    private String newState;

    /**
     * 状态变更附加信息
     */
    @TableField("message")
    private String message;

    /**
     * 变更时间
     */
    @TableField("change_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime changeTime;

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
     * 创建状态历史记录
     * 
     * @param pluginId 插件ID
     * @param version 插件版本
     * @param oldState 旧状态
     * @param newState 新状态
     * @param message 附加信息
     * @return 状态历史记录
     */
    public static SysPluginStateHistory create(String pluginId, String version, String oldState, String newState, String message) {
        SysPluginStateHistory history = new SysPluginStateHistory();
        history.setPluginId(pluginId);
        history.setVersion(version);
        history.setOldState(oldState);
        history.setNewState(newState);
        history.setMessage(message);
        history.setChangeTime(LocalDateTime.now());
        return history;
    }
} 