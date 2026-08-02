package io.novafs.system.workspace.member.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 工作空间成员
 */
@Getter
@Setter
@Table("sys_workspace_member")
public class SysWorkspaceMember {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long workspaceId;

    private Long userId;

    private Integer roleId;

    @Column(onInsertValue = "NOW()")
    private LocalDateTime joinedAt;

    @Column(onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;
}
