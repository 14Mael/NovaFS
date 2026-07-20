package io.novafs.system.user.entity;

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

    private String username;

    private String password;

    private String email;

    private String nickname;

    private String avatar;

    private Integer status;

    private LocalDateTime lastLoginAt;

    public boolean isEnabled() {
        return status == null || status == 0;
    }
}
