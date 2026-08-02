package io.novafs.system.workspace.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加成员请求
 */
@Data
@Schema(description = "添加成员请求")
public class AddMemberRequest {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "用户ID")
    private Long userId;

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "角色ID")
    private Integer roleId;
}
