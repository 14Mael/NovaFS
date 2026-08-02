# NovaFS — AI 开发助手行为指南

## 项目概述

NovaFS（Nova File System）是一个企业级文件管理网盘系统，基于 **Spring Boot 3.4.x + Java 21 + MyBatis-Flex**，支持多云存储、文件预览、分片上传、团队协作等功能。

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4.x |
| ORM | MyBatis-Flex 1.11.x |
| 认证授权 | Sa-Token 1.45.x |
| 数据库 | MySQL 8.x |
| 缓存 | Redis + Caffeine（多级缓存） |
| 构建工具 | Maven 多模块 |
| 对象转换 | MapStruct Plus |
| 连接池 | HikariCP |
| 接口文档 | SpringDoc OpenAPI |

## 模块结构

```
nova-fs/
├── fs-admin/                # Spring Boot 启动入口
├── fs-dependencies/         # BOM 统一依赖管理
├── fs-framework/            # 基础设施层
│   ├── fs-common-core/      # 公共核心（异常、返回结果、工具类）
│   ├── fs-orm/              # ORM 配置（基础实体、类型处理器）
│   ├── fs-redis/            # Redis 自动配置 + 通用仓储
│   ├── fs-security/         # Sa-Token 认证配置
│   ├── fs-sse/              # SSE 实时推送
│   ├── fs-notify/           # 邮件通知（事件驱动）
│   ├── fs-preview/          # 文件预览引擎（策略模式）
│   └── fs-swagger/          # 接口文档配置
├── fs-storage-plugin/       # 存储插件体系
│   ├── storage-plugin-core/ # SPI 核心接口 + 抽象基类
│   ├── storage-plugin-boot/ # Spring Boot 集成
│   ├── storage-plugin-local/ # 本地存储
│   ├── storage-plugin-minio/ # MinIO
│   ├── storage-plugin-aliyunoss/ # 阿里云 OSS
│   ├── storage-plugin-kodo/  # 七牛云 Kodo
│   ├── storage-plugin-obs/   # 华为云 OBS
│   └── storage-plugin-s3/    # S3 兼容
├── fs-modules/              # 业务模块层
│   ├── fs-system/            # 系统管理（用户、角色、工作空间）
│   ├── fs-storage/           # 存储配置管理
│   ├── fs-file/              # 文件核心业务
│   └── fs-log/               # 操作日志
```

## 关键设计原则

### 1. 编码规范
- 类名：`PascalCase`（如 `FileInfoServiceImpl`）
- 方法名：`camelCase`（如 `uploadFileChunk`）
- 常量：`UPPER_SNAKE_CASE`（如 `MAX_FILE_SIZE`）
- 枚举值：统一 `UPPER_CASE`（如 `UPLOADING`），不要用小写
- 包名：全小写单数（如 `io.novafs.file.service`）

### 2. 依赖注入
- ✅ 只使用**构造器注入**（`@RequiredArgsConstructor` + `private final`）
- ❌ 不要使用 `@Autowired` 字段注入

### 3. 异常处理
- 业务异常继承 `BaseException`（含 `code` + `message`）
- Service 层抛出业务异常，Controller 层不 catch
- `GlobalExceptionHandler` 统一处理 → `Result.fail()`
- catch 块中禁止吞没异常（仅 log.error 不 throw 视为吞没）

### 4. 分层职责
- **Controller**：参数校验 + 调用 Service，不包含业务逻辑
- **Service**：业务逻辑编排，事务管理
- **Mapper**：数据库操作，不含业务逻辑
- **Entity**：充血模型——携带与自身相关的行为方法

### 5. 代码规模
- 单个类不超过 **300 行**（超过则拆分）
- 单个方法不超过 **30 行**（超过则拆分子方法）
- 方法嵌套不超过 **3 层**（超过则通过卫语句或策略模式简化）

### 6. 领域模型
- Entity 使用 `Boolean` 而非 `Y`/`N` 字符串
- 状态字段使用枚举而非魔法数字
- Entity 包含业务判断方法（如 `isDirectory()`、`canBeDeletedBy(userId)`）

### 7. 包分层约定
每个业务模块内部按功能分包：

```
fs-file/
├── controller/        # 接收 HTTP 请求
├── service/           # 业务接口
│   └── impl/          # 业务实现
├── mapper/            # MyBatis-Flex Mapper
├── entity/            # 数据库实体（充血模型）
├── dto/               # 数据传输对象
├── vo/                # 视图对象（返回给前端）
├── event/             # 领域事件
└── enums/             # 模块专用枚举
```

## 开发节奏（分阶段）

1. **P0 — 基础设施搭建**：多模块骨架、通用返回结果、全局异常、ORM 配置、数据库
2. **P1 — 用户认证**：注册/登录/权限/工作空间
3. **P2 — 存储插件体系**：SPI 接口 + 本地存储实现
4. **P3 — 文件核心业务**：上传/下载/列表/删除/回收站
5. **P4 — 高级特性**：分片上传/断点续传/文件预览/分享
6. **P5 — 体验优化**：SSE 实时推送/国际化/通知
7. **P9 — RAG 检索增强**：文档解析、向量化、语义检索、文档问答(模块 fs-rag,详见 docs/05-rag-design.md)

## 测试要求
- 核心 Service 必须有单元测试（JUnit 5 + Mockito）
- 分片上传合并逻辑必须有测试覆盖
- 存储插件接口必须有 SPI 加载测试
- 使用 Testcontainers 测试 Redis/DB 交互

## 优化目标（相对于原项目）
- Service 类 ≤ 300 行（原项目有 1300+ 行的 God Class）
- 事件驱动解耦（原项目 Service 直接调 SSE 推送）
- 充血模型（原项目全是贫血模型）
- 无 `Y`/`N` 魔法字符串
- 测试覆盖率 > 60%（原项目 ≈ 0%）
