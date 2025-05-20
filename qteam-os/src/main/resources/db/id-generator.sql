-- 分布式ID生成器相关表
-- 用于支持分段式ID池方案

-- 序列号资源表（用于管理各业务实体的ID段）
CREATE TABLE IF NOT EXISTS `sys_sequence` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `business_code` varchar(100) NOT NULL COMMENT '业务编码',
  `step` int NOT NULL DEFAULT 1000 COMMENT '步长',
  `max_value` bigint(20) NOT NULL DEFAULT 9223372036854775807 COMMENT '最大值',
  `description` varchar(255) COMMENT '描述',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_code` (`business_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='序列号资源表';

-- 序列号分配表（记录各业务已分配ID段）
CREATE TABLE IF NOT EXISTS `sys_sequence_alloc` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `business_code` varchar(100) NOT NULL COMMENT '业务编码',
  `node_id` varchar(64) NOT NULL COMMENT '节点标识',
  `max_id` bigint(20) NOT NULL COMMENT '当前最大ID值',
  `step` int NOT NULL COMMENT '步长',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_node` (`business_code`, `node_id`),
  KEY `idx_business_code` (`business_code`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='序列号分配表';

-- 雪花算法机器节点表（记录雪花算法分配的机器ID）
CREATE TABLE IF NOT EXISTS `sys_snowflake_node` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `data_center_id` int NOT NULL COMMENT '数据中心ID',
  `worker_id` int NOT NULL COMMENT '工作节点ID',
  `node_desc` varchar(255) COMMENT '节点描述',
  `node_ip` varchar(50) COMMENT '节点IP',
  `last_heartbeat` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后心跳时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_alive` tinyint(1) DEFAULT 1 COMMENT '是否存活',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_datacenter_worker` (`data_center_id`, `worker_id`),
  KEY `idx_is_alive` (`is_alive`),
  KEY `idx_last_heartbeat` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='雪花算法机器节点表';

-- 初始化序列号资源表数据
INSERT INTO `sys_sequence` (`id`, `business_code`, `step`, `max_value`, `description`, `create_by`, `create_time`) VALUES
(1, 'sys_user', 1000, 9223372036854775807, '用户ID序列', 'system', NOW()),
(2, 'sys_role', 1000, 9223372036854775807, '角色ID序列', 'system', NOW()),
(3, 'sys_menu', 1000, 9223372036854775807, '菜单ID序列', 'system', NOW()),
(4, 'sys_dept', 1000, 9223372036854775807, '部门ID序列', 'system', NOW()),
(5, 'sys_config', 1000, 9223372036854775807, '配置ID序列', 'system', NOW()),
(6, 'sys_dict', 1000, 9223372036854775807, '字典ID序列', 'system', NOW());

-- 初始化雪花算法节点（预留一个节点）
INSERT INTO `sys_snowflake_node` (`id`, `data_center_id`, `worker_id`, `node_desc`, `create_time`, `is_alive`) VALUES
(1, 1, 1, '默认节点', NOW(), 1); 