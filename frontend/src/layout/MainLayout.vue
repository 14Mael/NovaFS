<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">N</div>
        <div class="brand-text">
          <span class="name">NovaFS</span>
          <span class="sub">云盘 · 文档 · AI</span>
        </div>
      </div>

      <nav class="nav">
        <router-link to="/files" class="nav-item">
          <span class="icon">▤</span> 我的文件
        </router-link>
        <router-link to="/chat" class="nav-item">
          <span class="icon">✦</span> AI 问答
        </router-link>
      </nav>

      <div class="footer">
        <div class="ws" @click="editWorkspace" title="点击切换工作空间">
          <span class="ws-label">工作空间</span>
          <span class="ws-id">#{{ workspaceId }}</span>
        </div>
        <div class="user">
          <el-avatar :size="30" class="avatar">{{ initial }}</el-avatar>
          <div class="user-meta">
            <span class="uname">{{ user?.nickname || user?.username }}</span>
          </div>
          <el-dropdown trigger="click">
            <span class="more">⋯</span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </aside>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const user = computed(() => auth.user)
const initial = computed(() => (user.value?.nickname || user.value?.username || 'U').slice(0, 1).toUpperCase())

const workspaceId = computed(() => Number(localStorage.getItem('novafs_workspace') || '1'))

async function editWorkspace() {
  try {
    const { value } = await ElMessageBox.prompt('输入工作空间 ID', '切换工作空间', {
      inputValue: String(workspaceId.value),
      inputPattern: /^\d+$/,
      inputErrorMessage: '必须是数字'
    })
    localStorage.setItem('novafs_workspace', value)
    location.reload()
  } catch { /* 取消 */ }
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { display: flex; height: 100%; }

.sidebar {
  width: 220px;
  background: var(--novafs-sidebar);
  color: #cdd2e3;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.brand { display: flex; align-items: center; gap: 10px; padding: 20px 18px 26px; }
.logo {
  width: 36px; height: 36px; border-radius: 10px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #fff; font-weight: 800; font-size: 20px;
  display: flex; align-items: center; justify-content: center;
}
.brand-text { display: flex; flex-direction: column; }
.brand-text .name { color: #fff; font-weight: 700; font-size: 17px; }
.brand-text .sub { font-size: 11px; color: #7c83a3; }

.nav { flex: 1; padding: 0 12px; display: flex; flex-direction: column; gap: 4px; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px;
  color: #cdd2e3; text-decoration: none; font-size: 14px;
  transition: background .15s;
}
.nav-item .icon { width: 20px; text-align: center; }
.nav-item:hover { background: rgba(255,255,255,.06); }
.nav-item.router-link-active { background: var(--novafs-sidebar-active); color: #fff; }

.footer { padding: 14px 12px; border-top: 1px solid rgba(255,255,255,.08); }
.ws { display: flex; justify-content: space-between; padding: 6px 10px; border-radius: 8px; cursor: pointer; margin-bottom: 10px; }
.ws:hover { background: rgba(255,255,255,.06); }
.ws-label { font-size: 12px; color: #7c83a3; }
.ws-id { font-size: 12px; color: #a5b4fc; font-weight: 600; }
.user { display: flex; align-items: center; gap: 10px; padding: 4px 6px; }
.avatar { background: #4f46e5; }
.user-meta { flex: 1; }
.uname { font-size: 13px; color: #e5e7f5; }
.more { cursor: pointer; color: #7c83a3; font-size: 18px; letter-spacing: 2px; }

.content { flex: 1; min-width: 0; overflow-y: auto; }
</style>