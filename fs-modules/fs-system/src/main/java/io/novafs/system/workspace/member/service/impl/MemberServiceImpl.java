package io.novafs.system.workspace.member.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.system.user.entity.SysUser;
import io.novafs.system.user.mapper.SysUserMapper;
import io.novafs.system.workspace.entity.SysWorkspace;
import io.novafs.system.workspace.mapper.SysWorkspaceMapper;
import io.novafs.system.workspace.member.dto.MemberResponse;
import io.novafs.system.workspace.member.entity.SysWorkspaceMember;
import io.novafs.system.workspace.member.mapper.SysWorkspaceMemberMapper;
import io.novafs.system.workspace.member.service.MemberService;
import io.novafs.system.workspace.role.dto.RoleResponse;
import io.novafs.system.workspace.role.entity.SysRole;
import io.novafs.system.workspace.role.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作空间成员管理服务实现
 */
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final SysWorkspaceMapper workspaceMapper;
    private final SysWorkspaceMemberMapper memberMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public List<MemberResponse> listMembers(Long workspaceId, Long operatorId) {
        requireMember(workspaceId, operatorId);

        List<SysWorkspaceMember> members = memberMapper.selectListByQuery(
                QueryWrapper.create().where(SysWorkspaceMember::getWorkspaceId).eq(workspaceId)
                        .orderBy(SysWorkspaceMember::getJoinedAt, true));
        Map<Long, SysUser> users = userMapper.selectListByQuery(
                        QueryWrapper.create().where(SysUser::getId).in(
                                members.stream().map(SysWorkspaceMember::getUserId).toList()))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Integer, SysRole> roles = roleMapper.selectListByQuery(
                        QueryWrapper.create().where(SysRole::getWorkspaceId).eq(workspaceId))
                .stream().collect(Collectors.toMap(SysRole::getId, Function.identity()));

        return members.stream().map(m -> toVO(m, users.get(m.getUserId()), roles.get(m.getRoleId()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long workspaceId, Long memberId, Integer roleId, Long operatorId) {
        requireAdmin(workspaceId, operatorId);
        requireRoleInWorkspace(workspaceId, roleId);

        SysWorkspaceMember member = memberMapper.selectOneById(memberId);
        if (member == null || !member.getWorkspaceId().equals(workspaceId)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "成员不存在");
        }
        member.setRoleId(roleId);
        memberMapper.update(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long workspaceId, Long memberId, Long operatorId) {
        requireAdmin(workspaceId, operatorId);

        SysWorkspaceMember member = memberMapper.selectOneById(memberId);
        if (member == null || !member.getWorkspaceId().equals(workspaceId)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "成员不存在");
        }
        SysWorkspace workspace = workspaceMapper.selectOneById(workspaceId);
        if (workspace.getOwnerId().equals(member.getUserId())) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "不能移除空间所有者");
        }

        memberMapper.deleteById(memberId);
        workspace.setMemberCount(Math.max(0, workspace.getMemberCount() - 1));
        workspaceMapper.update(workspace);
    }

    @Override
    public List<RoleResponse> listRoles(Long workspaceId, Long operatorId) {
        requireMember(workspaceId, operatorId);
        return roleMapper.selectListByQuery(
                        QueryWrapper.create().where(SysRole::getWorkspaceId).eq(workspaceId)
                                .orderBy(SysRole::getRoleType, true))
                .stream().map(MemberServiceImpl::toRoleVO).toList();
    }

    /** 校验角色属于该工作空间 */
    private void requireRoleInWorkspace(Long workspaceId, Integer roleId) {
        long count = roleMapper.selectCountByQuery(
                QueryWrapper.create().where(SysRole::getId).eq(roleId)
                        .and(SysRole::getWorkspaceId).eq(workspaceId));
        if (count == 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "角色不存在或不属于该工作空间");
        }
    }

    /** 校验是工作空间成员（列表/角色查看用） */
    private void requireMember(Long workspaceId, Long operatorId) {
        long count = memberMapper.selectCountByQuery(
                QueryWrapper.create().where(SysWorkspaceMember::getWorkspaceId).eq(workspaceId)
                        .and(SysWorkspaceMember::getUserId).eq(operatorId));
        if (count == 0) {
            throw new BaseException(ErrorCode.NOT_WORKSPACE_MEMBER);
        }
    }

    /** 校验是管理员（空间所有者或预设 admin 角色） */
    private void requireAdmin(Long workspaceId, Long operatorId) {
        SysWorkspace workspace = workspaceMapper.selectOneById(workspaceId);
        if (workspace == null) {
            throw new BaseException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        if (workspace.getOwnerId().equals(operatorId)) {
            return;
        }
        SysWorkspaceMember member = memberMapper.selectOneByQuery(
                QueryWrapper.create().where(SysWorkspaceMember::getWorkspaceId).eq(workspaceId)
                        .and(SysWorkspaceMember::getUserId).eq(operatorId));
        if (member == null) {
            throw new BaseException(ErrorCode.NOT_WORKSPACE_MEMBER);
        }
        SysRole role = roleMapper.selectOneById(member.getRoleId());
        if (role == null || !"admin".equals(role.getRoleCode())) {
            throw new BaseException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
    }

    private static MemberResponse toVO(SysWorkspaceMember member, SysUser user, SysRole role) {
        MemberResponse vo = new MemberResponse();
        vo.setId(member.getId());
        vo.setUserId(member.getUserId());
        vo.setUsername(user == null ? null : user.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setRoleId(member.getRoleId());
        vo.setRoleName(role == null ? null : role.getRoleName());
        vo.setJoinedAt(member.getJoinedAt());
        return vo;
    }

    private static RoleResponse toRoleVO(SysRole role) {
        RoleResponse vo = new RoleResponse();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setRoleType(role.getRoleType());
        return vo;
    }
}
