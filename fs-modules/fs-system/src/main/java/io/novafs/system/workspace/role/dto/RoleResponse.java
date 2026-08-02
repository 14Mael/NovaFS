package io.novafs.system.workspace.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色响应
 */
@Data
@Schema(description = "角色信息")
public class RoleResponse {

    @Schema(description = "角色ID")
    private Integer id;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "类型: 0系统预设 1自定义")
    private Integer roleType;
}
