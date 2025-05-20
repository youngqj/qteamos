package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 插件扩展点表实体类
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_plugin_extension_point")
public class SysPluginExtensionPoint extends BaseEntity {

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
     * 扩展点ID
     */
    @TableField("extension_point_id")
    private String extensionPointId;

    /**
     * 扩展点名称
     */
    @TableField("name")
    private String name;

    /**
     * 扩展点描述
     */
    @TableField("description")
    private String description;

    /**
     * 扩展点类型
     */
    @TableField("type")
    private String type;

    /**
     * 接口或抽象类全限定名
     */
    @TableField("interface_class")
    private String interfaceClass;

    /**
     * 是否允许多个实现
     */
    @TableField("multiple")
    private Boolean multiple;

    /**
     * 是否必须实现
     */
    @TableField("required")
    private Boolean required;
} 