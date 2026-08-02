package io.novafs.storage.plugin.core.model;

import lombok.Data;

import java.io.InputStream;

/**
 * 文件上传请求
 */
@Data
public class FileUploadRequest {

    /** 存储上的对象键 */
    private String objectKey;

    /** 文件内容流 */
    private InputStream inputStream;

    /** 文件大小 */
    private Long fileSize;

    /** 内容类型 */
    private String contentType;
}
