package io.novafs.system.user.service;

import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.system.user.entity.SysUser;
import io.novafs.system.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mybatisflex.core.query.QueryWrapper;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public SysUser register(String username, String password, String email) {
        if (isUsernameExists(username)) {
            throw new BaseException(ErrorCode.USER_EXISTS);
        }
        if (isEmailExists(email)) {
            throw new BaseException(ErrorCode.EMAIL_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setNickname(username);
        user.setStatus(0);
        user.setCreatedAt(LocalDateTime.now());

        sysUserMapper.insert(user);
        log.info("User registered: username={}", username);
        return user;
    }

    public SysUser loginByPassword(String username, String rawPassword) {
        SysUser user = findByUsername(username);
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.isEnabled()) {
            throw new BaseException(ErrorCode.USER_DISABLED);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BaseException(ErrorCode.PASSWORD_ERROR);
        }
        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.update(user);
        return user;
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
                new com.mybatisflex.core.query.QueryWrapper()
                        .eq(SysUser::getUsername, username)
        ) > 0;
    }

    private boolean isEmailExists(String email) {
        return sysUserMapper.selectCountByQuery(
                new com.mybatisflex.core.query.QueryWrapper()
                        .eq(SysUser::getEmail, email)
        ) > 0;
    }
}

