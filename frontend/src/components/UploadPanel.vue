<template>
  <div class="upload-panel">
    <el-upload drag multiple :show-file-list="false" :http-request="onUpload">
      <div class="upload-inner">
        <div class="upload-icon">⬆</div>
        <div class="upload-text">拖拽文件到此处，或 <em>点击上传</em></div>
        <div class="upload-hint">
          ≤5MB 直传 · 大文件自动分片（断点续传）· 进度见右下角任务栏
        </div>
        <div class="upload-target">📂 将上传到：{{ props.parentName }}</div>
        <div class="upload-option">
          <el-checkbox v-model="store.useMd5">启用秒传（计算 MD5，重复文件秒传）</el-checkbox>
        </div>
      </div>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import type { UploadRequestOptions } from 'element-plus'
import { useUploadStore } from '../stores/upload'

const props = defineProps<{
  /** 当前所在文件夹 ID（null=根目录），上传文件归属到该目录 */
  parentId: string | null
  /** 当前所在文件夹名称（提示用） */
  parentName: string
}>()

const store = useUploadStore()

const workspaceId = () => localStorage.getItem('novafs_workspace') || '1'

function onUpload(options: UploadRequestOptions) {
  void store.addFile(options.file as File, workspaceId(), props.parentId)
}
</script>

<style scoped>
.upload-panel { margin-bottom: 4px; }
.upload-inner { padding: 20px 0; }
.upload-icon { font-size: 28px; color: var(--novafs-primary); }
.upload-text { font-size: 14px; color: var(--novafs-text-sub); margin-top: 6px; }
.upload-text em { color: var(--novafs-primary); font-style: normal; font-weight: 600; }
.upload-hint { font-size: 12px; color: var(--novafs-text-muted); margin-top: 4px; }
.upload-target { font-size: 12px; color: var(--novafs-primary); margin-top: 4px; }
.upload-option { margin-top: 8px; }
</style>
