package io.novafs.storage.settings.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import io.novafs.framework.orm.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 存储配置
 */
@Getter
@Setter
@Table("storage_settings")
public class StorageSettings extends BaseEntity {

    private String platformIdentifier;

    private String configData;

    private Boolean enabled;

    private Long workspaceId;

    private String remark;

    @Column("is_deleted")
    private Boolean isDeleted;
}
