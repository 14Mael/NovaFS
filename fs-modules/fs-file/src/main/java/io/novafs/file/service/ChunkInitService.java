package io.novafs.file.service;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.dto.ChunkInitRequest;
import io.novafs.file.dto.ChunkInitResponse;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.enums.TransferTaskStatus;
import io.novafs.file.enums.TransferTaskType;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.mapper.FileTransferTaskMapper;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.storage.plugin.boot.StorageServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 分片上传初始化服务
 */
@Service
@RequiredArgsConstructor
public class ChunkInitService {

    /** 默认分片大小 5MB */
    public static final long DEFAULT_CHUNK_SIZE = 5L * 1024 * 1024;

    private final FileTransferTaskMapper taskMapper;
    private final FileInfoMapper fileInfoMapper;
    private final StorageConfigResolver configResolver;
    private final StorageServiceFacade storageFacade;

    @Transactional(rollbackFor = Exception.class)
    public ChunkInitResponse init(Long userId, Long workspaceId, ChunkInitRequest request) {
        // 断点续传：同一文件（md5 + 文件名 + 工作空间 + 用户）的未完成任务直接复用 uploadId，
        // 前端通过 chunk/list 查出已传分片，只补传缺失分片
        FileTransferTask resumable = findResumableTask(userId, workspaceId, request);
        if (resumable != null) {
            return new ChunkInitResponse(resumable.getUploadId(), resumable.getTaskId(),
                    resumable.getChunkSize(), resumable.getTotalChunks());
        }

        StorageConfigResolver.StorageTarget target = configResolver.resolve(request.getStoragePlatformSettingId());
        long chunkSize = request.getChunkSize() == null ? DEFAULT_CHUNK_SIZE : request.getChunkSize();

        FileTransferTask task = buildTask(userId, workspaceId, request, chunkSize);
        taskMapper.insert(task);

        String objectKey = task.getTaskId() + "/" + request.getFileName();
        String uploadId = initPluginChunk(target, request, objectKey);
        task.setUploadId(uploadId);
        task.setFileId(createFilePlaceholder(userId, workspaceId, request));
        taskMapper.update(task);

        return new ChunkInitResponse(uploadId, task.getTaskId(), chunkSize, request.getTotalChunks());
    }

    /**
     * 查找可断点续传的未完成任务（md5 必须存在，避免误复用同名不同内容文件）
     */
    private FileTransferTask findResumableTask(Long userId, Long workspaceId, ChunkInitRequest request) {
        if (request.getMd5() == null || request.getMd5().isBlank()) {
            return null;
        }
        return taskMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(FileTransferTask::getWorkspaceId).eq(workspaceId)
                        .and(FileTransferTask::getUserId).eq(userId)
                        .and(FileTransferTask::getFileName).eq(request.getFileName())
                        .and(FileTransferTask::getFileMd5).eq(request.getMd5())
                        .and(FileTransferTask::getStatus).eq(TransferTaskStatus.UPLOADING)
                        .and(FileTransferTask::getUploadedChunks).gt(0)
                        .orderBy(FileTransferTask::getId, false));
    }

    private FileTransferTask buildTask(Long userId, Long workspaceId, ChunkInitRequest request, long chunkSize) {
        FileTransferTask task = new FileTransferTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setWorkspaceId(workspaceId);
        task.setUserId(userId);
        task.setFileName(request.getFileName());
        task.setFileSize(request.getFileSize());
        task.setFileMd5(request.getMd5());
        task.setTotalChunks(request.getTotalChunks());
        task.setChunkSize(chunkSize);
        task.setUploadedChunks(0);
        task.setUploadedSize(0L);
        task.setStatus(TransferTaskStatus.UPLOADING);
        task.setTaskType(TransferTaskType.UPLOAD);
        task.setStoragePlatformSettingId(request.getStoragePlatformSettingId());
        return task;
    }

    private String initPluginChunk(StorageConfigResolver.StorageTarget target, ChunkInitRequest request, String objectKey) {
        io.novafs.storage.plugin.core.model.ChunkInitRequest pluginReq =
                new io.novafs.storage.plugin.core.model.ChunkInitRequest();
        pluginReq.setObjectKey(objectKey);
        pluginReq.setFileSize(request.getFileSize());
        pluginReq.setTotalChunks(request.getTotalChunks());
        return storageFacade.initChunkUpload(target.platformType(), target.config(), pluginReq);
    }

    /** 创建文件占位记录（合并时补充完整信息） */
    private Long createFilePlaceholder(Long userId, Long workspaceId, ChunkInitRequest request) {
        FileInfo file = new FileInfo();
        file.setWorkspaceId(workspaceId);
        file.setUserId(userId);
        file.setParentId(request.getParentId());
        file.setOriginalName(request.getFileName());
        file.setSuffix(suffixOf(request.getFileName()));
        file.setSize(request.getFileSize());
        file.setIsDir(false);
        file.setIsDeleted(false);
        fileInfoMapper.insert(file);
        return file.getId();
    }

    private String suffixOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase() : null;
    }

    /** 查询任务（供其他服务复用） */
    FileTransferTask requireTaskByUploadId(String uploadId) {
        FileTransferTask task = taskMapper.selectOneByQuery(
                QueryWrapper.create().where(FileTransferTask::getUploadId).eq(uploadId));
        if (task == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND, "上传任务不存在: " + uploadId);
        }
        return task;
    }
}
