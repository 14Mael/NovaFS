package io.novafs.storage.settings.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.framework.common.util.JsonUtils;
import io.novafs.storage.platform.mapper.StoragePlatformMapper;
import io.novafs.storage.settings.dto.CreateStorageRequest;
import io.novafs.storage.settings.dto.StorageSettingsResponse;
import io.novafs.storage.settings.dto.UpdateStorageRequest;
import io.novafs.storage.settings.entity.StorageSettings;
import io.novafs.storage.settings.mapper.StorageSettingsMapper;
import io.novafs.storage.settings.service.StorageSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 存储配置服务实现
 */
@Service
@RequiredArgsConstructor
public class StorageSettingsServiceImpl implements StorageSettingsService {

    private final StorageSettingsMapper settingsMapper;
    private final StoragePlatformMapper platformMapper;

    @Override
    public List<StorageSettingsResponse> listEnabled(Long workspaceId) {
        return settingsMapper.selectListByQuery(enabledQuery(workspaceId))
                .stream()
                .map(s -> toVO(s, false))
                .toList();
    }

    @Override
    public List<StorageSettingsResponse> listAdmin(Long workspaceId) {
        return settingsMapper.selectListByQuery(
                        QueryWrapper.create()
                                .where(StorageSettings::getWorkspaceId).eq(workspaceId)
                                .and(StorageSettings::getIsDeleted).eq(false)
                                .orderBy(StorageSettings::getId, false))
                .stream()
                .map(s -> toVO(s, true))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StorageSettingsResponse create(Long workspaceId, CreateStorageRequest request) {
        requirePlatformExists(request.getPlatformIdentifier());
        validateJson(request.getConfigData());

        StorageSettings settings = new StorageSettings();
        settings.setWorkspaceId(workspaceId);
        settings.setPlatformIdentifier(request.getPlatformIdentifier());
        settings.setConfigData(request.getConfigData());
        settings.setEnabled(request.getEnabled() == null || request.getEnabled());
        settings.setRemark(request.getRemark());
        settings.setIsDeleted(false);
        settingsMapper.insert(settings);
        return toVO(settings, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StorageSettingsResponse update(Long id, UpdateStorageRequest request) {
        StorageSettings settings = settingsMapper.selectOneById(id);
        if (settings == null || Boolean.TRUE.equals(settings.getIsDeleted())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "存储配置不存在");
        }
        if (request.getConfigData() != null && !request.getConfigData().isBlank()) {
            validateJson(request.getConfigData());
            settings.setConfigData(request.getConfigData());
        }
        if (request.getEnabled() != null) {
            settings.setEnabled(request.getEnabled());
        }
        if (request.getRemark() != null) {
            settings.setRemark(request.getRemark());
        }
        settingsMapper.update(settings);
        return toVO(settings, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        StorageSettings settings = settingsMapper.selectOneById(id);
        if (settings == null || Boolean.TRUE.equals(settings.getIsDeleted())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "存储配置不存在");
        }
        settings.setIsDeleted(true);
        settingsMapper.update(settings);
    }

    private QueryWrapper enabledQuery(Long workspaceId) {
        return QueryWrapper.create()
                .where(StorageSettings::getWorkspaceId).eq(workspaceId)
                .and(StorageSettings::getEnabled).eq(true)
                .and(StorageSettings::getIsDeleted).eq(false);
    }

    private void requirePlatformExists(String identifier) {
        if (platformMapper.selectCountByQuery(
                QueryWrapper.create().where(io.novafs.storage.platform.entity.StoragePlatform::getIdentifier)
                        .eq(identifier)) == 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "存储平台不存在: " + identifier);
        }
    }

    private void validateJson(String configData) {
        try {
            JsonUtils.parse(configData, Map.class);
        } catch (Exception e) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "配置数据必须是合法 JSON");
        }
    }

    private static StorageSettingsResponse toVO(StorageSettings settings, boolean withConfigData) {
        StorageSettingsResponse vo = new StorageSettingsResponse();
        vo.setId(settings.getId());
        vo.setPlatformIdentifier(settings.getPlatformIdentifier());
        // 敏感凭证（AK/SK 等）默认不下发浏览器，仅管理页回显时携带
        vo.setConfigData(withConfigData ? settings.getConfigData() : null);
        vo.setEnabled(settings.getEnabled());
        vo.setWorkspaceId(settings.getWorkspaceId());
        vo.setRemark(settings.getRemark());
        return vo;
    }
}
