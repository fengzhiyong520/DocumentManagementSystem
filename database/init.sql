-- 文件资料管理系统数据库初始化脚本

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `email` VARCHAR(100) COMMENT '邮箱',
  `phone` VARCHAR(20) COMMENT '手机号',
  `avatar` VARCHAR(255) COMMENT '头像',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `role_id` BIGINT COMMENT '角色ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) COMMENT '描述',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `permission_code` VARCHAR(50) NOT NULL COMMENT '权限编码',
  `permission_name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `resource` VARCHAR(255) COMMENT '资源路径',
  `method` VARCHAR(10) COMMENT '请求方法：GET, POST, PUT, DELETE',
  `description` VARCHAR(255) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- 菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `menu_code` VARCHAR(50) NOT NULL COMMENT '菜单编码（唯一标识）',
  `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `menu_type` VARCHAR(20) DEFAULT 'menu' COMMENT '菜单类型：menu-菜单 button-按钮',
  `path` VARCHAR(255) COMMENT '路由路径',
  `component` VARCHAR(255) COMMENT '组件路径',
  `icon` VARCHAR(100) COMMENT '图标',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID，0表示顶级菜单',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `description` VARCHAR(255) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 资料表
CREATE TABLE IF NOT EXISTS `file_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` VARCHAR(255) NOT NULL COMMENT '标题',
  `description` TEXT COMMENT '描述',
  `category` VARCHAR(50) COMMENT '分类',
  `file_type` VARCHAR(20) COMMENT '文件类型：image, text, file',
  `file_url` VARCHAR(500) COMMENT '文件URL',
  `file_name` VARCHAR(255) COMMENT '文件名',
  `file_size` BIGINT COMMENT '文件大小（字节）',
  `is_public` INT DEFAULT 0 COMMENT '是否公开：0-私有 1-公开',
  `upload_user_id` BIGINT COMMENT '上传用户ID',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_upload_user_id` (`upload_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料表';

-- 借阅表
CREATE TABLE IF NOT EXISTS `file_borrow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `document_id` BIGINT NOT NULL COMMENT '资料ID',
  `user_id` BIGINT NOT NULL COMMENT '借阅用户ID',
  `borrow_time` DATETIME COMMENT '借阅时间',
  `return_time` DATETIME COMMENT '归还时间',
  `expected_return_time` DATETIME COMMENT '预计归还时间',
  `status` INT DEFAULT 0 COMMENT '状态：0-待审批 1-已批准 2-已拒绝 3-已归还 4-已逾期',
  `reason` VARCHAR(500) COMMENT '借阅原因',
  `approver_id` BIGINT COMMENT '审批人ID',
  `process_definition_id` BIGINT COMMENT '流程定义ID',
  `process_instance_id` VARCHAR(64) COMMENT '流程实例ID',
  `task_id` VARCHAR(64) COMMENT '当前任务ID',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅表';

-- Flowable 工作流相关表

-- 流程定义表
CREATE TABLE IF NOT EXISTS `process_definition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '流程名称',
  `key` VARCHAR(50) NOT NULL COMMENT '流程Key（唯一标识）',
  `description` VARCHAR(500) COMMENT '流程描述',
  `status` INT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `category` VARCHAR(50) COMMENT '流程分类',
  `deployment_id` VARCHAR(64) COMMENT 'Flowable部署ID',
  `flowable_definition_id` VARCHAR(64) COMMENT 'Flowable流程定义ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义表';

-- 流程节点配置表
CREATE TABLE IF NOT EXISTS `process_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `process_definition_id` BIGINT NOT NULL COMMENT '流程定义ID',
  `node_id` VARCHAR(50) NOT NULL COMMENT '节点ID（流程中的节点标识）',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `node_type` VARCHAR(50) NOT NULL COMMENT '节点类型：startEvent, userTask, endEvent等',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `assignee` VARCHAR(500) COMMENT '审批人（用户ID，多个用逗号分隔）',
  `candidate_users` VARCHAR(500) COMMENT '候选审批人（用户ID，多个用逗号分隔）',
  `candidate_groups` VARCHAR(500) COMMENT '候选审批组（角色ID，多个用逗号分隔）',
  `form_key` VARCHAR(100) COMMENT '表单Key',
  `description` VARCHAR(500) COMMENT '节点描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '删除标志：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_process_definition_id` (`process_definition_id`),
  KEY `idx_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程节点配置表';

-- 插入初始数据
INSERT INTO `sys_role` (`role_code`, `role_name`, `description`) VALUES
('admin', '管理员', '系统管理员'),
('user', '普通用户', '普通用户');

-- 插入默认管理员用户（密码：admin123）
-- 注意：如果登录失败，请使用以下方法之一重置密码：
-- 1. 调用 API: POST /api/auth/reset-admin-password?newPassword=admin123
-- 2. 或者直接执行 SQL 更新（需要先运行 PasswordHashGenerator 生成新的哈希值）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `email`, `status`, `role_id`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8pJwC', '管理员', 'admin@example.com', 1, 1);

-- 插入菜单初始数据
-- 先插入所有顶级菜单
INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `parent_id`, `sort_order`, `status`, `description`) VALUES
('dashboard', '首页统计', 'menu', 'dashboard', 'views/Dashboard.vue', 'HomeOutline', 0, 0, 1, '首页统计'),
('role_config', '角色配置', 'menu', NULL, NULL, 'ShieldCheckmarkOutline', 0, 1, 1, '角色配置菜单组'),
('user_manage', '用户管理', 'menu', 'user', 'views/user/UserList.vue', 'PeopleOutline', 0, 2, 1, '用户管理'),
('document_manage', '资料管理', 'menu', 'document', 'views/document/DocumentList.vue', 'DocumentTextOutline', 0, 3, 1, '资料管理'),
('borrow_manage', '审批管理', 'menu', 'borrow', 'views/borrow/BorrowList.vue', 'LibraryOutline', 0, 4, 1, '审批管理'),
('borrow_history', '审批记录', 'menu', 'borrow-history', 'views/borrow/BorrowHistoryList.vue', 'CheckmarkCircleOutline', 0, 5, 1, '审批记录'),
('hall_manage', '大厅管理', 'menu', 'hall', 'views/hall/Hall.vue', 'StorefrontOutline', 0, 6, 1, '大厅管理'),
('process_config', '流程配置', 'menu', 'process-definition', 'views/process/ProcessDefinitionList.vue', 'SettingsOutline', 0, 7, 1, '流程配置');

-- 插入子菜单（使用子查询获取父菜单ID）
INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `parent_id`, `sort_order`, `status`, `description`) 
SELECT 
  'role_manage', '角色管理', 'menu', 'role', 'views/user/RoleList.vue', 'ShieldCheckmarkOutline', id, 1, 1, '角色管理'
FROM sys_menu WHERE menu_code = 'role_config' LIMIT 1;

INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `parent_id`, `sort_order`, `status`, `description`) 
SELECT 
  'permission_manage', '权限管理', 'menu', 'permission', 'views/user/PermissionList.vue', 'KeyOutline', id, 2, 1, '权限管理'
FROM sys_menu WHERE menu_code = 'role_config' LIMIT 1;

INSERT INTO `sys_menu` (`menu_code`, `menu_name`, `menu_type`, `path`, `component`, `icon`, `parent_id`, `sort_order`, `status`, `description`) 
SELECT 
  'menu_manage', '菜单管理', 'menu', 'menu', 'views/user/MenuList.vue', 'SettingsOutline', id, 3, 1, '菜单管理'
FROM sys_menu WHERE menu_code = 'role_config' LIMIT 1;

-- 为 admin 角色配置所有菜单（假设 admin 角色的 id 是 1）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) 
SELECT 1, id FROM sys_menu WHERE deleted = 0;

