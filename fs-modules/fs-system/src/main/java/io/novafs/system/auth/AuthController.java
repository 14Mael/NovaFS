package io.novafs.system.auth;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import io.novafs.framework.common.model.Result;
import io.novafs.system.auth.dto.LoginResponse;
import io.novafs.system.user.dto.LoginRequest;
import io.novafs.system.user.dto.RegisterRequest;
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

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        sysUserService.register(request);
        StpUtil.login(request.getUsername(), SaLoginModel.create()
                .setIsWriteHeader(true)
        );
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var loginResult = sysUserService.loginByPassword(request);
        StpUtil.login(loginResult.getUser().getUsername(), SaLoginModel.create()
                .setIsWriteHeader(true)
        );
        return Result.ok(new LoginResponse(
                StpUtil.getTokenValue(),
                loginResult.getUser()
        ));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.ok();
    }
}
