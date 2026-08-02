package io.novafs.file.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 合并分片请求
 */
@Data
public class ChunkMergeRequest {

    @NotBlank(message = "uploadId 不能为空")
    private String uploadId;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /** 完整文件 MD5，用于合并后二次校验 */
    private String md5;
}
