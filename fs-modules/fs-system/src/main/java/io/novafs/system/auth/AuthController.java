package io.novafs.system.auth;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import io.novafs.framework.common.model.Result;
import io.novafs.system.auth.dto.LoginRequest;
import io.novafs.system.auth.dto.LoginResponse;
import io.novafs.system.auth.dto.RegisterRequest;
import io.novafs.system.user.entity.SysUser;
import io.novafs.system.user.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        SysUser user = sysUserService.register(
                request.getUsername(),
                request.getPassword(),
                request.getEmail()
        );
        // 注册后自动登录
        StpUtil.login(user.getId(), SaLoginModel.create()
                .setIsWriteHeader(true)
        );
        return Result.ok();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SysUser user = sysUserService.loginByPassword(
                request.getUsername(),
                request.getPassword()
        );
        // 登录，Token 写入响应头
        StpUtil.login(user.getId(), SaLoginModel.create()
                .setIsWriteHeader(true)
        );
        return Result.ok(new LoginResponse(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getNickname()
        ));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }
}
