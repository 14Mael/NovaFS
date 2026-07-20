package io.novafs.system.workspace.service;

import io.novafs.system.workspace.dto.CreateWorkspaceRequest;
import io.novafs.system.workspace.dto.UpdateWorkspaceRequest;
import io.novafs.system.workspace.dto.WorkspaceDetailResponse;
import io.novafs.system.workspace.dto.WorkspaceResponse;

import java.util.List;

/**
 * 工作空间服务
 */
public interface SysWorkspaceService {

    /**
     * 查询用户的全部工作空间
     */
    List<WorkspaceResponse> getWorkspacesByUser(Long userId);

    /**
     * 创建工作空间
     */
    WorkspaceResponse createWorkspace(Long userId, CreateWorkspaceRequest request);

    /**
     * 获取工作空间详情
     */
    WorkspaceDetailResponse getDetail(Long workspaceId, Long userId);

    /**
     * 更新工作空间
     */
    WorkspaceResponse updateWorkspace(Long workspaceId, Long userId, UpdateWorkspaceRequest request);

    /**
     * 删除工作空间
     */
    void deleteWorkspace(Long workspaceId, Long userId);

    /**
     * 检查 slug 是否可用
     */
    boolean checkSlug(String slug);
}
