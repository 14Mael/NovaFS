package io.novafs.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 分片上传初始化结果
 */
@Data
@AllArgsConstructor
public class ChunkInitResponse {

    private String uploadId;

    private String taskId;

    private Long chunkSize;

    private Integer totalChunks;
}
