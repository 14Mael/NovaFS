package io.novafs.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 秒传校验请求
 */
@Data
public class CheckMd5Request {

    @NotBlank(message = "文件MD5不能为空")
    private String md5;

    private String fileName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
}
