package io.novafs.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒传落库请求（复用已存在文件的存储对象，归属到目标文件夹）
 */
@Data
@Schema(description = "秒传落库请求")
public class InstantUploadRequest {

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "文件名")
    private String fileName;

    @NotBlank(message = "MD5 不能为空")
    @Schema(description = "文件内容 MD5")
    private String md5;

    @NotNull(message = "文件大小不能为空")
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
}
