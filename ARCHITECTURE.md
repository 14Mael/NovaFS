# NovaFS 架构设计文档

## 一、系统架构总览

```
┌─────────────────────────────────────────────────────────────────────┐
│                        客户端层 (Client)                             │
│            Web 浏览器 / Mobile App / API 调用                        │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTP / WebSocket
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    接入层 (Gateway / Load Balancer)                   │
│                         Nginx / Spring Cloud Gateway                 │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                   表现层 (Presentation)                               │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                  fs-admin (Boot 入口)                         │   │
│  │  Spring MVC 控制器 / 全局异常处理 / CORS / 参数校验            │   │
│  └──────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                   业务层 (Business)                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ fs-system│ │  fs-file │ │fs-storage│ │  fs-log  │              │
│  │ 用户/权限 │ │  文件核心 │ │ 存储配置  │ │ 操作日志  │              │
│  │ 工作空间  │ │    业务   │ │   管理    │ │          │              │
│  └─────┬────┘ └────┬─────┘ └─────┬────┘ └────┬─────┘              │
│        │            │             │            │                    │
│        └────────────┴─────────────┴────────────┘                    │
│                         │ 依赖基础设施                               │
├─────────────────────────┼───────────────────────────────────────────┤
│                    基础设施层 (Infrastructure)                       │
│  ┌──────────┐ ┌────────┐ ┌──────────┐ ┌────────────┐              │
│  │fs-common │ │fs-orm  │ │ fs-redis │ │fs-security │              │
│  │  -core   │ │        │ │          │ │            │              │
│  ├──────────┤ ├────────┤ ├──────────┤ ├────────────┤              │
│  │fs-sse    │ │fs-notif│ │fs-preview│ │fs-swagger  │              │
│  │          │ │y       │ │          │ │            │              │
│  └──────────┘ └────────┘ └──────────┘ └────────────┘              │
├─────────────────────────────────────────────────────────────────────┤
│                    存储插件层 (Storage Plugin)                        │
│  ┌──────────────┐ ┌────────────────┐ ┌──────────────────┐         │
│  │ plugin-core  │ │  plugin-boot   │ │ plugin-local     │         │
│  │ SPI 接口定义  │ │  Spring 集成   │ │ 本地存储实现      │         │
│  ├──────────────┤ ├────────────────┤ ├──────────────────┤         │
│  │ plugin-minio │ │plugin-aliyunoss│ │ plugin-kodo      │         │
│  │ MinIO 实现   │ │ 阿里云 OSS     │ │ 七牛云 Kodo      │         │
│  ├──────────────┤ ├────────────────┤ ├──────────────────┤         │
│  │ plugin-obs   │ │  plugin-s3    │ │ plugin-rustfs    │         │
│  │ 华为云 OBS   │ │  S3 兼容      │ │ RustFS 实现      │         │
│  └──────────────┘ └────────────────┘ └──────────────────┘         │
├─────────────────────────────────────────────────────────────────────┤
│                   数据层 (Data Store)                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐      │
│  │  MySQL   │ │PostgreSQL│ │  Redis   │ │ Object Storage   │      │
│  │ 关系数据  │ │ 关系数据  │ │ 缓存/SSE │ │ 文件数据          │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────────┘      │
└─────────────────────────────────────────────────────────────────────┘
```

> 模块说明: 图中 fs-file / fs-storage / fs-log 等模块为规划目标;当前已落地 **fs-system**(用户/认证/工作空间)与 **fs-rag**(语义检索/文档问答,详见 docs/05-rag-design.md)。

## 二、关键数据流

### 2.1 文件上传流程

```
用户选择文件
    │
    ▼
[前端] 计算文件 MD5 → 发起秒传校验
    │
    ├── 已存在 → 返回"秒传成功"（无需上传）
    │
    └── 不存在 → 继续
         │
         ▼
    [前端] 检查文件大小
         │
         ├── < 阈值(5MB) → [POST /api/file/upload] 直接上传
         │
         └── ≥ 阈值 → 分片上传
              │
              ▼
         [前端] 发送初始化请求 → [后端] 创建上传任务 → 返回 uploadId
              │
              ▼
         [前端] 逐片上传 (POST /api/file/chunk)
              │
              ▼
         [后端] FileTransferTaskServiceImpl
              ├── 1. 校验分片
              ├── 2. 写入临时存储
              ├── 3. 通过 SSE 推送进度
              └── 4. 检查是否所有分片完成
                   │
                   ▼
              [前端] 发起合并请求 (POST /api/file/merge)
                   │
                   ▼
              [后端] StorageServiceFacade.mergeFile()
                   ├── 获取存储平台实例 (StoragePluginManager)
                   ├── 调用 IStorageOperationService.mergeFile()
                   │   ├── plugin-local: 本地文件合并
                   │   ├── plugin-minio: MinIO 分片合并
                   │   └── plugin-oss: OSS 分片合并
                   ├── 计算最终文件 MD5（二次校验）
                   ├── 写入 FileInfo 记录到数据库
                   └── 发送 FileUploadCompleteEvent
                        ├── SSE 推送"上传完成"
                        └── 写操作日志
```

### 2.2 文件预览流程

```
用户请求预览文件
    │
    ▼
FilePreviewController.getPreviewInfo(fileId)
    │
    ▼
PreviewService.getPreviewUrl(fileId)
    │
    ▼
PreviewStrategyManager.matchStrategy(fileInfo)
    │
    ├── support(FileTypeEnum.IMAGE)      → ImagePreviewStrategy
    │       → 直接返回图片 URL
    │
    ├── support(FileTypeEnum.VIDEO)      → VideoPreviewStrategy
    │       → 返回视频流地址 + 转码信息
    │
    ├── support(FileTypeEnum.OFFICE)     → OfficePreviewStrategy
    │       → OfficeToPdfConverter (LibreOffice)
    │       → 返回 PDF 预览 URL
    │
    ├── support(FileTypeEnum.CODE)       → CodePreviewStrategy
    │       → 读取文本内容 → 语法高亮 → 返回 HTML
    │
    ├── support(FileTypeEnum.ARCHIVE)    → ArchivePreviewStrategy
    │       → 解压到临时目录 → 列出文件树
    │
    └── support(FileTypeEnum.PDF)        → PdfPreviewStrategy
            → 返回 PDF URL + 防盗链签名
```

### 2.3 认证授权流程

```
登录请求 (POST /api/auth/login)
    │
    ▼
AuthController.login(LoginForm)
    │
    ▼
LoginStrategyFactory.getStrategy(form.getType())
    │
    ├── PasswordLoginStrategy
    │   ├── 校验用户名/密码 (BCrypt)
    │   └── 查询用户角色权限
    │
    └── EmailLoginStrategy
        ├── 发送验证码 → 校验验证码
        └── 登录/注册
    │
    ▼
Sa-Token (StpInterfaceImpl)
    ├── 获取用户权限列表
    ├── 获取用户角色列表
    └── 生成 JWT Token
    │
    ▼
返回 Token + 用户信息
    │
    ▼
后续请求 → SaTokenFilter 拦截
    ├── 校验 Token 有效性
    ├── 校验 URL 权限
    └── 放行/拒绝
```

## 三、核心领域模型

### 3.1 存储插件 SPI

```java
// 核心接口
public interface IStorageOperationService {
    // 文件操作
    String uploadFile(StoragePlatform platform, FileUploadRequest request);
    InputStream downloadFile(StoragePlatform platform, String filePath);
    void deleteFile(StoragePlatform platform, String filePath);
    void renameFile(StoragePlatform platform, String oldPath, String newPath);
    List<FileItem> listFiles(StoragePlatform platform, String prefix);

    // 分片操作
    String initChunkUpload(StoragePlatform platform, ChunkInitRequest request);
    void uploadChunk(StoragePlatform platform, ChunkUploadRequest request);
    String mergeChunks(StoragePlatform platform, ChunkMergeRequest request);
}

// 抽象基类
public abstract class AbstractStorageOperationService
        implements IStorageOperationService {
    // 公共逻辑：路径拼接、校验、日志
}

// S3 兼容基类
public abstract class AbstractS3CompatibleStorageService
        extends AbstractStorageOperationService {
    // S3 SDK 公共逻辑
}
```

### 3.2 文件预览策略

```java
public interface PreviewStrategy {
    boolean support(FileTypeEnum fileType);
    PreviewResult preview(FileInfo fileInfo, PreviewRequest request);
    int getOrder(); // 优先级排序
}

// 注册方式：Spring @Component + List 注入
@Component
public class PreviewStrategyManager {
    private final List<PreviewStrategy> strategies;

    public PreviewStrategy matchStrategy(FileTypeEnum fileType) {
        return strategies.stream()
            .filter(s -> s.support(fileType))
            .min(Comparator.comparingInt(PreviewStrategy::getOrder))
            .orElseThrow(() -> new UnsupportedPreviewException(fileType));
    }
}
```

### 3.3 事件驱动模型

```java
// 1. 定义事件
@Getter
public class FileUploadCompleteEvent extends ApplicationEvent {
    private final Long fileId;
    private final Long userId;
    private final Long workspaceId;
    private final Long fileSize;
}

// 2. 发布事件（Service 中）
eventPublisher.publishEvent(new FileUploadCompleteEvent(fileId, userId, workspaceId, fileSize));

// 3. 监听处理（分离的关注点）
@Component
public class SsePushListener {
    @EventListener
    public void onUploadComplete(FileUploadCompleteEvent event) {
        // 推送进度到前端
    }
}

@Component
public class LogRecordListener {
    @EventListener
    public void onUploadComplete(FileUploadCompleteEvent event) {
        // 记录操作日志
    }
}

@Component
public class QuotaUpdateListener {
    @EventListener
    public void onUploadComplete(FileUploadCompleteEvent event) {
        // 更新用户存储配额
    }
}
```

## 四、数据库核心表设计

```
┌─────────────────┐    ┌─────────────────────┐    ┌─────────────────┐
│   sys_user       │    │    sys_role          │    │ sys_workspace   │
│─────────────────│    │─────────────────────│    │─────────────────│
│ id (PK)          │───→│ id (PK)              │    │ id (PK)          │
│ username         │    │ name                 │←───│ name             │
│ email            │    │ code (ROLE_ADMIN)    │    │ slug (UNIQUE)    │
│ password (bcrypt)│    │ description          │    │ owner_id (FK)    │
│ avatar           │    └──────────┬──────────┘    │ member_count     │
│ status (NORMAL)  │               │               └────────┬─────────┘
└────────┬─────────┘               │                        │
         │                         │                        │
         ▼                         ▼                        ▼
┌──────────────────────┐ ┌──────────────────┐ ┌──────────────────────┐
│ sys_user_role         │ │ sys_role_perm    │ │ sys_workspace_member │
│──────────────────────│ │──────────────────│ │──────────────────────│
│ user_id               │ │ role_id          │ │ workspace_id         │
│ role_id               │ │ permission       │ │ user_id              │
└──────────────────────┘ │ resource_type    │ │ role_id              │
                         │ resource_id      │ │ invited_by           │
                         └──────────────────┘ │ status               │
                                               └──────────────────────┘

┌──────────────────┐    ┌──────────────────────┐   ┌──────────────────┐
│ file_info         │    │ file_transfer_task   │   │ file_share       │
│──────────────────│    │──────────────────────│   │──────────────────│
│ id (PK)           │    │ id (PK)              │   │ id (PK)          │
│ filename          │    │ file_name            │   │ file_id          │
│ file_path (存储路径)│    │ file_size            │   │ share_code (6位)  │
│ file_size         │    │ total_size           │   │ expire_time      │
│ file_md5          │    │ uploaded_size (已上传)│   │ view_count       │
│ storage_type      │    │ chunk_count          │   │ share_pwd (提取码)│
│ storage_path      │    │ completed_chunks     │   │ created_by       │
│ parent_id (自引用) │    │ status               │   └──────────────────┘
│ is_dir (Boolean)  │    │ type (UPLOAD/DOWNLOAD)│
│ is_deleted         │    │ sse_session_id       │
│ user_id           │    │ storage_config_id    │
│ workspace_id     │    │ created_by           │
│ thumbnail         │    └──────────────────────┘
│ file_type (枚举)   │
└──────────────────┘
```

**RAG 模块表**(_sql/rag.sql):

| 表 | 说明 | 关键字段 |
|---|---|---|
| rag_document | RAG 文档元数据 | workspace_id(隔离维度)、user_id、name、content_type、status(0解析中/1已索引/2失败)、chunk_count |
| rag_chunk | 文档切片 | document_id、chunk_index、content、token_count |

> 向量本体存储在 Qdrant 集合 
ovafs_docs,payload 含 workspaceId / documentId / chunkIndex / text,按 workspaceId 过滤实现数据隔离。

## 五、关键技术决策

| 决策 | 选项 | 选择理由 |
|---|---|---|
| ORM | MyBatis-Flex | 比 MyBatis-Plus 更轻量，编译期生成 QueryWrapper，无性能损耗 |
| 认证 | Sa-Token + JWT | 内置 Redis 支持、踢人下线、同端互斥等功能，比 Spring Security 更简洁 |
| 缓存 | Caffeine（本地） + Redis（分布式） | 二级缓存：热点数据查本地，一致数据查 Redis |
| 存储插件 | Java SPI + Spring 自动装配 | 标准的可插拔机制，新增存储无需改核心代码 |
| 事件机制 | Spring Events | 无需引入消息队列，同进程内解耦足够用（后续可切到 MQ） |
| 文件预览 | 策略模式 | 每种格式独立策略，新增格式只需加一个实现类 |
| SSE | 基于 Redis Pub/Sub | 多实例部署时，一个实例的推送可广播到所有实例 |
| RAG 检索 | Qdrant + OpenAI 兼容 API + 自研客户端 | 自研 RestClient 只依赖 embeddings/chat 两个端点,零框架绑定;Qdrant 按 workspaceId payload 隔离 |

## 六、安全设计

1. **密码存储**：BCrypt（非 SHA-256）
2. **文件访问**：预览 URL 带时效签名，防止盗链
3. **路径穿越**：所有文件路径做 `normalize()` + 前缀检查
4. **分片校验**：每个分片校验 MD5 + 最终合并后校验全文件 MD5
5. **权限控制**：RBAC + 工作空间隔离，文件归属到 workspace + user
6. **敏感操作**：删除/修改需要二次确认（Token 验证）
