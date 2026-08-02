package io.novafs.system.workspace.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.system.workspace.dto.CreateWorkspaceRequest;
import io.novafs.system.workspace.dto.UpdateWorkspaceRequest;
import io.novafs.system.workspace.dto.WorkspaceDetailResponse;
import io.novafs.system.workspace.dto.WorkspaceResponse;
import io.novafs.system.workspace.entity.SysWorkspace;
import io.novafs.system.workspace.invitation.entity.SysWorkspaceInvitation;
import io.novafs.system.workspace.invitation.mapper.SysWorkspaceInvitationMapper;
import io.novafs.system.workspace.mapper.SysWorkspaceMapper;
import io.novafs.system.workspace.member.entity.SysWorkspaceMember;
import io.novafs.system.workspace.member.mapper.SysWorkspaceMemberMapper;
import io.novafs.system.workspace.role.entity.SysRole;
import io.novafs.system.workspace.role.entity.SysRolePermission;
import io.novafs.system.workspace.role.mapper.SysRoleMapper;
import io.novafs.system.workspace.role.mapper.SysRolePermissionMapper;
import io.novafs.system.workspace.service.SysWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 工作空间服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysWorkspaceServiceImpl implements SysWorkspaceService {

    /** 预设角色编码 */
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_MEMBER = "member";
    private static final String ROLE_VIEWER = "viewer";

    /** 预设角色权限 */
    private static final List<String> ADMIN_PERMISSIONS =
            List.of("file:read", "file:write", "file:share", "storage:manage", "member:manage");
    private static final List<String> MEMBER_PERMISSIONS =
            List.of("file:read", "file:write", "file:share");
    private static final List<String> VIEWER_PERMISSIONS =
            List.of("file:read");

    private final SysWorkspaceMapper workspaceMapper;
    private final SysWorkspaceMemberMapper memberMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysWorkspaceInvitationMapper invitationMapper;

    @Override
    public List<WorkspaceResponse> getWorkspacesByUser(Long userId) {
        List<Long> workspaceIds = memberMapper.selectListByQuery(
                        QueryWrapper.create().where(SysWorkspaceMember::getUserId).eq(userId))
                .stream().map(SysWorkspaceMember::getWorkspaceId).toList();
        if (workspaceIds.isEmpty()) {
            return List.of();
        }
        return workspaceMapper.selectListByQuery(
                        QueryWrapper.create().where(SysWorkspace::getId).in(workspaceIds)
                                .orderBy(SysWorkspace::getCreatedAt, false))
                .stream().map(SysWorkspaceServiceImpl::toWorkspaceResponse).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceResponse createWorkspace(Long userId, CreateWorkspaceRequest request) {
        if (workspaceMapper.selectCountByQuery(
                new QueryWrapper().eq(SysWorkspace::getSlug, request.getSlug())
        ) > 0) {
            throw new BaseException(ErrorCode.WORKSPACE_SLUG_EXISTS);
        }
        SysWorkspace workspace = new SysWorkspace();
        workspace.setName(request.getName());
        workspace.setSlug(request.getSlug());
        workspace.setDescription(request.getDescription());
        workspace.setOwnerId(userId);
        workspace.setMemberCount(1);
        workspaceMapper.insert(workspace);

        initPresetRolesAndOwner(workspace.getId(), userId);
        log.info("Workspace created: name={}, slug={}, ownerId={}", request.getName(), request.getSlug(), userId);

        return toWorkspaceResponse(workspace);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceResponse createDefaultWorkspace(Long userId, String username) {
        String base = username.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "");
        if (base.isBlank()) {
            base = "ws";
        }
        String slug = base;
        int suffix = 1;
        while (workspaceMapper.selectCountByQuery(
                new QueryWrapper().eq(SysWorkspace::getSlug, slug)) > 0) {
            slug = base + "-" + suffix++;
        }

        CreateWorkspaceRequest request = new CreateWorkspaceRequest();
        request.setName(username + " 的空间");
        request.setSlug(slug);
        return createWorkspace(userId, request);
    }

    @Override
    public WorkspaceDetailResponse getDetail(Long workspaceId, Long userId) {
        SysWorkspace workspace = workspaceMapper.selectOneById(workspaceId);
        if (workspace == null) {
            throw new BaseException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        SysWorkspaceMember member = memberMapper.selectOneByQuery(
                QueryWrapper.create().where(SysWorkspaceMember::getWorkspaceId).eq(workspaceId)
                        .and(SysWorkspaceMember::getUserId).eq(userId));
        if (member == null) {
            throw new BaseException(ErrorCode.NOT_WORKSPACE_MEMBER);
        }

        WorkspaceDetailResponse response = toDetailResponse(workspace);
        SysRole role = roleMapper.selectOneById(member.getRoleId());
        if (role != null) {
            response.setRoleCode(role.getRoleCode());
            response.setRoleName(role.getRoleName());
        }
        response.setPermissions(loadPermissions(member.getRoleId()));
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

        List<Integer> roleIds = findRoleIds(workspaceId);
        invitationMapper.deleteByQuery(
                QueryWrapper.create().where(SysWorkspaceInvitation::getWorkspaceId).eq(workspaceId));
        memberMapper.deleteByQuery(
                QueryWrapper.create().where(SysWorkspaceMember::getWorkspaceId).eq(workspaceId));
        deleteRolesAndPermissions(roleIds);
        workspaceMapper.deleteById(workspaceId);
        log.info("Workspace deleted: id={}", workspaceId);
    }

    @Override
    public boolean checkSlug(String slug) {
        return workspaceMapper.selectCountByQuery(
                new QueryWrapper().eq(SysWorkspace::getSlug, slug)
        ) == 0;
    }

    // ========== 私有方法 ==========

    /** 创建预设角色（admin/member/viewer）并将创建者以管理员身份加入成员表 */
    private void initPresetRolesAndOwner(Long workspaceId, Long userId) {
        int adminRoleId = insertRole(workspaceId, ROLE_ADMIN, "管理员", 0, ADMIN_PERMISSIONS);
        insertRole(workspaceId, ROLE_MEMBER, "成员", 1, MEMBER_PERMISSIONS);
        insertRole(workspaceId, ROLE_VIEWER, "访客", 1, VIEWER_PERMISSIONS);

        SysWorkspaceMember owner = new SysWorkspaceMember();
        owner.setWorkspaceId(workspaceId);
        owner.setUserId(userId);
        owner.setRoleId(adminRoleId);
        memberMapper.insert(owner);
    }

    private int insertRole(Long workspaceId, String code, String name, int type, List<String> permissions) {
        SysRole role = new SysRole();
        role.setWorkspaceId(workspaceId);
        role.setRoleCode(code);
        role.setRoleName(name);
        role.setRoleType(type);
        roleMapper.insert(role);

        for (String permission : permissions) {
            SysRolePermission rp = new SysRolePermission();
            rp.setRoleId(role.getId());
            rp.setPermissionCode(permission);
            rolePermissionMapper.insert(rp);
        }
        return role.getId();
    }

    private List<Integer> findRoleIds(Long workspaceId) {
        return roleMapper.selectListByQuery(
                        QueryWrapper.create().where(SysRole::getWorkspaceId).eq(workspaceId))
                .stream().map(SysRole::getId).toList();
    }

    private void deleteRolesAndPermissions(List<Integer> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        rolePermissionMapper.deleteByQuery(
                QueryWrapper.create().where(SysRolePermission::getRoleId).in(roleIds));
        roleMapper.deleteByQuery(
                QueryWrapper.create().where(SysRole::getId).in(roleIds));
    }

    private List<String> loadPermissions(Integer roleId) {
        return rolePermissionMapper.selectListByQuery(
                        QueryWrapper.create().where(SysRolePermission::getRoleId).eq(roleId))
                .stream().map(SysRolePermission::getPermissionCode).toList();
    }

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
