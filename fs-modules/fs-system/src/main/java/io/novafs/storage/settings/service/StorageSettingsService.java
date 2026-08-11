package io.novafs.storage.settings.service;

import io.novafs.storage.settings.dto.CreateStorageRequest;
import io.novafs.storage.settings.dto.StorageSettingsResponse;
import io.novafs.storage.settings.dto.UpdateStorageRequest;

import java.util.List;

/**
 * 存储配置服务（查询 + 管理）
 */
public interface StorageSettingsService {

    /**
     * 查询指定工作空间下已启用的存储配置（脱敏，供上传选择）
     */
    List<StorageSettingsResponse> listEnabled(Long workspaceId);

    /**
     * 查询指定工作空间下的全部配置（含 configData，供管理页编辑回显）
     */
    List<StorageSettingsResponse> listAdmin(Long workspaceId);

    /**
     * 创建存储配置
     */
    StorageSettingsResponse create(Long workspaceId, CreateStorageRequest request);

    /**
     * 更新存储配置（configData/enabled/remark 为空表示保留原值）
     */
    StorageSettingsResponse update(Long id, UpdateStorageRequest request);

    /**
     * 删除存储配置（逻辑删除，历史文件仍可访问）
     */
    void remove(Long id);
}
