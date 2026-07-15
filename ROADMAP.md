# NovaFS 分阶段开发路线图

从零到完整实现的逐步开发计划，每个阶段产出可运行的、可部署的代码。

---

## P0：基础设施搭建（预计 2-3 天）

**目标**：搭建 Maven 多模块骨架，跑通"Hello World"级别的请求链路。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 0.1 | 创建根 POM，声明模块和统一依赖 | `pom.xml` |
| 0.2 | 创建 `fs-dependencies` BOM 模块 | `fs-dependencies/pom.xml`（管理所有三方库版本） |
| 0.3 | 创建 `fs-admin` 启动模块 | Spring Boot 启动类 + `application.yml` |
| 0.4 | 创建 `fs-common-core` | `Result<T>`、`PageQuery`/`PageResult`、`GlobalExceptionHandler`、`CommonConstant` |
| 0.5 | 创建 `fs-orm` | `BaseEntity`（id、createTime、updateTime）、MyBatis-Flex 配置、JSON TypeHandler |
| 0.6 | 创建 `fs-swagger` | SpringDoc OpenAPI 配置 |
| 0.7 | 创建测试用 Controller + Service | 验证：`GET /api/health` 返回 `{"code":200,"msg":"ok"}` |

**验证标准**：
- [x] `mvn clean install` 编译通过
- [x] `java -jar fs-admin/target/*.jar` 启动成功
- [x] 访问 `http://localhost:8080/api/health` 返回正常
- [x] 访问 `http://localhost:8080/swagger-ui.html` 显示接口文档

---

## P1：用户认证系统（预计 3-4 天）

**目标**：实现完整的注册、登录、权限控制。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 1.1 | 创建 `fs-security`（Sa-Token 集成） | `SaTokenAutoConfigure`、`StpInterfaceImpl` |
| 1.2 | 创建数据库表：`sys_user`、`sys_role`、`sys_user_role`、`sys_role_permission` | SQL 建表脚本 |
| 1.3 | 创建 `fs-system` 模块 | 模块 POM + 分包结构 |
| 1.4 | 实现用户注册 | `POST /api/auth/register`（密码 BCrypt 加密） |
| 1.5 | 实现密码登录 | `POST /api/auth/login` → 返回 Token |
| 1.6 | 实现邮箱验证码登录 | 验证码发送 + 校验 + 登录 |
| 1.7 | 实现 RBAC 权限控制 | 注解 `@SaCheckPermission` / `@SaCheckRole` |
| 1.8 | 创建 `fs-notify`（邮件通知） | 邮件发送 + 模板引擎（验证码邮件） |
| 1.9 | 创建 `fs-redis` | Redis 配置 + `RedisRepository` |

**验证标准**：
- [x] 注册 → 登录 → 获取 Token → 携带 Token 访问受保护接口 → 正常
- [x] 无 Token 访问 → 401
- [x] 权限不足 → 403
- [x] 邮箱验证码登录全流程跑通

---

## P2：工作空间与团队协作（预计 2-3 天）

**目标**：实现多工作空间隔离，团队协作基础。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 2.1 | 创建数据库表：`sys_workspace`、`sys_workspace_member` | SQL 建表脚本 |
| 2.2 | 实现工作空间 CRUD | `POST/GET/PUT/DELETE /api/workspace` |
| 2.3 | 实现成员邀请（邮件邀请链接） | 邀请码生成 + 邮件发送 + 验证加入 |
| 2.4 | 实现成员管理 | 列表/移除/修改角色 |
| 2.5 | 实现工作空间上下文过滤器 | 每次请求解析当前工作空间 ID → `WorkspaceContext` |
| 2.6 | 工作空间级权限校验 | 用户只能操作自己工作空间内的数据 |

**验证标准**：
- [x] 创建两个工作空间，分别邀请用户 → 用户 A 看不到 B 工作空间的文件
- [x] 邀请邮件发送 → 点击链接 → 验证加入成功
- [x] 移除成员后 → 该成员无法访问该工作空间

---

## P3：存储插件体系（预计 3-4 天）

**目标**：搭建可插拔的存储后端，用本地存储跑通文件上传。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 3.1 | 创建 `storage-plugin-core` | `IStorageOperationService` 接口 + `AbstractStorageOperationService` |
| 3.2 | 创建 `storage-plugin-boot` | `StoragePluginManager`（SPI 加载）、`StoragePluginRegistry` |
| 3.3 | 创建 `storage-plugin-local` | 本地文件系统实现 |
| 3.4 | 创建数据库表：`storage_setting`、`storage_platform` | SQL 建表脚本 |
| 3.5 | 创建 `fs-storage` 业务模块 | `StorageServiceFacade`（统一门面） |
| 3.6 | 存储平台配置管理 API | `POST/GET/PUT/DELETE /api/storage/platform` |
| 3.7 | 实现文件上传（普通上传，非分片） | `POST /api/file/upload` → 验证存储插件调用链路 |
| 3.8 | 实现文件下载 | `GET /api/file/download/{id}` |

**验证标准**：
- [x] 在后台配置本地存储 → 上传文件 → 文件出现在本地磁盘
- [x] 上传后下载 → 文件完整性校验（MD5）通过
- [x] 切换存储平台 → 新文件上传到新平台
- [x] SPI 加载：新增插件只需加入依赖 + 实现接口

---

## P4：文件核心业务（预计 4-5 天）

**目标**：文件 CRUD、文件夹管理、回收站。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 4.1 | 创建 `fs-file` 模块 | 模块 POM + 分包结构 |
| 4.2 | 创建数据库表：`file_info`、`file_recycle_bin` | SQL 建表脚本 |
| 4.3 | 实现文件/文件夹 CRUD | 创建文件夹/重命名/移动/删除 |
| 4.4 | 实现文件列表（分页 + 目录树） | `GET /api/file/list?parentId=x&page=1&size=20` |
| 4.5 | 实现回收站 | 还原/彻底删除/清空/自动清理 |
| 4.6 | 实现文件分享 | 分享链接 + 提取码（6位） + 过期时间 |
| 4.7 | 实现文件搜索 | 按文件名模糊搜索 |
| 4.8 | 文件列表返回缩略图（图片） | 集成 Thumbnailator |

**验证标准**：
- [x] 创建文件夹 → 进入文件夹 → 上传文件 → 删除 → 回收站可见 → 还原 → 文件恢复
- [x] 分享文件 → 其他用户用提取码访问
- [x] 搜索：能按文件名匹配到文件

---

## P5：分片上传与断点续传（预计 3-4 天）

**目标**：支持 TB 级大文件分片上传，断点续传，秒传。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 5.1 | 创建数据库表：`file_transfer_task`、`file_chunk_info` | SQL 建表脚本 |
| 5.2 | 实现秒传校验 | `POST /api/file/check-md5` → 比对 MD5 |
| 5.3 | 实现分片上传初始化 | `POST /api/file/chunk/init` → 返回 uploadId |
| 5.4 | 实现分片上传 | `POST /api/file/chunk/upload`（含分片校验） |
| 5.5 | 实现分片合并 | `POST /api/file/chunk/merge` → 调用存储插件合并 |
| 5.6 | 实现断点续传（查询已上传分片列表） | `GET /api/file/chunk/list?uploadId=x` |
| 5.7 | 拆分 `FileTransferTaskService` | 按职责拆为 3-4 个小类（`UploadService`、`ChunkService`、`MergeService`） |

**验证标准**：
- [x] 上传 1GB 文件 → 分片成功 → 合并成功 → MD5 一致
- [x] 上传中断 → 重试 → 续传成功（跳过已上传分片）
- [x] 秒传：上传相同文件 → 瞬间完成（秒传成功）
- [x] 各小类 ≤ 300 行

---

## P6：文件预览引擎（预计 3-4 天）

**目标**：支持 10+ 种文件格式在线预览。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 6.1 | 创建 `fs-preview` 基础模块 | `PreviewStrategy` 接口 + `PreviewStrategyManager` |
| 6.2 | 实现图片预览 | `ImagePreviewStrategy`（缩略图 + 原图） |
| 6.3 | 实现视频/音频预览 | `VideoPreviewStrategy` / `AudioPreviewStrategy`（流式） |
| 6.4 | 实现 PDF 预览 | `PdfPreviewStrategy`（带防盗链签名） |
| 6.5 | 实现 Office 预览 | `OfficePreviewStrategy`（LibreOffice 转 PDF） |
| 6.6 | 实现代码/Markdown 预览 | `CodePreviewStrategy`（语法高亮） |
| 6.7 | 实现压缩包预览 | `ArchivePreviewStrategy`（列出文件树） |
| 6.8 | 实现文本文件预览 | `TextPreviewStrategy` |

**验证标准**：
- [x] 上传 .docx → 预览 → 浏览器显示文档内容（已转 PDF）
- [x] 上传 .mp4 → 浏览器播放视频
- [x] 上传 .zip → 显示压缩包内文件列表
- [x] 新增格式只需实现 `PreviewStrategy` 接口 + 注册 Spring Bean

---

## P7：实时推送与体验优化（预计 2-3 天）

**目标**：SSE 实时进度推送、国际化、部署配置。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 7.1 | 创建 `fs-sse` 模块 | `SseConnectionManager` + SSE 端点 |
| 7.2 | 上传进度实时推送 | 每上传一个分片推送一次进度 |
| 7.3 | Redis Pub/Sub 集群支持 | 多实例间 SSE 事件广播 |
| 7.4 | 国际化支持 | i18n 资源文件 + 中英双语 |
| 7.5 | Docker 部署 | Dockerfile + docker-compose.yml |
| 7.6 | 配置文件模板 | `application-dev.yml`、`application-prod.yml` |

**验证标准**：
- [x] 上传文件时前端实时看到进度百分比
- [x] 切换浏览器语言 → 接口返回对应语言的错误信息
- [x] `docker-compose up` → 系统正常运行

---

## P8：测试与质量保障（持续）

**目标**：保障代码质量，搭建 CI 流水线。

### 任务清单

| # | 任务 | 产出 |
|---|------|------|
| 8.1 | 核心 Service 单元测试 | JUnit 5 + Mockito |
| 8.2 | 存储插件 SPI 加载测试 | 验证 SPI 机制正确加载 |
| 8.3 | 分片上传集成测试 | Testcontainers（MySQL + Redis） |
| 8.4 | 性能测试 | 10 并发上传 100MB 文件 |
| 8.5 | CI 配置 | GitHub Actions（编译 + 测试 + 构建镜像） |

---

## 总计预估工时

| 阶段 | 天数 | 累计 |
|------|------|------|
| P0 基础设施 | 2-3 天 | 第 1-3 天 |
| P1 用户认证 | 3-4 天 | 第 4-7 天 |
| P2 工作空间 | 2-3 天 | 第 8-10 天 |
| P3 存储插件 | 3-4 天 | 第 11-14 天 |
| P4 文件核心 | 4-5 天 | 第 15-19 天 |
| P5 分片上传 | 3-4 天 | 第 20-23 天 |
| P6 文件预览 | 3-4 天 | 第 24-27 天 |
| P7 实时推送 | 2-3 天 | 第 28-30 天 |
| **合计** | **22-30 天** | 约 **1 个月** |
