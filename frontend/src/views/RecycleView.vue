<template>
  <div class="recycle-page">
    <header class="recycle-header">
      <div>
        <h2>回收站</h2>
        <span class="sub">已删除文件保留于此，可恢复或彻底删除</span>
      </div>
      <div class="header-actions">
        <router-link to="/files">
          <el-button>← 返回文件</el-button>
        </router-link>
      </div>
    </header>

    <div v-loading="loading" class="list-wrap">
      <table class="file-table">
        <thead>
          <tr>
            <th>名称</th>
            <th style="width: 110px">大小</th>
            <th style="width: 170px">上传时间</th>
            <th style="width: 170px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in files" :key="f.id">
            <td>
              <span class="fname">{{ iconOf(f) }} {{ f.originalName }}</span>
            </td>
            <td class="muted">{{ f.isDir ? '—' : formatSize(f.size) }}</td>
            <td class="muted">{{ formatTime(f.uploadTime) }}</td>
            <td>
              <el-button link type="primary" size="small" @click="restore(f)">恢复</el-button>
              <el-button link type="danger" size="small" @click="purge(f)">彻底删除</el-button>
            </td>
          </tr>
          <tr v-if="!loading && files.length === 0">
            <td colspan="4" class="empty-row">
              <el-empty description="回收站是空的" :image-size="70" />
            </td>
          </tr>
        </tbody>
      </table>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileApi, type FileInfo } from '../api/file'

const workspaceId = () => localStorage.getItem('novafs_workspace') || '1'

const files = ref<FileInfo[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

onMounted(() => load(1))

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const resp = await fileApi.recycle(workspaceId(), p, pageSize.value)
    files.value = resp.records
    total.value = resp.total
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function restore(f: FileInfo) {
  try {
    await fileApi.restore(f.id)
    ElMessage.success(`已恢复「${f.originalName}」`)
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function purge(f: FileInfo) {
  try {
    await ElMessageBox.confirm(
      `彻底删除「${f.originalName}」将同时删除存储中的文件，且无法恢复。确定继续吗？`,
      '彻底删除',
      { type: 'warning' }
    )
    await fileApi.purge(f.id)
    ElMessage.success('已彻底删除')
    await load()
  } catch {
    /* 取消或已提示 */
  }
}

function iconOf(f: FileInfo): string {
  if (f.isDir) return '📁'
  const s = (f.suffix || '').toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(s)) return '🖼️'
  if (['mp4', 'webm', 'mov', 'avi'].includes(s)) return '🎬'
  if (['mp3', 'wav', 'flac', 'ogg'].includes(s)) return '🎵'
  if (s === 'pdf') return '📕'
  if (['txt', 'md', 'log', 'json', 'html', 'css', 'js', 'xml'].includes(s)) return '📄'
  return '📎'
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

function formatTime(t: string): string {
  return t ? t.slice(0, 16).replace('T', ' ') : ''
}
</script>

<style scoped>
.recycle-page { padding: 20px 28px 28px; }
.recycle-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.recycle-header h2 { margin: 0 0 4px; font-size: 20px; color: var(--novafs-text); }
.recycle-header .sub { font-size: 13px; color: var(--novafs-text-sub); }
.header-actions a { text-decoration: none; }

.list-wrap {
  background: #fff;
  border: 1px solid var(--novafs-card-border);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(59, 157, 255, 0.06);
  overflow: hidden;
}
.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th {
  text-align: left; padding: 12px 16px; background: #f4f9ff;
  color: var(--novafs-text-sub); font-weight: 600; font-size: 12px;
  border-bottom: 1px solid var(--novafs-card-border);
}
.file-table td { padding: 12px 16px; border-bottom: 1px solid #eef4fc; color: var(--novafs-text); }
.file-table tr:hover td { background: #f7fbff; }
.fname { font-weight: 500; }
.muted { color: var(--novafs-text-sub); }
.empty-row td { padding: 30px; }
.pager { margin-top: 20px; justify-content: center; }
</style>
