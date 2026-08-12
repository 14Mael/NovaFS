package io.novafs.system.workspace.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import io.novafs.framework.common.model.Result;
import io.novafs.system.user.service.SysUserService;
import io.novafs.system.workspace.dto.CreateWorkspaceRequest;
import io.novafs.system.workspace.dto.UpdateWorkspaceRequest;
import io.novafs.system.workspace.dto.WorkspaceDetailResponse;
import io.novafs.system.workspace.dto.WorkspaceResponse;
import io.novafs.system.workspace.invitation.dto.AcceptInvitationRequest;
import io.novafs.system.workspace.invitation.dto.CreateInvitationRequest;
import io.novafs.system.workspace.invitation.dto.InvitationResponse;
import io.novafs.system.workspace.invitation.service.InvitationService;
import io.novafs.system.workspace.member.dto.MemberResponse;
import io.novafs.system.workspace.member.dto.UpdateMemberRoleRequest;
import io.novafs.system.workspace.member.service.MemberService;
import io.novafs.system.workspace.role.dto.RoleResponse;
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
    private final MemberService memberService;
    private final InvitationService invitationService;

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

    // ===== 成员管理 =====

    /** 成员列表 */
    @GetMapping("/{workspaceId}/members")
    public Result<List<MemberResponse>> listMembers(@PathVariable Long workspaceId) {
        return Result.ok(memberService.listMembers(workspaceId, currentUserId()));
    }

    /** 修改成员角色（需管理员） */
    @PutMapping("/{workspaceId}/members/{memberId}")
    public Result<Void> updateMemberRole(@PathVariable Long workspaceId,
                                         @PathVariable Long memberId,
                                         @Valid @RequestBody UpdateMemberRoleRequest request) {
        memberService.updateRole(workspaceId, memberId, request.getRoleId(), currentUserId());
        return Result.ok();
    }

    /** 移除成员（需管理员，不能移除空间所有者） */
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    public Result<Void> removeMember(@PathVariable Long workspaceId,
                                     @PathVariable Long memberId) {
        memberService.removeMember(workspaceId, memberId, currentUserId());
        return Result.ok();
    }

    /** 角色列表（管理员管理成员/邀请用） */
    @GetMapping("/{workspaceId}/roles")
    public Result<List<RoleResponse>> listRoles(@PathVariable Long workspaceId) {
        return Result.ok(memberService.listRoles(workspaceId, currentUserId()));
    }

    // ===== 成员邀请 =====

    /** 创建邀请并发送邮件（需管理员） */
    @PostMapping("/{workspaceId}/invitations")
    public Result<InvitationResponse> createInvitation(@PathVariable Long workspaceId,
                                                       @Valid @RequestBody CreateInvitationRequest request) {
        return Result.ok(invitationService.createInvitation(workspaceId, currentUserId(), request));
    }

    /** 邀请列表（需管理员） */
    @GetMapping("/{workspaceId}/invitations")
    public Result<List<InvitationResponse>> listInvitations(@PathVariable Long workspaceId) {
        return Result.ok(invitationService.listInvitations(workspaceId, currentUserId()));
    }

    /** 取消邀请（需管理员） */
    @DeleteMapping("/{workspaceId}/invitations/{invitationId}")
    public Result<Void> cancelInvitation(@PathVariable Long workspaceId,
                                         @PathVariable Long invitationId) {
        invitationService.cancelInvitation(workspaceId, invitationId, currentUserId());
        return Result.ok();
    }

    /** 通过邀请令牌加入工作空间（登录用户） */
    @PostMapping("/invitations/accept")
    public Result<WorkspaceResponse> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        return Result.ok(invitationService.acceptInvitation(currentUserId(), request.getToken()));
    }

    private Long currentUserId() {
        return sysUserService.findByUsername(StpUtil.getLoginIdAsString()).getId();
    }
}
