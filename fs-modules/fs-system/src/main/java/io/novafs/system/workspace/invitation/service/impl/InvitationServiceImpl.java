package io.novafs.system.workspace.invitation.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.framework.common.exception.BaseException;
import io.novafs.framework.common.exception.ErrorCode;
import io.novafs.framework.notify.EmailNotifyService;
import io.novafs.system.user.entity.SysUser;
import io.novafs.system.user.mapper.SysUserMapper;
import io.novafs.system.workspace.dto.WorkspaceResponse;
import io.novafs.system.workspace.entity.SysWorkspace;
import io.novafs.system.workspace.invitation.dto.CreateInvitationRequest;
import io.novafs.system.workspace.invitation.dto.InvitationResponse;
import io.novafs.system.workspace.invitation.entity.SysWorkspaceInvitation;
import io.novafs.system.workspace.invitation.mapper.SysWorkspaceInvitationMapper;
import io.novafs.system.workspace.invitation.service.InvitationService;
import io.novafs.system.workspace.mapper.SysWorkspaceMapper;
import io.novafs.system.workspace.member.entity.SysWorkspaceMember;
import io.novafs.system.workspace.member.mapper.SysWorkspaceMemberMapper;
import io.novafs.system.workspace.role.entity.SysRole;
import io.novafs.system.workspace.role.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * 工作空间邀请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    /** 邀请有效期（天） */
    private static final int EXPIRE_DAYS = 7;
    /** 邀请状态：0 待接受 1 已接受 2 已过期 3 已取消 */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACCEPTED = 1;
    private static final int STATUS_EXPIRED = 2;
    private static final int STATUS_CANCELLED = 3;

    private final SysWorkspaceInvitationMapper invitationMapper;
    private final SysWorkspaceMemberMapper memberMapper;
    private final SysWorkspaceMapper workspaceMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final EmailNotifyService emailNotifyService;
    private final SecureRandom random = new SecureRandom();

    @Value("${novafs.frontend-url:http://localhost:8080}")
    private String frontendUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InvitationResponse createInvitation(Long workspaceId, Long operatorId, CreateInvitationRequest request) {
        requireAdmin(workspaceId, operatorId);
        requireRoleInWorkspace(workspaceId, request.getRoleId());
        ensureNotDuplicate(workspaceId, request.getEmail());
        ensureNotAlreadyMember(workspaceId, request.getEmail());

        SysWorkspaceInvitation invitation = new SysWorkspaceInvitation();
        invitation.setWorkspaceId(workspaceId);
        invitation.setEmail(request.getEmail());
        invitation.setRoleId(request.getRoleId());
        invitation.setInvitedBy(operatorId);
        invitation.setToken(generateToken());
        invitation.setStatus(STATUS_PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(EXPIRE_DAYS));
        invitationMapper.insert(invitation);

        sendInvitationEmail(request.getEmail(), invitation.getToken());
        log.info("Invitation created: workspaceId={}, email={}, token={}", workspaceId, request.getEmail(), invitation.getToken());
        return toVO(invitation);
    }

    @Override
    public List<InvitationResponse> listInvitations(Long workspaceId, Long operatorId) {
        requireAdmin(workspaceId, operatorId);
        return invitationMapper.selectListByQuery(
                        QueryWrapper.create().where(SysWorkspaceInvitation::getWorkspaceId).eq(workspaceId)
                                .orderBy(SysWorkspaceInvitation::getCreatedAt, false))
                .stream().map(InvitationServiceImpl::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelInvitation(Long workspaceId, Long invitationId, Long operatorId) {
        requireAdmin(workspaceId, operatorId);
        SysWorkspaceInvitation invitation = invitationMapper.selectOneById(invitationId);
        if (invitation == null || !invitation.getWorkspaceId().equals(workspaceId)) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "邀请不存在");
        }
        invitation.setStatus(STATUS_CANCELLED);
        invitationMapper.update(invitation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkspaceResponse acceptInvitation(Long userId, String token) {
        SysWorkspaceInvitation invitation = invitationMapper.selectOneByQuery(
                QueryWrapper.create().where(SysWorkspaceInvitation::getToken).eq(token));
        if (invitation == null) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "邀请链接无效");
        }
        if (invitation.getStatus() == STATUS_ACCEPTED) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "该邀请已被接受");
        }
        if (invitation.getStatus() == STATUS_CANCELLED) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "该邀请已取消");
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(STATUS_EXPIRED);
            invitationMapper.update(invitation);
            throw new BaseException(ErrorCode.BAD_REQUEST, "该邀请已过期");
        }

        SysWorkspace workspace = workspaceMapper.selectOneById(invitation.getWorkspaceId());
        if (workspace == null) {
            throw new BaseException(ErrorCode.WORKSPACE_NOT_FOUND);
        }
        requireRoleInWorkspace(workspace.getId(), invitation.getRoleId());

        boolean alreadyMember = memberMapper.selectCountByQuery(
                QueryWrapper.create().where(SysWorkspaceMember::getWorkspaceId).eq(workspace.getId())
                        .and(SysWorkspaceMember::getUserId).eq(userId)) > 0;
        if (!alreadyMember) {
            SysWorkspaceMember member = new SysWorkspaceMember();
            member.setWorkspaceId(workspace.getId());
            member.setUserId(userId);
            member.setRoleId(invitation.getRoleId());
            memberMapper.insert(member);
            workspace.setMemberCount(workspace.getMemberCount() + 1);
            workspaceMapper.update(workspace);
        }

        invitation.setStatus(STATUS_ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationMapper.update(invitation);
        log.info("Invitation accepted: workspaceId={}, userId={}", workspace.getId(), userId);

        return toWorkspaceResponse(workspace);
    }

    // ========== 私有方法 ==========

    private void ensureNotDuplicate(Long workspaceId, String email) {
        long count = invitationMapper.selectCountByQuery(
                QueryWrapper.create().where(SysWorkspaceInvitation::getWorkspaceId).eq(workspaceId)
                        .and(SysWorkspaceInvitation::getEmail).eq(email)
                        .and(SysWorkspaceInvitation::getStatus).eq(STATUS_PENDING));
        if (count > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "该邮箱已有待接受的邀请");
        }
    }

    private void ensureNotAlreadyMember(Long workspaceId, String email) {
        SysUser user = userMapper.selectOneByQuery(
                QueryWrapper.create().where(SysUser::getEmail).eq(email));
        if (user == null) {
            return;
        }
        long count = memberMapper.selectCountByQuery(
                QueryWrapper.create().where(SysWorkspaceMember::getWorkspaceId).eq(workspaceId)
                        .and(SysWorkspaceMember::getUserId).eq(user.getId()));
        if (count > 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "该邮箱已是工作空间成员");
        }
    }

    private void requireRoleInWorkspace(Long workspaceId, Integer roleId) {
        long count = roleMapper.selectCountByQuery(
                QueryWrapper.create().where(SysRole::getId).eq(roleId)
                        .and(SysRole::getWorkspaceId).eq(workspaceId));
        if (count == 0) {
            throw new BaseException(ErrorCode.BAD_REQUEST, "角色不存在或不属于该工作空间");
        }
    }

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

    private void sendInvitationEmail(String email, String token) {
        String base = frontendUrl == null ? "http://localhost:8080" : frontendUrl;
        String link = base + "/join?token=" + token;
        String content = """
                您好：

                您被邀请加入 NovaFS 工作空间。
                请点击以下链接接受邀请（7 天内有效）：

                %s

                如果链接无法点击，请复制到浏览器打开。
                """.formatted(link);
        emailNotifyService.sendMail(email, "NovaFS 工作空间邀请", content);
    }

    private String generateToken() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static InvitationResponse toVO(SysWorkspaceInvitation invitation) {
        InvitationResponse vo = new InvitationResponse();
        vo.setId(invitation.getId());
        vo.setEmail(invitation.getEmail());
        vo.setToken(invitation.getToken());
        vo.setRoleId(invitation.getRoleId());
        vo.setStatus(invitation.getStatus());
        vo.setExpiresAt(invitation.getExpiresAt());
        vo.setCreatedAt(invitation.getCreatedAt());
        return vo;
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
}
