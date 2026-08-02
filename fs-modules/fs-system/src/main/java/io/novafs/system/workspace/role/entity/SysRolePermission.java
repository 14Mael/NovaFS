package io.novafs.system.workspace.role.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色权限关联
 */
@Getter
@Setter
@Table("sys_role_permission")
public class SysRolePermission {

    @Id(keyType = KeyType.Auto)
    private Integer id;

    private Integer roleId;

    private String permissionCode;
}
