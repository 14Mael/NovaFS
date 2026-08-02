-- NovaFS 文件模块建表脚本（P3 文件核心 + P4 分片上传/分享）
-- 适用数据库: MySQL 8.0+，依赖 schema.sql 已执行（sys_user / sys_workspace / storage_settings）

-- ----------------------------
-- 1. 文件资源表
-- ----------------------------
DROP TABLE IF EXISTS `file_info`;
CREATE TABLE `file_info` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `workspace_id` bigint NOT NULL COMMENT '所属工作空间ID',
  `user_id` bigint NOT NULL COMMENT '上传者ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父文件夹ID，NULL为根',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `display_name` varchar(255) DEFAULT NULL COMMENT '显示名称',
  `suffix` varchar(32) DEFAULT NULL COMMENT '扩展名',
  `size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `mime_type` varchar(128) DEFAULT NULL COMMENT 'MIME类型',
  `is_dir` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否文件夹',
  `object_key` varchar(512) DEFAULT NULL COMMENT '存储平台对象键',
  `content_md5` varchar(64) DEFAULT NULL COMMENT '文件MD5（秒传校验）',
  `storage_platform_setting_id` bigint DEFAULT NULL COMMENT '存储平台配置ID',
  `upload_time` datetime DEFAULT NULL COMMENT '上传时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `deleted_time` datetime DEFAULT NULL COMMENT '删除时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workspace_query` (`workspace_id`, `user_id`, `is_deleted`, `parent_id`) USING BTREE,
  KEY `idx_md5` (`content_md5`, `size`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件资源表';

-- ----------------------------
-- 2. 传输任务表
-- ----------------------------
DROP TABLE IF EXISTS `file_transfer_task`;
CREATE TABLE `file_transfer_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` varchar(64) NOT NULL COMMENT 'UUID任务标识',
  `upload_id` varchar(255) DEFAULT NULL COMMENT '存储平台uploadId',
  `file_id` bigint DEFAULT NULL COMMENT '关联文件ID',
  `workspace_id` bigint NOT NULL COMMENT '工作空间ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `file_name` varchar(255) NOT NULL COMMENT '文件名',
  `file_size` bigint NOT NULL COMMENT '文件大小',
  `file_md5` varchar(64) DEFAULT NULL COMMENT '文件MD5',
  `total_chunks` int NOT NULL COMMENT '总分片数',
  `uploaded_chunks` int NOT NULL DEFAULT 0 COMMENT '已上传分片数',
  `chunk_size` bigint NOT NULL DEFAULT 5242880 COMMENT '分片大小',
  `uploaded_size` bigint NOT NULL DEFAULT 0 COMMENT '已上传字节数',
  `status` varchar(32) NOT NULL DEFAULT 'UPLOADING' COMMENT '状态: UPLOADING/COMPLETED/FAILED',
  `task_type` varchar(32) NOT NULL DEFAULT 'UPLOAD' COMMENT '类型: UPLOAD/DOWNLOAD',
  `storage_platform_setting_id` bigint DEFAULT NULL COMMENT '存储平台配置ID',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_task_id` (`task_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='传输任务表';

-- ----------------------------
-- 3. 分片上传明细表
-- ----------------------------
DROP TABLE IF EXISTS `file_chunk_info`;
CREATE TABLE `file_chunk_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `task_id` varchar(64) NOT NULL COMMENT '关联传输任务ID',
  `upload_id` varchar(255) NOT NULL COMMENT '上传ID',
  `chunk_number` int NOT NULL COMMENT '分片序号（从1开始）',
  `chunk_size` bigint NOT NULL COMMENT '分片大小',
  `chunk_md5` varchar(64) DEFAULT NULL COMMENT '分片MD5',
  `storage_path` varchar(512) NOT NULL COMMENT '分片在临时存储的路径',
  `e_tag` varchar(255) DEFAULT NULL COMMENT '存储平台返回的ETag',
  `uploaded_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_upload_chunk` (`upload_id`, `chunk_number`) USING BTREE,
  KEY `idx_task_id` (`task_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分片上传明细表';

-- ----------------------------
-- 4. 文件分享表
-- ----------------------------
DROP TABLE IF EXISTS `file_shares`;
CREATE TABLE `file_shares` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '分享人ID',
  `workspace_id` bigint NOT NULL COMMENT '工作空间ID',
  `file_id` bigint NOT NULL COMMENT '被分享文件ID',
  `share_code` varchar(10) NOT NULL COMMENT '分享码',
  `share_pwd` varchar(64) DEFAULT NULL COMMENT '提取密码（BCrypt加密，可为空）',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间，NULL为永久',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '查看次数',
  `max_view_count` int DEFAULT NULL COMMENT '查看次数上限',
  `download_count` int NOT NULL DEFAULT 0 COMMENT '下载次数',
  `max_download_count` int DEFAULT NULL COMMENT '下载次数上限',
  `scope` varchar(64) NOT NULL DEFAULT 'PREVIEW,DOWNLOAD' COMMENT '权限范围',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_share_code` (`share_code`) USING BTREE,
  KEY `idx_file_id` (`file_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='文件分享表';
