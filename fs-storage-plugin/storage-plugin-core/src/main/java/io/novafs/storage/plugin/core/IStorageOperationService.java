package io.novafs.storage.plugin.core;

import io.novafs.storage.plugin.core.exception.StoragePluginException;
import io.novafs.storage.plugin.core.model.ChunkInitRequest;
import io.novafs.storage.plugin.core.model.ChunkMergeRequest;
import io.novafs.storage.plugin.core.model.ChunkUploadRequest;
import io.novafs.storage.plugin.core.model.FileItem;
import io.novafs.storage.plugin.core.model.FileMetadata;
import io.novafs.storage.plugin.core.model.FileUploadRequest;
import io.novafs.storage.plugin.core.model.StorageConfig;

import java.io.InputStream;
import java.util.List;

/**
 * 存储操作 SPI 接口。
 * 每个存储平台（本地、MinIO、OSS 等）实现该接口，并通过 SPI 或 Spring Bean 注册。
 */
public interface IStorageOperationService {

    // ===== 文件操作 =====

    /** 上传文件，返回存储上的 objectKey */
    String uploadFile(StorageConfig config, FileUploadRequest request) throws StoragePluginException;

    /** 下载文件 */
    InputStream downloadFile(StorageConfig config, String objectKey) throws StoragePluginException;

    /** 删除文件 */
    void deleteFile(StorageConfig config, String objectKey) throws StoragePluginException;

    /** 重命名文件 */
    void renameFile(StorageConfig config, String oldKey, String newKey) throws StoragePluginException;

    /** 复制文件 */
    void copyFile(StorageConfig config, String sourceKey, String destKey) throws StoragePluginException;

    /** 获取文件元信息 */
    FileMetadata getFileMetadata(StorageConfig config, String objectKey) throws StoragePluginException;

    /** 列出指定前缀下的文件 */
    List<FileItem> listFiles(StorageConfig config, String prefix) throws StoragePluginException;

    /** 生成预签名 URL（私有存储需要签名，公开存储可直接返回访问地址） */
    String generatePresignedUrl(StorageConfig config, String objectKey, long expirySeconds)
            throws StoragePluginException;

    // ===== 分片上传 =====

    /** 分片上传初始化，返回 uploadId */
    String initChunkUpload(StorageConfig config, ChunkInitRequest request) throws StoragePluginException;

    /** 上传单个分片 */
    void uploadChunk(StorageConfig config, ChunkUploadRequest request) throws StoragePluginException;

    /** 合并分片，返回最终 objectKey */
    String mergeChunks(StorageConfig config, ChunkMergeRequest request) throws StoragePluginException;

    /** 获取平台类型标识（如 local、minio、aliyunoss） */
    String getPlatformType();
}
