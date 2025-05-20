-- 应用基础系统相关表
-- 遵循新的命名规范，前缀使用sys_
-- 使用BIGINT类型主键，不使用自增，后续由雪花算法生成
-- 保留公共字段: create_by, create_time, update_by, update_time, remark, is_deleted, version

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `salt` varchar(20) COMMENT '加密盐',
  `real_name` varchar(50) COMMENT '真实姓名',
  `avatar` varchar(255) COMMENT '头像地址',
  `email` varchar(100) COMMENT '邮箱',
  `phone` varchar(20) COMMENT '手机号',
  `gender` char(1) DEFAULT '0' COMMENT '性别(0-未知，1-男，2-女)',
  `dept_id` bigint(20) COMMENT '部门ID',
  `status` char(1) DEFAULT '0' COMMENT '状态(0-正常，1-禁用)',
  `login_ip` varchar(50) COMMENT '最后登录IP',
  `login_time` datetime COMMENT '最后登录时间',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限标识',
  `role_sort` int DEFAULT 0 COMMENT '显示顺序',
  `status` char(1) DEFAULT '0' COMMENT '状态(0-正常，1-禁用)',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围(1-全部，2-自定义，3-本部门，4-本部门及以下，5-仅本人)',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`),
  KEY `idx_role_name` (`role_name`),
  KEY `idx_role_sort` (`role_sort`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父菜单ID',
  `menu_type` char(1) NOT NULL COMMENT '菜单类型(M-目录，C-菜单，F-按钮)',
  `menu_sort` int DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) COMMENT '路由地址',
  `component` varchar(255) COMMENT '组件路径',
  `query` varchar(255) COMMENT '路由参数',
  `is_external` tinyint(1) DEFAULT 0 COMMENT '是否外链(0-否，1-是)',
  `is_cache` tinyint(1) DEFAULT 0 COMMENT '是否缓存(0-否，1-是)',
  `is_visible` tinyint(1) DEFAULT 1 COMMENT '是否显示(0-否，1-是)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0-正常，1-禁用)',
  `permission` varchar(100) COMMENT '权限标识',
  `icon` varchar(100) COMMENT '菜单图标',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_menu_type` (`menu_type`),
  KEY `idx_menu_sort` (`menu_sort`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 部门表
CREATE TABLE IF NOT EXISTS `sys_dept` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dept_name` varchar(50) NOT NULL COMMENT '部门名称',
  `parent_id` bigint(20) DEFAULT 0 COMMENT '父部门ID',
  `ancestors` varchar(500) COMMENT '祖级列表',
  `dept_sort` int DEFAULT 0 COMMENT '显示顺序',
  `leader` varchar(50) COMMENT '负责人',
  `phone` varchar(20) COMMENT '联系电话',
  `email` varchar(100) COMMENT '邮箱',
  `status` char(1) DEFAULT '0' COMMENT '状态(0-正常，1-禁用)',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_dept_sort` (`dept_sort`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户和角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- 角色和菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';

-- 角色和部门关联表
CREATE TABLE IF NOT EXISTS `sys_role_dept` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `dept_id` bigint(20) NOT NULL COMMENT '部门ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_dept` (`role_id`, `dept_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和部门关联表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `config_name` varchar(100) NOT NULL COMMENT '配置名称',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` varchar(1000) NOT NULL COMMENT '配置值',
  `config_type` varchar(20) DEFAULT 'text' COMMENT '配置类型(text-文本，number-数字，boolean-布尔，json-JSON)',
  `is_system` tinyint(1) DEFAULT 0 COMMENT '是否系统配置(0-否，1-是)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0-正常，1-禁用)',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_name` (`config_name`),
  KEY `idx_config_type` (`config_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `title` varchar(100) NOT NULL COMMENT '操作模块',
  `business_type` int DEFAULT 0 COMMENT '业务类型(0-其它，1-新增，2-修改，3-删除...)',
  `method` varchar(200) COMMENT '方法名称',
  `request_method` varchar(10) COMMENT '请求方式',
  `operator_type` int DEFAULT 0 COMMENT '操作类别(0-其它，1-后台用户，2-手机端用户)',
  `operator_name` varchar(50) COMMENT '操作人员',
  `dept_name` varchar(50) COMMENT '部门名称',
  `request_url` varchar(255) COMMENT '请求URL',
  `request_ip` varchar(50) COMMENT '主机地址',
  `request_location` varchar(100) COMMENT '操作地点',
  `request_param` text COMMENT '请求参数',
  `response_result` text COMMENT '返回参数',
  `status` int DEFAULT 0 COMMENT '操作状态(0-正常，1-异常)',
  `error_message` text COMMENT '错误消息',
  `execution_time` bigint(20) DEFAULT 0 COMMENT '执行时长(毫秒)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business_type` (`business_type`),
  KEY `idx_operator_name` (`operator_name`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 系统登录日志表
CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `username` varchar(50) COMMENT '用户名',
  `login_ip` varchar(50) COMMENT '登录IP',
  `login_location` varchar(100) COMMENT '登录地点',
  `browser` varchar(100) COMMENT '浏览器类型',
  `os` varchar(100) COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态(0-成功，1-失败)',
  `message` varchar(255) COMMENT '提示消息',
  `login_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_login_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统登录日志表';

-- 数据字典类型表
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dict_name` varchar(100) NOT NULL COMMENT '字典名称',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `status` char(1) DEFAULT '0' COMMENT '状态(0-正常，1-禁用)',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`),
  KEY `idx_dict_name` (`dict_name`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型表';

-- 数据字典数据表
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `id` bigint(20) NOT NULL COMMENT '主键ID',
  `dict_type` varchar(100) NOT NULL COMMENT '字典类型',
  `dict_label` varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value` varchar(100) NOT NULL COMMENT '字典键值',
  `dict_sort` int DEFAULT 0 COMMENT '显示顺序',
  `css_class` varchar(100) COMMENT '样式属性',
  `list_class` varchar(100) COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认(Y-是，N-否)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0-正常，1-禁用)',
  `create_by` varchar(64) NOT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) COMMENT '更新者',
  `update_time` datetime ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  `is_deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除(0-未删除，1-已删除)',
  `version` int DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_dict_type` (`dict_type`),
  KEY `idx_dict_sort` (`dict_sort`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典数据表'; 