import request from './request'

/** 存储平台配置（storage_settings 表） */
export interface StorageSetting {
  id: number
  platformIdentifier: string
  configData: string
  enabled: boolean
  workspaceId: number
  remark: string | null
}

export const storageApi = {
  /** 查询工作空间下已启用的存储配置 */
  listEnabled(workspaceId: number) {
    return request.get<StorageSetting[]>('/storage/settings', { params: { workspaceId } })
  }
}
