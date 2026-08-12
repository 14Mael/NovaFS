package io.novafs.system.workspace.invitation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请信息响应
 */
@Data
@Schema(description = "邀请信息")
public class InvitationResponse {

    @Schema(description = "邀请ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "邀请令牌（创建时返回，供拼接邀请链接）")
    private String token;

    @Schema(description = "角色ID")
    private Integer roleId;

    @Schema(description = "状态: 0待接受 1已接受 2已过期 3已取消")
    private Integer status;

    @Schema(description = "过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
