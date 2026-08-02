<template>
  <div class="lib-page">
    <header class="lib-header">
      <div>
        <h2>文档库</h2>
        <span class="sub">共 {{ total }} 份文档 · 上传后自动解析索引,可供 AI 问答检索</span>
      </div>
    </header>

    <el-upload
      class="uploader"
      drag
      multiple
      :show-file-list="false"
      :http-request="doUpload"
    >
      <div class="upload-inner">
        <div class="upload-icon">⬆</div>
        <div class="upload-text">拖拽文件到此处,或 <em>点击上传</em></div>
        <div class="upload-hint">支持 PDF / Word / Markdown / TXT 等格式</div>
      </div>
    </el-upload>

    <div v-loading="loading" class="grid">
      <div v-for="doc in docs" :key="doc.id" class="card">
        <div class="card-top">
          <div class="file-icon">📄</div>
          <el-tag size="small" :type="statusType(doc.status)" effect="light">
            {{ statusText(doc.status) }}
          </el-tag>
        </div>
        <div class="card-name" :title="doc.name">{{ doc.name }}</div>
        <div class="card-meta">{{ formatSize(doc.size) }} · {{ doc.chunkCount }} 片</div>
        <div class="card-foot">
          <span class="time">{{ formatTime(doc.createdAt) }}</span>
          <el-button link type="danger" size="small" @click="remove(doc)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!loading && docs.length === 0" description="还没有文档,上传第一份吧" />
    </div>

    <el-pagination
      v-if="total > pageSize"
      class="pager"
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="page"
      @current-change="load"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { ragApi, type RagDocument } from '../api/rag'

const workspaceId = () => Number(localStorage.getItem('novafs_workspace') || '1')
const docs = ref<RagDocument[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => load(1))

async function load(p: number) {
  page.value = p
  loading.value = true
  try {
    const resp = await ragApi.page(workspaceId(), p, pageSize.value)
    docs.value = resp.records
    total.value = resp.total
  } catch { /* 拦截器已提示 */ }
  finally { loading.value = false }
}

async function doUpload(options: UploadRequestOptions) {
  try {
    const doc = await ragApi.upload(workspaceId(), options.file as File)
    ElMessage.success(`「${doc.name}」已入库`)
    await load(page.value)
  } catch { /* 拦截器已提示 */ }
}

async function remove(doc: RagDocument) {
  try {
    await ElMessageBox.confirm(`确定删除「${doc.name}」及其索引吗？`, '删除文档')
    await ragApi.remove(doc.id)
    ElMessage.success('已删除')
    await load(page.value)
  } catch { /* 取消或已提示 */ }
}

function statusType(s: number): 'success' | 'warning' | 'danger' {
  return s === 1 ? 'success' : s === 0 ? 'warning' : 'danger'
}
function statusText(s: number): string {
  return s === 1 ? '已索引' : s === 0 ? '解析中' : '失败'
}
function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}
function formatTime(t: string): string {
  return t ? t.slice(0, 16).replace('T', ' ') : ''
}
</script>

<style scoped>
.lib-page { padding: 20px 28px 28px; }
.lib-header { margin-bottom: 16px; }
.lib-header h2 { margin: 0 0 4px; font-size: 20px; color: #1e2240; }
.lib-header .sub { font-size: 13px; color: #8a90b0; }

.uploader { margin-bottom: 22px; }
.upload-inner { padding: 26px 0; }
.upload-icon { font-size: 30px; color: #6366f1; }
.upload-text { font-size: 14px; color: #4a5070; margin-top: 6px; }
.upload-text em { color: #6366f1; font-style: normal; font-weight: 600; }
.upload-hint { font-size: 12px; color: #a0a4c0; margin-top: 4px; }

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px;
  min-height: 120px;
}
.card {
  background: #fff; border-radius: 12px; padding: 14px;
  box-shadow: 0 2px 10px rgba(30,34,64,.05);
  transition: transform .15s, box-shadow .15s;
  cursor: default;
}
.card:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(30,34,64,.1); }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; }
.file-icon { font-size: 26px; }
.card-name {
  margin: 10px 0 4px; font-size: 14px; font-weight: 600; color: #2a2f52;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-meta { font-size: 12px; color: #9aa0bd; }
.card-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.time { font-size: 11px; color: #b0b4cf; }

.pager { margin-top: 20px; justify-content: center; }
</style>