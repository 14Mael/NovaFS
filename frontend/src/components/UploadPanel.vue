<template>
  <div class="upload-panel">
    <el-upload drag multiple :show-file-list="false" :http-request="onUpload">
      <div class="upload-inner">
        <div class="upload-icon">⬆</div>
        <div class="upload-text">拖拽文件到此处，或 <em>点击上传</em></div>
        <div class="upload-hint">≤5MB 直传 · 大文件自动分片（断点续传）· MD5 秒传</div>
      </div>
    </el-upload>

    <div v-if="tasks.length" class="task-list">
      <div v-for="task in tasks" :key="task.id" class="task">
        <div class="task-head">
          <span class="task-name" :title="task.name">{{ task.name }}</span>
          <span class="task-status" :class="task.status">{{ task.statusText }}</span>
        </div>
        <el-progress
          :percentage="task.percent"
          :stroke-width="8"
          :status="task.status === 'error' ? 'exception' : task.status === 'done' ? 'success' : undefined"
        />
        <div v-if="task.error" class="task-error">{{ task.error }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import type { UploadRequestOptions } from 'element-plus'
import SparkMD5 from 'spark-md5'
import { connectSse } from '../api/sse'
import { fileApi, UPLOAD_CHUNK_SIZE, getStorageSettingId } from '../api/file'
import { storageApi } from '../api/storage'

type TaskStatus = 'hashing' | 'uploading' | 'merging' | 'done' | 'error'

interface UploadTask {
  id: number
  name: string
  size: number
  percent: number
  status: TaskStatus
  statusText: string
  error?: string
  uploadId?: string
}

const emit = defineEmits<{ (e: 'uploaded'): void }>()

const props = defineProps<{
  /** 当前所在文件夹 ID（null=根目录），上传文件归属到该目录 */
  parentId: string | null
}>()

const tasks = ref<UploadTask[]>([])
let seq = 0
let closeSse: (() => void) | null = null

const workspaceId = () => localStorage.getItem('novafs_workspace') || '1'

// SSE：上传进度/完成实时推送
onMounted(() => {
  closeSse = connectSse({
    onEvent: (event, data) => {
      if (event === 'upload-progress') {
        const p = data as { uploadId: string; percent: number }
        const task = tasks.value.find((t) => t.uploadId === p.uploadId)
        if (task && task.status === 'uploading' && typeof p.percent === 'number') {
          task.percent = Math.max(task.percent, p.percent)
        }
      } else if (event === 'upload-complete') {
        const c = data as { fileId: string; workspaceId: string }
        if (c.workspaceId === workspaceId()) emit('uploaded')
      }
    }
  })
})
onBeforeUnmount(() => closeSse?.())

async function onUpload(options: UploadRequestOptions) {
  const file = options.file as File
  const task: UploadTask = {
    id: ++seq,
    name: file.name,
    size: file.size,
    percent: 0,
    status: 'hashing',
    statusText: '计算 MD5 中…'
  }
  tasks.value.push(task)
  try {
    const settingId = await resolveSettingId()
    if (!settingId) {
      throw new Error('当前工作空间未启用存储配置，请先在后端 storage_settings 表插入启用配置')
    }
    const md5 = await calcMd5(file)
    if (file.size <= UPLOAD_CHUNK_SIZE) {
      task.statusText = '上传中…'
      await fileApi.upload(workspaceId(), file, settingId, props.parentId)
      finish(task, '已上传')
      emit('uploaded')
      return
    }
    const check = await fileApi.checkMd5(workspaceId(), {
      md5,
      fileName: file.name,
      fileSize: file.size
    })
    if (check.exists) {
      finish(task, '秒传（服务端已存在）')
      emit('uploaded')
      return
    }
    await chunkUpload(task, file, md5, settingId)
  } catch (err) {
    task.status = 'error'
    task.statusText = '失败'
    task.error = err instanceof Error ? err.message : String(err)
  }
}

async function chunkUpload(task: UploadTask, file: File, md5: string, settingId: string) {
  const total = Math.ceil(file.size / UPLOAD_CHUNK_SIZE)
  const init = await fileApi.chunkInit(workspaceId(), {
    fileName: file.name,
    fileSize: file.size,
    totalChunks: total,
    chunkSize: UPLOAD_CHUNK_SIZE,
    md5,
    parentId: props.parentId,
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
  await fileApi.chunkMerge(workspaceId(), { uploadId: init.uploadId, fileName: file.name, md5 })
  finish(task, '上传完成')
  emit('uploaded')
}

async function resolveSettingId(): Promise<string> {
  const cached = getStorageSettingId()
  if (cached) return cached
  const list = await storageApi.listEnabled(workspaceId())
  if (!list.length) return ''
  localStorage.setItem('novafs_storage_setting_id', list[0].id)
  return list[0].id
}

function calcMd5(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const spark = new SparkMD5.ArrayBuffer()
    const chunkSize = 2 * 1024 * 1024
    let offset = 0
    const reader = new FileReader()
    reader.onerror = () => reject(reader.error ?? new Error('读取文件失败'))
    reader.onload = (e) => {
      spark.append(e.target?.result as ArrayBuffer)
      offset += chunkSize
      if (offset < file.size) {
        readNext()
      } else {
        resolve(spark.end())
      }
    }
    const readNext = () => reader.readAsArrayBuffer(file.slice(offset, offset + chunkSize))
    readNext()
  })
}

function finish(task: UploadTask, text: string) {
  task.status = 'done'
  task.statusText = text
  task.percent = 100
}
</script>

<style scoped>
.upload-panel { margin-bottom: 4px; }
.upload-inner { padding: 22px 0; }
.upload-icon { font-size: 28px; color: var(--novafs-primary); }
.upload-text { font-size: 14px; color: #4a6a96; margin-top: 6px; }
.upload-text em { color: var(--novafs-primary); font-style: normal; font-weight: 600; }
.upload-hint { font-size: 12px; color: var(--novafs-text-muted); margin-top: 4px; }

.task-list { margin-top: 14px; display: flex; flex-direction: column; gap: 10px; }
.task {
  background: #fff;
  border: 1px solid var(--novafs-card-border);
  border-radius: 10px;
  padding: 10px 14px;
}
.task-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.task-name {
  font-size: 13px; font-weight: 600; color: var(--novafs-text);
  max-width: 70%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.task-status { font-size: 12px; color: var(--novafs-text-sub); }
.task-status.done { color: #34a853; }
.task-status.error { color: #e5484d; }
.task-error { font-size: 12px; color: #e5484d; margin-top: 6px; }
</style>
