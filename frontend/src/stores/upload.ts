import { defineStore } from 'pinia'
import { connectSse } from '../api/sse'
import { fileApi, UPLOAD_CHUNK_SIZE, getStorageSettingId } from '../api/file'
import { storageApi } from '../api/storage'

export type TaskStatus = 'hashing' | 'uploading' | 'merging' | 'done' | 'error'

export interface UploadTask {
  id: number
  name: string
  size: number
  percent: number
  status: TaskStatus
  statusText: string
  error?: string
  uploadId?: string
}

const MD5_CHUNK_SIZE = 2 * 1024 * 1024

let seq = 0

/**
 * 全局上传任务 Store
 * <p>任务与上传流程存放在全局 store，切换页面/路由不丢失；SSE 进度实时更新。</p>
 */
export const useUploadStore = defineStore('upload', {
  state: () => ({
    tasks: [] as UploadTask[],
    /** SSE 连接关闭函数（全局单连接） */
    sseClose: null as (() => void) | null,
    /** 上传完成计数（成功或失败），供页面监听刷新列表 */
    completedCount: 0,
    /** 是否启用秒传（默认开启；关闭后大文件跳过 MD5 直接分片，更快） */
    useMd5: true
  }),
  actions: {
    /** 建立全局 SSE 连接（任务栏挂载时调用一次） */
    connectSse(workspaceId: () => string) {
      if (this.sseClose) return
      this.sseClose = connectSse({
        onEvent: (event, data) => {
          if (event === 'upload-progress') {
            const p = data as { uploadId: string; percent: number }
            const task = this.tasks.find((t) => t.uploadId === p.uploadId)
            if (task && task.status === 'uploading' && typeof p.percent === 'number') {
              task.percent = Math.max(task.percent, p.percent)
            }
          } else if (event === 'upload-complete') {
            const c = data as { fileId: string; workspaceId: string }
            if (c.workspaceId === workspaceId()) {
              this.completedCount++
            }
          }
        }
      })
    },
    disconnectSse() {
      this.sseClose?.()
      this.sseClose = null
    },

    /** 添加上传任务（文件选择/拖拽入口） */
    async addFile(file: File, workspaceId: string, parentId: string | null) {
      const task: UploadTask = {
        id: ++seq,
        name: file.name,
        size: file.size,
        percent: 0,
        status: file.size > UPLOAD_CHUNK_SIZE && this.useMd5 ? 'hashing' : 'uploading',
        statusText: file.size > UPLOAD_CHUNK_SIZE && this.useMd5 ? '计算 MD5 中…' : '上传中…'
      }
      this.tasks.push(task)
      try {
        const settingId = await this.resolveSettingId(workspaceId)
        if (!settingId) {
          throw new Error('当前工作空间未启用存储配置')
        }
        if (file.size <= UPLOAD_CHUNK_SIZE) {
          await fileApi.upload(workspaceId, file, settingId, parentId)
          this.finish(task, '已上传')
          return
        }
        let md5 = ''
        if (this.useMd5) {
          md5 = await this.calcMd5(file, task)
          const check = await fileApi.checkMd5(workspaceId, {
            md5,
            fileName: file.name,
            fileSize: file.size
          })
          if (check.exists) {
            this.finish(task, '秒传（服务端已存在）')
            return
          }
        }
        await this.chunkUpload(task, file, workspaceId, parentId, md5, settingId)
      } catch (err) {
        task.status = 'error'
        task.statusText = '失败'
        task.error = err instanceof Error ? err.message : String(err)
      } finally {
        this.completedCount++
      }
    },

    async chunkUpload(
      task: UploadTask,
      file: File,
      workspaceId: string,
      parentId: string | null,
      md5: string,
      settingId: string
    ) {
      const total = Math.ceil(file.size / UPLOAD_CHUNK_SIZE)
      const init = await fileApi.chunkInit(workspaceId, {
        fileName: file.name,
        fileSize: file.size,
        totalChunks: total,
        chunkSize: UPLOAD_CHUNK_SIZE,
        md5: md5 || undefined,
        parentId,
        storagePlatformSettingId: settingId
      })
      task.uploadId = init.uploadId
      task.status = 'uploading'
      task.statusText = '上传中…'

      // 断点续传：查询已上传分片
      let uploaded: number[] = []
      try {
        const state = await fileApi.chunkList(init.uploadId)
        uploaded = state.uploadedChunks
        task.percent = Math.floor((state.uploadedSize / file.size) * 100)
      } catch {
        /* 查询失败按全新上传处理 */
      }

      const missing = Array.from({ length: total }, (_, i) => i + 1).filter((n) => !uploaded.includes(n))
      let doneCount = uploaded.length
      const updateProgress = () => {
        const bytes = Math.min(doneCount * UPLOAD_CHUNK_SIZE, file.size)
        task.percent = Math.min(99, Math.floor((bytes / file.size) * 100))
      }
      const queue = [...missing]
      const concurrency = Math.min(3, Math.max(1, queue.length))
      await Promise.all(
        Array.from({ length: concurrency }, async () => {
          while (queue.length) {
            const n = queue.shift() as number
            const start = (n - 1) * UPLOAD_CHUNK_SIZE
            const blob = file.slice(start, Math.min(start + UPLOAD_CHUNK_SIZE, file.size))
            await fileApi.chunkUpload(init.uploadId, n, blob)
            doneCount++
            updateProgress()
          }
        })
      )

      task.status = 'merging'
      task.statusText = '合并中…'
      await fileApi.chunkMerge(workspaceId, { uploadId: init.uploadId, fileName: file.name, md5: md5 || undefined })
      this.finish(task, '上传完成')
    },

    /** Web Worker 计算 MD5（不阻塞主线程，进度回写任务） */
    calcMd5(file: File, task: UploadTask): Promise<string> {
      return new Promise((resolve, reject) => {
        const worker = new Worker(new URL('../workers/md5.worker.ts', import.meta.url), { type: 'module' })
        worker.onmessage = (e: MessageEvent<{ type: string; percent?: number; md5?: string; message?: string }>) => {
          const data = e.data
          if (data.type === 'progress' && typeof data.percent === 'number') {
            task.percent = data.percent
          } else if (data.type === 'done' && data.md5) {
            worker.terminate()
            resolve(data.md5)
          } else if (data.type === 'error') {
            worker.terminate()
            reject(new Error(data.message || 'MD5 计算失败'))
          }
        }
        worker.onerror = () => {
          worker.terminate()
          reject(new Error('MD5 计算失败'))
        }
        worker.postMessage({ file, chunkSize: MD5_CHUNK_SIZE })
      })
    },

    async resolveSettingId(workspaceId: string): Promise<string> {
      const cached = getStorageSettingId()
      if (cached) return cached
      const list = await storageApi.listEnabled(workspaceId)
      if (!list.length) return ''
      localStorage.setItem('novafs_storage_setting_id', list[0].id)
      return list[0].id
    },

    finish(task: UploadTask, text: string) {
      task.status = 'done'
      task.statusText = text
      task.percent = 100
    },

    /** 清除已完成/失败的任务 */
    clearFinished() {
      this.tasks = this.tasks.filter((t) => t.status === 'uploading' || t.status === 'hashing' || t.status === 'merging')
    }
  }
})
