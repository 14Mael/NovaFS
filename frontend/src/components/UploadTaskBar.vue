<template>
  <div v-if="visible" class="task-bar" :class="{ collapsed }">
    <!-- 收起态：悬浮按钮 -->
    <button v-if="collapsed" class="fab" @click="collapsed = false">
      <span class="fab-icon">⬆</span>
      <span v-if="activeCount" class="fab-badge">{{ activeCount }}</span>
    </button>

    <!-- 展开态：任务面板 -->
    <div v-else class="panel">
      <div class="panel-head">
        <span>上传任务</span>
        <div class="head-actions">
          <el-button v-if="finishedCount" link size="small" @click="store.clearFinished()">清除已完成</el-button>
          <el-button link size="small" @click="collapsed = true">收起</el-button>
        </div>
      </div>
      <div class="task-list">
        <div v-if="store.tasks.length === 0" class="empty">暂无上传任务</div>
        <div v-for="task in store.tasks" :key="task.id" class="task">
          <div class="task-head">
            <span class="task-name" :title="task.name">{{ task.name }}</span>
            <span class="task-status" :class="task.status">{{ task.statusText }}</span>
          </div>
          <el-progress
            :percentage="task.percent"
            :stroke-width="6"
            :status="task.status === 'error' ? 'exception' : task.status === 'done' ? 'success' : undefined"
          />
          <div v-if="task.error" class="task-error">{{ task.error }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useUploadStore } from '../stores/upload'

const store = useUploadStore()
const collapsed = ref(true)

const workspaceId = () => localStorage.getItem('novafs_workspace') || '1'

const activeCount = computed(
  () => store.tasks.filter((t) => ['hashing', 'uploading', 'merging'].includes(t.status)).length
)
const finishedCount = computed(() => store.tasks.filter((t) => t.status === 'done' || t.status === 'error').length)

/** 无任务时整体隐藏；新任务到来时自动展开 */
const visible = computed(() => store.tasks.length > 0)
watch(
  () => store.tasks.length,
  (n, old) => {
    if (n > old) collapsed.value = false
  }
)

onMounted(() => {
  store.connectSse(workspaceId)
})
</script>

<style scoped>
.task-bar {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 3000;
  font-size: 13px;
}
.fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: var(--novafs-gradient);
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  box-shadow: 0 8px 22px rgba(59, 157, 255, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}
.fab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background: #e5484d;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}
.panel {
  width: 340px;
  max-height: 420px;
  background: var(--novafs-card-bg);
  border: 1px solid var(--novafs-card-border);
  border-radius: 12px;
  box-shadow: 0 18px 50px rgba(23, 66, 130, 0.2);
  overflow: hidden;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  background: var(--novafs-table-head);
  font-weight: 700;
  color: var(--novafs-text);
  border-bottom: 1px solid var(--novafs-card-border);
}
.head-actions { display: flex; align-items: center; }
.task-list {
  max-height: 340px;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.empty { text-align: center; color: var(--novafs-text-muted); padding: 16px 0; }
.task { border-bottom: 1px solid var(--novafs-divider); padding-bottom: 10px; }
.task:last-child { border-bottom: none; }
.task-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.task-name {
  font-weight: 600;
  color: var(--novafs-text);
  max-width: 65%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-status { font-size: 12px; color: var(--novafs-text-sub); }
.task-status.done { color: #34a853; }
.task-status.error { color: #e5484d; }
.task-error { font-size: 12px; color: #e5484d; margin-top: 4px; }
</style>
