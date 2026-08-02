# NovaFS — AI 开发助手行为指南

## Project

NovaFS（Nova File System）——企业级文件管理网盘系统：多云存储插件、分片上传/断点续传、文件预览、团队工作空间 + RBAC、SSE 实时推送、邮件通知、国际化，以及基于 Qdrant 的 RAG 文档检索问答。

- 技术栈：Java 21 · Spring Boot 3.4.x · MyBatis-Flex 1.11.x · Sa-Token 1.45.x · MySQL 8 · Redis + Caffeine · Maven 多模块
- 启动入口：`fs-admin/src/main/java/io/novafs/NovaApplication.java`（组件扫描根包 `io.novafs`）
- 数据库脚本：`_sql/schema.sql`（系统/RBAC）、`_sql/fs-file.sql`（文件/分片/分享）、`_sql/rag.sql`（RAG）

## Commands

```bash
mvn -q compile                                     # 全量编译
mvn -q install -DskipTests                        # 安装内部模块到本地仓库（IDE 依赖解析失败时先执行）
mvn -q test -pl fs-modules/fs-file -am -DskipTests=false     # 跑单个模块测试（根 pom 默认 skipTests=true，必须显式覆盖）
mvn spring-boot:run -pl fs-admin                   # 启动应用（需先 docker compose up -d 起 MySQL/Redis/Qdrant）
```

- 现有测试：fs-file 12 个（分片合并/上传幂等/分享）、fs-rag 17 个、storage-plugin-local 10 个，共 39 个。
- `fs-admin` 的 `NovaApplicationTests` 是 `@SpringBootTest`，需要 MySQL/Redis，默认跳过。

## Architecture

```
nova-fs/
├── fs-admin/             # 启动入口 + HealthController + SpaController + i18n 资源 + 国际化/通知配置
├── fs-dependencies/      # BOM 统一依赖管理
├── fs-framework/         # 基础设施层
│   ├── fs-common-core/   # Result/PageQuery、BaseException/ErrorCode/GlobalExceptionHandler、JsonUtils/SpringUtils/MessageUtils
│   ├── fs-orm/           # BaseEntity（雪花 ID + 时间自动填充）、MyBatis-Flex 配置、JsonStringTypeHandler
│   ├── fs-redis/         # RedisTemplate 配置 + RedisRepository
│   ├── fs-security/      # Sa-Token + JWT 配置（拦截 /api/**，白名单 /api/health、/api/auth/**、/api/share/**）
│   ├── fs-sse/           # SSE 连接管理 + Redis Pub/Sub 多实例广播（SseConnectionManager/SsePublisher）
│   └── fs-notify/        # 邮件通知（EmailNotifyService，novafs.notify.enabled 开关）
├── fs-storage-plugin/    # 存储插件体系
│   ├── storage-plugin-core/   # SPI 接口 IStorageOperationService + AbstractStorageOperationService + model/exception（纯 Java）
│   ├── storage-plugin-boot/   # @StoragePlugin 注解 + StoragePluginRegistry（SPI+Bean 双通道）+ StorageServiceFacade
│   └── storage-plugin-local/  # LocalStorageOperationService（上传/分片/MD5 校验/路径穿越防护）+ META-INF/services 注册
├── fs-modules/           # 业务模块层
│   ├── fs-system/        # 用户/认证/工作空间/RBAC（StpInterfaceImpl 查库）/存储配置实体/登录日志/SseController
│   ├── fs-file/          # 文件核心+高级特性：秒传/分片上传断点续传/基础 CRUD/回收站/分享/预览（策略模式）/SSE 监听器
│   └── fs-rag/           # 文档解析→切片→向量化→Qdrant 检索→问答（自研 RestClient，零框架绑定）
└── _sql/                 # schema.sql / fs-file.sql / rag.sql
```

依赖方向（单向）：`fs-admin` → `fs-modules` → `fs-framework` / `fs-storage-plugin`；`fs-file` 依赖 `fs-system`（工作空间/存储配置/用户）+ `storage-plugin-boot` + `fs-sse` + `fs-notify`；框架层不得依赖业务模块。

## Conventions

- 类名 `PascalCase`、方法 `camelCase`、常量 `UPPER_SNAKE_CASE`、枚举值 `UPPER_CASE`、包名全小写单数。
- 依赖注入只用构造器注入（`@RequiredArgsConstructor` + `private final`），禁用 `@Autowired` 字段注入。
- 业务异常继承 `BaseException`（code + message），Service 抛、Controller 不 catch、`GlobalExceptionHandler` 统一转 `Result.fail()`；catch 块禁止吞异常。
- 分层：Controller 薄（校验+调用）、Service 编排（@Transactional）、Mapper 无业务、Entity 充血模型（业务判断方法，如 `isDirectory()`/`canBeDeletedBy(userId)`）。
- 状态字段用枚举（`UserStatus`/`TransferTaskStatus` 等，Entity 存 `Integer` + 枚举 `.getCode()` 比较），Boolean 而非 Y/N。
- 单类 ≤300 行、单方法 ≤30 行、嵌套 ≤3 层。
- 当前用户从 Sa-Token 取：`StpUtil.getLoginIdAsString()`（username）→ `SysUserService.findByUsername()` 拿 userId。
- 路径安全：存储层做 `normalize()` + 前缀检查防穿越；分片/文件 MD5 双端校验。
- 文件事件（`ChunkUploadProgressEvent`/`FileUploadCompleteEvent`）经 Spring Events 解耦，SSE/邮件监听器在 fs-file 的 `listener` 包。

## Notes

- 待办收尾：存储配置（StorageSettings）管理 Controller、Office 预览（LibreOffice 转 PDF）、`target/` 与 `.idea/` 已跟踪文件清理（`git rm -r --cached -- '*target*'`）、fs-swagger 文档模块。
- 进度：P0–P5 + P9（RAG）已全部落地；P2 的 S3 兼容基类（AbstractS3CompatibleStorageService）留待 MinIO 插件时引入（依赖 AWS SDK）。
- 分片上传接口对 `workspaceId` 未做成员校验（文件归属按 userId 校验），后续可挂 `NOT_WORKSPACE_MEMBER`。
