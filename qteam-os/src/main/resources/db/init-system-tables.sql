-- 插件系统数据库表结构
-- 为避免与插件业务表重名，所有表名加上 sys_plugin_ 前缀

-- 插件基本信息表
CREATE TABLE IF NOT EXISTS `sys_plugin_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `name` varchar(100) NOT NULL COMMENT '插件名称',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  `description` text COMMENT '插件描述',
  `author` varchar(100) DEFAULT NULL COMMENT '插件作者',
  `main_class` varchar(255) NOT NULL COMMENT '插件主类全限定名',
  `type` varchar(20) DEFAULT 'normal' COMMENT '插件类型：normal-普通插件，system-系统插件',
  `trust` varchar(20) DEFAULT 'trusted' COMMENT '信任级别：trusted-受信任的，official-官方的',
  `required_system_version` varchar(20) DEFAULT NULL COMMENT '所需最低系统版本',
  `priority` int(11) DEFAULT '10' COMMENT '插件优先级，数值越小优先级越高',
  `provider` varchar(100) DEFAULT NULL COMMENT '插件提供者/开发者',
  `license` varchar(50) DEFAULT NULL COMMENT '插件许可证类型',
  `category` varchar(50) DEFAULT NULL COMMENT '插件分类',
  `website` varchar(255) DEFAULT NULL COMMENT '插件官网或文档地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_id_version` (`plugin_id`, `version`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_type` (`type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件基本信息表';

-- 插件状态表
CREATE TABLE IF NOT EXISTS `sys_plugin_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `status` varchar(20) DEFAULT 'INSTALLED' COMMENT '插件状态：INSTALLED-已安装，RUNNING-运行中，STOPPED-已停止，ERROR-错误',
  `error_message` text COMMENT '错误信息',
  `installed_time` datetime DEFAULT NULL COMMENT '安装时间',
  `last_start_time` datetime DEFAULT NULL COMMENT '最后启动时间',
  `last_stop_time` datetime DEFAULT NULL COMMENT '最后停止时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_id_version` (`plugin_id`, `version`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_status` (`status`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件状态表';

-- 插件依赖关系表
CREATE TABLE IF NOT EXISTS `sys_plugin_dependency` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `dependency_plugin_id` varchar(100) NOT NULL COMMENT '依赖的插件ID',
  `version_requirement` varchar(50) DEFAULT '*' COMMENT '版本要求，如：>=1.0.0 <2.0.0',
  `optional` tinyint(1) DEFAULT '0' COMMENT '是否可选依赖',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_dependency` (`plugin_id`, `plugin_version`, `dependency_plugin_id`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_dependency_plugin_id` (`dependency_plugin_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件依赖关系表';

-- 插件配置表
CREATE TABLE IF NOT EXISTS `sys_plugin_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `config_type` varchar(20) DEFAULT 'STRING' COMMENT '配置类型：STRING-字符串，NUMBER-数字，BOOLEAN-布尔值，JSON-JSON对象',
  `description` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `default_value` text COMMENT '默认值',
  `is_system` tinyint(1) DEFAULT '0' COMMENT '是否系统配置',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_config` (`plugin_id`, `plugin_version`, `config_key`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_is_system` (`is_system`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件配置表';

-- 插件更新历史表
CREATE TABLE IF NOT EXISTS `sys_plugin_update_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `previous_version` varchar(20) NOT NULL COMMENT '更新前版本',
  `target_version` varchar(20) NOT NULL COMMENT '更新后版本',

  `status` varchar(20) NOT NULL COMMENT '更新状态：SUCCESS-成功，FAILED-失败，ROLLBACK-已回滚',
  `update_log` text COMMENT '更新日志',
  `error_message` text COMMENT '错误信息',
  `executed_by` varchar(64) DEFAULT NULL COMMENT '执行人',
  `backup_path` varchar(255) DEFAULT NULL COMMENT '备份路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_status` (`status`),
  KEY `idx_update_time` (`update_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件更新历史表';

-- 数据库迁移记录表
CREATE TABLE IF NOT EXISTS `sys_plugin_migration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  `script_name` varchar(255) NOT NULL COMMENT '迁移脚本名称',
  `script_path` varchar(255) NOT NULL COMMENT '迁移脚本路径',
  `checksum` varchar(64) DEFAULT NULL COMMENT '脚本校验和',
  `execution_time` datetime NOT NULL COMMENT '执行时间',
  `execution_duration` bigint(20) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `status` varchar(20) NOT NULL COMMENT '执行状态：SUCCESS-成功，FAILED-失败',
  `error_message` text COMMENT '错误信息',
  `executed_by` varchar(64) DEFAULT NULL COMMENT '执行人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_script` (`plugin_id`, `version`, `script_name`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_version` (`version`),
  KEY `idx_status` (`status`),
  KEY `idx_execution_time` (`execution_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库迁移记录表';

-- 插件权限表
CREATE TABLE IF NOT EXISTS `sys_plugin_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `permission` varchar(100) NOT NULL COMMENT '权限标识',
  `granted` tinyint(1) DEFAULT '1' COMMENT '是否已授权',
  `granted_time` datetime DEFAULT NULL COMMENT '授权时间',
  `granted_by` varchar(64) DEFAULT NULL COMMENT '授权人',
  `grant_reason` varchar(255) DEFAULT NULL COMMENT '授权原因',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_permission` (`plugin_id`, `plugin_version`, `permission`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_permission` (`permission`),
  KEY `idx_granted` (`granted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件权限表';

-- 插件作者表
CREATE TABLE IF NOT EXISTS `sys_plugin_author` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `author_name` varchar(100) NOT NULL COMMENT '作者名称',
  `author_email` varchar(100) DEFAULT NULL COMMENT '作者邮箱',
  `author_url` varchar(255) DEFAULT NULL COMMENT '作者网址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_author_name` (`author_name`),
  KEY `idx_author_email` (`author_email`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件作者表';

-- 插件资源文件表
CREATE TABLE IF NOT EXISTS `sys_plugin_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `resource_path` varchar(255) NOT NULL COMMENT '资源路径',
  `resource_type` varchar(20) NOT NULL COMMENT '资源类型：file-文件，directory-目录',
  `description` varchar(255) DEFAULT NULL COMMENT '资源描述',
  `required` tinyint(1) DEFAULT '1' COMMENT '是否必须',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_resource` (`plugin_id`, `plugin_version`, `resource_path`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_resource_type` (`resource_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件资源文件表';

-- 插件扩展点表
CREATE TABLE IF NOT EXISTS `sys_plugin_extension_point` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `extension_point_id` varchar(100) NOT NULL COMMENT '扩展点ID',
  `name` varchar(100) NOT NULL COMMENT '扩展点名称',
  `description` text COMMENT '扩展点描述',
  `type` varchar(20) DEFAULT 'interface' COMMENT '扩展点类型',
  `interface_class` varchar(255) DEFAULT NULL COMMENT '接口或抽象类全限定名',
  `multiple` tinyint(1) DEFAULT '1' COMMENT '是否允许多个实现',
  `required` tinyint(1) DEFAULT '0' COMMENT '是否必须实现',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_extension_point` (`plugin_id`, `plugin_version`, `extension_point_id`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_extension_point_id` (`extension_point_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件扩展点表';

-- 插件扩展实现表
CREATE TABLE IF NOT EXISTS `sys_plugin_extension_impl` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '实现插件ID',
  `plugin_version` varchar(20) NOT NULL COMMENT '实现插件版本',
  `provider_plugin_id` varchar(100) NOT NULL COMMENT '提供扩展点的插件ID',
  `extension_point_id` varchar(100) NOT NULL COMMENT '扩展点ID',
  `implementation_class` varchar(255) NOT NULL COMMENT '实现类全限定名',
  `priority` int(11) DEFAULT '10' COMMENT '优先级，数值越小优先级越高',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_extension_impl` (`plugin_id`, `plugin_version`, `provider_plugin_id`, `extension_point_id`, `implementation_class`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_provider_plugin_id` (`provider_plugin_id`),
  KEY `idx_extension_point_id` (`extension_point_id`),
  KEY `idx_priority` (`priority`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件扩展实现表'; 

-- 插件基本信息表
CREATE TABLE IF NOT EXISTS `sys_plugin_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `name` varchar(100) NOT NULL COMMENT '插件名称',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  `description` text COMMENT '插件描述',
  `author` varchar(100) COMMENT '插件作者',
  `main_class` varchar(200) NOT NULL COMMENT '插件主类',
  `plugin_type` varchar(20) DEFAULT 'normal' COMMENT '插件类型: normal-普通插件, system-系统插件',
  `trust_level` varchar(20) DEFAULT 'normal' COMMENT '信任级别: normal-普通, trusted-受信任的, official-官方的',
  `required_system_version` varchar(20) COMMENT '所需最低系统版本',
  `priority` int DEFAULT 0 COMMENT '插件优先级，数值越小优先级越高',
  `jar_path` varchar(255) COMMENT '插件JAR文件路径',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_id_version` (`plugin_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件基本信息表';

-- 插件状态表
CREATE TABLE IF NOT EXISTS `sys_plugin_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `version` varchar(20) NOT NULL COMMENT '插件版本',
  `enabled` tinyint(1) DEFAULT 0 COMMENT '是否启用',
  `state` varchar(20) DEFAULT 'CREATED' COMMENT '插件状态',
  `last_start_time` datetime COMMENT '最后启动时间',
  `last_stop_time` datetime COMMENT '最后停止时间',
  `error_message` text COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_plugin_id_version` (`plugin_id`, `version`),
  KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件状态表';


-- 插件事件记录表
CREATE TABLE IF NOT EXISTS `sys_plugin_event_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` varchar(36) NOT NULL COMMENT '事件ID',
  `topic` varchar(50) NOT NULL COMMENT '事件主题',
  `type` varchar(50) NOT NULL COMMENT '事件类型',
  `source` varchar(100) NOT NULL COMMENT '事件来源',
  `target` varchar(100) COMMENT '事件目标',
  `data` text COMMENT '事件数据(JSON)',
  `timestamp` bigint NOT NULL COMMENT '事件时间戳',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_topic_type` (`topic`, `type`),
  KEY `idx_source` (`source`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件事件记录表';

-- 插件资源使用表
CREATE TABLE IF NOT EXISTS `sys_plugin_resource_usage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `plugin_version` varchar(20) NOT NULL COMMENT '插件版本',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型',
  `resource_name` varchar(100) NOT NULL COMMENT '资源名称',
  `metrics_data` text COMMENT '指标数据(JSON)',
  `record_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_plugin_resource` (`plugin_id`, `resource_type`),
  KEY `idx_record_time` (`record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件资源使用表'; 


-- 插件灰度发布状态表
CREATE TABLE IF NOT EXISTS `sys_plugin_rollout_status` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plugin_id` varchar(100) NOT NULL COMMENT '插件唯一标识符',
  `current_version` varchar(20) NOT NULL COMMENT '当前版本',
  `target_version` varchar(20) NOT NULL COMMENT '目标版本',
  `batch_size` int(11) NOT NULL DEFAULT 20 COMMENT '批次大小(百分比)',
  `validate_time_minutes` int(11) NOT NULL DEFAULT 30 COMMENT '验证时间(分钟)',
  `current_batch` int(11) NOT NULL DEFAULT 0 COMMENT '当前批次',
  `current_percentage` int(11) NOT NULL DEFAULT 0 COMMENT '当前百分比',
  `state` varchar(20) NOT NULL COMMENT '状态：INITIALIZED-初始化，IN_PROGRESS-进行中，PAUSED-暂停，COMPLETED-完成，FAILED-失败',
  `message` varchar(255) DEFAULT NULL COMMENT '状态消息',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `last_batch_time` datetime DEFAULT NULL COMMENT '上次批次时间',
  `completion_time` datetime DEFAULT NULL COMMENT '完成时间',
  `metadata` text DEFAULT NULL COMMENT '元数据(JSON格式)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `version_num` int(11) DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_plugin_id` (`plugin_id`),
  KEY `idx_state` (`state`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件灰度发布状态表'; 


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

