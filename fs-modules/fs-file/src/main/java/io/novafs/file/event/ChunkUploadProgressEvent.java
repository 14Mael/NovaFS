package io.novafs.file.event;

import lombok.Getter;

/**
 * 分片上传进度事件（事件驱动解耦，SSE 推送监听器在 P5 fs-sse 模块接入）
 */
@Getter
public class ChunkUploadProgressEvent {

    private final String uploadId;

    private final Integer chunkNumber;

    public ChunkUploadProgressEvent(String uploadId, Integer chunkNumber) {
        this.uploadId = uploadId;
        this.chunkNumber = chunkNumber;
    }
}
