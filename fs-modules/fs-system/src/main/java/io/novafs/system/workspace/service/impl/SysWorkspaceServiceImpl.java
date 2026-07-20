package io.novafs.system.workspace.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.system.workspace.dto.CreateWorkspaceRequest;
import io.novafs.system.workspace.dto.UpdateWorkspaceRequest;
import io.novafs.system.workspace.dto.WorkspaceDetailResponse;
import io.novafs.system.workspace.dto.WorkspaceResponse;
import io.novafs.system.workspace.entity.SysWorkspace;
import io.novafs.system.workspace.mapper.SysWorkspaceMapper;
import io.novafs.system.workspace.service.SysWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 工作空间服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysWorkspaceServiceImpl implements SysWorkspaceService {

    private final SysWorkspaceMapper workspaceMapper;

    @Override
    public List<WorkspaceResponse> getWorkspacesByUser(Long userId) {
        // TODO: 待 SysWorkspaceMember 实体+Service就绪后，join查询用户加入的工作空间
        return Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceResponse createWorkspace(Long userId, CreateWorkspaceRequest request) {
        // 1. 校验 slug 唯一性
        if (workspaceMapper.selectCountByQuery(
                new QueryWrapper().eq(SysWorkspace::getSlug, request.getSlug())
        ) > 0) {
            throw new BaseException(ErrorCode.WORKSPACE_SLUG_EXISTS);
        }

        // 2. 创建实体
        SysWorkspace workspace = new SysWorkspace();
        workspace.setName(request.getName());
        workspace.setSlug(request.getSlug());
        workspace.setDescription(request.getDescription());
        workspace.setOwnerId(userId);
        workspace.setMemberCount(1);

        workspaceMapper.insert(workspace);
        log.info("Workspace created: name={}, slug={}, ownerId={}", request.getName(), request.getSlug(), userId);

        // TODO: 创建系统预设角色（admin/member/viewer）+ 创建者加入成员表
        return toWorkspaceResponse(workspace);
    }

    @Override
    public WorkspaceDetailResponse getDetail(Long workspaceId, Long userId) {
        SysWorkspace workspace = workspaceMapper.selectOneById(workspaceId);
        if (workspace == null) {
            throw new BaseException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        // TODO: 校验用户是否为工作空间成员
        // TODO: 查询角色和权限信息

        WorkspaceDetailResponse response = toDetailResponse(workspace);
        response.setRoleCode(null);
        response.setRoleName(null);
        response.setPermissions(null);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceResponse updateWorkspace(Long workspaceId, Long userId, UpdateWorkspaceRequest request) {
        SysWorkspace workspace = workspaceMapper.selectOneById(workspaceId);
        if (workspace == null) {
            throw new BaseException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        if (!workspace.isOwnedBy(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN);
        }

        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());
        workspaceMapper.update(workspace);
        log.info("Workspace updated: id={}, name={}", workspaceId, request.getName());

        return toWorkspaceResponse(workspace);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkspace(Long workspaceId, Long userId) {
        SysWorkspace workspace = workspaceMapper.selectOneById(workspaceId);
        if (workspace == null) {
            throw new BaseException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        if (!workspace.canBeDeletedBy(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN);
        }

        // TODO: 级联删除邀请->成员->角色权限->角色
        workspaceMapper.deleteById(workspaceId);
        log.info("Workspace deleted: id={}", workspaceId);
    }

    @Override
    public boolean checkSlug(String slug) {
        return workspaceMapper.selectCountByQuery(
                new QueryWrapper().eq(SysWorkspace::getSlug, slug)
        ) == 0;
    }

    // ========== 私有转换方法 ==========

    private static WorkspaceResponse toWorkspaceResponse(SysWorkspace workspace) {
        WorkspaceResponse response = new WorkspaceResponse();
        response.setId(workspace.getId());
        response.setName(workspace.getName());
        response.setSlug(workspace.getSlug());
        response.setDescription(workspace.getDescription());
        response.setOwnerId(workspace.getOwnerId());
        response.setMemberCount(workspace.getMemberCount());
        response.setCreatedAt(workspace.getCreatedAt());
        response.setUpdatedAt(workspace.getUpdatedAt());
        return response;
    }

    private static WorkspaceDetailResponse toDetailResponse(SysWorkspace workspace) {
        WorkspaceDetailResponse response = new WorkspaceDetailResponse();
        response.setId(workspace.getId());
        response.setName(workspace.getName());
        response.setSlug(workspace.getSlug());
        response.setDescription(workspace.getDescription());
        response.setOwnerId(workspace.getOwnerId());
        response.setMemberCount(workspace.getMemberCount());
        response.setCreatedAt(workspace.getCreatedAt());
        response.setUpdatedAt(workspace.getUpdatedAt());
        return response;
    }
}
