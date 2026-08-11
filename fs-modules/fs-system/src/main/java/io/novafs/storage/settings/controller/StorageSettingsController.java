package io.novafs.storage.settings.controller;

import io.novafs.framework.common.model.Result;
import io.novafs.storage.platform.dto.StoragePlatformResponse;
import io.novafs.storage.platform.service.StoragePlatformService;
import io.novafs.storage.settings.dto.CreateStorageRequest;
import io.novafs.storage.settings.dto.StorageSettingsResponse;
import io.novafs.storage.settings.dto.UpdateStorageRequest;
import io.novafs.storage.settings.service.StorageSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 存储配置控制器（查询 + 管理）
 * <p>上传接口需要 storagePlatformSettingId，前端通过本接口获取与管理工作空间存储配置。</p>
 */
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageSettingsController {

    private final StorageSettingsService storageSettingsService;
    private final StoragePlatformService storagePlatformService;

    /** 查询工作空间下已启用的存储配置（脱敏，供上传选择） */
    @GetMapping("/settings")
    public Result<List<StorageSettingsResponse>> listEnabled(@RequestParam Long workspaceId) {
        return Result.ok(storageSettingsService.listEnabled(workspaceId));
    }

    /** 查询工作空间下全部配置（含 configData，供管理页编辑回显） */
    @GetMapping("/settings/admin")
    public Result<List<StorageSettingsResponse>> listAdmin(@RequestParam Long workspaceId) {
        return Result.ok(storageSettingsService.listAdmin(workspaceId));
    }

    /** 存储平台列表（创建配置时选择平台） */
    @GetMapping("/platforms")
    public Result<List<StoragePlatformResponse>> listPlatforms() {
        return Result.ok(storagePlatformService.listAll());
    }

    /** 创建存储配置 */
    @PostMapping("/settings")
    public Result<StorageSettingsResponse> create(@RequestParam Long workspaceId,
                                                  @Valid @RequestBody CreateStorageRequest request) {
        return Result.ok(storageSettingsService.create(workspaceId, request));
    }

    /** 更新存储配置（configData/enabled/remark 为空表示保留原值） */
    @PutMapping("/settings/{id}")
    public Result<StorageSettingsResponse> update(@PathVariable Long id,
                                                  @RequestBody UpdateStorageRequest request) {
        return Result.ok(storageSettingsService.update(id, request));
    }

    /** 删除存储配置（逻辑删除，历史文件仍可访问） */
    @DeleteMapping("/settings/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        storageSettingsService.remove(id);
        return Result.ok();
    }
}
