package io.novafs.system.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 登录响应（HTTP返回用）
 */
@Data
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "认证令牌")
    private String token;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    public LoginResponse(String token, io.novafs.system.user.dto.UserResponse user) {
        this.token = token;
        this.username = user.getUsername();
        this.nickname = user.getNickname();
    }
}
