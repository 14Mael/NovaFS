-- NovaFS RAG 模块建表脚本
-- 适用数据库: MySQL 8.0+
-- 执行前请先创建数据库: CREATE DATABASE IF NOT EXISTS nova_fs DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;

-- ----------------------------
-- 1. RAG 文档表
-- ----------------------------
DROP TABLE IF EXISTS `rag_document`;
CREATE TABLE `rag_document` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `workspace_id` bigint NOT NULL COMMENT '所属工作空间ID',
  `user_id` bigint NOT NULL COMMENT '上传用户ID',
  `name` varchar(255) NOT NULL COMMENT '文档名称',
  `content_type` varchar(128) NOT NULL COMMENT 'MIME类型',
  `size` bigint NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0解析中 1已索引 2失败',
  `chunk_count` int NOT NULL DEFAULT 0 COMMENT '切片数量',
  `error_msg` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_workspace_id` (`workspace_id`) USING BTREE,
  KEY `idx_user_id` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='RAG 文档表';

-- ----------------------------
-- 2. RAG 文档切片表
-- ----------------------------
DROP TABLE IF EXISTS `rag_chunk`;
CREATE TABLE `rag_chunk` (
  `id` bigint NOT NULL COMMENT '雪花ID',
  `document_id` bigint NOT NULL COMMENT '所属文档ID',
  `chunk_index` int NOT NULL COMMENT '切片序号',
  `content` text NOT NULL COMMENT '切片内容',
  `token_count` int NOT NULL DEFAULT 0 COMMENT '预估token数',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_document_id` (`document_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='RAG 文档切片表';