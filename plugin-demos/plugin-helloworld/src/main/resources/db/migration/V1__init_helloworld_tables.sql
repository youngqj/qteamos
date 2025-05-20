-- 创建插件消息表
CREATE TABLE IF NOT EXISTS `plugin_hw_message` (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content` varchar(255) NOT NULL COMMENT '消息内容',
  `user_id` bigint(0) DEFAULT NULL COMMENT '用户ID',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HelloWorld消息表';

-- 创建配置表
CREATE TABLE IF NOT EXISTS `plugin_hw_config` (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text COMMENT '配置值',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='HelloWorld配置表';

-- 初始化一些配置数据
INSERT INTO `plugin_helloworld_config` (`config_key`, `config_value`, `description`)
VALUES 
  ('default_greeting', 'Hello, World!', '默认问候语'),
  ('enable_debug', 'true', '是否启用调试模式'); 