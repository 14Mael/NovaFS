package io.novafs.storage.plugin.core.exception;

/**
 * 存储插件异常
 */
public class StoragePluginException extends RuntimeException {

    public StoragePluginException(String message) {
        super(message);
    }

    public StoragePluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
