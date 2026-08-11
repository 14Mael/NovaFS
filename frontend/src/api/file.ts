import request from './request'

/** 文件信息（file_info 表） */
export interface FileInfo {
  id: number
  workspaceId: number
  userId: number
  parentId: number | null
  originalName: string
  displayName: string | null
  suffix: string | null
  size: number
  mimeType: string | null
  isDir: boolean
  contentMd5: string | null
  uploadTime: string
  isDeleted: boolean
}

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  pages: number
  records: T[]
}

/** 秒传校验结果 */
export interface CheckMd5Result {
  exists: boolean
  fileId?: number
  fileName?: string
  fileSize?: number
}

/** 分片上传初始化结果 */
export interface ChunkInitResponse {
  uploadId: string
  taskId: string
  chunkSize: number
  totalChunks: number
}

/** 已上传分片列表（断点续传） */
export interface UploadedChunksResponse {
  uploadId: string
  uploadedChunks: number[]
  uploadedSize: number
  totalChunks: number
  completed: boolean
}

/** 预览信息 */
export interface FilePreviewInfo {
  previewType: 'IMAGE' | 'VIDEO' | 'AUDIO' | 'TEXT' | 'PDF' | 'UNSUPPORTED'
  url?: string
  content?: string
  fileName: string
}

const FILE_CHUNK_SIZE = 5 * 1024 * 1024 // 5MB
export const UPLOAD_CHUNK_SIZE = FILE_CHUNK_SIZE

/** 从 localStorage 读取存储配置 ID（上传目标） */
export function getStorageSettingId(): number {
  return Number(localStorage.getItem('novafs_storage_setting_id') || '0')
}

export const fileApi = {
  /** 文件列表 */
  list(workspaceId: number, params: { parentId?: number | null; page?: number; pageSize?: number }) {
    return request.get<PageResult<FileInfo>>('/file/list', {
      params: { workspaceId, parentId: params.parentId ?? undefined, page: params.page ?? 1, pageSize: params.pageSize ?? 20 }
    })
  },

  /** 普通上传（小文件） */
  upload(workspaceId: number, file: File, storagePlatformSettingId: number) {
    const form = new FormData()
    form.append('workspaceId', String(workspaceId))
    form.append('storagePlatformSettingId', String(storagePlatformSettingId))
    form.append('file', file)
    return request.post<FileInfo>('/file/upload', form)
  },

  /** 秒传校验 */
  checkMd5(workspaceId: number, data: { md5: string; fileName: string; fileSize: number }) {
    return request.post<CheckMd5Result>('/file/check-md5', data, { params: { workspaceId } })
  },

  /** 初始化分片上传 */
  chunkInit(
    workspaceId: number,
    data: {
      fileName: string
      fileSize: number
      totalChunks: number
      chunkSize?: number
      md5?: string
      storagePlatformSettingId: number
    }
  ) {
    return request.post<ChunkInitResponse>('/file/chunk/init', data, { params: { workspaceId } })
  },

  /** 上传单个分片 */
  chunkUpload(uploadId: string, chunkNumber: number, blob: Blob) {
    const form = new FormData()
    form.append('uploadId', uploadId)
    form.append('chunkNumber', String(chunkNumber))
    form.append('file', blob, `chunk-${chunkNumber}`)
    return request.post<{ chunkNumber: number; alreadyExists: boolean }>('/file/chunk/upload', form)
  },

  /** 查询已上传分片（断点续传） */
  chunkList(uploadId: string) {
    return request.get<UploadedChunksResponse>('/file/chunk/list', { params: { uploadId } })
  },

  /** 合并分片 */
  chunkMerge(workspaceId: number, data: { uploadId: string; fileName: string; md5?: string }) {
    return request.post<FileInfo>('/file/chunk/merge', data, { params: { workspaceId } })
  },

  /** 删除（软删除进回收站） */
  remove(fileId: number) {
    return request.delete<null>(`/file/${fileId}`)
  },

  /** 创建文件夹 */
  createFolder(workspaceId: number, parentId: number | null, name: string) {
    return request.post<FileInfo>('/file/folder', { name }, { params: { workspaceId, parentId: parentId ?? undefined } })
  },

  /** 重命名（文件或文件夹） */
  rename(fileId: number, name: string) {
    return request.put<FileInfo>(`/file/${fileId}`, { name })
  },

  /** 移动（parentId 为空表示移动到根目录） */
  move(fileId: number, parentId: number | null) {
    return request.put<FileInfo>(`/file/${fileId}/move`, null, { params: { parentId: parentId ?? undefined } })
  },

  /** 回收站列表 */
  recycle(workspaceId: number, page = 1, pageSize = 20) {
    return request.get<PageResult<FileInfo>>('/file/recycle', { params: { workspaceId, page, pageSize } })
  },

  /** 从回收站恢复 */
  restore(fileId: number) {
    return request.post<null>(`/file/${fileId}/restore`)
  },

  /** 彻底删除 */
  purge(fileId: number) {
    return request.delete<null>(`/file/${fileId}/purge`)
  },

  /** 预览信息 */
  preview(fileId: number) {
    return request.get<FilePreviewInfo>(`/file/preview/${fileId}`)
  }
}

/** 带鉴权的 blob 请求（下载/预览内容流，axios 拦截器不适用于流式响应） */
export async function fetchBlob(url: string): Promise<Blob> {
  const token = localStorage.getItem('novafs_token')
  const resp = await fetch(url, { headers: token ? { Authorization: token } : {} })
  if (!resp.ok) {
    throw new Error(`请求失败(${resp.status})`)
  }
  return resp.blob()
}

export function downloadUrl(fileId: number): string {
  return `/api/file/download/${fileId}`
}

export function previewContentUrl(fileId: number): string {
  return `/api/file/preview/content/${fileId}`
}
