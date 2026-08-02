package io.novafs.system.workspace.invitation.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import io.novafs.framework.orm.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工作空间邀请
 */
@Getter
@Setter
@Table("sys_workspace_invitation")
public class SysWorkspaceInvitation extends BaseEntity {

    private Long workspaceId;

    private String email;

    @Column("role_id")
    private Integer roleId;

    private Long invitedBy;

    private String token;

    private Integer status;

    private LocalDateTime expiresAt;

    private LocalDateTime acceptedAt;
}
