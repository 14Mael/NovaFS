package io.novafs.system.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新工作空间请求
 */
@Data
@Schema(description = "更新工作空间请求")
public class UpdateWorkspaceRequest {

    @NotBlank(message = "工作空间名称不能为空")
    @Size(max = 100, message = "工作空间名称最长100个字符")
    @Schema(description = "工作空间名称", example = "我的团队")
    private String name;

    @Size(max = 500, message = "描述最长500个字符")
    @Schema(description = "工作空间描述")
    private String description;
}
