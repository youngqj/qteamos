-- 插件健康状态历史表
CREATE TABLE IF NOT EXISTS `sys_plugin_health_history` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plugin_id` VARCHAR(100) NOT NULL COMMENT '插件ID',
    `version` VARCHAR(50) NOT NULL COMMENT '插件版本',
    `state` VARCHAR(20) NOT NULL COMMENT '插件状态',
    `healthy` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否健康：1健康，0不健康',
    `health_message` VARCHAR(500) DEFAULT NULL COMMENT '健康状态消息',
    `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败计数',
    `total_error_count` INT NOT NULL DEFAULT 0 COMMENT '总错误次数',
    `consecutive_error_count` INT NOT NULL DEFAULT 0 COMMENT '连续错误次数',
    `last_error_message` VARCHAR(500) DEFAULT NULL COMMENT '最后错误消息',
    `memory_usage_mb` INT NOT NULL DEFAULT 0 COMMENT '内存使用量(MB)',
    `thread_count` INT NOT NULL DEFAULT 0 COMMENT '线程数量',
    `collect_time` DATETIME NOT NULL COMMENT '采集时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_plugin_id` (`plugin_id`),
    INDEX `idx_collect_time` (`collect_time`),
    INDEX `idx_healthy` (`healthy`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件健康状态历史表';

-- 插件资源使用历史表
CREATE TABLE IF NOT EXISTS `sys_plugin_resource_usage` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plugin_id` VARCHAR(100) NOT NULL COMMENT '插件ID',
    `cpu_usage_percent` INT NOT NULL DEFAULT 0 COMMENT 'CPU使用率(%)',
    `memory_usage_mb` INT NOT NULL DEFAULT 0 COMMENT '内存使用量(MB)',
    `thread_count` INT NOT NULL DEFAULT 0 COMMENT '线程数量',
    `file_descriptor_count` INT NOT NULL DEFAULT 0 COMMENT '文件描述符数量',
    `network_connection_count` INT NOT NULL DEFAULT 0 COMMENT '网络连接数',
    `collect_time` DATETIME NOT NULL COMMENT '采集时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_plugin_id` (`plugin_id`),
    INDEX `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件资源使用历史表';

-- 插件资源违规记录表
CREATE TABLE IF NOT EXISTS `sys_plugin_resource_violation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plugin_id` VARCHAR(100) NOT NULL COMMENT '插件ID',
    `resource_type` VARCHAR(20) NOT NULL COMMENT '资源类型：MEMORY/CPU/THREAD/FILE/NETWORK',
    `violation_count` INT NOT NULL DEFAULT 1 COMMENT '违规次数',
    `threshold_value` INT NOT NULL COMMENT '阈值',
    `actual_value` INT NOT NULL COMMENT '实际值',
    `restriction_level` TINYINT NOT NULL DEFAULT 0 COMMENT '限制级别：0无限制，1轻微，2严重，3暂停',
    `restriction_reason` VARCHAR(200) DEFAULT NULL COMMENT '限制原因',
    `violation_time` DATETIME NOT NULL COMMENT '违规时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_plugin_id` (`plugin_id`),
    INDEX `idx_violation_time` (`violation_time`),
    INDEX `idx_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件资源违规记录表';

-- 插件统计数据表（按天汇总数据）
CREATE TABLE IF NOT EXISTS `sys_plugin_stats_daily` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plugin_id` VARCHAR(100) NOT NULL COMMENT '插件ID',
    `stats_date` DATE NOT NULL COMMENT '统计日期',
    `avg_cpu_usage` FLOAT NOT NULL DEFAULT 0 COMMENT '平均CPU使用率(%)',
    `max_cpu_usage` FLOAT NOT NULL DEFAULT 0 COMMENT '最大CPU使用率(%)',
    `avg_memory_usage` FLOAT NOT NULL DEFAULT 0 COMMENT '平均内存使用量(MB)',
    `max_memory_usage` FLOAT NOT NULL DEFAULT 0 COMMENT '最大内存使用量(MB)',
    `avg_thread_count` FLOAT NOT NULL DEFAULT 0 COMMENT '平均线程数',
    `max_thread_count` INT NOT NULL DEFAULT 0 COMMENT '最大线程数',
    `error_count` INT NOT NULL DEFAULT 0 COMMENT '错误次数',
    `violation_count` INT NOT NULL DEFAULT 0 COMMENT '资源违规次数',
    `downtime_minutes` INT NOT NULL DEFAULT 0 COMMENT '停机时间(分钟)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plugin_date` (`plugin_id`, `stats_date`),
    INDEX `idx_stats_date` (`stats_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件统计数据表（按天）'; 