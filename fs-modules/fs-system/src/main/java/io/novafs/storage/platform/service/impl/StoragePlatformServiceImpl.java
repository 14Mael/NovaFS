package io.novafs.storage.platform.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.storage.platform.dto.StoragePlatformResponse;
import io.novafs.storage.platform.entity.StoragePlatform;
import io.novafs.storage.platform.mapper.StoragePlatformMapper;
import io.novafs.storage.platform.service.StoragePlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 存储平台查询服务实现
 */
@Service
@RequiredArgsConstructor
public class StoragePlatformServiceImpl implements StoragePlatformService {

    private final StoragePlatformMapper platformMapper;

    @Override
    public List<StoragePlatformResponse> listAll() {
        return platformMapper.selectListByQuery(
                        QueryWrapper.create()
                                .orderBy(StoragePlatform::getIsDefault, false)
                                .orderBy(StoragePlatform::getId, true))
                .stream()
                .map(StoragePlatformServiceImpl::toVO)
                .toList();
    }

    private static StoragePlatformResponse toVO(StoragePlatform platform) {
        StoragePlatformResponse vo = new StoragePlatformResponse();
        vo.setId(platform.getId());
        vo.setName(platform.getName());
        vo.setIdentifier(platform.getIdentifier());
        vo.setIcon(platform.getIcon());
        vo.setIsDefault(platform.getIsDefault());
        vo.setDescription(platform.getDescription());
        return vo;
    }
}
