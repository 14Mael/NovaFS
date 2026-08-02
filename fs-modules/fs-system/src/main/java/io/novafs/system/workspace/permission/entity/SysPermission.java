package io.novafs.system.workspace.permission.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 权限
 */
@Getter
@Setter
@Table("sys_permission")
public class SysPermission {

    @Id(keyType = KeyType.Auto)
    private Integer id;

    private String permissionCode;

    private String permissionName;

    private String module;

    private String description;

    private Integer sort;

    @Column(onInsertValue = "NOW()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;
}
