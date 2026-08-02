package io.novafs.system.workspace.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建邀请请求
 */
@Data
@Schema(description = "创建邀请请求")
public class CreateInvitationRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "被邀请人邮箱", example = "user@example.com")
    private String email;

    @NotNull(message = "角色ID不能为空")
    @Schema(description = "分配的角色ID")
    private Integer roleId;
}
