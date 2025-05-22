-- 插件健康检查历史表
CREATE TABLE IF NOT EXISTS `sys_plugin_health_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件ID',
  `version` varchar(50) DEFAULT NULL COMMENT '插件版本',
  `state` varchar(20) DEFAULT NULL COMMENT '插件状态',
  `healthy` tinyint(1) DEFAULT NULL COMMENT '是否健康',
  `health_message` varchar(500) DEFAULT NULL COMMENT '健康状态消息',
  `fail_count` int(11) DEFAULT 0 COMMENT '失败计数',
  `memory_usage_mb` int(11) DEFAULT NULL COMMENT '内存使用量(MB)',
  `thread_count` int(11) DEFAULT NULL COMMENT '线程数量',
  `check_type` varchar(20) DEFAULT 'AUTO' COMMENT '检查类型（AUTO-自动, MANUAL-手动）',
  `collect_time` datetime DEFAULT NULL COMMENT '采集时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_healthy` (`healthy`),
  KEY `idx_collect_time` (`collect_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件健康检查历史记录表'; 