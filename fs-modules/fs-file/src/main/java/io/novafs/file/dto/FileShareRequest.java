package io.novafs.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建分享请求
 */
@Data
public class FileShareRequest {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    /** 提取密码，可为空 */
    private String sharePwd;

    /** 过期时间，NULL 为永久 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;

    /** 查看次数上限 */
    private Integer maxViewCount;

    /** 下载次数上限 */
    private Integer maxDownloadCount;

    /** 权限范围，逗号分隔，默认 PREVIEW,DOWNLOAD */
    private String scope;
}
