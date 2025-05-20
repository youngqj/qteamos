package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 插件权限表实体类
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_plugin_permission")
public class SysPluginPermission extends BaseEntity {

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
     * 权限标识
     */
    @TableField("permission")
    private String permission;

    /**
     * 是否已授权
     */
    @TableField("granted")
    private Boolean granted;

    /**
     * 授权时间
     */
    @TableField("granted_time")
    private LocalDateTime grantedTime;

    /**
     * 授权人
     */
    @TableField("granted_by")
    private String grantedBy;

    /**
     * 授权原因
     */
    @TableField("grant_reason")
    private String grantReason;
} 