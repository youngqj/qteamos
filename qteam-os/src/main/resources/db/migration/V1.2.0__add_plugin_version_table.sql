-- 插件版本历史表
CREATE TABLE IF NOT EXISTS `sys_plugin_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  `previous_version` varchar(20) DEFAULT NULL COMMENT '前置版本',
  `deployed` tinyint(1) DEFAULT 0 COMMENT '是否已部署',
  `record_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `deploy_time` datetime DEFAULT NULL COMMENT '部署时间',
  `upgrade_path` text DEFAULT NULL COMMENT '升级路径，JSON格式',
  `release_notes` text DEFAULT NULL COMMENT '版本说明',
  `change_type` varchar(20) DEFAULT 'patch' COMMENT '版本变更类型：major-主版本，minor-次版本，patch-补丁，alpha-测试版，beta-预览版',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_id_version` (`plugin_id`, `version`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_record_time` (`record_time`),
  KEY `idx_change_type` (`change_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件版本历史表';

-- 添加注释
COMMENT ON TABLE `sys_plugin_version` IS '插件版本历史表';
COMMENT ON COLUMN `sys_plugin_version`.`id` IS '主键ID';
COMMENT ON COLUMN `sys_plugin_version`.`plugin_id` IS '插件唯一标识符';
COMMENT ON COLUMN `sys_plugin_version`.`version` IS '插件版本';
COMMENT ON COLUMN `sys_plugin_version`.`previous_version` IS '前置版本';
COMMENT ON COLUMN `sys_plugin_version`.`deployed` IS '是否已部署';
COMMENT ON COLUMN `sys_plugin_version`.`record_time` IS '记录时间';
COMMENT ON COLUMN `sys_plugin_version`.`deploy_time` IS '部署时间';
COMMENT ON COLUMN `sys_plugin_version`.`upgrade_path` IS '升级路径，JSON格式';
COMMENT ON COLUMN `sys_plugin_version`.`release_notes` IS '版本说明';
COMMENT ON COLUMN `sys_plugin_version`.`change_type` IS '版本变更类型：major-主版本，minor-次版本，patch-补丁，alpha-测试版，beta-预览版'; 