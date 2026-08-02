package io.novafs.storage.settings.controller;

import io.novafs.framework.common.model.Result;
import io.novafs.storage.settings.dto.StorageSettingsResponse;
import io.novafs.storage.settings.service.StorageSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 存储配置控制器（只读）
 * <p>上传接口需要 storagePlatformSettingId，前端通过此接口获取当前工作空间已启用的配置。</p>
 */
@RestController
@RequestMapping("/api/storage/settings")
@RequiredArgsConstructor
public class StorageSettingsController {

    private final StorageSettingsService storageSettingsService;

    /** 查询工作空间下已启用的存储配置 */
    @GetMapping
    public Result<List<StorageSettingsResponse>> listEnabled(@RequestParam Long workspaceId) {
        return Result.ok(storageSettingsService.listEnabled(workspaceId));
    }
}
