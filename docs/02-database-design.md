# 数据库设计文档

## 一、E-R 概览

```
┌───────────────┐     ┌───────────────────┐     ┌──────────────────┐
│   sys_user    │     │   sys_workspace   │     │   file_info      │
│───────────────│     │───────────────────│     │──────────────────│
│ id (PK)       │────→│ owner_id (FK)    │     │ id (PK)          │
│ username      │     │ id (PK)           │────→│ workspace_id (FK)│
│ password      │     │ name              │     │ user_id (FK)     │
│ email         │     │ slug (UNIQUE)     │     │ parent_id (自引用)│
│ nickname      │     │ description       │     │ original_name    │
│ avatar        │     │ member_count      │     │ object_key       │
│ status        │     └────────┬──────────┘     │ display_name     │
│ created_at    │              │                │ suffix           │
│ updated_at    │              │                │ size             │
│ last_login_at │              │                │ mime_type        │
└───────┬───────┘              │                │ is_dir (Boolean) │
        │                      │                │ content_md5      │
        │                      │                │ storage_path     │
        │                      │                │ upload_time      │
        │                      │                │ is_deleted       │
        │                      │                │ deleted_time     │
        ├──────────────────────┤                └──────────────────┘
        │                      │
        ▼                      ▼
┌───────────────────┐ ┌──────────────────────┐
│ sys_workspace_    │ │ sys_workspace_       │
│ member            │ │ invitation            │
│───────────────────│ │──────────────────────│
│ id (PK)           │ │ id (PK)               │
│ workspace_id (FK) │ │ workspace_id (FK)     │
│ user_id (FK)      │ │ email                 │
│ role_id (FK)      │ │ role_id               │
│ joined_at         │ │ invited_by (FK→user)  │
└────────┬──────────┘ │ token (UNIQUE)        │
         │            │ status (枚举)          │
         ▼            │ expires_at            │
┌───────────────────┐ │ created_at            │
│ sys_role          │ └──────────────────────┘
│───────────────────│
│ id (PK)           │     ┌──────────────────┐
│ workspace_id (FK) │     │ storage_platform  │
│ role_code(UNIQUE) │     │──────────────────│
│ role_name         │     │ id (PK)           │
│ role_type         │     │ name              │
│ created_at        │     │ identifier(UNIQUE)│
└────────┬──────────┘     │ config_scheme(JSON)│
         │                │ icon              │
         ▼                │ is_default        │
┌───────────────────┐     └────────┬─────────┘
│ sys_role_         │              │
│ permission        │              │
│───────────────────│              │
│ id (PK)           │              ▼
│ role_id (FK)      │     ┌──────────────────┐
│ permission_code   │     │ storage_settings  │
└───────────────────┘     │──────────────────│
                          │ id (PK)           │
┌──────────────────┐      │ platform_id (FK)  │
│ sys_permission   │      │ config_data (JSON)│
│──────────────────│      │ workspace_id (FK) │
│ id (PK)          │      │ enabled (Boolean) │
│ permission_code  │      │ remark            │
│ permission_name  │      └──────────────────┘
│ module           │
│ description      │     ┌──────────────────────┐
│ sort             │     │ file_transfer_task    │
└──────────────────┘     │──────────────────────│
                         │ id (PK/AUTO)          │
┌──────────────────┐      │ task_id (UNIQUE)       │
│ file_shares      │      │ upload_id             │
│──────────────────│      │ file_id (FK)          │
│ id (PK)          │      │ workspace_id (FK)     │
│ user_id (FK)     │      │ user_id (FK)          │
│ workspace_id (FK)│      │ file_name / file_size │
│ share_code       │      │ file_md5              │
│ expire_time      │      │ total_chunks          │
│ share_pwd        │      │ uploaded_chunks       │
│ view_count       │      │ chunk_size            │
│ scope            │      │ uploaded_size         │
│ create/update    │      │ status (enum)         │
└────────┬─────────┘      │ task_type             │
         │                │ start/complete_time   │
         ▼                └──────────────────────┘
┌──────────────────┐
│ file_share_items │
│──────────────────│
│ share_id (FK)    │
│ file_id (FK)     │
│ created_at       │
└──────────────────┘
```

## 二、核心表设计

### 2.1 sys_user — 用户表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(128) PK | 雪花算法生成 |
| username | VARCHAR(128) UNIQUE | 用户名 |
| password | VARCHAR(128) | BCrypt 加密 |
| email | VARCHAR(128) | 邮箱（用于登录和邀请） |
| nickname | VARCHAR(128) | 昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| status | INT | 0=正常 1=禁用 |
| created_at / updated_at / last_login_at | DATETIME | 时间戳 |

### 2.2 sys_workspace — 工作空间表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(128) PK | 雪花ID |
| name | VARCHAR(100) | 工作空间名称 |
| slug | VARCHAR(64) UNIQUE | URL友好的唯一标识 |
| description | VARCHAR(500) | 描述 |
| owner_id | VARCHAR(128) FK→user | 创建者 |
| member_count | INT | 冗余字段，列表展示用 |
| created_at / updated_at | DATETIME | 时间戳 |

### 2.3 file_info — 文件资源表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(128) PK | 雪花ID |
| object_key | VARCHAR(128) | 存储平台上的资源 key |
| original_name | VARCHAR(128) | 原始文件名 |
| display_name | VARCHAR(128) | 显示别名 |
| suffix | VARCHAR(20) | 扩展名 |
| size | BIGINT | 文件大小（字节） |
| mime_type | VARCHAR(128) | MIME 类型 |
| is_dir | TINYINT(1) | 是否文件夹 |
| parent_id | VARCHAR(128) FK→self | 父文件夹（NULL 为根） |
| workspace_id | VARCHAR(128) FK→workspace | 所属工作空间 |
| user_id | VARCHAR(128) FK→user | 上传者 |
| content_md5 | TEXT | 文件 MD5（秒传校验用） |
| storage_platform_setting_id | VARCHAR(128) FK→settings | 存储平台配置 ID |
| upload_time | DATETIME | 上传时间 |
| is_deleted | TINYINT(1) | 软删除标记 |
| deleted_time | DATETIME | 删除时间 |

### 2.4 file_transfer_task — 传输任务表

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT AUTO PK | 自增主键 |
| task_id | VARCHAR(64) UNIQUE | UUID 任务标识 |
| upload_id | VARCHAR(255) | 存储平台的 uploadId |
| file_id | VARCHAR(128) FK | 关联文件 ID |
| workspace_id | VARCHAR(128) FK | 工作空间 |
| user_id | VARCHAR(128) FK | 用户 |
| file_name / file_size / file_md5 | - | 文件元信息 |
| total_chunks | INT | 总分片数 |
| uploaded_chunks | INT | 已上传分片数 |
| chunk_size | BIGINT(默认 5MB) | 分片大小 |
| uploaded_size | BIGINT | 已上传字节数 |
| status | VARCHAR(20) | UPLOADING / DOWNLOADING / COMPLETED / FAILED |
| task_type | VARCHAR(32) | UPLOAD / DOWNLOAD |
| start_time / complete_time | DATETIME | 起止时间 |

### 2.5 sys_role / sys_permission / sys_role_permission — RBAC 权限表

**sys_role**：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | INT AUTO PK | 自增 |
| workspace_id | VARCHAR(128) FK | 所属工作空间 |
| role_code | VARCHAR(255) UNIQUE(ws+code) | 角色编码 |
| role_name | VARCHAR(255) | 角色名称 |
| role_type | TINYINT | 0=系统预设 1=自定义 |
| description | TEXT | 描述 |

**sys_permission**：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | INT AUTO PK | 自增 |
| permission_code | VARCHAR(128) UNIQUE | 权限编码，如 `file:read` |
| permission_name | VARCHAR(128) | 名称 |
| module | VARCHAR(64) | 所属模块 |
| sort | INT | 排序 |

预设权限：`file:read` `file:write` `file:share` `storage:manage` `member:manage`

预设角色：`admin`（全部权限）、`member`（读写+分享）、`viewer`（只读）

### 2.6 storage_platform / storage_settings — 存储平台

**storage_platform** — 存储平台定义（类型）：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | INT AUTO PK | 自增 |
| name | VARCHAR(255) | 名称 |
| identifier | VARCHAR(128) UNIQUE | 标识符（如 `local`、`minio`、`aliyunoss`） |
| config_scheme | JSON | 配置项的 JSON Schema 描述 |
| icon / link | VARCHAR(255) | 图标 / 链接 |
| is_default | TINYINT | 是否默认 |

**storage_settings** — 存储平台配置实例：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(128) PK | 雪花ID |
| platform_identifier | VARCHAR(128) FK | 关联平台标识 |
| config_data | JSON | 具体配置（endpoint、accessKey、secretKey、bucket 等） |
| enabled | TINYINT(1) | 启用状态 |
| workspace_id | VARCHAR(128) FK | 所属工作空间 |
| remark | VARCHAR(255) | 备注 |
| deleted | TINYINT(1) | 逻辑删除 |

### 2.7 file_shares / file_share_items — 文件分享

**file_shares**：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | VARCHAR(128) PK | 分享 ID |
| user_id / workspace_id | FK | 分享人 + 工作空间 |
| share_name | VARCHAR(255) | 分享名称 |
| share_code | VARCHAR(6) | 提取码 |
| expire_time | DATETIME | 过期时间 |
| share_pwd | VARCHAR(128) | 提取密码 |
| view_count / max_view_count | INT | 查看次数限制 |
| download_count / max_download_count | INT | 下载次数限制 |
| scope | VARCHAR(255) | 权限范围（preview,download） |

## 三、索引策略

```sql
-- file_info: 按工作空间查询文件列表
INDEX idx_workspace_query (workspace_id, user_id, is_deleted, parent_id)

-- file_transfer_task: 按用户查询历史
INDEX idx_user_id (user_id)
INDEX idx_status (status)
UNIQUE INDEX uk_task_id (task_id)

-- sys_workspace_member: 查询用户在哪些工作空间
UNIQUE INDEX uk_workspace_user (workspace_id, user_id)
INDEX idx_user_id (user_id)

-- sys_workspace_invitation: 按邮箱和状态查邀请
INDEX idx_email_status (email, status)
UNIQUE INDEX uk_token (token)
```

## 四、建表顺序

按依赖关系执行：

```
1. sys_user                  ← 无依赖
2. sys_workspace             ← 依赖 sys_user (owner_id)
3. sys_workspace_member      ← 依赖 sys_workspace + sys_user
4. sys_workspace_invitation  ← 依赖 sys_workspace + sys_user
5. sys_permission            ← 无依赖
6. sys_role                  ← 依赖 sys_workspace
7. sys_role_permission       ← 依赖 sys_role + sys_permission
8. storage_platform          ← 无依赖
9. storage_settings          ← 依赖 storage_platform + sys_workspace
10. file_info                ← 依赖 sys_user + sys_workspace + storage_settings
11. file_transfer_task       ← 依赖 file_info + sys_user + sys_workspace
12. file_shares              ← 依赖 sys_user + sys_workspace
13. file_share_items         ← 依赖 file_shares + file_info
14. sys_login_log            ← 无依赖
15. sys_user_transfer_setting ← 依赖 sys_user
```

## 五、优化建议（对比原项目）

| 原项目问题 | 改进方案 |
|---|---|
| `is_dir` 用 `TINYINT` + `Y`/`N` 字符串 | 用 `TINYINT(1)` + Entity 中用 `Boolean` |
| `status` 用魔法数字 0/1/2/3 | 用 `TINYINT` + Java 枚举映射 |
| 用户 ID 用 `VARCHAR(128)` 雪花 ID | 同上，但考虑用 `BIGINT` 自增/雪花 |
| 无外键约束（SET FOREIGN_KEY_CHECKS = 0） | 添加外键索引保证引用完整性 |
| 分片上传无分片明细表 | 新增 `file_chunk_info` 表记录每个分片的 MD5 和上传状态 |
