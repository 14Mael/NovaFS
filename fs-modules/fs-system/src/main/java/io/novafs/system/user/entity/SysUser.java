package io.novafs.system.user.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import io.novafs.framework.orm.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Getter
@Setter
@Table("sys_user")
public class SysUser extends BaseEntity {

    @Column("username")
    private String username;

    @Column("password")
    private String password;

    @Column("email")
    private String email;

    @Column("nickname")
    private String nickname;

    @Column("avatar")
    private String avatar;

    @Column("status")
    private Integer status;

    @Column("last_login_at")
    private LocalDateTime lastLoginAt;

    public boolean isEnabled() {
        return status == null || status == 0;
    }
}
