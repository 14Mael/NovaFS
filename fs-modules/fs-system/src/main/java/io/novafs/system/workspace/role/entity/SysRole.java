package io.novafs.system.workspace.role.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 角色
 */
@Getter
@Setter
@Table("sys_role")
public class SysRole {

    @Id(keyType = KeyType.Auto)
    private Integer id;

    private Long workspaceId;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer roleType;

    @Column(onInsertValue = "NOW()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;
}
