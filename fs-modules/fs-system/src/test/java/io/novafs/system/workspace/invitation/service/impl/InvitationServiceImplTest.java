package io.novafs.system.workspace.invitation.service.impl;

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
import io.novafs.system.workspace.mapper.SysWorkspaceMapper;
import io.novafs.system.workspace.member.entity.SysWorkspaceMember;
import io.novafs.system.workspace.member.mapper.SysWorkspaceMemberMapper;
import io.novafs.system.workspace.role.entity.SysRole;
import io.novafs.system.workspace.role.mapper.SysRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 工作空间邀请服务测试：创建/查重/接受/过期
 */
@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    @Mock
    private SysWorkspaceInvitationMapper invitationMapper;
    @Mock
    private SysWorkspaceMemberMapper memberMapper;
    @Mock
    private SysWorkspaceMapper workspaceMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private EmailNotifyService emailNotifyService;

    private InvitationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InvitationServiceImpl(invitationMapper, memberMapper, workspaceMapper,
                roleMapper, userMapper, emailNotifyService);
    }

    @Test
    void shouldCreateInvitationAndSendEmail() {
        SysWorkspace ws = workspace(10L, 1L);
        when(workspaceMapper.selectOneById(10L)).thenReturn(ws);
        when(invitationMapper.selectCountByQuery(any())).thenReturn(0L);
        when(memberMapper.selectCountByQuery(any())).thenReturn(0L);
        when(roleMapper.selectCountByQuery(any())).thenReturn(1L);

        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("user@example.com");
        request.setRoleId(2);
        InvitationResponse vo = service.createInvitation(10L, 1L, request);

        ArgumentCaptor<SysWorkspaceInvitation> captor = ArgumentCaptor.forClass(SysWorkspaceInvitation.class);
        verify(invitationMapper).insert(captor.capture());
        SysWorkspaceInvitation saved = captor.getValue();
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getStatus()).isZero();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(vo.getEmail()).isEqualTo("user@example.com");
        verify(emailNotifyService).sendMail(anyString(), anyString(), anyString());
    }

    @Test
    void shouldRejectDuplicatePendingInvitation() {
        SysWorkspace ws = workspace(10L, 1L);
        when(workspaceMapper.selectOneById(10L)).thenReturn(ws);
        when(invitationMapper.selectCountByQuery(any())).thenReturn(1L);

        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("user@example.com");
        request.setRoleId(2);

        assertThatThrownBy(() -> service.createInvitation(10L, 1L, request))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verify(invitationMapper, never()).insert(any());
    }

    @Test
    void shouldRejectInvitationForExistingMember() {
        SysWorkspace ws = workspace(10L, 1L);
        SysUser user = new SysUser();
        user.setId(100L);
        when(workspaceMapper.selectOneById(10L)).thenReturn(ws);
        when(invitationMapper.selectCountByQuery(any())).thenReturn(0L);
        when(userMapper.selectOneByQuery(any())).thenReturn(user);
        when(memberMapper.selectCountByQuery(any())).thenReturn(1L);

        CreateInvitationRequest request = new CreateInvitationRequest();
        request.setEmail("user@example.com");
        request.setRoleId(2);

        assertThatThrownBy(() -> service.createInvitation(10L, 1L, request))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verify(invitationMapper, never()).insert(any());
    }

    @Test
    void shouldAcceptInvitationAndJoinWorkspace() {
        SysWorkspaceInvitation invitation = invitation("token123", 10L, 2, LocalDateTime.now().plusDays(1));
        when(invitationMapper.selectOneByQuery(any())).thenReturn(invitation);
        SysWorkspace ws = workspace(10L, 1L);
        ws.setMemberCount(2);
        when(workspaceMapper.selectOneById(10L)).thenReturn(ws);
        when(roleMapper.selectCountByQuery(any())).thenReturn(1L);
        when(memberMapper.selectCountByQuery(any())).thenReturn(0L);

        WorkspaceResponse vo = service.acceptInvitation(50L, "token123");

        assertThat(vo.getId()).isEqualTo(10L);
        verify(memberMapper).insert(any(SysWorkspaceMember.class));
        assertThat(ws.getMemberCount()).isEqualTo(3);
        assertThat(invitation.getStatus()).isEqualTo(1);
        assertThat(invitation.getAcceptedAt()).isNotNull();
    }

    @Test
    void shouldMarkExpiredInvitationAndReject() {
        SysWorkspaceInvitation invitation = invitation("token123", 10L, 2, LocalDateTime.now().minusHours(1));
        when(invitationMapper.selectOneByQuery(any())).thenReturn(invitation);

        assertThatThrownBy(() -> service.acceptInvitation(50L, "token123"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(invitation.getStatus()).isEqualTo(2);
        verify(invitationMapper).update(invitation);
    }

    @Test
    void shouldRejectAlreadyAcceptedInvitation() {
        SysWorkspaceInvitation invitation = invitation("token123", 10L, 2, LocalDateTime.now().plusDays(1));
        invitation.setStatus(1);
        when(invitationMapper.selectOneByQuery(any())).thenReturn(invitation);

        assertThatThrownBy(() -> service.acceptInvitation(50L, "token123"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        verify(memberMapper, never()).insert(any());
    }

    @Test
    void shouldRejectInvalidToken() {
        when(invitationMapper.selectOneByQuery(any())).thenReturn(null);

        assertThatThrownBy(() -> service.acceptInvitation(50L, "bad-token"))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getCode());
    }

    private static SysWorkspace workspace(Long id, Long ownerId) {
        SysWorkspace ws = new SysWorkspace();
        ws.setId(id);
        ws.setOwnerId(ownerId);
        ws.setMemberCount(1);
        return ws;
    }

    private static SysWorkspaceInvitation invitation(String token, Long workspaceId, Integer roleId, LocalDateTime expiresAt) {
        SysWorkspaceInvitation inv = new SysWorkspaceInvitation();
        inv.setToken(token);
        inv.setWorkspaceId(workspaceId);
        inv.setRoleId(roleId);
        inv.setStatus(0);
        inv.setExpiresAt(expiresAt);
        return inv;
    }
}
