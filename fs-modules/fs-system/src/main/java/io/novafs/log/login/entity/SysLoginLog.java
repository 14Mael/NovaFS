package io.novafs.log.login.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 登录日志
 */
@Getter
@Setter
@Table("sys_login_log")
public class SysLoginLog {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private String username;

    private String loginIp;

    private String loginAddress;

    private String browser;

    private String os;

    private String loginType;

    private Integer status;

    private String msg;

    private LocalDateTime loginTime;
}
