package io.novafs.storage.plugin.core.exception;

/**
 * 不支持的存储平台异常
 */
public class UnsupportedStoragePlatformException extends StoragePluginException {

    public UnsupportedStoragePlatformException(String platformType) {
        super("不支持的存储平台: " + platformType);
    }
}
