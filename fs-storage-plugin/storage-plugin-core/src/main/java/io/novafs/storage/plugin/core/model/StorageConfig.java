package io.novafs.storage.plugin.core.model;

import lombok.Data;

/**
 * 存储配置，由数据库 JSON 字段反序列化而来
 */
@Data
public class StorageConfig {

    /** 服务端点 */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 密钥 */
    private String secretKey;

    /** 区域 */
    private String region;

    /** 存储桶名称 */
    private String bucketName;

    /** 基础路径前缀（本地存储为根目录） */
    private String basePath;

    /** 自定义访问域名 */
    private String domain;

    /** 是否为私有存储（需要签名访问） */
    private Boolean isPrivate;
}
