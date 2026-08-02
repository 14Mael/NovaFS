package io.novafs.storage.plugin.boot;

import io.novafs.storage.plugin.core.IStorageOperationService;
import io.novafs.storage.plugin.core.model.ChunkInitRequest;
import io.novafs.storage.plugin.core.model.ChunkMergeRequest;
import io.novafs.storage.plugin.core.model.ChunkUploadRequest;
import io.novafs.storage.plugin.core.model.FileItem;
import io.novafs.storage.plugin.core.model.FileMetadata;
import io.novafs.storage.plugin.core.model.FileUploadRequest;
import io.novafs.storage.plugin.core.model.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * 存储服务门面：业务侧统一入口，按平台类型委派到对应插件。
 */
@Service
@RequiredArgsConstructor
public class StorageServiceFacade {

    private final StoragePluginRegistry pluginRegistry;

    public String uploadFile(String platformType, StorageConfig config, FileUploadRequest request) {
        return getPlugin(platformType).uploadFile(config, request);
    }

    public InputStream downloadFile(String platformType, StorageConfig config, String objectKey) {
        return getPlugin(platformType).downloadFile(config, objectKey);
    }

    public void deleteFile(String platformType, StorageConfig config, String objectKey) {
        getPlugin(platformType).deleteFile(config, objectKey);
    }

    public void renameFile(String platformType, StorageConfig config, String oldKey, String newKey) {
        getPlugin(platformType).renameFile(config, oldKey, newKey);
    }

    public void copyFile(String platformType, StorageConfig config, String sourceKey, String destKey) {
        getPlugin(platformType).copyFile(config, sourceKey, destKey);
    }

    public FileMetadata getFileMetadata(String platformType, StorageConfig config, String objectKey) {
        return getPlugin(platformType).getFileMetadata(config, objectKey);
    }

    public List<FileItem> listFiles(String platformType, StorageConfig config, String prefix) {
        return getPlugin(platformType).listFiles(config, prefix);
    }

    public String generatePresignedUrl(String platformType, StorageConfig config, String objectKey, long expirySeconds) {
        return getPlugin(platformType).generatePresignedUrl(config, objectKey, expirySeconds);
    }

    public String initChunkUpload(String platformType, StorageConfig config, ChunkInitRequest request) {
        return getPlugin(platformType).initChunkUpload(config, request);
    }

    public void uploadChunk(String platformType, StorageConfig config, ChunkUploadRequest request) {
        getPlugin(platformType).uploadChunk(config, request);
    }

    public String mergeChunks(String platformType, StorageConfig config, ChunkMergeRequest request) {
        return getPlugin(platformType).mergeChunks(config, request);
    }

    private IStorageOperationService getPlugin(String platformType) {
        return pluginRegistry.getPlugin(platformType);
    }
}
