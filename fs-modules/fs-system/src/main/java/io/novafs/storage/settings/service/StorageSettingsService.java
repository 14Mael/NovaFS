package io.novafs.storage.settings.service;

import io.novafs.storage.settings.dto.StorageSettingsResponse;

import java.util.List;

/**
 * 存储配置服务（只读查询，供前端选择上传目标）
 */
public interface StorageSettingsService {

    /**
     * 查询指定工作空间下已启用的存储配置
     */
    List<StorageSettingsResponse> listEnabled(Long workspaceId);
}
