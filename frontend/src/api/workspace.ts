import request from './request'

/** 工作空间信息 */
export interface Workspace {
  id: string
  name: string
  slug: string
  description: string | null
  ownerId: string
  memberCount: number
  createdAt: string
  updatedAt: string
}

/** 工作空间详情（含当前用户角色权限） */
export interface WorkspaceDetail extends Workspace {
  roleCode: string
  roleName: string
  permissions: string[]
}

export interface CreateWorkspaceParams {
  name: string
  slug: string
  description?: string
}

export interface UpdateWorkspaceParams {
  name: string
  description?: string
}

/** 工作空间成员 */
export interface WorkspaceMember {
  id: string
  userId: string
  username: string | null
  nickname: string | null
  roleId: number
  roleName: string | null
  joinedAt: string
}

/** 工作空间角色 */
export interface WorkspaceRole {
  id: number
  roleCode: string
  roleName: string
  description: string | null
  roleType: number
}

/** 邀请 */
export interface Invitation {
  id: string
  email: string
  token: string
  roleId: number
  status: number // 0 待接受 1 已接受 2 已过期 3 已取消
  expiresAt: string
  createdAt: string
}

export const workspaceApi = {
  /** 当前用户的工作空间列表 */
  list() {
    return request.get<Workspace[]>('/workspaces')
  },

  /** 工作空间详情 */
  detail(workspaceId: string) {
    return request.get<WorkspaceDetail>(`/workspaces/${workspaceId}`)
  },

  /** 检查 slug 是否可用 */
  checkSlug(slug: string) {
    return request.get<boolean>('/workspaces/slug/check', { params: { slug } })
  },

  /** 创建工作空间 */
  create(data: CreateWorkspaceParams) {
    return request.post<Workspace>('/workspaces', data)
  },

  /** 更新工作空间（需 member:manage 权限） */
  update(workspaceId: string, data: UpdateWorkspaceParams) {
    return request.put<Workspace>(`/workspaces/${workspaceId}`, data)
  },

  /** 删除工作空间（需 member:manage 权限） */
  remove(workspaceId: string) {
    return request.delete<null>(`/workspaces/${workspaceId}`)
  },

  /** 成员列表 */
  listMembers(workspaceId: string) {
    return request.get<WorkspaceMember[]>(`/workspaces/${workspaceId}/members`)
  },

  /** 修改成员角色（需管理员） */
  updateMemberRole(workspaceId: string, memberId: string, roleId: number) {
    return request.put<null>(`/workspaces/${workspaceId}/members/${memberId}`, { roleId })
  },

  /** 移除成员（需管理员） */
  removeMember(workspaceId: string, memberId: string) {
    return request.delete<null>(`/workspaces/${workspaceId}/members/${memberId}`)
  },

  /** 角色列表 */
  listRoles(workspaceId: string) {
    return request.get<WorkspaceRole[]>(`/workspaces/${workspaceId}/roles`)
  },

  /** 创建邀请（需管理员） */
  createInvitation(workspaceId: string, data: { email: string; roleId: number }) {
    return request.post<Invitation>(`/workspaces/${workspaceId}/invitations`, data)
  },

  /** 邀请列表 */
  listInvitations(workspaceId: string) {
    return request.get<Invitation[]>(`/workspaces/${workspaceId}/invitations`)
  },

  /** 取消邀请 */
  cancelInvitation(workspaceId: string, invitationId: string) {
    return request.delete<null>(`/workspaces/${workspaceId}/invitations/${invitationId}`)
  },

  /** 接受邀请（登录用户） */
  acceptInvitation(token: string) {
    return request.post<Workspace>('/workspaces/invitations/accept', { token })
  }
}
