package io.novafs.storage.plugin.core.model;

import lombok.Data;

import java.io.InputStream;

/**
 * 分片上传请求
 */
@Data
public class ChunkUploadRequest {

    /** 上传任务 ID */
    private String uploadId;

    /** 分片序号（从 1 开始） */
    private Integer chunkNumber;

    /** 分片内容流 */
    private InputStream inputStream;

    /** 分片大小 */
    private Long chunkSize;

    /** 分片 MD5，用于校验 */
    private String md5;
}
