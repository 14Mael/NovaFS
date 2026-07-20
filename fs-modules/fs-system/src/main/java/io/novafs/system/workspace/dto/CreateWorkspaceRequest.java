package io.novafs.system.workspace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建工作空间请求
 */
@Data
@Schema(description = "创建工作空间请求")
public class CreateWorkspaceRequest {

    @NotBlank(message = "工作空间名称不能为空")
    @Size(max = 100, message = "工作空间名称最长100个字符")
    @Schema(description = "工作空间名称", example = "我的团队")
    private String name;

    @NotBlank(message = "工作空间标识不能为空")
    @Size(max = 64, message = "工作空间标识最长64个字符")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "工作空间标识只能包含小写字母、数字和中划线")
    @Schema(description = "URL友好标识", example = "my-team")
    private String slug;

    @Size(max = 500, message = "描述最长500个字符")
    @Schema(description = "工作空间描述")
    private String description;
}
