package io.novafs.system.workspace.invitation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 接受邀请请求
 */
@Data
@Schema(description = "接受邀请请求")
public class AcceptInvitationRequest {

    @NotBlank(message = "邀请令牌不能为空")
    @Schema(description = "邀请令牌（邮件链接中的 token）")
    private String token;
}
