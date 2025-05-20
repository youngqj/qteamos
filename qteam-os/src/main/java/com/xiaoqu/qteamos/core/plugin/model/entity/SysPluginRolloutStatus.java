package com.xiaoqu.qteamos.core.plugin.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xiaoqu.qteamos.core.plugin.manager.PluginRolloutManager.RolloutState;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 插件灰度发布状态实体类
 * 对应表 sys_plugin_rollout_status
 *
 * @author yangqijun
 * @date 2024-07-25
 */
@Data
@Accessors(chain = true)
@TableName("sys_plugin_rollout_status")
public class SysPluginRolloutStatus implements Serializable {

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
     * 当前版本
     */
    @TableField("current_version")
    private String currentVersion;

    /**
     * 目标版本
     */
    @TableField("target_version")
    private String targetVersion;

    /**
     * 批次大小(百分比)
     */
    @TableField("batch_size")
    private Integer batchSize;

    /**
     * 验证时间(分钟)
     */
    @TableField("validate_time_minutes")
    private Integer validateTimeMinutes;

    /**
     * 当前批次
     */
    @TableField("current_batch")
    private Integer currentBatch;

    /**
     * 当前百分比
     */
    @TableField("current_percentage")
    private Integer currentPercentage;

    /**
     * 状态
     */
    @TableField("state")
    private String state;

    /**
     * 状态消息
     */
    @TableField("message")
    private String message;

    /**
     * 开始时间
     */
    @TableField("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 上次批次时间
     */
    @TableField("last_batch_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastBatchTime;

    /**
     * 完成时间
     */
    @TableField("completion_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;

    /**
     * 元数据(JSON格式)
     */
    @TableField("metadata")
    private String metadata;

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

    /**
     * 从RolloutStatus转换
     */
    public static SysPluginRolloutStatus fromRolloutStatus(
            com.xiaoqu.qteamos.core.plugin.manager.PluginRolloutManager.RolloutStatus status) {
        SysPluginRolloutStatus entity = new SysPluginRolloutStatus();
        entity.setPluginId(status.getPluginId());
        entity.setCurrentVersion(status.getCurrentVersion());
        entity.setTargetVersion(status.getTargetVersion());
        entity.setBatchSize(status.getBatchSize());
        entity.setValidateTimeMinutes(status.getValidateTimeMinutes());
        entity.setCurrentBatch(status.getCurrentBatch());
        entity.setCurrentPercentage(status.getCurrentPercentage());
        entity.setState(status.getState().name());
        entity.setMessage(status.getMessage());
        entity.setStartTime(status.getStartTime());
        entity.setLastBatchTime(status.getLastBatchTime());
        entity.setCompletionTime(status.getCompletionTime());
        
        // 将Map转为JSON字符串
        if (status.getMetadata() != null && !status.getMetadata().isEmpty()) {
            try {
                entity.setMetadata(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(status.getMetadata()));
            } catch (Exception e) {
                // 日志记录异常
            }
        }
        
        return entity;
    }

    /**
     * 转换为RolloutStatus
     */
    public com.xiaoqu.qteamos.core.plugin.manager.PluginRolloutManager.RolloutStatus toRolloutStatus() {
        com.xiaoqu.qteamos.core.plugin.manager.PluginRolloutManager.RolloutStatus status = 
            new com.xiaoqu.qteamos.core.plugin.manager.PluginRolloutManager.RolloutStatus(
                this.pluginId,
                this.currentVersion,
                this.targetVersion,
                this.batchSize,
                this.validateTimeMinutes
            );
        
        status.setCurrentBatch(this.currentBatch);
        status.setCurrentPercentage(this.currentPercentage);
        status.setState(RolloutState.valueOf(this.state));
        status.setMessage(this.message);
        status.setLastBatchTime(this.lastBatchTime);
        status.setCompletionTime(this.completionTime);
        
        // 将JSON字符串转为Map
        if (this.metadata != null && !this.metadata.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(this.metadata, Map.class);
                status.setMetadata(metadataMap);
            } catch (Exception e) {
                // 日志记录异常
            }
        }
        
        return status;
    }
} 