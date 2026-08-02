# NovaFS Frontend — AI 文档工作空间

基于 **Vue 3 + Vite + TypeScript + Pinia + Element Plus** 的前端，整体**浅蓝色调**。以"网盘文件管理 + AI 问答"为核心，与后端 fs-file / fs-rag / fs-system 全部接口同步。

## 页面

| 路由 | 功能 |
|---|---|
| `/login` | 登录 / 注册（注册后自动登录） |
| `/files` | **我的文件**：列表/目录浏览、普通上传（≤5MB）、分片上传（MD5 秒传 / 断点续传 / SSE 实时进度）、下载、在线预览、创建分享、删除（进回收站） |
| `/chat` | **AI 问答**：基于工作空间文档问答，回答带引用来源 |
| `/library` | **文档库**：RAG 文档拖拽上传、卡片视图、索引状态、AI 语义搜索、删除 |
| `/recycle` | **回收站**：恢复 / 彻底删除 |
| `/share/:code` | **公开分享页**（免登录）：提取码校验、在线预览、下载 |

## 关键实现

- **分片上传**：`spark-md5` 增量计算文件 MD5 → `check-md5` 秒传 → `chunk/init` → 并发 3 片上传统计进度 → `chunk/merge`；断点续传通过 `chunk/list` 查询已传分片，刷新后自动跳过
- **SSE 实时推送**：EventSource 无法携带 `Authorization` 头，改用 `fetch` 流式解析 SSE 帧（`src/api/sse.ts`），接收 `upload-progress` / `upload-complete` 事件实时更新进度
- **上传目标**：`storagePlatformSettingId` 通过 `GET /api/storage/settings?workspaceId` 获取并缓存到 `localStorage.novafs_storage_setting_id`；**需先在 `storage_settings` 表插入一条启用配置**，否则上传会报"存储配置不存在或未启用"
- **在线预览**：`/api/file/preview/{id}` 返回类型，图片/视频/音频/PDF 通过带鉴权头的 `fetch` 拉取 blob 转 objectURL 渲染，文本直接展示
- **分享**：创建分享（提取码/有效期/次数/范围）→ 生成 `/share/{code}` 链接；公开页走免登录白名单，下载/内联预览走 `GET /api/share/{code}/download`
- **认证**：Sa-Token，`Authorization` 头直接携带 token；`login` 响应为 `{token, username, nickname}`，登出调用后端 `/auth/logout`
- **工作空间**：侧栏下拉从 `GET /api/workspaces` 拉取，可新建；当前工作空间存 `localStorage.novafs_workspace`（默认 1），切换后刷新页面

## 技术说明

- API 代理：开发时 `/api` → `http://localhost:8080`（vite.config.ts）
- 主题：`src/style.css` 定义浅蓝色调 CSS 变量（主色 `#3b9dff`、背景 `#f1f7ff`、侧栏深蓝 `#0e2b52`），覆盖 Element Plus `--el-color-primary` 系列

## 运行

```bash
# 开发模式（需后端已启动）
npm run dev            # http://localhost:5173

# 生产构建（产物输出到 ../fs-admin/src/main/resources/static，由 Spring Boot 直接托管）
npm run build
```

> 本机 npm 全局安装损坏时，可用便携版：
> `C:\Users\14\node-v24.16.0-win-x64\npm.cmd run dev`
