# NovaFS Frontend — AI 文档工作空间

基于 **Vue 3 + Vite + TypeScript + Pinia + Element Plus** 的前端,与 free-fs 传统网盘后台做出差异化:以 **AI 问答为核心入口**的现代文档工作空间。

## 页面

| 路由 | 功能 |
|---|---|
| `/login` | 登录 / 注册(注册后自动登录) |
| `/chat` | **AI 问答**(默认首页):基于工作空间文档问答,回答带引用来源 |
| `/library` | 文档库:拖拽上传、卡片视图、索引状态、删除 |

## 技术说明

- 认证:Sa-Token,`Authorization` 头直接携带 token(登录后自动持久化)
- 工作空间:后端工作空间 API 尚未实现,前端用 `localStorage.novafs_workspace`(默认 1),侧栏点击可切换
- API 代理:开发时 `/api` → `http://localhost:8080`

## 运行

```bash
# 开发模式(需后端已启动)
npm run dev            # http://localhost:5173

# 生产构建(产物输出到 ../fs-admin/src/main/resources/static,由 Spring Boot 直接托管)
npm run build
```

> 本机 npm 全局安装损坏时,可用便携版:
> `C:\Users\14\node-v24.16.0-win-x64\npm.cmd run dev`