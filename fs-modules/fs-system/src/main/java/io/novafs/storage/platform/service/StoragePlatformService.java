package io.novafs.storage.platform.service;

import io.novafs.storage.platform.dto.StoragePlatformResponse;

import java.util.List;

/**
 * 存储平台查询服务
 */
public interface StoragePlatformService {

    /**
     * 全部存储平台（按默认优先排序）
     */
    List<StoragePlatformResponse> listAll();
}
