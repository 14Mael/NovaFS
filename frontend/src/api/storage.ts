import request from './request'

/** 存储平台配置（storage_settings 表） */
export interface StorageSetting {
  id: number
  platformIdentifier: string
  configData: string | null
  enabled: boolean
  workspaceId: number
  remark: string | null
}

/** 存储平台定义（storage_platform 表） */
export interface StoragePlatform {
  id: number
  name: string
  identifier: string
  icon: string | null
  isDefault: boolean
  description: string | null
}

export interface CreateStorageParams {
  platformIdentifier: string
  configData: string
  enabled?: boolean
  remark?: string
}

export interface UpdateStorageParams {
  configData?: string
  enabled?: boolean
  remark?: string
}

export const storageApi = {
  /** 查询工作空间下已启用的存储配置（脱敏，上传选择用） */
  listEnabled(workspaceId: number) {
    return request.get<StorageSetting[]>('/storage/settings', { params: { workspaceId } })
  },

  /** 查询全部配置（含 configData，管理页编辑回显） */
  listAdmin(workspaceId: number) {
    return request.get<StorageSetting[]>('/storage/settings/admin', { params: { workspaceId } })
  },

  /** 存储平台列表 */
  listPlatforms() {
    return request.get<StoragePlatform[]>('/storage/platforms')
  },

  /** 创建配置 */
  create(workspaceId: number, data: CreateStorageParams) {
    return request.post<StorageSetting>('/storage/settings', data, { params: { workspaceId } })
  },

  /** 更新配置（configData/enabled/remark 为空保留原值） */
  update(id: number, data: UpdateStorageParams) {
    return request.put<StorageSetting>(`/storage/settings/${id}`, data)
  },

  /** 删除配置（逻辑删除） */
  remove(id: number) {
    return request.delete<null>(`/storage/settings/${id}`)
  }
}
