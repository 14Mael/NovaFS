package io.novafs.system.workspace.entity;

import com.mybatisflex.annotation.Table;
import io.novafs.framework.orm.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * 工作空间
 */
@Getter
@Setter
@Table("sys_workspace")
public class SysWorkspace extends BaseEntity {

    private String name;

    private String slug;

    private String description;

    private Long ownerId;

    private Integer memberCount;

    /** 判断是否被指定用户拥有 */
    public boolean isOwnedBy(Long userId) {
        return Objects.equals(this.ownerId, userId);
    }

    /** 判断是否可被指定用户删除（仅所有者） */
    public boolean canBeDeletedBy(Long userId) {
        return isOwnedBy(userId);
    }
}
