package io.novafs.system.workspace.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 修改成员角色请求
 */
@Data
@Schema(description = "修改成员角色请求")
public class UpdateMemberRoleRequest {

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "目标角色ID（必须属于同一工作空间）")
    private Integer roleId;
}
