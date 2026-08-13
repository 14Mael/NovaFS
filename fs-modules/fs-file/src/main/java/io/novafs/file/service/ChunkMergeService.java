package io.novafs.file.service;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.dto.ChunkMergeRequest;
import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.entity.FileChunkInfo;
import io.novafs.file.entity.FileInfo;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.event.FileUploadCompleteEvent;
import io.novafs.file.mapper.FileChunkInfoMapper;
import io.novafs.file.mapper.FileInfoMapper;
import io.novafs.file.mapper.FileTransferTaskMapper;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.storage.plugin.boot.StorageServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 分片合并服务
 */
@Service
@RequiredArgsConstructor
public class ChunkMergeService {

    private final FileTransferTaskMapper taskMapper;
    private final FileChunkInfoMapper chunkInfoMapper;
    private final FileInfoMapper fileInfoMapper;
    private final ChunkInitService chunkInitService;
    private final StorageConfigResolver configResolver;
    private final StorageServiceFacade storageFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskFailureMarker failureMarker;

    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO merge(Long userId, Long workspaceId, ChunkMergeRequest request) {
        FileTransferTask task = chunkInitService.requireTaskByUploadId(request.getUploadId());
        try {
            if (task.isCompleted()) {
                throw new BaseException(ErrorCode.CONFLICT, "任务已合并完成");
            }
            ensureAllChunksUploaded(task);

            String objectKey = mergePluginChunks(task, request);
            FileInfo file = finalizeFileInfo(task, objectKey, request.getMd5());
            task.markCompleted();
            taskMapper.update(task);

            eventPublisher.publishEvent(new FileUploadCompleteEvent(file.getId(), userId, workspaceId, file.getSize()));
            return toVO(file);
        } catch (Exception e) {
            // 合并失败（如存储端分片目录丢失）：主事务回滚会残留 UPLOADING 僵尸任务，
            // 用独立事务标记 FAILED，避免断点续传反复复用同一 uploadId
            failureMarker.markFailed(request.getUploadId());
            throw e;
        }
    }

    private void ensureAllChunksUploaded(FileTransferTask task) {
        long uploaded = chunkInfoMapper.selectCountByQuery(
                QueryWrapper.create().where(FileChunkInfo::getUploadId).eq(task.getUploadId()));
        if (uploaded < task.getTotalChunks()) {
            throw new BaseException(ErrorCode.CHUNK_NOT_COMPLETE,
                    "分片未上传完成: " + uploaded + "/" + task.getTotalChunks());
        }
    }

    private String mergePluginChunks(FileTransferTask task, ChunkMergeRequest request) {
        StorageConfigResolver.StorageTarget target = configResolver.resolve(task.getStoragePlatformSettingId());
        io.novafs.storage.plugin.core.model.ChunkMergeRequest pluginReq =
                new io.novafs.storage.plugin.core.model.ChunkMergeRequest();
        pluginReq.setUploadId(task.getUploadId());
        pluginReq.setObjectKey(task.getTaskId() + "/" + task.getFileName());
        pluginReq.setFileName(task.getFileName());
        return storageFacade.mergeChunks(target.platformType(), target.config(), pluginReq);
    }

    private FileInfo finalizeFileInfo(FileTransferTask task, String objectKey, String md5) {
        FileInfo file = fileInfoMapper.selectOneById(task.getFileId());
        if (file == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND, "文件记录不存在: " + task.getFileId());
        }
        file.setObjectKey(objectKey);
        file.setContentMd5(md5);
        file.setUploadTime(LocalDateTime.now());
        file.setStoragePlatformSettingId(task.getStoragePlatformSettingId());
        fileInfoMapper.update(file);
        return file;
    }

    private static FileInfoVO toVO(FileInfo file) {
        FileInfoVO vo = new FileInfoVO();
        vo.setId(file.getId());
        vo.setWorkspaceId(file.getWorkspaceId());
        vo.setUserId(file.getUserId());
        vo.setParentId(file.getParentId());
        vo.setOriginalName(file.getOriginalName());
        vo.setDisplayName(file.getDisplayName());
        vo.setSuffix(file.getSuffix());
        vo.setSize(file.getSize());
        vo.setMimeType(file.getMimeType());
        vo.setIsDir(file.getIsDir());
        vo.setContentMd5(file.getContentMd5());
        vo.setUploadTime(file.getUploadTime());
        vo.setIsDeleted(file.getIsDeleted());
        return vo;
    }
}
