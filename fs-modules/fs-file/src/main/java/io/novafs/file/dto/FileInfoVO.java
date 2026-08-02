package io.novafs.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息视图对象
 */
@Data
public class FileInfoVO {

    private Long id;

    private Long workspaceId;

    private Long userId;

    private Long parentId;

    private String originalName;

    private String displayName;

    private String suffix;

    private Long size;

    private String mimeType;

    private Boolean isDir;

    private String contentMd5;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    private Boolean isDeleted;
}
