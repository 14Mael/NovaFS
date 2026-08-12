<template>
  <div class="join-page">
    <div class="card">
      <div class="logo">N</div>
      <h2>接受工作空间邀请</h2>

      <div v-if="error" class="err">{{ error }}</div>

      <template v-else-if="!result">
        <p class="desc">你已受邀加入一个 NovaFS 工作空间，点击下方按钮加入。</p>
        <el-button type="primary" size="large" :loading="accepting" @click="accept">接受邀请</el-button>
      </template>

      <template v-else>
        <div class="ok">
          🎉 已加入「{{ result.name }}」工作空间
        </div>
        <el-button type="primary" @click="goFiles">进入工作空间</el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { workspaceApi, type Workspace } from '../api/workspace'

const route = useRoute()
const router = useRouter()

const token = ref(String(route.query.token || ''))
const accepting = ref(false)
const error = ref('')
const result = ref<Workspace | null>(null)

onMounted(() => {
  if (!token.value) {
    error.value = '邀请链接无效：缺少邀请令牌'
  }
})

async function accept() {
  if (!token.value) return
  accepting.value = true
  error.value = ''
  try {
    result.value = await workspaceApi.acceptInvitation(token.value)
    ElMessage.success('已加入工作空间')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '接受邀请失败'
  } finally {
    accepting.value = false
  }
}

function goFiles() {
  if (result.value) {
    localStorage.setItem('novafs_workspace', result.value.id)
  }
  router.push('/files')
}
</script>

<style scoped>
.join-page {
  height: 100%;
  display: flex; align-items: center; justify-content: center;
  background:
    radial-gradient(1200px 600px at 20% -10%, rgba(59, 157, 255, 0.2), transparent),
    radial-gradient(1000px 500px at 90% 110%, rgba(43, 108, 236, 0.18), transparent),
    var(--novafs-bg);
}
.card {
  width: 420px; padding: 36px 34px 30px; text-align: center;
  background: #fff; border-radius: 16px;
  box-shadow: 0 24px 60px rgba(23, 66, 130, 0.18);
}
.logo {
  width: 52px; height: 52px; margin: 0 auto 12px; border-radius: 14px;
  background: var(--novafs-gradient);
  color: #fff; font-weight: 800; font-size: 28px;
  display: flex; align-items: center; justify-content: center;
}
h2 { margin: 0 0 14px; font-size: 20px; color: var(--novafs-text); }
.desc { font-size: 14px; color: var(--novafs-text-sub); margin: 0 0 20px; }
.err { color: #e5484d; font-size: 14px; padding: 8px 0; }
.ok { font-size: 16px; font-weight: 700; color: #34a853; margin: 6px 0 20px; }
</style>
