package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 插件更新历史实体类
 * 对应表 sys_plugin_update_history
 *
 * @author yangqijun
 * @date 2024-07-25
 */
@Data
@Accessors(chain = true)
@TableName("sys_plugin_update_history")
public class SysPluginUpdateHistory implements Serializable {

    private static final long serialVersionUID = 1L;

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
     * 更新前版本
     */
    @TableField("previous_version")
    private String previousVersion;

    /**
     * 更新后版本
     */
    @TableField("target_version")
    private String targetVersion;

    /**
     * 更新状态
     */
    @TableField("status")
    private String status;  // SUCCESS-成功，FAILED-失败，ROLLBACK-已回滚

    /**
     * 更新日志
     */
    @TableField("update_log")
    private String updateLog;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 执行人
     */
    @TableField("executed_by")
    private String executedBy;

    /**
     * 备份路径
     */
    @TableField("backup_path")
    private String backupPath;

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
    private LocalDateTime updateModifyTime;

    /**
     * 创建者
     */
    @TableField("create_by")
    private String createBy;

    /**
     * 更新者
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 是否删除
     */
    @TableLogic
    @TableField("deleted")
    private Boolean deleted;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version_num")
    private Integer versionNum;
} 