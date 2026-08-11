package io.novafs.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 文件夹名/重命名请求（创建文件夹与重命名共用）
 */
@Data
@Schema(description = "文件夹名称请求")
public class FolderNameRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 255, message = "名称长度不能超过 255")
    @Schema(description = "名称（不允许包含 / 或 \\）", example = "项目资料")
    private String name;
}
