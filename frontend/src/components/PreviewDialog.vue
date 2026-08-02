<template>
  <el-dialog
    :model-value="modelValue"
    :title="fileName"
    width="760px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @open="load"
    @closed="cleanup"
  >
    <div v-loading="loading" class="preview-body">
      <img v-if="type === 'IMAGE'" :src="objectUrl" class="preview-img" alt="预览" />
      <video v-else-if="type === 'VIDEO'" :src="objectUrl" controls class="preview-media" />
      <audio v-else-if="type === 'AUDIO'" :src="objectUrl" controls class="preview-audio" />
      <iframe v-else-if="type === 'PDF'" :src="objectUrl" class="preview-pdf" title="预览" />
      <pre v-else-if="type === 'TEXT'" class="preview-text">{{ content }}</pre>
      <el-empty v-else-if="!loading" description="该类型暂不支持预览" />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fileApi, fetchBlob, previewContentUrl } from '../api/file'

const props = defineProps<{
  modelValue: boolean
  fileId: number
  fileName: string
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const loading = ref(false)
const type = ref('')
const content = ref('')
const objectUrl = ref('')

watch(
  () => props.fileId,
  () => {
    if (props.modelValue) load()
  }
)

async function load() {
  loading.value = true
  type.value = ''
  content.value = ''
  try {
    const info = await fileApi.preview(props.fileId)
    type.value = info.previewType
    if (info.previewType === 'TEXT') {
      content.value = info.content || ''
    } else if (['IMAGE', 'VIDEO', 'AUDIO', 'PDF'].includes(info.previewType)) {
      const blob = await fetchBlob(previewContentUrl(props.fileId))
      objectUrl.value = URL.createObjectURL(blob)
    }
  } catch {
    ElMessage.error('预览加载失败')
  } finally {
    loading.value = false
  }
}

function cleanup() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ''
  }
}
</script>

<style scoped>
.preview-body { min-height: 260px; display: flex; align-items: center; justify-content: center; }
.preview-img { max-width: 100%; max-height: 62vh; border-radius: 8px; }
.preview-media { max-width: 100%; max-height: 62vh; }
.preview-audio { width: 90%; }
.preview-pdf { width: 100%; height: 62vh; border: none; border-radius: 8px; }
.preview-text {
  width: 100%; max-height: 62vh; overflow: auto; margin: 0;
  background: #f7faff; border: 1px solid var(--novafs-card-border);
  border-radius: 8px; padding: 14px 16px;
  font-size: 13px; line-height: 1.7; color: var(--novafs-text);
  white-space: pre-wrap; word-break: break-all;
}
</style>
