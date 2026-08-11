<template>
  <div class="storage-page">
    <header class="storage-header">
      <div>
        <h2>存储设置</h2>
        <span class="sub">工作空间 #{{ currentWorkspaceId }} 的存储配置，上传时自动选择已启用的配置</span>
      </div>
      <el-button type="primary" @click="openCreate">＋ 新建配置</el-button>
    </header>

    <div v-loading="loading" class="list-wrap">
      <table class="config-table">
        <thead>
          <tr>
            <th>平台</th>
            <th>标识</th>
            <th style="width: 90px">启用</th>
            <th>备注</th>
            <th style="width: 150px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in list" :key="s.id">
            <td>
              <span class="platform-name">{{ platformName(s.platformIdentifier) }}</span>
            </td>
            <td class="mono">{{ s.platformIdentifier }}</td>
            <td>
              <el-switch :model-value="s.enabled" @change="(v: string | number | boolean) => toggleEnabled(s, v)" />
            </td>
            <td class="muted">{{ s.remark || '—' }}</td>
            <td>
              <el-button link type="primary" size="small" @click="openEdit(s)">编辑</el-button>
              <el-button link type="danger" size="small" @click="remove(s)">删除</el-button>
            </td>
          </tr>
          <tr v-if="!loading && list.length === 0">
            <td colspan="5" class="empty-row">
              <el-empty description="还没有存储配置，点击右上角新建" :image-size="70" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑存储配置' : '新建存储配置'"
      width="520px"
      destroy-on-close
    >
      <el-form label-width="72px">
        <el-form-item label="平台" required>
          <el-select
            v-model="form.platformIdentifier"
            placeholder="选择存储平台"
            style="width: 100%"
            :disabled="!!editing"
          >
            <el-option
              v-for="p in platforms"
              :key="p.identifier"
              :label="`${p.name} (${p.identifier})`"
              :value="p.identifier"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="配置 JSON">
          <el-input
            v-model="form.configData"
            type="textarea"
            :rows="5"
            placeholder='{"basePath":"D:/novafs-storage"} 本地存储示例；云存储填写 endpoint/AK/SK 等'
          />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { storageApi, type StoragePlatform, type StorageSetting } from '../api/storage'

const currentWorkspaceId = () => localStorage.getItem('novafs_workspace') || '1'

const list = ref<StorageSetting[]>([])
const platforms = ref<StoragePlatform[]>([])
const loading = ref(false)
const saving = ref(false)

const dialogVisible = ref(false)
const editing = ref<StorageSetting | null>(null)
const form = reactive({
  platformIdentifier: '',
  configData: '',
  enabled: true,
  remark: ''
})

onMounted(async () => {
  try {
    platforms.value = await storageApi.listPlatforms()
  } catch {
    /* 拦截器已提示 */
  }
  await load()
})

async function load() {
  loading.value = true
  try {
    list.value = await storageApi.listAdmin(currentWorkspaceId())
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

function platformName(identifier: string): string {
  return platforms.value.find((p) => p.identifier === identifier)?.name || identifier
}

function openCreate() {
  editing.value = null
  form.platformIdentifier = ''
  form.configData = ''
  form.enabled = true
  form.remark = ''
  dialogVisible.value = true
}

function openEdit(s: StorageSetting) {
  editing.value = s
  form.platformIdentifier = s.platformIdentifier
  form.configData = s.configData || ''
  form.enabled = s.enabled
  form.remark = s.remark || ''
  dialogVisible.value = true
}

async function submit() {
  if (!form.platformIdentifier) {
    ElMessage.warning('请选择存储平台')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await storageApi.update(editing.value.id, {
        configData: form.configData.trim() || undefined,
        enabled: form.enabled,
        remark: form.remark.trim() || undefined
      })
    } else {
      await storageApi.create(currentWorkspaceId(), {
        platformIdentifier: form.platformIdentifier,
        configData: form.configData.trim(),
        enabled: form.enabled,
        remark: form.remark.trim() || undefined
      })
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(s: StorageSetting, v: string | number | boolean) {
  try {
    await storageApi.update(s.id, { enabled: Boolean(v) })
    s.enabled = Boolean(v)
    ElMessage.success(v ? '已启用' : '已停用')
  } catch {
    /* 失败时回滚开关状态 */
    s.enabled = !v
  }
}

async function remove(s: StorageSetting) {
  try {
    await ElMessageBox.confirm(`确定删除「${platformName(s.platformIdentifier)}」配置吗？历史文件仍可访问。`, '删除配置')
    await storageApi.remove(s.id)
    ElMessage.success('已删除')
    await load()
  } catch {
    /* 取消或已提示 */
  }
}
</script>

<style scoped>
.storage-page { padding: 20px 28px 28px; }
.storage-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 16px;
}
.storage-header h2 { margin: 0 0 4px; font-size: 20px; color: var(--novafs-text); }
.storage-header .sub { font-size: 13px; color: var(--novafs-text-sub); }

.list-wrap {
  background: #fff;
  border: 1px solid var(--novafs-card-border);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(59, 157, 255, 0.06);
  overflow: hidden;
}
.config-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.config-table th {
  text-align: left; padding: 12px 16px; background: #f4f9ff;
  color: var(--novafs-text-sub); font-weight: 600; font-size: 12px;
  border-bottom: 1px solid var(--novafs-card-border);
}
.config-table td { padding: 12px 16px; border-bottom: 1px solid #eef4fc; color: var(--novafs-text); }
.config-table tr:hover td { background: #f7fbff; }
.platform-name { font-weight: 600; }
.mono { font-family: Consolas, monospace; color: var(--novafs-text-sub); }
.muted { color: var(--novafs-text-sub); }
.empty-row td { padding: 30px; }
</style>
