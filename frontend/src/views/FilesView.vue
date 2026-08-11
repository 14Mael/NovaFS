<template>
  <div class="files-page">
    <header class="files-header">
      <div>
        <h2>我的文件</h2>
        <span class="sub">共 {{ total }} 项 · 支持秒传 / 分片上传 / 断点续传</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" plain @click="openCreateFolder">＋ 新建文件夹</el-button>
        <el-button @click="load()">刷新</el-button>
        <router-link to="/recycle">
          <el-button>🗑 回收站</el-button>
        </router-link>
      </div>
    </header>

    <UploadPanel @uploaded="load()" />

    <!-- 面包屑 -->
    <div class="crumbs">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item
          v-for="(c, i) in crumbs"
          :key="c.id ?? 'root'"
          :to="i < crumbs.length - 1 ? undefined : undefined"
        >
          <span class="crumb" :class="{ active: i === crumbs.length - 1 }" @click="goCrumb(i)">
            {{ c.name }}
          </span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 文件表格 -->
    <div v-loading="loading" class="list-wrap">
      <table class="file-table">
        <thead>
          <tr>
            <th>名称</th>
            <th style="width: 110px">大小</th>
            <th style="width: 170px">上传时间</th>
            <th style="width: 300px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="f in files" :key="f.id" :class="{ dir: f.isDir }">
            <td>
              <span class="fname" :title="f.originalName" @dblclick="openFolder(f)">
                <span class="ficon">{{ iconOf(f) }}</span>
                {{ f.originalName }}
              </span>
            </td>
            <td class="muted">{{ f.isDir ? '—' : formatSize(f.size) }}</td>
            <td class="muted">{{ formatTime(f.uploadTime) }}</td>
            <td>
              <template v-if="!f.isDir">
                <el-button link type="primary" size="small" @click="openPreview(f)">预览</el-button>
                <el-button link type="primary" size="small" @click="download(f)">下载</el-button>
                <el-button link type="primary" size="small" @click="openShare(f)">分享</el-button>
              </template>
              <el-button link type="primary" size="small" @click="openRename(f)">重命名</el-button>
              <el-button link type="primary" size="small" @click="openMove(f)">移动到</el-button>
              <el-button link type="danger" size="small" @click="remove(f)">删除</el-button>
            </td>
          </tr>
          <tr v-if="!loading && files.length === 0">
            <td colspan="4" class="empty-row">
              <el-empty description="还没有文件，拖拽或点击上方区域上传" :image-size="70" />
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

    <PreviewDialog
      v-model="previewVisible"
      :file-id="previewFile?.id ?? 0"
      :file-name="previewFile?.originalName ?? ''"
    />
    <ShareDialog v-model="shareVisible" :file-id="shareFile?.id ?? 0" :file-name="shareFile?.originalName ?? ''" />

    <!-- 新建文件夹 / 重命名 -->
    <el-dialog
      v-model="nameDialogVisible"
      :title="nameDialogMode === 'create' ? '新建文件夹' : '重命名'"
      width="420px"
      @closed="nameValue = ''"
    >
      <el-input
        v-model="nameValue"
        :placeholder="nameDialogMode === 'create' ? '请输入文件夹名称' : '请输入新名称'"
        maxlength="255"
        @keyup.enter="submitName"
      />
      <template #footer>
        <el-button @click="nameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitName">确定</el-button>
      </template>
    </el-dialog>

    <!-- 移动到 -->
    <el-dialog v-model="moveVisible" title="移动到" width="420px">
      <el-tree
        :props="{ label: 'name', children: 'children' }"
        :load="loadMoveChildren"
        lazy
        highlight-current
        class="move-tree"
        @node-click="onMoveNodeClick"
      />
      <div class="move-tip">不选择任何目录则移动到根目录</div>
      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" @click="doMove">确定移动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileApi, fetchBlob, downloadUrl, type FileInfo } from '../api/file'
import UploadPanel from '../components/UploadPanel.vue'
import PreviewDialog from '../components/PreviewDialog.vue'
import ShareDialog from '../components/ShareDialog.vue'

const workspaceId = () => Number(localStorage.getItem('novafs_workspace') || '1')

const files = ref<FileInfo[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const parentId = ref<number | null>(null)
const crumbs = ref<{ id: number | null; name: string }[]>([{ id: null, name: '全部文件' }])

const previewFile = ref<FileInfo | null>(null)
const previewVisible = ref(false)
const shareFile = ref<FileInfo | null>(null)
const shareVisible = ref(false)

const nameDialogVisible = ref(false)
const nameDialogMode = ref<'create' | 'rename'>('create')
const nameValue = ref('')
const nameFile = ref<FileInfo | null>(null)

const moveVisible = ref(false)
const moveFile = ref<FileInfo | null>(null)
const moveTarget = ref<number | null>(null)

onMounted(() => load(1))

async function load(p = page.value) {
  page.value = p
  loading.value = true
  try {
    const resp = await fileApi.list(workspaceId(), {
      parentId: parentId.value,
      page: p,
      pageSize: pageSize.value
    })
    files.value = resp.records
    total.value = resp.total
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function openFolder(f: FileInfo) {
  if (!f.isDir) return
  parentId.value = f.id
  crumbs.value.push({ id: f.id, name: f.originalName })
  load(1)
}

function goCrumb(i: number) {
  if (i === crumbs.value.length - 1) return
  crumbs.value = crumbs.value.slice(0, i + 1)
  parentId.value = crumbs.value[i].id
  load(1)
}

function openPreview(f: FileInfo) {
  previewFile.value = f
  previewVisible.value = true
}

function openShare(f: FileInfo) {
  shareFile.value = f
  shareVisible.value = true
}

function openCreateFolder() {
  nameDialogMode.value = 'create'
  nameValue.value = ''
  nameFile.value = null
  nameDialogVisible.value = true
}

function openRename(f: FileInfo) {
  nameDialogMode.value = 'rename'
  nameValue.value = f.originalName
  nameFile.value = f
  nameDialogVisible.value = true
}

async function submitName() {
  const name = nameValue.value.trim()
  if (!name) {
    ElMessage.warning('请输入名称')
    return
  }
  try {
    if (nameDialogMode.value === 'create') {
      await fileApi.createFolder(workspaceId(), parentId.value, name)
      ElMessage.success('文件夹已创建')
    } else {
      await fileApi.rename(nameFile.value!.id, name)
      ElMessage.success('已重命名')
    }
    nameDialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

function openMove(f: FileInfo) {
  moveFile.value = f
  moveTarget.value = null
  moveVisible.value = true
}

/** 懒加载目录树：根节点加载一级子文件夹，展开时加载更深层 */
async function loadMoveChildren(node: any, resolve: (data: unknown[]) => void) {
  const parentId = node.level === 0 ? null : node.data.id
  try {
    const resp = await fileApi.list(workspaceId(), { parentId, page: 1, pageSize: 200 })
    resolve(
      resp.records
        .filter((x) => x.isDir)
        .map((x) => ({ id: x.id, name: x.originalName }))
    )
  } catch {
    resolve([])
  }
}

function onMoveNodeClick(data: { id: number }) {
  moveTarget.value = data.id
}

async function doMove() {
  if (!moveFile.value) return
  try {
    await fileApi.move(moveFile.value.id, moveTarget.value)
    ElMessage.success('已移动')
    moveVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  }
}

async function download(f: FileInfo) {
  try {
    const blob = await fetchBlob(downloadUrl(f.id))
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = f.originalName
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

async function remove(f: FileInfo) {
  try {
    await ElMessageBox.confirm(`确定删除「${f.originalName}」吗？删除后进入回收站。`, '删除文件')
    await fileApi.remove(f.id)
    ElMessage.success('已移入回收站')
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
  if (['zip', 'tar', 'gz', 'rar', '7z'].includes(s)) return '🗜️'
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
.files-page { padding: 20px 28px 28px; }

.files-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.files-header h2 { margin: 0 0 4px; font-size: 20px; color: var(--novafs-text); }
.files-header .sub { font-size: 13px; color: var(--novafs-text-sub); }
.header-actions { display: flex; gap: 8px; }
.header-actions a { text-decoration: none; }

.crumbs { margin: 14px 0 10px; }
.crumb { cursor: pointer; font-size: 13px; }
.crumb:hover { color: var(--novafs-primary); }
.crumb.active { color: var(--novafs-primary-dark); font-weight: 600; }

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
.fname { font-weight: 500; cursor: default; }
tr.dir .fname { cursor: pointer; }
tr.dir .fname:hover { color: var(--novafs-primary); }
.ficon { margin-right: 6px; }
.muted { color: var(--novafs-text-sub); }
.empty-row td { padding: 30px; }

.pager { margin-top: 20px; justify-content: center; }
.move-tree { max-height: 300px; overflow-y: auto; }
.move-tip { font-size: 12px; color: var(--novafs-text-muted); margin-top: 8px; }
</style>
