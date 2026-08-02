package io.novafs.storage.plugin.core.model;

import lombok.Data;

/**
 * 分片上传初始化请求
 */
@Data
public class ChunkInitRequest {

    /** 存储上的对象键 */
    private String objectKey;

    /** 文件总大小 */
    private Long fileSize;

    /** 分片总数 */
    private Integer totalChunks;
}
