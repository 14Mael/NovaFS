# RAG 检索增强生成模块设计

> 模块: fs-rag | 定位: 文件语义检索 + 文档问答
> 版本: 1.0.0-SNAPSHOT | 技术栈: Spring Boot 3.4.4 + MySQL 8 + Qdrant + OpenAI 兼容 API

---

## 一、概述

RAG(Retrieval-Augmented Generation,检索增强生成)模块为 NovaFS 提供**基于文档的语义检索与智能问答**能力:

- **语义检索**: 上传文档(PDF / Word / Markdown / TXT 等)后自动解析、切片、向量化,支持按"含义"而非仅关键词检索文档内容;
- **文档问答**: 针对工作空间内的文档提问,模型基于检索到的上下文片段作答,并返回引用来源。

与文件系统的关系: 模块独立于 `fs-file`,提供自己的文档入库 API;后续 `fs-file` 上传完成后可通过领域事件自动触发索引,实现"文件上传 → 自动可被检索问答"。

## 二、技术选型

| 组件 | 选型 | 理由 |
|---|---|---|
| 模型调用 | 自研客户端(Spring `RestClient`) | 仅依赖 OpenAI 兼容协议的两个端点(`/embeddings`、`/chat/completions`),不引入 Spring AI / LangChain4j,避免框架版本与 Boot 绑定风险,贴合项目轻量自研风格 |
| 向量存储 | Qdrant(REST API 自封装) | 独立向量库,Docker 单容器部署;支持按 payload 过滤(工作空间/文档级隔离);百万级向量规模 |
| 文档解析 | Apache Tika 2.9 | 一个引擎覆盖 pdf / docx / xlsx / pptx / md / txt / html / rtf 等格式 |
| 文本切片 | 自研递归字符切片器 | 按 段落 → 换行 → 句号 → 空格 → 硬切 的优先级切分,支持相邻块重叠 |
| 数据库 | MySQL 8(复用项目主库) | 文档元数据与切片内容落库,便于管理与审计;向量本体在 Qdrant |

> 说明: 向量库选择 Qdrant 而非 pgvector,是因为项目技术栈确定为 MySQL(MySQL 无 pgvector 扩展)。若后续切换 PostgreSQL,可将 `QdrantClient` 替换为 pgvector 实现,业务层无感知。

## 三、架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                         Controller 层                        │
│   RagDocumentController(入库/列表/删除/切片)                  │
│   RagChatController(语义检索/问答)                            │
└──────────────────────────────┬──────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────┐
│                         Service 层                           │
│   RagDocumentService   入库编排: 解析→切片→向量化→落库        │
│   RagSearchService     语义检索: 向量化→Qdrant 检索→组装      │
│   RagChatService       问答: 检索→拼 Prompt→模型生成          │
└───────────────┬──────────────────────┬──────────────────────┘
                │                      │
┌───────────────▼───────────┐  ┌───────▼──────────────────────┐
│     基础设施层(自研)        │  │     基础设施层(三方)          │
│  EmbeddingClient          │  │  Apache Tika(文档解析)        │
│  ChatClient               │  │  MyBatis-Flex(MySQL)         │
│  QdrantClient             │  │  Sa-Token(登录态)             │
│  TextSplitter             │  │                              │
│  ParserManager + 解析策略  │  │                              │
└───────────────┬───────────┘  └───────────────┬──────────────┘
                │                              │
        OpenAI 兼容 API                MySQL / Qdrant
   (OpenAI / DeepSeek / 通义 / 智谱…)   (Docker 部署)
```

**设计原则**(对齐项目 AGENTS.md):

- **策略模式**: `DocumentParser` 接口 + `TextParser` / `TikaParser` 按序匹配分发,新增格式零侵入;
- **充血模型**: `RagDocument` 自带 `isIndexed()` / `markIndexed()` / `markFailed()` 行为;
- **构造器注入**: 全部使用 `@RequiredArgsConstructor` + `private final`;
- **单类 ≤ 300 行、单方法 ≤ 30 行**: 每个基础设施类职责单一;
- **错误不吞没**: 外部调用失败统一包装为 `BaseException`(错误码 6001~6005),由 `GlobalExceptionHandler` 统一返回。

## 四、模块结构

```
fs-modules/fs-rag/
├── pom.xml
└── src/main/java/io/novafs/rag/
    ├── controller/          # HTTP 接口
    │   ├── RagDocumentController.java
    │   └── RagChatController.java
    ├── service/             # 业务接口
    │   ├── RagDocumentService.java
    │   ├── RagSearchService.java
    │   └── RagChatService.java
    │   └── impl/            # 业务实现
    ├── mapper/              # MyBatis-Flex Mapper
    │   ├── RagDocumentMapper.java
    │   └── RagChunkMapper.java
    ├── entity/              # 数据库实体(充血模型)
    │   ├── RagDocument.java
    │   └── RagChunk.java
    ├── dto/                 # 请求参数
    │   ├── IngestTextRequest.java
    │   ├── SearchRequest.java
    │   └── ChatRequest.java
    ├── vo/                  # 返回视图
    │   ├── DocumentVO.java
    │   ├── ChunkVO.java
    │   ├── SearchResultVO.java
    │   └── ChatResponseVO.java
    ├── client/              # 外部服务客户端(自研)
    │   ├── EmbeddingClient.java   # OpenAI 兼容 /embeddings
    │   ├── ChatClient.java        # OpenAI 兼容 /chat/completions
    │   └── QdrantClient.java      # Qdrant REST 封装
    ├── parser/              # 文档解析策略
    │   ├── DocumentParser.java
    │   ├── TextParser.java
    │   ├── TikaParser.java
    │   └── ParserManager.java
    ├── splitter/            # 文本切片
    │   └── TextSplitter.java
    ├── enums/               # 枚举
    │   └── RagDocumentStatus.java
    └── config/              # 配置
        ├── RagProperties.java
        └── RagAutoConfiguration.java
```

## 五、数据模型

### 5.1 MySQL 表(`_sql/rag.sql`)

**rag_document — RAG 文档表**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 雪花 ID(继承 BaseEntity) |
| workspace_id | bigint | 所属工作空间(数据隔离维度) |
| user_id | bigint | 上传用户 |
| name | varchar(255) | 文档名称 |
| content_type | varchar(128) | MIME 类型 |
| size | bigint | 文件大小(字节) |
| status | tinyint | 0 解析中 / 1 已索引 / 2 失败 |
| chunk_count | int | 切片数量 |
| error_msg | varchar(500) | 失败原因 |
| created_at / updated_at | datetime | 审计字段 |

**rag_chunk — RAG 切片表**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 雪花 ID |
| document_id | bigint | 所属文档 |
| chunk_index | int | 切片序号(从 0 开始) |
| content | text | 切片文本 |
| token_count | int | 预估 token 数(字符数/4) |
| created_at | datetime | 创建时间 |

### 5.2 Qdrant 向量点结构

- **集合**: `novafs_docs`(可配置),向量距离 **Cosine**;
- **点 ID**: `UUID.nameUUIDFromBytes(documentId + ":" + chunkIndex)`,确定性生成,重复入库幂等覆盖;
- **Payload**(过滤与展示用):

| key | 类型 | 说明 |
|---|---|---|
| workspaceId | long | 工作空间(检索强制过滤) |
| documentId | long | 文档 ID |
| documentName | string | 文档名(检索结果展示,免联表) |
| chunkIndex | int | 切片序号 |
| text | string | 切片内容(作为 LLM 上下文) |

### 5.3 错误码(ErrorCode 6001+)

| code | 常量 | 含义 |
|---|---|---|
| 6001 | RAG_EMBEDDING_FAILED | 向量化失败 |
| 6002 | RAG_CHAT_FAILED | 对话生成失败 |
| 6003 | RAG_DOCUMENT_NOT_FOUND | RAG 文档不存在 |
| 6004 | RAG_DOCUMENT_PARSE_FAILED | 文档解析失败 |
| 6005 | RAG_QDRANT_UNAVAILABLE | 向量库不可用 |

## 六、核心流程

### 6.1 文档入库(文件 / 文本)

```
① 校验登录态 → ② 落库 rag_document(status=PARSING)
③ ParserManager 按扩展名/MIME 选择解析器 → 提取纯文本
④ TextSplitter 递归切片(段落→换行→句号→空格→硬切,支持 overlap)
⑤ EmbeddingClient 批量向量化(chunks → vectors)
⑥ QdrantClient.ensureCollection() + upsert(点 ID 幂等)
⑦ 落库 rag_chunk × N + rag_document 更新(status=INDEXED, chunk_count)
失败路径: 捕获异常 → 补偿删除 Qdrant 向量 → rag_document 标记 FAILED → 抛出 BaseException(事务回滚)
```

### 6.2 语义检索

```
① 校验登录态 → ② EmbeddingClient.embed(query)
③ QdrantClient.search(vector, topK, filter=[workspaceId])
④ 按 min-score 阈值过滤 → 组装 SearchResultVO(含文档名与原文片段)
```

### 6.3 文档问答

```
① 语义检索 Top-K 片段 → ② 拼装 system prompt(注入文档内容 + 来源标注)
③ ChatClient.chat(system, question) → ④ 返回 answer + sources 引用
```

### 6.4 删除文档

```
① 校验文档存在 → ② 删除 rag_chunk + rag_document(事务)
③ QdrantClient.deleteByFilter(documentId) 清理向量
```

## 七、API 参考

> 鉴权: 所有接口需携带 `Authorization: <token>`(Sa-Token 全局拦截 `/api/**`)
> 统一返回: `Result<T>` → `{"code":200,"msg":"ok","data":...}`

### 7.1 文件入库

```
POST /api/rag/documents
Content-Type: multipart/form-data
参数: workspaceId (query) + file (form-data 文件)
```

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "id": 1902358064344670209,
    "workspaceId": 1,
    "userId": 100,
    "name": "产品手册.pdf",
    "contentType": "application/pdf",
    "size": 2048576,
    "status": 1,
    "chunkCount": 12,
    "errorMsg": null,
    "createdAt": "2026-08-01T15:30:00",
    "updatedAt": "2026-08-01T15:30:01"
  }
}
```

### 7.2 文本直入

```
POST /api/rag/documents/text
Content-Type: application/json
```

```json
{
  "workspaceId": 1,
  "name": "需求文档.md",
  "content": "第一版需求:……"
}
```

### 7.3 文档列表(分页)

```
GET /api/rag/documents?workspaceId=1&page=1&pageSize=20
```

### 7.4 删除文档

```
DELETE /api/rag/documents/{id}
```

### 7.5 切片列表

```
GET /api/rag/documents/{id}/chunks
```

### 7.6 语义检索

```
POST /api/rag/search
Content-Type: application/json
```

请求:

```json
{ "workspaceId": 1, "query": "产品的计费方式是什么?", "topK": 5 }
```

响应:

```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    {
      "pointId": "3f9a…-uuid",
      "documentId": 1902358064344670209,
      "documentName": "产品手册.pdf",
      "score": 0.8732,
      "content": "计费方式分为按量付费与包年包月两种……"
    }
  ]
}
```

### 7.7 文档问答

```
POST /api/rag/chat
Content-Type: application/json
```

请求:

```json
{ "workspaceId": 1, "question": "计费方式有哪几种?" }
```

响应:

```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "answer": "根据《产品手册.pdf》,计费方式分为按量付费与包年包月两种:……",
    "sources": [
      {
        "pointId": "3f9a…",
        "documentId": 1902358064344670209,
        "documentName": "产品手册.pdf",
        "score": 0.8732,
        "content": "计费方式分为按量付费与包年包月两种……"
      }
    ]
  }
}
```

## 八、配置说明(`application.yml`)

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `novafs.rag.enabled` | `true` | 模块总开关 |
| `novafs.rag.embedding.base-url` | `https://api.openai.com/v1` | Embedding 服务地址(OpenAI 兼容) |
| `novafs.rag.embedding.api-key` | `${RAG_API_KEY:}` | API Key(环境变量注入) |
| `novafs.rag.embedding.model` | `text-embedding-3-small` | Embedding 模型(1536 维) |
| `novafs.rag.embedding.dimensions` | `1536` | 向量维度,需与模型一致 |
| `novafs.rag.chat.base-url` | `https://api.openai.com/v1` | Chat 服务地址 |
| `novafs.rag.chat.api-key` | `${RAG_API_KEY:}` | API Key |
| `novafs.rag.chat.model` | `gpt-4o-mini` | 对话模型 |
| `novafs.rag.chat.temperature` | `0.3` | 采样温度(低=更严谨) |
| `novafs.rag.chat.max-tokens` | `1024` | 最大输出 token |
| `novafs.rag.qdrant.base-url` | `http://localhost:6333` | Qdrant REST 地址 |
| `novafs.rag.qdrant.api-key` | `${QDRANT_API_KEY:}` | Qdrant 鉴权 Key(可选) |
| `novafs.rag.qdrant.collection` | `novafs_docs` | 集合名 |
| `novafs.rag.qdrant.vector-size` | `1536` | 集合向量维度 |
| `novafs.rag.splitter.chunk-size` | `800` | 切片字符数 |
| `novafs.rag.splitter.chunk-overlap` | `100` | 相邻切片重叠 |
| `novafs.rag.search.top-k` | `5` | 默认返回片段数 |
| `novafs.rag.search.min-score` | `0.0` | 最低相似度阈值 |

**切换模型服务示例**(例如 DeepSeek):

```yaml
novafs:
  rag:
    embedding:
      base-url: https://api.deepseek.com/v1
      model: text-embedding-3-small   # 按服务商支持的模型填写
    chat:
      base-url: https://api.deepseek.com/v1
      model: deepseek-chat
```

## 九、部署指南

### 9.1 启动依赖(docker-compose)

项目根目录 `docker-compose.yml` 已包含三个服务:

```bash
docker compose up -d          # 启动 mysql + redis + qdrant
docker compose ps             # 确认 qdrant 健康(端口 6333/6334)
```

### 9.2 初始化数据库

```bash
mysql -uroot -proot nova_fs < _sql/schema.sql   # 已有业务表
mysql -uroot -proot nova_fs < _sql/rag.sql      # RAG 表
```

### 9.3 配置环境变量

```bash
export RAG_API_KEY=sk-xxxx            # Embedding 与 Chat 共用的 OpenAI 兼容 Key
# 可选
export QDRANT_API_KEY=xxx             # Qdrant 开启鉴权时
```

### 9.4 启动应用

```bash
mvn spring-boot:run -pl fs-admin
# 验证: curl http://localhost:8080/api/health
```

## 十、权限与数据隔离

- **登录态**: Sa-Token 拦截 `/api/**`,所有 RAG 接口必须携带 Token;
- **数据隔离**: 向量检索强制携带 `workspaceId` 过滤条件(Qdrant payload filter),不同工作空间互不可见;
- **成员级权限**: 当前依赖调用方传入 workspaceId;待 P2 工作空间上下文(`WorkspaceContext`)落地后,接入成员资格校验(非成员禁止访问/入库)。

## 十一、单元测试

| 测试类 | 覆盖点 |
|---|---|
| `TextSplitterTest` | 短文本、段落切分、无分隔符硬切、overlap、参数校验 |
| `EmbeddingClientTest` | MockRestServiceServer 模拟 `/embeddings`,批量顺序、鉴权头 |
| `ChatClientTest` | `/chat/completions` 正常响应、空 choices 异常 |
| `RagDocumentServiceImplTest` | 入库成功全链路、embedding 失败标记 FAILED + 补偿清理、删除/文档不存在 |
| `RagSearchServiceImplTest` | workspaceId 过滤、minScore 过滤、topK 透传 |
| `RagChatServiceImplTest` | 检索结果注入 prompt、sources 返回 |

## 十二、后续规划

- [ ] **与 fs-file 集成**: 文件上传完成事件 → 触发自动索引(`file:upload` 事件监听);
- [ ] **SSE 流式问答**: `chat/stream` 流式输出,提升体验;
- [ ] **Hybrid Search**: 向量检索 + MySQL 全文检索融合(RRF 排序),提升精确匹配召回;
- [ ] **知识库管理**: 文档级启用/禁用索引、索引重建、增量更新;
- [ ] **embedding 结果缓存**: Caffeine 缓存高频 query 的向量,降低模型调用成本;
- [ ] **工作空间成员权限接入**: 依赖 `WorkspaceContext` 落地后补齐。