<template>
  <div class="share-page">
    <div class="card">
      <div class="brand">
        <div class="logo">N</div>
        <div class="brand-text">
          <span class="name">NovaFS</span>
          <span class="sub">文件分享</span>
        </div>
      </div>

      <div v-loading="loading" class="body">
        <el-result v-if="error" icon="error" :title="error">
          <template #extra>
            <el-button type="primary" @click="load">重试</el-button>
          </template>
        </el-result>

        <template v-else-if="share">
          <div class="file-info">
            <span class="ficon">{{ iconOf(share.suffix) }}</span>
            <div class="file-meta">
              <div class="fname">{{ share.fileName }}</div>
              <div class="fsub">
                {{ formatSize(share.fileSize) }}
                <span v-if="share.expireTime"> · 有效期至 {{ share.expireTime }}</span>
                <span> · 已查看 {{ share.viewCount }} 次</span>
              </div>
            </div>
          </div>

          <el-input
            v-if="needPassword"
            v-model="password"
            placeholder="该分享已加密，请输入提取码"
            class="pwd-input"
            @keyup.enter="load"
          >
            <template #append>
              <el-button @click="load">确认</el-button>
            </template>
          </el-input>

          <div v-if="previewing && previewType === 'image'" class="preview">
            <img :src="inlineUrl" alt="预览" />
          </div>
          <div v-else-if="previewing && previewType === 'pdf'" class="preview">
            <iframe :src="inlineUrl" title="预览" />
          </div>
          <div v-else-if="previewing && !previewType" class="preview-tip">
            该类型暂不支持在线预览，请下载后查看
          </div>

          <div class="actions">
            <el-button v-if="canPreview && previewType" type="primary" plain @click="previewing = !previewing">
              {{ previewing ? '收起预览' : '在线预览' }}
            </el-button>
            <el-button v-if="canDownload" type="primary" @click="doDownload">下载文件</el-button>
          </div>
          <div v-if="!canPreview && !canDownload" class="preview-tip">分享未开放任何权限</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { shareApi, type FileShareVO } from '../api/share'

const route = useRoute()
const shareCode = computed(() => String(route.params.code))

const loading = ref(false)
const error = ref('')
const share = ref<FileShareVO | null>(null)
const password = ref('')
const needPassword = ref(false)
const previewing = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    share.value = await shareApi.access(shareCode.value, password.value || undefined)
    needPassword.value = false
    previewing.value = false
  } catch (err) {
    const msg = err instanceof Error ? err.message : String(err)
    if (msg.includes('提取码')) {
      needPassword.value = true
      share.value = null
    } else {
      error.value = msg || '分享不存在或已失效'
      share.value = null
    }
  } finally {
    loading.value = false
  }
}

const canDownload = computed(
  () => !!share.value && share.value.scope.split(',').includes('DOWNLOAD')
)
const canPreview = computed(
  () => !!share.value && share.value.scope.split(',').includes('PREVIEW')
)
const previewType = computed(() => {
  const s = (share.value?.suffix || '').toLowerCase()
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(s)) return 'image'
  if (s === 'pdf') return 'pdf'
  return ''
})
const inlineUrl = computed(() => shareApi.downloadUrl(shareCode.value, password.value || undefined, true))

function doDownload() {
  window.open(shareApi.downloadUrl(shareCode.value, password.value || undefined), '_blank')
}

function iconOf(suffix: string | null): string {
  const s = (suffix || '').toLowerCase()
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
</script>

<style scoped>
.share-page {
  height: 100%;
  display: flex; align-items: center; justify-content: center;
  background:
    radial-gradient(1200px 600px at 20% -10%, rgba(59, 157, 255, 0.2), transparent),
    radial-gradient(1000px 500px at 90% 110%, rgba(43, 108, 236, 0.18), transparent),
    var(--novafs-bg);
}
.card {
  width: 460px; padding: 30px 32px 26px;
  background: var(--novafs-card-bg); border-radius: 16px;
  box-shadow: 0 24px 60px rgba(23, 66, 130, 0.18);
}
.brand { display: flex; align-items: center; gap: 12px; margin-bottom: 22px; }
.logo {
  width: 44px; height: 44px; border-radius: 12px;
  background: var(--novafs-gradient);
  color: #fff; font-weight: 800; font-size: 22px;
  display: flex; align-items: center; justify-content: center;
}
.brand-text { display: flex; flex-direction: column; }
.brand-text .name { font-size: 18px; font-weight: 800; color: var(--novafs-text); }
.brand-text .sub { font-size: 12px; color: var(--novafs-text-sub); }

.body { min-height: 200px; }
.file-info { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.ficon { font-size: 40px; }
.file-meta { min-width: 0; }
.fname {
  font-size: 16px; font-weight: 700; color: var(--novafs-text);
  word-break: break-all;
}
.fsub { font-size: 12px; color: var(--novafs-text-sub); margin-top: 4px; }

.pwd-input { margin-bottom: 16px; }
.actions { display: flex; gap: 10px; justify-content: center; margin-top: 18px; }
.preview { margin-top: 14px; }
.preview img { max-width: 100%; max-height: 46vh; border-radius: 8px; display: block; margin: 0 auto; }
.preview iframe { width: 100%; height: 46vh; border: 1px solid var(--novafs-card-border); border-radius: 8px; }
.preview-tip { text-align: center; font-size: 13px; color: var(--novafs-text-muted); padding: 14px 0; }
</style>
