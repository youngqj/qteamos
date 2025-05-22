package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


/**
 * 插件基本信息表实体类
 *
 * @author yangqijun
 * @since 2025-04-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("sys_plugin_info")
public class SysPluginInfo extends BaseEntity {

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
     * 插件名称
     */
    @TableField("name")
    private String name;

    /**
     * 插件版本
     */
    @TableField("version")
    private String version;

    /**
     * 插件描述
     */
    @TableField("description")
    private String description;

    /**
     * 插件作者
     */
    @TableField("author")
    private String author;

    /**
     * 插件主类全限定名
     */
    @TableField("main_class")
    private String mainClass;

    /**
     * 插件类型：normal-普通插件，system-系统插件
     */
    @TableField("type")
    private String type;

    /**
     * 信任级别：trust-受信任的，untrusted-不受信任的
     */
    @TableField("trust")
    private String trust;

    /**
     * 所需最低系统版本
     */
    @TableField("required_system_version")
    private String requiredSystemVersion;

    /**
     * 插件优先级，数值越小优先级越高
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 插件提供者/开发者
     */
    @TableField("provider")
    private String provider;

    /**
     * 插件许可证类型
     */
    @TableField("license")
    private String license;

    /**
     * 插件分类
     */
    @TableField("category")
    private String category;

    /**
     * 插件官网或文档地址
     */
    @TableField("website")
    private String website;
    
    /**
     * 插件JAR文件路径
     */
    @TableField("jar_file")
    private String jarPath;

    /**
     * 是否有依赖，默认0没有
     */
    @TableField("have_dependency")
    private Integer haveDependency;
} 