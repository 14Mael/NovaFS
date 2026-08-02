package io.novafs.file.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 文件分享
 */
@Getter
@Setter
@Table("file_shares")
public class FileShare {

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long userId;

    private Long workspaceId;

    private Long fileId;

    private String shareCode;

    private String sharePwd;

    private LocalDateTime expireTime;

    private Integer viewCount;

    private Integer maxViewCount;

    private Integer downloadCount;

    private Integer maxDownloadCount;

    /** 权限范围，逗号分隔，如 PREVIEW,DOWNLOAD */
    private String scope;

    @Column(onInsertValue = "NOW()")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "NOW()", onUpdateValue = "NOW()")
    private LocalDateTime updatedAt;

    /** 是否已过期 */
    public boolean isExpired() {
        return expireTime != null && expireTime.isBefore(LocalDateTime.now());
    }

    /** 查看次数是否已达上限 */
    public boolean viewLimitReached() {
        return maxViewCount != null && viewCount != null && viewCount >= maxViewCount;
    }

    /** 下载次数是否已达上限 */
    public boolean downloadLimitReached() {
        return maxDownloadCount != null && downloadCount != null && downloadCount >= maxDownloadCount;
    }
}
