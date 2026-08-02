package io.novafs.file.storage;

import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.framework.common.util.JsonUtils;
import io.novafs.storage.plugin.core.model.StorageConfig;
import io.novafs.storage.settings.entity.StorageSettings;
import io.novafs.storage.settings.mapper.StorageSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 存储配置解析：将 storage_settings 配置解析为插件可用的 StorageConfig + 平台类型
 */
@Component
@RequiredArgsConstructor
public class StorageConfigResolver {

    private final StorageSettingsMapper settingsMapper;

    /**
     * 按配置 ID 解析存储目标
     */
    public StorageTarget resolve(Long settingsId) {
        StorageSettings settings = settingsId == null ? null : settingsMapper.selectOneById(settingsId);
        if (settings == null || !Boolean.TRUE.equals(settings.getEnabled())) {
            throw new BaseException(ErrorCode.STORAGE_CONFIG_ERROR, "存储配置不存在或未启用: " + settingsId);
        }
        StorageConfig config = JsonUtils.parse(settings.getConfigData(), StorageConfig.class);
        if (config == null) {
            throw new BaseException(ErrorCode.STORAGE_CONFIG_ERROR, "存储配置解析失败: " + settingsId);
        }
        return new StorageTarget(settings.getPlatformIdentifier(), config);
    }

    /**
     * 存储目标：平台类型 + 配置
     */
    public record StorageTarget(String platformType, StorageConfig config) {
    }
}
