# 分片上传与断点续传设计

## 一、整体流程

```
[前端]                  [后端]                    [存储平台]
  │                       │                         │
  │ 1. 计算文件 MD5        │                         │
  │─────────────────────→ │                         │
  │                       │                         │
  │ 2. 秒传校验            │                         │
  │ POST /api/file/check-md5                       │
  │─────────────────────→ │                         │
  │                       ├── 查询 file_info 表     │
  │                       │   是否已存在同 MD5 文件  │
  │                       │                         │
  │  ←── 已存在: 秒传成功  │                         │
  │  ←── 不存在: 继续上传  │                         │
  │                       │                         │
  │ 3. 初始化分片上传       │                         │
  │ POST /api/file/chunk/init                      │
  │  {fileName, fileSize, totalChunks, chunkSize}   │
  │─────────────────────→ │                         │
  │                       ├── 创建 file_transfer_task│
  │                       ├── 调用存储平台 initChunk │
  │                       ├── 返回 uploadId          │
  │  ←── 返回 uploadId    │                         │
  │                       │                         │
  │ 4. 逐片上传（并发）     │                         │
  │ POST /api/file/chunk/upload                    │
  │  {uploadId, chunkNumber, file, md5}            │
  │─────────────────────→ │                         │
  │                       ├── 校验分片 MD5           │
  │                       ├── 存储分片到临时目录      │
  │                       ├── 记录分片上传状态        │
  │                       ├── 通过 SSE 推送进度      │
  │  ←── 返回 chunkNumber │                         │
  │                       │                         │
  │ 5. 查询已上传分片       │                         │
  │（断点续传时调用）       │                         │
  │ GET /api/file/chunk/list?uploadId=x            │
  │─────────────────────→ │                         │
  │  ←── 返回已上传分片列表  │                         │
  │                       │                         │
  │ 6. 合并分片            │                         │
  │ POST /api/file/chunk/merge                     │
  │  {uploadId, fileName, md5}                     │
  │─────────────────────→ │                         │
  │                       ├── 调用存储平台 merge     │
  │                       ├── 计算最终文件 MD5       │
  │                       ├── 写入 file_info 表     │
  │                       ├── 发送上传完成事件       │
  │                       │   ├── SSE 推送完成通知   │
  │                       │   ├── 写操作日志         │
  │                       │   └── 更新配额          │
  │  ←── 返回 fileInfo    │                         │
```

## 二、API 定义

### 2.1 秒传校验

```
POST /api/file/check-md5
Request:  { "md5": "文件MD5", "fileName": "文件名", "fileSize": 1024000 }
Response: {
  "code": 200,
  "data": {
    "exists": true,           // true=已存在，可以秒传
    "fileInfo": { ... }       // exists=true 时返回已有文件信息
  }
}
```

实现逻辑：查询 `file_info` 表，匹配 `content_md5` + `file_size` 一致的记录。

> **安全提醒**：不要只用 MD5 判断，必须同时校验文件大小，防止 MD5 碰撞。

### 2.2 初始化分片上传

```
POST /api/file/chunk/init
Request: {
  "fileName": "bigfile.iso",
  "fileSize": 2147483648,        // 2GB
  "totalChunks": 400,
  "chunkSize": 5242880,          // 5MB
  "parentId": "文件夹ID",         // 可选，上传到指定文件夹
  "md5": "完整文件MD5"             // 可选
}
Response: {
  "code": 200,
  "data": {
    "uploadId": "uuid-xxxx",      // 本次上传的唯一标识
    "taskId": "task-uuid",        // 传输任务 ID
    "chunkSize": 5242880,
    "totalChunks": 400
  }
}
```

### 2.3 上传分片

```
POST /api/file/chunk/upload
Request (multipart/form-data):
  uploadId: "uuid-xxxx"
  chunkNumber: 5
  file: (二进制文件内容)
  md5: "分片MD5"                  // 可选，用于服务端校验
Response: {
  "code": 200,
  "data": {
    "chunkNumber": 5,
    "eTag": "分片etag"
  }
}
```

### 2.4 查询已上传分片（断点续传用）

```
GET /api/file/chunk/list?uploadId=uuid-xxxx
Response: {
  "code": 200,
  "data": {
    "uploadId": "uuid-xxxx",
    "uploadedChunks": [1, 2, 3, 4, 6],    // 已上传的分片编号
    "uploadedSize": 26214400,               // 已上传字节数
    "isCompleted": false
  }
}
```

前端逻辑：对比 `uploadedChunks`，只上传缺失的分片（如示例中的 5, 7, 8...）。

### 2.5 合并分片

```
POST /api/file/chunk/merge
Request: {
  "uploadId": "uuid-xxxx",
  "fileName": "bigfile.iso",
  "md5": "完整文件MD5"              // 用于合并后校验
}
Response: {
  "code": 200,
  "data": {
    "fileId": "file-xxxx",
    "fileName": "bigfile.iso",
    "fileSize": 2147483648,
    "md5": "完整文件MD5（与上传前一致）"
  }
}
```

## 三、数据表设计

### 3.1 传输任务表（新增字段）

在 `file_transfer_task` 基础上，增加关键字段：

```sql
CREATE TABLE file_chunk_info (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id     VARCHAR(64) NOT NULL COMMENT '关联传输任务ID',
    upload_id   VARCHAR(255) NOT NULL COMMENT '上传ID',
    chunk_number INT NOT NULL COMMENT '分片序号（从1开始）',
    chunk_size  BIGINT NOT NULL COMMENT '分片大小',
    chunk_md5   VARCHAR(64) NULL COMMENT '分片MD5',
    storage_path VARCHAR(512) NOT NULL COMMENT '分片在临时存储的路径',
    e_tag       VARCHAR(255) NULL COMMENT '存储平台返回的ETag',
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_upload_chunk (upload_id, chunk_number),
    INDEX idx_task_id (task_id)
) COMMENT '分片上传明细表';
```

## 四、服务拆分设计

原项目 `FileTransferTaskServiceImpl` 1322 行，**拆分为 4 个小类**：

```
┌───────────────────────────────────────────┐
│          ChunkUploadOrchestrator          │
│            （上传编排器-门面）              │
│  职责: 编排上传流程, 不包含具体实现         │
├───────────────────────────────────────────┤
│                                           │
│  ┌──────────────┐   ┌──────────────────┐  │
│  │ Md5CheckService│ │  ChunkInitService │  │
│  │ 秒传校验服务    │   │  初始化分片服务    │  │
│  │ 职责:          │   │  职责:            │  │
│  │ · 查重MD5+尺寸 │   │ · 创建传输任务    │  │
│  │ · 返回秒传结果 │   │ · 分配 uploadId  │  │
│  └──────────────┘   └──────────────────┘  │
│                                           │
│  ┌──────────────┐   ┌──────────────────┐  │
│  │ ChunkUploadService│ │  ChunkMergeService│
│  │ 分片上传服务    │   │  分片合并服务      │  │
│  │ 职责:          │   │  职责:            │  │
│  │ · 接收分片      │   │ · 调用存储合并    │  │
│  │ · 校验分片MD5   │   │ · MD5二次校验    │  │
│  │ · 存入临时存储   │   │ · 写入file_info  │  │
│  │ · 更新进度      │   │ · 发布事件       │  │
│  └──────────────┘   └──────────────────┘  │
│                                           │
│  ┌──────────────────────────────┐         │
│  │  TransferProgressService     │         │
│  │  进度推送服务                 │         │
│  │ 职责:                         │         │
│  │ · 计算上传速度/剩余时间        │         │
│  │ · 缓存进度状态                │         │
│  │ · 通过 SSE 推送给前端          │         │
│  └──────────────────────────────┘         │
└───────────────────────────────────────────┘
```

### 4.1 ChunkUploadOrchestrator（门面）

```java
@Service
@RequiredArgsConstructor
public class ChunkUploadOrchestrator {

    private final Md5CheckService md5CheckService;
    private final ChunkInitService chunkInitService;
    private final ChunkUploadService chunkUploadService;
    private final ChunkMergeService chunkMergeService;
    private final TransferProgressService progressService;

    /**
     * 秒传校验
     */
    public Md5CheckResult checkMd5(Md5CheckRequest request) {
        return md5CheckService.check(request);
    }

    /**
     * 初始化分片上传
     */
    public ChunkInitResponse initChunkUpload(ChunkInitRequest request) {
        ChunkInitResponse response = chunkInitService.init(request);
        progressService.initializeProgress(response.getTaskId());
        return response;
    }

    /**
     * 上传单个分片
     */
    public ChunkUploadResponse uploadChunk(ChunkUploadRequest request) {
        ChunkUploadResponse response = chunkUploadService.upload(request);
        // 通过事件驱动推送进度，而不是直接调用
        eventPublisher.publishEvent(
            new ChunkUploadProgressEvent(request.getUploadId(),
                                          request.getChunkNumber()));
        return response;
    }

    /**
     * 合并分片
     */
    public ChunkMergeResponse mergeChunks(ChunkMergeRequest request) {
        ChunkMergeResponse response = chunkMergeService.merge(request);
        // 事件驱动：推送完成 + 写日志 + 更新配额
        eventPublisher.publishEvent(
            new FileUploadCompleteEvent(response.getFileId(), ...));
        return response;
    }

    /**
     * 断点续传：查询已上传分片列表
     */
    public UploadedChunksResponse listUploadedChunks(String uploadId) {
        return chunkUploadService.listUploadedChunks(uploadId);
    }
}
```

### 4.2 分片上传服务（核心）

```java
@Service
@RequiredArgsConstructor
public class ChunkUploadService {

    private final ChunkInfoMapper chunkInfoMapper;
    private final TransferTaskMapper taskMapper;
    private final StorageServiceFacade storageFacade;

    /**
     * 上传分片
     */
    @Transactional
    public ChunkUploadResponse upload(ChunkUploadRequest request) {
        // 1. 校验：分片是否已上传（幂等性）
        ChunkInfo existing = chunkInfoMapper.selectByUploadIdAndNumber(
            request.getUploadId(), request.getChunkNumber());
        if (existing != null) {
            return ChunkUploadResponse.alreadyExists(existing);
        }

        // 2. 校验分片 MD5（前端传的 md5 与服务端计算一致）
        String computedMd5 = DigestUtils.md5DigestAsHex(request.getFile().getInputStream());
        if (request.getMd5() != null && !request.getMd5().equals(computedMd5)) {
            throw new ChunkMd5MismatchException(request.getChunkNumber());
        }

        // 3. 获取传输任务和存储配置
        FileTransferTask task = taskMapper.selectByUploadId(request.getUploadId());

        // 4. 调用存储插件保存分片
        String storagePath = storageFacade.uploadChunk(
            task.getStoragePlatformSettingId(), request);

        // 5. 记录分片明细
        ChunkInfo chunkInfo = new ChunkInfo();
        chunkInfo.setTaskId(task.getTaskId());
        chunkInfo.setUploadId(request.getUploadId());
        chunkInfo.setChunkNumber(request.getChunkNumber());
        chunkInfo.setChunkSize(request.getFile().getSize());
        chunkInfo.setChunkMd5(computedMd5);
        chunkInfo.setStoragePath(storagePath);
        chunkInfoMapper.insert(chunkInfo);

        // 6. 更新任务进度
        taskMapper.incrementUploadedChunks(request.getUploadId(), 1,
            request.getFile().getSize());

        return ChunkUploadResponse.success(request.getChunkNumber());
    }

    /**
     * 查询已上传分片列表（断点续传用）
     */
    public List<Integer> listUploadedChunks(String uploadId) {
        return chunkInfoMapper.selectChunkNumbersByUploadId(uploadId);
    }
}
```

### 4.3 合并服务

```java
@Service
@RequiredArgsConstructor
public class ChunkMergeService {

    private final ChunkInfoMapper chunkInfoMapper;
    private final TaskMapper taskMapper;
    private final FileInfoMapper fileInfoMapper;
    private final StorageServiceFacade storageFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChunkMergeResponse merge(ChunkMergeRequest request) {
        // 1. 校验所有分片已上传
        FileTransferTask task = taskMapper.selectByUploadId(request.getUploadId());
        int uploadedCount = chunkInfoMapper.countByUploadId(request.getUploadId());
        if (uploadedCount < task.getTotalChunks()) {
            throw new ChunksNotCompleteException(
                task.getTotalChunks(), uploadedCount);
        }

        // 2. 调用存储平台合并
        String objectKey = storageFacade.mergeChunks(
            task.getStoragePlatformSettingId(), request);

        // 3. 写入文件记录
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(request.getFileName());
        fileInfo.setObjectKey(objectKey);
        fileInfo.setSize(task.getFileSize());
        fileInfo.setContentMd5(request.getMd5());
        fileInfo.setWorkspaceId(task.getWorkspaceId());
        fileInfo.setUserId(task.getUserId());
        fileInfoMapper.insert(fileInfo);

        // 4. 更新任务状态为完成
        taskMapper.markCompleted(request.getUploadId());

        // 5. 发布事件（解耦：SSE推送 + 写日志 + 更新配额）
        eventPublisher.publishEvent(
            new FileUploadCompleteEvent(fileInfo.getId(),
                task.getUserId(), task.getWorkspaceId(), fileInfo.getSize()));

        return ChunkMergeResponse.success(fileInfo);
    }
}
```

## 五、进度推送（事件驱动）

### 5.1 事件定义

```java
@Getter
public class ChunkUploadProgressEvent extends ApplicationEvent {
    private final String uploadId;
    private final Integer chunkNumber;
    // getters
}

@Getter
public class FileUploadCompleteEvent extends ApplicationEvent {
    private final Long fileId;
    private final Long userId;
    private final Long workspaceId;
    private final Long fileSize;
}
```

### 5.2 监听器

```java
@Component
@RequiredArgsConstructor
public class UploadProgressListener {

    private final TransferProgressService progressService;
    private final SseConnectionManager sseManager;

    @EventListener
    public void onChunkUploaded(ChunkUploadProgressEvent event) {
        ProgressInfo progress = progressService.calculateProgress(event.getUploadId());
        // 通过 SSE 推送给前端
        sseManager.send(event.getUploadId(), progress);
    }
}

@Component
@RequiredArgsConstructor
public class UploadCompleteListener {

    private final SysLogService logService;
    private final QuotaService quotaService;
    private final SseConnectionManager sseManager;

    @EventListener
    public void onFileUploadComplete(FileUploadCompleteEvent event) {
        // 并行执行：各自独立，互不影响
        logService.recordUploadLog(event.getFileId(), event.getUserId());
        quotaService.updateStorageUsed(event.getWorkspaceId(), event.getUserId(),
            event.getFileSize());
        sseManager.send(event.getUserId(), "上传完成");
    }
}
```

## 六、断点续传实现要点

### 6.1 前端逻辑

```typescript
async function uploadWithResume(file: File) {
  // 1. 检查是否已有进行中的上传任务
  const existingTask = await checkExistingTask(file.md5);

  if (existingTask) {
    // 2. 有未完成的任务: 查已上传分片
    const uploaded = await listUploadedChunks(existingTask.uploadId);
    const missingChunks = getMissingChunks(totalChunks, uploaded);

    // 3. 只上传缺失的分片
    await Promise.all(
      missingChunks.map(n => uploadChunk(existingTask.uploadId, n, file))
    );
  } else {
    // 4. 全新上传: 按正常流程
    const { uploadId } = await initChunkUpload(file);
    await uploadAllChunks(uploadId, file);
  }
}
```

### 6.2 后端幂等性

上传分片接口是**幂等**的：同一 `uploadId` + 同一 `chunkNumber` 重复调用，第二次直接返回已存在，不会重复写入。

## 七、对比原项目的改进

| 方面 | 原项目 | 改进后 |
|---|---|---|
| 服务拆分 | 单个类 1322 行 | 4 个类各 ≤ 300 行 |
| 进度推送 | Service 直接推 SSE | 事件驱动（Spring Events），可扩展 |
| 分片明细 | 只有总数和已上传数 | 有 `file_chunk_info` 明细表，每片持久化 |
| 分片校验 | 未实现 | 前端传 MD5 + 服务端校验 |
| 异常恢复 | 上传中断后无记录 | 查询已上传分片，支持续传 |
| 最终校验 | 无 | 合并后重新计算全文件 MD5 比对 |
| 并发控制 | 无 | 分片上传幂等性设计 |
