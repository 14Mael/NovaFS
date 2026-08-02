package io.novafs.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 初始化分片上传请求
 */
@Data
public class ChunkInitRequest {

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    @NotNull(message = "总分片数不能为空")
    private Integer totalChunks;

    /** 分片大小，默认 5MB */
    private Long chunkSize;

    /** 父文件夹 ID，可选 */
    private Long parentId;

    /** 完整文件 MD5，可选 */
    private String md5;

    @NotNull(message = "存储平台配置不能为空")
    private Long storagePlatformSettingId;
}
