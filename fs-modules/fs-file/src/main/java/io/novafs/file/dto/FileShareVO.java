package io.novafs.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分享信息视图对象
 */
@Data
public class FileShareVO {

    private Long id;

    private Long fileId;

    private String fileName;

    private String suffix;

    private Long fileSize;

    /** 分享码 */
    private String shareCode;

    private boolean hasPassword;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    private Integer viewCount;

    private Integer downloadCount;

    private String scope;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
