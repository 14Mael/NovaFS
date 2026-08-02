import request from './request'

/** 工作空间信息 */
export interface Workspace {
  id: number
  name: string
  slug: string
  description: string | null
  ownerId: number
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

export const workspaceApi = {
  /** 当前用户的工作空间列表 */
  list() {
    return request.get<Workspace[]>('/workspaces')
  },

  /** 工作空间详情 */
  detail(workspaceId: number) {
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
  update(workspaceId: number, data: UpdateWorkspaceParams) {
    return request.put<Workspace>(`/workspaces/${workspaceId}`, data)
  },

  /** 删除工作空间（需 member:manage 权限） */
  remove(workspaceId: number) {
    return request.delete<null>(`/workspaces/${workspaceId}`)
  }
}
