package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 插件依赖关系表实体类
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_plugin_dependency")
public class SysPluginDependency extends BaseEntity {

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
     * 依赖的插件ID
     */
    @TableField("dependency_plugin_id")
    private String dependencyPluginId;

    /**
     * 版本要求，如：>=1.0.0 <2.0.0
     */
    @TableField("version_requirement")
    private String versionRequirement;

    /**
     * 是否可选依赖
     */
    @TableField("optional")
    private Boolean optional;
} 