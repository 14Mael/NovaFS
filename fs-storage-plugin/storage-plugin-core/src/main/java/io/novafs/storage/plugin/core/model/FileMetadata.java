package io.novafs.storage.plugin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * 文件元信息
 */
@Data
public class FileMetadata {

    /** 对象键 */
    private String objectKey;

    /** 文件大小 */
    private Long fileSize;

    /** 内容类型 */
    private String contentType;

    /** ETag */
    private String etag;

    /** 最后修改时间 */
    private Date lastModified;
}
