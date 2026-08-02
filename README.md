# NovaFS — Nova File System

企业级文件管理网盘系统,基于 **Spring Boot 3.4 + Java 21 + MyBatis-Flex**,支持多云存储、文件预览、分片上传、团队协作、**RAG 文档智能检索与问答**。

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4.x |
| ORM | MyBatis-Flex 1.11.x |
| 认证授权 | Sa-Token 1.45.x |
| 数据库 | MySQL 8.x |
| 缓存 | Redis + Caffeine(多级缓存) |
| 构建工具 | Maven 多模块 |
| 向量库 | Qdrant |
| 模型服务 | OpenAI 兼容 API(OpenAI / DeepSeek / 通义 / 智谱等) |

## 模块结构

```
nova-fs/
├── fs-admin/                 # Spring Boot 启动入口
├── fs-dependencies/          # BOM 统一依赖管理
├── fs-framework/             # 基础设施层
│   ├── fs-common-core/       # 公共核心(异常、返回结果、工具类)
│   ├── fs-orm/               # ORM 配置(基础实体、类型处理器)
│   ├── fs-redis/             # Redis 自动配置 + 通用仓储
│   ├── fs-security/          # Sa-Token 认证配置
│   └── ...
├── fs-modules/               # 业务模块层
│   ├── fs-system/            # 系统管理(用户、工作空间、存储配置)
│   └── fs-rag/               # RAG 检索增强生成(文档解析、向量化、语义检索、问答)
└── docs/                     # 设计文档
```

## 快速开始

```bash
# 1. 启动依赖(MySQL + Redis + Qdrant)
docker compose up -d

# 2. 初始化数据库
mysql -uroot -proot nova_fs < _sql/schema.sql
mysql -uroot -proot nova_fs < _sql/rag.sql

# 3. 配置模型服务 Key
export RAG_API_KEY=sk-xxxx

# 4. 启动应用
mvn spring-boot:run -pl fs-admin
# 访问 http://localhost:8080/api/health
```

## RAG 功能

- 上传 PDF / Word / Markdown / TXT 等文档,自动解析、切片、向量化;
- 按工作空间隔离的**语义检索**;
- 基于文档上下文的 **AI 问答**(返回引用来源);
- 详细设计见 [`docs/05-rag-design.md`](docs/05-rag-design.md)。

## 文档

- [`AGENTS.md`](AGENTS.md) — AI 开发助手行为指南
- [`ARCHITECTURE.md`](ARCHITECTURE.md) — 架构设计
- [`ROADMAP.md`](ROADMAP.md) — 分阶段开发路线图
- [`docs/`](docs/) — 各模块设计文档