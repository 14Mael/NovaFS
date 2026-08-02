package io.novafs.storage.plugin.core.model;

import lombok.Data;

/**
 * 分片合并请求
 */
@Data
public class ChunkMergeRequest {

    /** 上传任务 ID */
    private String uploadId;

    /** 最终对象键 */
    private String objectKey;

    /** 文件名 */
    private String fileName;
}
