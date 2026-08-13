<template>
  <div class="files-page">
    <header class="files-header">
      <div>
        <h2>我的文件</h2>
        <span class="sub">共 {{ total }} 项 · 支持秒传 / 分片上传 / 断点续传</span>
      </div>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索文件名…"
          clearable
          class="search-input"
          @keyup.enter="doSearch"
          @clear="clearSearch"
        >
          <template #append>
            <el-button @click="doSearch">搜索</el-button>
          </template>
        </el-input>
        <el-button type="primary" plain @click="openCreateFolder">＋ 新建文件夹</el-button>
        <el-button @click="load()">刷新</el-button>
        <router-link to="/recycle">
          <el-button>🗑 回收站</el-button>
        </router-link>
      </div>
    </header>

    <UploadPanel :parent-id="parentId" :parent-name="currentDirName" @uploaded="load()" />

    <!-- 面包屑 / 搜索态 -->
    <div v-if="searching" class="crumbs search-crumb">
      <span>🔍 搜索「{{ searchKeyword }}」的结果（{{ total }} 项）</span>
      <el-button link type="primary" @click="clearSearch">清除搜索返回目录</el-button>
    </div>
    <div v-else class="crumbs">
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
          <tr
            v-for="f in files"
            :key="f.id"
            :class="{
              dir: f.isDir,
              'drop-target': dragFile && dragFile.id !== f.id && f.isDir
            }"
            draggable="true"
            @dragstart="onDragStart($event, f)"
            @dragend="onDragEnd"
            @dragover="onDragOver($event, f)"
            @drop="onDrop($event, f)"
          >
            <td>
              <span class="fname" :title="f.originalName" @dblclick.prevent="openFolder(f)">
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
      @current-change="onPageChange"
    />

    <PreviewDialog
      v-model="previewVisible"
      :file-id="previewFile?.id ?? ''"
      :file-name="previewFile?.originalName ?? ''"
    />
    <ShareDialog v-model="shareVisible" :file-id="shareFile?.id ?? ''" :file-name="shareFile?.originalName ?? ''" />

    <!-- 新建文件夹 / 重命名 -->
    <el-dialog
      v-model="nameDialogVisible"
      :title="nameDialogMode === 'create' ? '新建文件夹' : '重命名'"
      width="420px"
      @closed="nameValue = ''"
    >
      <el-input
        v-model="nameValue"
        :placeholder="nameDialogMode === 'create' ? '请输入文件夹名称' : '请输入新名称（后缀将自动保留）'"
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
      <div class="move-toolbar">
        <el-button size="small" plain @click="moveToRoot">移动到根目录</el-button>
        <span v-if="moveTargetName" class="move-current">当前选中：{{ moveTargetName }}</span>
      </div>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileApi, fetchBlob, downloadUrl, type FileInfo } from '../api/file'
import { useUploadStore } from '../stores/upload'
import { registerFilesUp, setBackBlock, consumeBackBlock } from '../utils/filesNav'
import UploadPanel from '../components/UploadPanel.vue'
import PreviewDialog from '../components/PreviewDialog.vue'
import ShareDialog from '../components/ShareDialog.vue'

const workspaceId = () => localStorage.getItem('novafs_workspace') || '1'

const files = ref<FileInfo[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const parentId = ref<string | null>(null)
const crumbs = ref<{ id: string | null; name: string }[]>([{ id: null, name: '全部文件' }])
const currentDirName = computed(() => {
  const last = crumbs.value[crumbs.value.length - 1]
  return last && last.id !== null ? last.name : '根目录'
})

// 上传完成（成功/失败）后自动刷新当前列表
const uploadStore = useUploadStore()
watch(() => uploadStore.completedCount, () => load())

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
const moveTarget = ref<string | null>(null)
const moveTargetName = ref('')

const searchKeyword = ref('')
const searching = ref(false)

const dragFile = ref<FileInfo | null>(null)

onMounted(() => {
  registerFilesUp(() => goUp())
  window.addEventListener('mousedown', onMouseDown)
  load(1)
})
onBeforeUnmount(() => {
  registerFilesUp(null)
  window.removeEventListener('mousedown', onMouseDown)
})

/**
 * 鼠标后退侧键：preventDefault 尽力阻止；路由守卫拦截兜底（返回上级而非跳历史）。
 * 若无导航发生（preventDefault 生效），300ms 后手动返回上级。
 */
function onMouseDown(e: MouseEvent) {
  if (e.button !== 3) return
  e.preventDefault()
  setBackBlock()
  setTimeout(() => {
    if (consumeBackBlock()) {
      goUp()
    }
  }, 300)
}

function goUp() {
  if (searching.value) {
    clearSearch()
    return
  }
  if (crumbs.value.length > 1) {
    goCrumb(crumbs.value.length - 2)
  }
}

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

async function doSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  searching.value = true
  loading.value = true
  try {
    const resp = await fileApi.search(workspaceId(), kw, page.value, pageSize.value)
    files.value = resp.records
    total.value = resp.total
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  searchKeyword.value = ''
  searching.value = false
  load(1)
}

function onPageChange(p: number) {
  page.value = p
  if (searching.value) {
    doSearch()
  } else {
    load(p)
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
  // 文件只允许修改主名，后缀自动保留
  nameValue.value = f.isDir ? f.originalName : splitExt(f.originalName).base
  nameFile.value = f
  nameDialogVisible.value = true
}

function splitExt(name: string): { base: string; ext: string } {
  const idx = name.lastIndexOf('.')
  if (idx <= 0) return { base: name, ext: '' }
  return { base: name.slice(0, idx), ext: name.slice(idx) }
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
      const f = nameFile.value!
      // 后缀保护：文件自动拼接原后缀（用户已输入相同后缀则不重复）
      const finalName =
        f.isDir || !f.suffix
          ? name
          : name.toLowerCase().endsWith('.' + f.suffix.toLowerCase())
            ? name
            : `${name}.${f.suffix}`
      await fileApi.rename(f.id, finalName)
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
  moveTargetName.value = ''
  moveVisible.value = true
}

/** 拖拽开始：记录被拖拽的文件/文件夹 */
function onDragStart(e: DragEvent, f: FileInfo) {
  dragFile.value = f
  e.dataTransfer?.setData('text/plain', f.id)
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move'
}

function onDragEnd() {
  dragFile.value = null
}

/** 拖到文件夹行上方时允许放置 */
function onDragOver(e: DragEvent, f: FileInfo) {
  if (!dragFile.value || !f.isDir || dragFile.value.id === f.id) return
  e.preventDefault()
  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
}

/** 松手：移动到目标文件夹 */
function onDrop(e: DragEvent, f: FileInfo) {
  e.preventDefault()
  const file = dragFile.value
  dragFile.value = null
  if (!file || !f.isDir || file.id === f.id) return
  moveTo(file, f.id)
}

/** 执行移动并刷新 */
function moveTo(file: FileInfo, targetParentId: string | null) {
  fileApi
    .move(file.id, targetParentId)
    .then(() => {
      ElMessage.success(`已移动到「${targetFolderName(targetParentId)}」`)
      return load()
    })
    .catch(() => {
      /* 拦截器已提示 */
    })
}

function targetFolderName(parentId: string | null): string {
  if (!parentId) return '根目录'
  return files.value.find((x) => x.id === parentId)?.originalName ?? '目标文件夹'
}

function moveToRoot() {
  moveTarget.value = null
  moveTargetName.value = ''
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

function onMoveNodeClick(data: { id: string; name: string }) {
  moveTarget.value = data.id
  moveTargetName.value = data.name
}

async function doMove() {
  if (!moveFile.value) return
  moveVisible.value = false
  moveTo(moveFile.value, moveTarget.value)
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
.header-actions { display: flex; gap: 8px; align-items: center; }
.header-actions a { text-decoration: none; }
.search-input { width: 220px; }

.crumbs { margin: 14px 0 10px; }
.crumb { cursor: pointer; font-size: 13px; }
.crumb:hover { color: var(--novafs-primary); }
.crumb.active { color: var(--novafs-primary-dark); font-weight: 600; }
.search-crumb { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--novafs-text); }

.list-wrap {
  background: var(--novafs-card-bg);
  border: 1px solid var(--novafs-card-border);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(59, 157, 255, 0.06);
  overflow: hidden;
}
.file-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.file-table th {
  text-align: left; padding: 12px 16px; background: var(--novafs-table-head);
  color: var(--novafs-text-sub); font-weight: 600; font-size: 12px;
  border-bottom: 1px solid var(--novafs-card-border);
}
.file-table td { padding: 12px 16px; border-bottom: 1px solid var(--novafs-divider); color: var(--novafs-text); }
.file-table tr:hover td { background: var(--novafs-hover); }
.fname { font-weight: 500; cursor: default; user-select: none; -webkit-user-select: none; }
tr.dir .fname { cursor: pointer; }
tr[draggable="true"] { cursor: grab; }
tr.drop-target td { background: var(--novafs-primary-light) !important; }
tr.drop-target .fname { outline: 1px dashed var(--novafs-primary); outline-offset: 2px; border-radius: 4px; }
tr.dir .fname:hover { color: var(--novafs-primary); }
.ficon { margin-right: 6px; }
.muted { color: var(--novafs-text-sub); }
.empty-row td { padding: 30px; }

.pager { margin-top: 20px; justify-content: center; }
.move-tree { max-height: 300px; overflow-y: auto; }
.move-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.move-current { font-size: 12px; color: var(--novafs-primary); }
.move-tip { font-size: 12px; color: var(--novafs-text-muted); margin-top: 8px; }
</style>
