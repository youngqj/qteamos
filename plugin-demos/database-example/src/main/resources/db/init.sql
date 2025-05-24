-- 插件用户表
CREATE TABLE IF NOT EXISTS plugin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '用户名',
    email VARCHAR(200) NOT NULL COMMENT '邮箱',
    department VARCHAR(100) COMMENT '部门',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '删除标记',
    INDEX idx_name (name),
    INDEX idx_department (department)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='插件用户表';

-- 插入测试数据
INSERT IGNORE INTO plugin_users (name, email, department) VALUES
('张三', 'zhangsan@example.com', '技术部'),
('李四', 'lisi@example.com', '产品部'),
('王五', 'wangwu@example.com', '技术部'),
('赵六', 'zhaoliu@example.com', '市场部'); 