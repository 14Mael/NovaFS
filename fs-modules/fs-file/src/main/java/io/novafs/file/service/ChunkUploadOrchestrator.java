package io.novafs.file.service;

import io.novafs.file.dto.CheckMd5Request;
import io.novafs.file.dto.CheckMd5Result;
import io.novafs.file.dto.ChunkInitRequest;
import io.novafs.file.dto.ChunkInitResponse;
import io.novafs.file.dto.ChunkMergeRequest;
import io.novafs.file.dto.ChunkUploadResponse;
import io.novafs.file.dto.FileInfoVO;
import io.novafs.file.dto.UploadedChunksResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 分片上传编排器（门面）：编排秒传/初始化/上传/合并/断点续传流程
 */
@Service
@RequiredArgsConstructor
public class ChunkUploadOrchestrator {

    private final Md5CheckService md5CheckService;
    private final ChunkInitService chunkInitService;
    private final ChunkUploadService chunkUploadService;
    private final ChunkMergeService chunkMergeService;

    /** 秒传校验 */
    public CheckMd5Result checkMd5(CheckMd5Request request, Long workspaceId) {
        return md5CheckService.check(request, workspaceId);
    }

    /** 初始化分片上传 */
    public ChunkInitResponse initChunkUpload(Long userId, Long workspaceId, ChunkInitRequest request) {
        return chunkInitService.init(userId, workspaceId, request);
    }

    /** 上传单个分片 */
    public ChunkUploadResponse uploadChunk(String uploadId, Integer chunkNumber, String md5,
                                           InputStream in, long chunkSize) {
        return chunkUploadService.upload(uploadId, chunkNumber, md5, in, chunkSize);
    }

    /** 合并分片 */
    public FileInfoVO mergeChunks(Long userId, Long workspaceId, ChunkMergeRequest request) {
        return chunkMergeService.merge(userId, workspaceId, request);
    }

    /** 断点续传：查询已上传分片 */
    public UploadedChunksResponse listUploadedChunks(String uploadId) {
        return chunkUploadService.listUploadedChunks(uploadId);
    }
}
