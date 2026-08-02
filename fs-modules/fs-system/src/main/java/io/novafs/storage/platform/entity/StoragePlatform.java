package io.novafs.storage.platform.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 存储平台定义
 */
@Getter
@Setter
@Table("storage_platform")
public class StoragePlatform {

    @Id(keyType = KeyType.Auto)
    private Integer id;

    private String name;

    private String identifier;

    private String configScheme;

    private String icon;

    private String link;

    private Boolean isDefault;

    private String description;

    @Column(onInsertValue = "NOW()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;
}
