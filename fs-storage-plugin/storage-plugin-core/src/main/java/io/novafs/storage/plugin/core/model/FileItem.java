package io.novafs.storage.plugin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * 文件列表项
 */
@Data
public class FileItem {

    /** 对象键 */
    private String objectKey;

    /** 文件大小 */
    private Long fileSize;

    /** 是否为目录 */
    private Boolean isDir;

    /** 最后修改时间 */
    private Date lastModified;
}
