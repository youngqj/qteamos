-- 插件状态变更历史表
CREATE TABLE IF NOT EXISTS `sys_plugin_state_history` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plugin_id` VARCHAR(100) NOT NULL COMMENT '插件ID',
    `version` VARCHAR(50) NOT NULL COMMENT '插件版本',
    `old_state` VARCHAR(20) DEFAULT NULL COMMENT '旧状态',
    `new_state` VARCHAR(20) NOT NULL COMMENT '新状态',
    `message` VARCHAR(500) DEFAULT NULL COMMENT '状态变更附加信息',
    `change_time` DATETIME NOT NULL COMMENT '变更时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_plugin_id` (`plugin_id`),
    INDEX `idx_change_time` (`change_time`),
    INDEX `idx_new_state` (`new_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件状态变更历史表'; 