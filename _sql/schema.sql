-- NovaFS 数据库建表脚本
-- 适用数据库: MySQL 8.0+
-- 执行前请先创建数据库: CREATE DATABASE IF NOT EXISTS nova_fs DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` varchar(128) NOT NULL COMMENT '邮箱',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1禁用',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_username` (`username`) USING BTREE,
  UNIQUE KEY `uk_email` (`email`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- ----------------------------
-- 2. 工作空间表
-- ----------------------------
DROP TABLE IF EXISTS `sys_workspace`;
CREATE TABLE `sys_workspace` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `name` varchar(100) NOT NULL COMMENT '工作空间名称',
  `slug` varchar(64) NOT NULL COMMENT 'URL友好的唯一标识',
  `description` varchar(500) DEFAULT NULL COMMENT '工作空间描述',
  `owner_id` bigint NOT NULL COMMENT '创建者/拥有者用户ID',
  `member_count` int NOT NULL DEFAULT 1 COMMENT '成员数量',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_slug` (`slug`) USING BTREE,
  KEY `idx_owner_id` (`owner_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作空间表';

-- ----------------------------
-- 3. 工作空间成员表
-- ----------------------------
DROP TABLE IF EXISTS `sys_workspace_member`;
CREATE TABLE `sys_workspace_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workspace_id` bigint NOT NULL COMMENT '工作空间ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  `joined_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_workspace_user` (`workspace_id`, `user_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作空间成员表';

-- ----------------------------
-- 4. 工作空间邀请表
-- ----------------------------
DROP TABLE IF EXISTS `sys_workspace_invitation`;
CREATE TABLE `sys_workspace_invitation` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `workspace_id` bigint NOT NULL COMMENT '工作空间ID',
  `email` varchar(128) NOT NULL COMMENT '被邀请人邮箱',
  `role_id` int NOT NULL COMMENT '分配的角色ID',
  `invited_by` bigint NOT NULL COMMENT '邀请人用户ID',
  `token` varchar(64) NOT NULL COMMENT '邀请令牌',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0待接受 1已接受 2已过期 3已取消',
  `expires_at` datetime NOT NULL COMMENT '邀请过期时间',
  `accepted_at` datetime DEFAULT NULL COMMENT '接受时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_token` (`token`) USING BTREE,
  KEY `idx_workspace_id` (`workspace_id`) USING BTREE,
  KEY `idx_email_status` (`email`, `status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工作空间邀请表';

-- ----------------------------
-- 5. 权限表
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `permission_code` varchar(128) NOT NULL COMMENT '权限编码，如 file:upload',
  `permission_name` varchar(128) NOT NULL COMMENT '权限名称',
  `module` varchar(64) NOT NULL COMMENT '所属模块',
  `description` varchar(255) DEFAULT NULL COMMENT '权限描述',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_permission_code` (`permission_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='权限表';

-- ----------------------------
-- 6. 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `workspace_id` bigint NOT NULL COMMENT '所属工作空间ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(64) NOT NULL COMMENT '角色名称',
  `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
  `role_type` tinyint NOT NULL DEFAULT 1 COMMENT '类型: 0系统预设 1自定义',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_workspace_role_code` (`workspace_id`, `role_code`) USING BTREE,
  KEY `idx_workspace_id` (`workspace_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- ----------------------------
-- 7. 角色权限关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` int NOT NULL COMMENT '角色ID',
  `permission_code` varchar(128) NOT NULL COMMENT '权限编码',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色权限关联表';

-- ----------------------------
-- 8. 存储平台定义表
-- ----------------------------
DROP TABLE IF EXISTS `storage_platform`;
CREATE TABLE `storage_platform` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL COMMENT '存储平台名称',
  `identifier` varchar(128) NOT NULL COMMENT '存储平台标识符',
  `config_scheme` json NOT NULL COMMENT '配置项JSON Schema',
  `icon` varchar(128) DEFAULT NULL COMMENT '图标',
  `link` varchar(255) DEFAULT NULL COMMENT '平台链接',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_identifier` (`identifier`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='存储平台定义表';

-- ----------------------------
-- 9. 存储平台配置表
-- ----------------------------
DROP TABLE IF EXISTS `storage_settings`;
CREATE TABLE `storage_settings` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `platform_identifier` varchar(128) NOT NULL COMMENT '存储平台标识符',
  `config_data` json NOT NULL COMMENT '存储配置JSON',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用 0否 1是',
  `workspace_id` bigint NOT NULL COMMENT '所属工作空间ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workspace_id` (`workspace_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='存储平台配置表';

-- ----------------------------
-- 10. 登录日志表
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `login_ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `login_address` varchar(255) DEFAULT NULL COMMENT '登录地址',
  `browser` varchar(255) DEFAULT NULL COMMENT '浏览器',
  `os` varchar(255) DEFAULT NULL COMMENT '操作系统',
  `login_type` varchar(32) DEFAULT NULL COMMENT '登录方式',
  `status` tinyint NOT NULL COMMENT '状态: 0成功 1失败',
  `msg` varchar(255) DEFAULT NULL COMMENT '提示消息',
  `login_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_login_time` (`login_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- ----------------------------
-- 初始化数据：预设权限
-- ----------------------------
INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `module`, `sort`) VALUES
('file:read', '文件读取', '文件管理', 1),
('file:write', '文件编辑', '文件管理', 2),
('file:share', '文件分享', '文件管理', 3),
('storage:manage', '存储管理', '存储管理', 4),
('member:manage', '成员管理', '系统管理', 5);

-- ----------------------------
-- 初始化数据：预设存储平台
-- ----------------------------
INSERT INTO `storage_platform` (`identifier`, `name`, `config_scheme`, `is_default`) VALUES
('local', '本地存储', '[]', 1),
('minio', 'MinIO', '[]', 0),
('aliyunoss', '阿里云 OSS', '[]', 0),
('kodo', '七牛云 Kodo', '[]', 0),
('obs', '华为云 OBS', '[]', 0);
