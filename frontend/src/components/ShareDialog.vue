<template>
  <el-dialog
    :model-value="modelValue"
    :title="share ? '分享已创建' : '分享文件'"
    width="520px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="!share">
      <p class="share-file">📄 {{ fileName }}</p>
      <el-form label-width="86px">
        <el-form-item label="提取码">
          <el-input v-model="form.sharePwd" placeholder="留空则无需密码" show-password />
        </el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="留空则永久有效"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="查看上限">
          <el-input-number v-model="form.maxViewCount" :min="0" placeholder="0=不限" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下载上限">
          <el-input-number v-model="form.maxDownloadCount" :min="0" placeholder="0=不限" style="width: 100%" />
        </el-form-item>
        <el-form-item label="权限范围">
          <el-checkbox-group v-model="form.scope">
            <el-checkbox value="PREVIEW">允许预览</el-checkbox>
            <el-checkbox value="DOWNLOAD">允许下载</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
    </template>

    <template v-else>
      <div class="share-result">
        <div class="share-link">
          <span class="share-label">分享链接</span>
          <el-input :model-value="shareUrl" readonly>
            <template #append>
              <el-button @click="copyUrl">复制</el-button>
            </template>
          </el-input>
        </div>
        <div v-if="share.hasPassword" class="share-pwd">提取码：{{ form.sharePwd || '（已设置）' }}</div>
        <div class="share-meta">
          已查看 {{ share.viewCount }} 次 · 已下载 {{ share.downloadCount }} 次
          <span v-if="share.expireTime"> · 有效期至 {{ share.expireTime }}</span>
        </div>
      </div>
    </template>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ share ? '关闭' : '取消' }}</el-button>
      <el-button v-if="!share" type="primary" :loading="loading" @click="create">创建分享</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { shareApi, type FileShareVO } from '../api/share'

const props = defineProps<{
  modelValue: boolean
  fileId: string
  fileName: string
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const loading = ref(false)
const share = ref<FileShareVO | null>(null)
const form = reactive({
  sharePwd: '',
  expireTime: '',
  maxViewCount: 0,
  maxDownloadCount: 0,
  scope: ['PREVIEW', 'DOWNLOAD'] as string[]
})

const shareUrl = () =>
  share.value ? `${location.origin}/share/${share.value.shareCode}` : ''

async function create() {
  loading.value = true
  try {
    share.value = await shareApi.create({
      fileId: props.fileId,
      sharePwd: form.sharePwd || undefined,
      expireTime: form.expireTime || null,
      maxViewCount: form.maxViewCount || undefined,
      maxDownloadCount: form.maxDownloadCount || undefined,
      scope: form.scope.join(',')
    })
    ElMessage.success('分享创建成功')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function copyUrl() {
  try {
    await navigator.clipboard.writeText(shareUrl())
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.share-file { margin: 0 0 14px; font-size: 14px; font-weight: 600; color: var(--novafs-text); }
.share-result { padding: 4px 2px; }
.share-link { margin-bottom: 10px; }
.share-label { display: block; font-size: 12px; color: var(--novafs-text-sub); margin-bottom: 6px; }
.share-pwd { font-size: 13px; color: #e59a2b; margin-bottom: 6px; }
.share-meta { font-size: 12px; color: var(--novafs-text-muted); }
</style>
