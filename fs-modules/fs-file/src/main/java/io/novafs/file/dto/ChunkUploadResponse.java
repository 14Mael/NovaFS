package io.novafs.file.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 分片上传结果
 */
@Data
@AllArgsConstructor
public class ChunkUploadResponse {

    private Integer chunkNumber;

    /** 是否重复上传（幂等命中） */
    private boolean alreadyExists;
}
