<template>
  <div class="files-page">
    <!-- 工具栏 -->
    <div class="toolbar">
      <h2>我的文件</h2>
      <div class="ai-search">
        <el-input
          v-model="query"
          placeholder="🔍 AI 语义搜索文档内容,回车检索…"
          clearable
          size="default"
          @keyup.enter="doSearch"
          @clear="clearSearch"
        >
          <template #append>
            <el-button :loading="searching" @click="doSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
      <el-upload
        multiple
        :show-file-list="false"
        :http-request="doUpload"
      >
        <el-button type="primary">⬆ 上传</el-button>
      </el-upload>
      <el-radio-group v-model="view" size="default">
        <el-radio-button value="list">列表</el-radio-button>
        <el-radio-button value="grid">网格</el-radio-button>
      </el-radio-group>
    </div>

    <!-- AI 搜索结果(命中文档片段)-->
    <div v-if="searching" class="ai-result">
      <div class="ai-result-head">
        <span>AI 检索结果</span>
        <el-button link @click="clearSearch">返回文件列表</el-button>
      </div>
      <div v-if="hits.length === 0" class="ai-empty">未找到相关文档内容</div>
      <div v-for="(h, i) in hits" :key="i" class="hit-card">
        <div class="hit-head">
          <span class="hit-name">📄 {{ h.documentName }}</span>
          <span class="hit-score">{{ (h.score * 100).toFixed(1) }}% 相关</span>
        </div>
        <div class="hit-content">{{ h.content }}</div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else-if="view === 'list'" v-loading="loading" class="list-wrap">
      <table class="file-table">
        <thead>
          <tr>
            <th>名称</th>
            <th style="width: 110px">大小</th>
            <th style="width: 100px">索引状态</th>
            <th style="width: 80px">切片</th>
            <th style="width: 150px">上传时间</th>
            <th style="width: 80px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="doc in docs" :key="doc.id">
            <td>
              <span class="fname">📄 {{ doc.name }}</span>
            </td>
            <td class="muted">{{ formatSize(doc.size) }}</td>
            <td>
              <el-tag size="small" :type="statusType(doc.status)" effect="light">{{ statusText(doc.status) }}</el-tag>
            </td>
            <td class="muted">{{ doc.chunkCount }}</td>
            <td class="muted">{{ formatTime(doc.createdAt) }}</td>
            <td><el-button link type="danger" size="small" @click="remove(doc)">删除</el-button></td>
          </tr>
          <tr v-if="!loading && docs.length === 0">
            <td colspan="6" class="empty-row">
              <el-empty description="还没有文件,点击右上角上传" :image-size="70" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 网格视图 -->
    <div v-else v-loading="loading" class="grid">
      <div v-for="doc in docs" :key="doc.id" class="card">
        <div class="card-top">
          <div class="file-icon">📄</div>
          <el-tag size="small" :type="statusType(doc.status)" effect="light">{{ statusText(doc.status) }}</el-tag>
        </div>
        <div class="card-name" :title="doc.name">{{ doc.name }}</div>
        <div class="card-meta">{{ formatSize(doc.size) }} · {{ doc.chunkCount }} 片</div>
        <div class="card-foot">
          <span class="time">{{ formatTime(doc.createdAt) }}</span>
          <el-button link type="danger" size="small" @click="remove(doc)">删除</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && docs.length === 0" style="grid-column: 1/-1" description="还没有文件,点击右上角上传" />
    </div>

    <el-pagination
      v-if="total > pageSize && !searching"
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
import { ragApi, type RagDocument, type SearchResult } from '../api/rag'

const workspaceId = () => Number(localStorage.getItem('novafs_workspace') || '1')
const docs = ref<RagDocument[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const view = ref<'list' | 'grid'>('list')

const query = ref('')
const searching = ref(false)
const hits = ref<SearchResult[]>([])

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
    ElMessage.success(`「${doc.name}」已上传并开始索引`)
    await load(page.value)
  } catch { /* 拦截器已提示 */ }
}

async function remove(doc: RagDocument) {
  try {
    await ElMessageBox.confirm(`确定删除「${doc.name}」吗?删除后无法在 AI 问答中检索。`, '删除文件')
    await ragApi.remove(doc.id)
    ElMessage.success('已删除')
    await load(page.value)
  } catch { /* 取消 */ }
}

async function doSearch() {
  const q = query.value.trim()
  if (!q) return
  searching.value = true
  try {
    hits.value = await ragApi.search(workspaceId(), q, 6)
  } catch { hits.value = [] }
  finally { searching.value = false }
}

function clearSearch() {
  query.value = ''
  hits.value = []
  searching.value = false
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
.files-page { padding: 20px 28px 28px; }

.toolbar { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
.toolbar h2 { margin: 0; font-size: 20px; color: #1e2240; white-space: nowrap; }
.ai-search { flex: 1; min-width: 260px; max-width: 480px; }

.list-wrap {
  background: #fff; border-radius: 12px; box-shadow: 0 2px 10px rgba(30,34,64,.05);
  overflow: hidden;
}
.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th {
  text-align: left; padding: 12px 16px; background: #f7f8fc;
  color: #8a90b0; font-weight: 600; font-size: 12px;
  border-bottom: 1px solid #eceef5;
}
.file-table td { padding: 12px 16px; border-bottom: 1px solid #f3f4fa; color: #2a2f52; }
.file-table tr:hover td { background: #fafaff; }
.fname { font-weight: 500; }
.muted { color: #8a90b0; }
.empty-row td { padding: 30px; }

.ai-result { background: #fff; border-radius: 12px; box-shadow: 0 2px 10px rgba(30,34,64,.05); padding: 16px 20px; }
.ai-result-head { display: flex; justify-content: space-between; align-items: center; font-size: 14px; font-weight: 700; color: #2a2f52; margin-bottom: 12px; }
.ai-empty { color: #9aa0bd; font-size: 13px; padding: 20px 0; text-align: center; }
.hit-card { border: 1px solid #eceef5; border-radius: 10px; padding: 10px 14px; margin-bottom: 10px; background: #fafaff; }
.hit-head { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px; }
.hit-name { color: #6366f1; font-weight: 600; }
.hit-score { color: #f59e0b; font-weight: 700; }
.hit-content { font-size: 12px; color: #6b7290; line-height: 1.6; }

.grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
  gap: 14px; min-height: 120px;
}
.card { background: #fff; border-radius: 12px; padding: 14px; box-shadow: 0 2px 10px rgba(30,34,64,.05); transition: transform .15s, box-shadow .15s; }
.card:hover { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(30,34,64,.1); }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; }
.file-icon { font-size: 26px; }
.card-name { margin: 10px 0 4px; font-size: 14px; font-weight: 600; color: #2a2f52; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-meta { font-size: 12px; color: #9aa0bd; }
.card-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.time { font-size: 11px; color: #b0b4cf; }

.pager { margin-top: 20px; justify-content: center; }
</style>