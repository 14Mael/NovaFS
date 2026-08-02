package io.novafs.system.user.service;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.system.user.dto.LoginRequest;
import io.novafs.system.user.dto.LoginResponse;
import io.novafs.system.user.dto.RegisterRequest;
import io.novafs.system.user.dto.UserResponse;
import io.novafs.system.user.entity.SysUser;
import io.novafs.system.user.enums.UserStatus;
import io.novafs.system.user.mapper.SysUserMapper;
import io.novafs.system.workspace.service.SysWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysWorkspaceService workspaceService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional(rollbackFor = Exception.class)
    public UserResponse register(RegisterRequest request) {
        if (isUsernameExists(request.getUsername())) {
            throw new BaseException(ErrorCode.USER_EXISTS);
        }
        if (isEmailExists(request.getEmail())) {
            throw new BaseException(ErrorCode.EMAIL_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getUsername());
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setCreatedAt(LocalDateTime.now());

        sysUserMapper.insert(user);
        log.info("User registered: username={}", request.getUsername());

        // 新用户默认个人工作空间（创建者自动以 admin 身份加入）
        workspaceService.createDefaultWorkspace(user.getId(), user.getUsername());

        return toUserResponse(user);
    }

    public LoginResponse loginByPassword(LoginRequest request) {
        SysUser user = findByUsername(request.getUsername());
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.isEnabled()) {
            throw new BaseException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BaseException(ErrorCode.PASSWORD_ERROR);
        }
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.update(user);

        return new LoginResponse(null, toUserResponse(user));
    }

    public SysUser findByUsername(String username) {
        return sysUserMapper.selectOneByQuery(
                QueryWrapper.create().eq(SysUser::getUsername, username)
        );
    }

    public SysUser findByEmail(String email) {
        return sysUserMapper.selectOneByQuery(
                QueryWrapper.create().eq(SysUser::getEmail, email)
        );
    }

    public SysUser findById(Long id) {
        return sysUserMapper.selectOneById(id);
    }

    private boolean isUsernameExists(String username) {
        return sysUserMapper.selectCountByQuery(
                new QueryWrapper().eq(SysUser::getUsername, username)
        ) > 0;
    }

    private boolean isEmailExists(String email) {
        return sysUserMapper.selectCountByQuery(
                new QueryWrapper().eq(SysUser::getEmail, email)
        ) > 0;
    }

    private static UserResponse toUserResponse(SysUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setStatus(user.getStatus());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
