package io.novafs.file.service;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.file.dto.ChunkUploadResponse;
import io.novafs.file.dto.UploadedChunksResponse;
import io.novafs.file.entity.FileChunkInfo;
import io.novafs.file.entity.FileTransferTask;
import io.novafs.file.event.ChunkUploadProgressEvent;
import io.novafs.file.mapper.FileChunkInfoMapper;
import io.novafs.file.mapper.FileTransferTaskMapper;
import io.novafs.file.storage.StorageConfigResolver;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.storage.plugin.boot.StorageServiceFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * 分片上传服务（幂等 + MD5 校验 + 断点续传查询）
 */
@Service
@RequiredArgsConstructor
public class ChunkUploadService {

    private final FileChunkInfoMapper chunkInfoMapper;
    private final FileTransferTaskMapper taskMapper;
    private final ChunkInitService chunkInitService;
    private final StorageConfigResolver configResolver;
    private final StorageServiceFacade storageFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public ChunkUploadResponse upload(String uploadId, Integer chunkNumber, String md5,
                                      InputStream in, long chunkSize) {
        if (chunkAlreadyExists(uploadId, chunkNumber)) {
            return new ChunkUploadResponse(chunkNumber, true);
        }

        FileTransferTask task = chunkInitService.requireTaskByUploadId(uploadId);
        if (task.isCompleted()) {
            throw new BaseException(ErrorCode.CONFLICT, "任务已完成，禁止继续上传分片");
        }

        MessageDigest digest = newDigest();
        DigestInputStream digestStream = new DigestInputStream(in, digest);
        storeChunk(task, chunkNumber, md5, digestStream, chunkSize);
        String computedMd5 = HexFormat.of().formatHex(digest.digest());

        recordChunk(task, chunkNumber, chunkSize, computedMd5);
        task.setUploadedChunks(task.getUploadedChunks() + 1);
        task.setUploadedSize(task.getUploadedSize() + chunkSize);
        taskMapper.update(task);

        eventPublisher.publishEvent(new ChunkUploadProgressEvent(uploadId, chunkNumber));
        return new ChunkUploadResponse(chunkNumber, false);
    }

    /**
     * 查询已上传分片（断点续传用）
     */
    public UploadedChunksResponse listUploadedChunks(String uploadId) {
        FileTransferTask task = chunkInitService.requireTaskByUploadId(uploadId);
        List<Integer> numbers = chunkInfoMapper.selectListByQuery(
                        QueryWrapper.create().where(FileChunkInfo::getUploadId).eq(uploadId))
                .stream().map(FileChunkInfo::getChunkNumber).toList();

        UploadedChunksResponse response = new UploadedChunksResponse();
        response.setUploadId(uploadId);
        response.setUploadedChunks(numbers);
        response.setUploadedSize(task.getUploadedSize());
        response.setTotalChunks(task.getTotalChunks());
        response.setCompleted(task.isCompleted());
        return response;
    }

    private boolean chunkAlreadyExists(String uploadId, Integer chunkNumber) {
        return chunkInfoMapper.selectCountByQuery(
                QueryWrapper.create().where(FileChunkInfo::getUploadId).eq(uploadId)
                        .and(FileChunkInfo::getChunkNumber).eq(chunkNumber)) > 0;
    }

    private void storeChunk(FileTransferTask task, Integer chunkNumber, String md5,
                            DigestInputStream digestStream, long chunkSize) {
        StorageConfigResolver.StorageTarget target = configResolver.resolve(task.getStoragePlatformSettingId());
        io.novafs.storage.plugin.core.model.ChunkUploadRequest pluginReq =
                new io.novafs.storage.plugin.core.model.ChunkUploadRequest();
        pluginReq.setUploadId(task.getUploadId());
        pluginReq.setChunkNumber(chunkNumber);
        pluginReq.setInputStream(digestStream);
        pluginReq.setChunkSize(chunkSize);
        pluginReq.setMd5(md5);
        storageFacade.uploadChunk(target.platformType(), target.config(), pluginReq);
    }

    private void recordChunk(FileTransferTask task, Integer chunkNumber, long chunkSize, String computedMd5) {
        FileChunkInfo chunk = new FileChunkInfo();
        chunk.setTaskId(task.getTaskId());
        chunk.setUploadId(task.getUploadId());
        chunk.setChunkNumber(chunkNumber);
        chunk.setChunkSize(chunkSize);
        chunk.setChunkMd5(computedMd5);
        chunk.setStoragePath(String.valueOf(chunkNumber));
        chunkInfoMapper.insert(chunk);
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
