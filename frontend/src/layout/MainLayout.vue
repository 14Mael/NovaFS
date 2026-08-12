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
        <router-link to="/library" class="nav-item">
          <span class="icon">▦</span> 文档库
        </router-link>
        <router-link to="/recycle" class="nav-item">
          <span class="icon">🗑</span> 回收站
        </router-link>
        <router-link to="/members" class="nav-item">
          <span class="icon">👥</span> 成员管理
        </router-link>
        <router-link to="/storage" class="nav-item">
          <span class="icon">◈</span> 存储设置
        </router-link>
      </nav>

      <div class="footer">
        <div class="ws">
          <el-select
            v-if="ws.list.length"
            :model-value="currentWsId"
            size="small"
            class="ws-select"
            placeholder="选择工作空间"
            @change="switchWorkspace"
          >
            <el-option
              v-for="w in ws.list"
              :key="w.id"
              :label="`${w.name} (#${w.id})`"
              :value="w.id"
            />
          </el-select>
          <el-button v-else link class="ws-manual" @click="manualWorkspace">
            #{{ currentWsId }} 切换
          </el-button>
          <el-button link class="ws-add" title="新建工作空间" @click="createVisible = true">＋</el-button>
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

    <!-- 新建工作空间 -->
    <el-dialog v-model="createVisible" title="新建工作空间" width="440px">
      <el-form label-width="64px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="如：我的团队" maxlength="100" />
        </el-form-item>
        <el-form-item label="标识">
          <el-input
            v-model="createForm.slug"
            placeholder="小写字母/数字/中划线，如 my-team"
            maxlength="64"
          />
          <div v-if="createForm.slug && !/^[a-z0-9-]+$/.test(createForm.slug)" class="slug-tip">
            只能包含小写字母、数字和中划线
          </div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createWorkspace">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { useWorkspaceStore } from '../stores/workspace'

const auth = useAuthStore()
const ws = useWorkspaceStore()
const router = useRouter()

const user = computed(() => auth.user)
const initial = computed(() => (user.value?.nickname || user.value?.username || 'U').slice(0, 1).toUpperCase())

const currentWsId = ref(localStorage.getItem('novafs_workspace') || '1')

const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '', slug: '', description: '' })

onMounted(async () => {
  try {
    await ws.load()
    currentWsId.value = ws.currentId
  } catch {
    /* 工作空间接口不可用时保留手动切换兜底 */
  }
})

function switchWorkspace(id: string) {
  if (id === ws.currentId) return
  ws.switchTo(id)
  location.reload()
}

async function manualWorkspace() {
  try {
    const { value } = await ElMessageBox.prompt('输入工作空间 ID', '切换工作空间', {
      inputValue: String(currentWsId.value),
      inputPattern: /^\d+$/,
      inputErrorMessage: '必须是数字'
    })
    ws.switchTo(value)
    location.reload()
  } catch {
    /* 取消 */
  }
}

async function createWorkspace() {
  if (!createForm.name.trim() || !createForm.slug.trim()) {
    ElMessage.warning('请填写名称与标识')
    return
  }
  if (!/^[a-z0-9-]+$/.test(createForm.slug)) {
    ElMessage.warning('标识只能包含小写字母、数字和中划线')
    return
  }
  creating.value = true
  try {
    await ws.create({
      name: createForm.name.trim(),
      slug: createForm.slug.trim(),
      description: createForm.description.trim() || undefined
    })
    ElMessage.success('工作空间已创建')
    createVisible.value = false
    location.reload()
  } catch {
    /* 拦截器已提示 */
  } finally {
    creating.value = false
  }
}

async function logout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { display: flex; height: 100%; }

.sidebar {
  width: 220px;
  background: var(--novafs-sidebar);
  color: #c9d8ee;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.brand { display: flex; align-items: center; gap: 10px; padding: 20px 18px 26px; }
.logo {
  width: 36px; height: 36px; border-radius: 10px;
  background: var(--novafs-gradient);
  color: #fff; font-weight: 800; font-size: 20px;
  display: flex; align-items: center; justify-content: center;
}
.brand-text { display: flex; flex-direction: column; }
.brand-text .name { color: #fff; font-weight: 700; font-size: 17px; }
.brand-text .sub { font-size: 11px; color: #6f87ac; }

.nav { flex: 1; padding: 0 12px; display: flex; flex-direction: column; gap: 4px; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; border-radius: 8px;
  color: #c9d8ee; text-decoration: none; font-size: 14px;
  transition: background 0.15s;
}
.nav-item .icon { width: 20px; text-align: center; }
.nav-item:hover { background: rgba(255, 255, 255, 0.06); }
.nav-item.router-link-active { background: var(--novafs-sidebar-active); color: #fff; }

.footer { padding: 14px 12px; border-top: 1px solid rgba(255, 255, 255, 0.08); }
.ws { display: flex; align-items: center; gap: 4px; margin-bottom: 10px; }
.ws-select { flex: 1; }
.ws-select :deep(.el-select__wrapper) { background: rgba(255, 255, 255, 0.06); box-shadow: none; }
.ws-select :deep(.el-select__placeholder), .ws-select :deep(.el-select__selected-item) { color: #a5c4f5; }
.ws-manual { color: #a5c4f5; font-size: 12px; padding: 0 8px; }
.ws-add { color: #6f87ac; font-size: 16px; padding: 0 6px; }
.ws-add:hover { color: #fff; }
.slug-tip { font-size: 12px; color: #e59a2b; margin-top: 4px; }

.user { display: flex; align-items: center; gap: 10px; padding: 4px 6px; }
.avatar { background: #2f7edb; }
.user-meta { flex: 1; }
.uname { font-size: 13px; color: #e5ecf8; }
.more { cursor: pointer; color: #6f87ac; font-size: 18px; letter-spacing: 2px; }

.content { flex: 1; min-width: 0; overflow-y: auto; }
</style>
