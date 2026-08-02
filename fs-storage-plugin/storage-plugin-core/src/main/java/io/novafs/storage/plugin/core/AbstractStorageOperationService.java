package io.novafs.storage.plugin.core;

import io.novafs.storage.plugin.core.exception.StoragePluginException;
import io.novafs.storage.plugin.core.model.FileUploadRequest;
import io.novafs.storage.plugin.core.model.StorageConfig;

/**
 * 存储操作抽象基类。
 * 封装公共逻辑（参数校验、对象键拼接），子类只需实现 doXxx 方法。
 */
public abstract class AbstractStorageOperationService implements IStorageOperationService {

    @Override
    public String uploadFile(StorageConfig config, FileUploadRequest request) throws StoragePluginException {
        validateConfig(config);
        String finalKey = buildObjectKey(config, request.getObjectKey());
        return doUpload(config, finalKey, request);
    }

    /** 子类实现：上传文件 */
    protected abstract String doUpload(StorageConfig config, String objectKey, FileUploadRequest request)
            throws StoragePluginException;

    /** 公共配置校验 */
    protected void validateConfig(StorageConfig config) {
        if (config == null) {
            throw new StoragePluginException("存储配置不能为空");
        }
    }

    /** 拼接基础路径前缀与对象键 */
    protected String buildObjectKey(StorageConfig config, String originalKey) {
        if (originalKey == null || originalKey.isBlank()) {
            throw new StoragePluginException("objectKey 不能为空");
        }
        String basePath = config.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return originalKey;
        }
        return basePath.replaceAll("/+$", "") + "/" + originalKey;
    }
}
