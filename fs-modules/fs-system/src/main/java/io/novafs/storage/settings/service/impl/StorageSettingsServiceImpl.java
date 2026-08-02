package io.novafs.storage.settings.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.storage.settings.dto.StorageSettingsResponse;
import io.novafs.storage.settings.entity.StorageSettings;
import io.novafs.storage.settings.mapper.StorageSettingsMapper;
import io.novafs.storage.settings.service.StorageSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 存储配置服务实现
 */
@Service
@RequiredArgsConstructor
public class StorageSettingsServiceImpl implements StorageSettingsService {

    private final StorageSettingsMapper settingsMapper;

    @Override
    public List<StorageSettingsResponse> listEnabled(Long workspaceId) {
        return settingsMapper.selectListByQuery(
                        QueryWrapper.create()
                                .where(StorageSettings::getWorkspaceId).eq(workspaceId)
                                .and(StorageSettings::getEnabled).eq(true)
                                .and(StorageSettings::getIsDeleted).eq(false))
                .stream()
                .map(StorageSettingsServiceImpl::toVO)
                .toList();
    }

    private static StorageSettingsResponse toVO(StorageSettings settings) {
        StorageSettingsResponse vo = new StorageSettingsResponse();
        vo.setId(settings.getId());
        vo.setPlatformIdentifier(settings.getPlatformIdentifier());
        // 敏感凭证（AK/SK 等）不下发浏览器，前端仅需 id + platformIdentifier
        vo.setConfigData(null);
        vo.setEnabled(settings.getEnabled());
        vo.setWorkspaceId(settings.getWorkspaceId());
        vo.setRemark(settings.getRemark());
        return vo;
    }
}
