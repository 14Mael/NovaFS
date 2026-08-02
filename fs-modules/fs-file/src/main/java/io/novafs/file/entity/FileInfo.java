package io.novafs.file.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import io.novafs.framework.orm.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 文件资源（充血模型）
 */
@Getter
@Setter
@Table("file_info")
public class FileInfo extends BaseEntity {

    private Long workspaceId;

    private Long userId;

    private Long parentId;

    private String originalName;

    private String displayName;

    private String suffix;

    private Long size;

    private String mimeType;

    @Column("is_dir")
    private Boolean isDir;

    private String objectKey;

    private String contentMd5;

    private Long storagePlatformSettingId;

    private LocalDateTime uploadTime;

    @Column("is_deleted")
    private Boolean isDeleted;

    private LocalDateTime deletedTime;

    /** 是否为文件夹 */
    public boolean isDirectory() {
        return Boolean.TRUE.equals(isDir);
    }

    /** 是否已删除（回收站） */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(isDeleted);
    }

    /** 是否可被指定用户删除（上传者本人） */
    public boolean canBeDeletedBy(Long userId) {
        return Objects.equals(this.userId, userId);
    }
}
