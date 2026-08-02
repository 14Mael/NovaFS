package io.novafs.system.workspace.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import io.novafs.framework.common.model.Result;
import io.novafs.system.user.service.SysUserService;
import io.novafs.system.workspace.dto.CreateWorkspaceRequest;
import io.novafs.system.workspace.dto.UpdateWorkspaceRequest;
import io.novafs.system.workspace.dto.WorkspaceDetailResponse;
import io.novafs.system.workspace.dto.WorkspaceResponse;
import io.novafs.system.workspace.service.SysWorkspaceService;
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
 * 工作空间控制器
 */
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final SysWorkspaceService workspaceService;
    private final SysUserService sysUserService;

    /**
     * 查询当前用户的工作空间列表
     */
    @GetMapping
    public Result<List<WorkspaceResponse>> list() {
        return Result.ok(workspaceService.getWorkspacesByUser(currentUserId()));
    }

    /**
     * 检查 slug 是否可用
     */
    @GetMapping("/slug/check")
    public Result<Boolean> checkSlug(@RequestParam String slug) {
        return Result.ok(workspaceService.checkSlug(slug));
    }

    /**
     * 工作空间详情（含当前用户角色与权限）
     */
    @GetMapping("/{workspaceId}")
    public Result<WorkspaceDetailResponse> detail(@PathVariable Long workspaceId) {
        return Result.ok(workspaceService.getDetail(workspaceId, currentUserId()));
    }

    /**
     * 创建工作空间
     */
    @PostMapping
    public Result<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return Result.ok(workspaceService.createWorkspace(currentUserId(), request));
    }

    /**
     * 更新工作空间（需 member:manage 权限）
     */
    @PutMapping("/{workspaceId}")
    @SaCheckPermission("member:manage")
    public Result<WorkspaceResponse> update(@PathVariable Long workspaceId,
                                            @Valid @RequestBody UpdateWorkspaceRequest request) {
        return Result.ok(workspaceService.updateWorkspace(workspaceId, currentUserId(), request));
    }

    /**
     * 删除工作空间（需 member:manage 权限）
     */
    @DeleteMapping("/{workspaceId}")
    @SaCheckPermission("member:manage")
    public Result<Void> delete(@PathVariable Long workspaceId) {
        workspaceService.deleteWorkspace(workspaceId, currentUserId());
        return Result.ok();
    }

    private Long currentUserId() {
        return sysUserService.findByUsername(StpUtil.getLoginIdAsString()).getId();
    }
}
