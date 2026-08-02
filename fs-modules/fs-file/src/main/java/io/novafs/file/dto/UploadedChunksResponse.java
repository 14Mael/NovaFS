package io.novafs.file.dto;

import lombok.Data;

import java.util.List;

/**
 * 已上传分片列表（断点续传用）
 */
@Data
public class UploadedChunksResponse {

    private String uploadId;

    /** 已上传的分片编号 */
    private List<Integer> uploadedChunks;

    /** 已上传字节数 */
    private Long uploadedSize;

    private Integer totalChunks;

    private boolean completed;
}
