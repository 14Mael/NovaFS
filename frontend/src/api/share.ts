import request from './request'

/** 分享信息 */
export interface FileShareVO {
  id: number
  fileId: number
  fileName: string
  suffix: string | null
  fileSize: number
  shareCode: string
  hasPassword: boolean
  expireTime: string | null
  viewCount: number
  downloadCount: number
  scope: string
  createdAt: string
}

export interface CreateShareParams {
  fileId: number
  sharePwd?: string
  expireTime?: string | null
  maxViewCount?: number
  maxDownloadCount?: number
  scope?: string
}

export const shareApi = {
  /** 创建分享（需登录） */
  create(data: CreateShareParams) {
    return request.post<FileShareVO>('/shares', data)
  },

  /** 通过分享码访问（公开） */
  access(shareCode: string, password?: string) {
    return request.get<FileShareVO>(`/share/${shareCode}`, { params: password ? { password } : {} })
  },

  /** 取消分享（需登录） */
  cancel(shareId: number) {
    return request.delete<null>(`/shares/${shareId}`)
  },

  /** 公开下载地址（免登录，直接用 <a> 或 window.open 访问） */
  downloadUrl(shareCode: string, password?: string, inline = false) {
    const params = new URLSearchParams()
    if (password) params.set('password', password)
    if (inline) params.set('inline', 'true')
    const qs = params.toString()
    return `/api/share/${shareCode}/download${qs ? `?${qs}` : ''}`
  }
}
