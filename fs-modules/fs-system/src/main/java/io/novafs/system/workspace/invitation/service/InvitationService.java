package io.novafs.system.workspace.invitation.service;

import io.novafs.system.workspace.dto.WorkspaceResponse;
import io.novafs.system.workspace.invitation.dto.CreateInvitationRequest;
import io.novafs.system.workspace.invitation.dto.InvitationResponse;

import java.util.List;

/**
 * 工作空间邀请服务
 */
public interface InvitationService {

    /**
     * 创建邀请并发送邮件（需管理员）
     */
    InvitationResponse createInvitation(Long workspaceId, Long operatorId, CreateInvitationRequest request);

    /**
     * 邀请列表（需管理员）
     */
    List<InvitationResponse> listInvitations(Long workspaceId, Long operatorId);

    /**
     * 取消邀请（需管理员）
     */
    void cancelInvitation(Long workspaceId, Long invitationId, Long operatorId);

    /**
     * 通过邀请令牌加入工作空间（登录用户）
     */
    WorkspaceResponse acceptInvitation(Long userId, String token);
}
