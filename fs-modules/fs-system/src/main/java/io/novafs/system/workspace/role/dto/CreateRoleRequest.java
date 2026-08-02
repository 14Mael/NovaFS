package io.novafs.system.workspace.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建角色请求
 */
@Data
@Schema(description = "创建角色请求")
public class CreateRoleRequest {

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码", example = "admin")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称", example = "管理员")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;
}
