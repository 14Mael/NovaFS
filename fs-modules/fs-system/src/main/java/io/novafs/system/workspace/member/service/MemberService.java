package io.novafs.system.workspace.member.service;

import io.novafs.system.workspace.member.dto.MemberResponse;
import io.novafs.system.workspace.role.dto.RoleResponse;

import java.util.List;

/**
 * 工作空间成员管理服务
 */
public interface MemberService {

    /**
     * 成员列表（工作空间成员可见）
     */
    List<MemberResponse> listMembers(Long workspaceId, Long operatorId);

    /**
     * 修改成员角色（需管理员）
     */
    void updateRole(Long workspaceId, Long memberId, Integer roleId, Long operatorId);

    /**
     * 移除成员（需管理员，不能移除空间所有者）
     */
    void removeMember(Long workspaceId, Long memberId, Long operatorId);

    /**
     * 工作空间角色列表（管理员管理成员/邀请用）
     */
    List<RoleResponse> listRoles(Long workspaceId, Long operatorId);
}
