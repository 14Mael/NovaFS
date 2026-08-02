package io.novafs.system.security;

import cn.dev33.satoken.stp.StpInterface;
import com.mybatisflex.core.query.QueryWrapper;
import io.novafs.system.user.entity.SysUser;
import io.novafs.system.user.mapper.SysUserMapper;
import io.novafs.system.workspace.member.entity.SysWorkspaceMember;
import io.novafs.system.workspace.member.mapper.SysWorkspaceMemberMapper;
import io.novafs.system.workspace.role.entity.SysRole;
import io.novafs.system.workspace.role.entity.SysRolePermission;
import io.novafs.system.workspace.role.mapper.SysRoleMapper;
import io.novafs.system.workspace.role.mapper.SysRolePermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限接口实现：从数据库查询用户角色与权限。
 * 登录 ID 为用户名，经 sys_workspace_member 关联到角色。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserMapper userMapper;
    private final SysWorkspaceMemberMapper memberMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<Integer> roleIds = findRoleIds(loginId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return rolePermissionMapper.selectListByQuery(
                        QueryWrapper.create().where(SysRolePermission::getRoleId).in(roleIds))
                .stream().map(SysRolePermission::getPermissionCode).distinct().toList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        List<Integer> roleIds = findRoleIds(loginId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectListByQuery(
                        QueryWrapper.create().where(SysRole::getId).in(roleIds))
                .stream().map(SysRole::getRoleCode).distinct().toList();
    }

    private List<Integer> findRoleIds(Object loginId) {
        if (!(loginId instanceof String username)) {
            return List.of();
        }
        SysUser user = userMapper.selectOneByQuery(
                QueryWrapper.create().where(SysUser::getUsername).eq(username));
        if (user == null) {
            return List.of();
        }
        return memberMapper.selectListByQuery(
                        QueryWrapper.create().where(SysWorkspaceMember::getUserId).eq(user.getId()))
                .stream().map(SysWorkspaceMember::getRoleId).distinct().toList();
    }
}
