<template>
  <div class="members-page">
    <header class="members-header">
      <div>
        <h2>成员管理</h2>
        <span class="sub">工作空间 #{{ currentWorkspaceId }} · {{ members.length }} 位成员 · 仅管理员可管理</span>
      </div>
      <el-button v-if="isAdmin" type="primary" @click="openInvite">＋ 邀请成员</el-button>
    </header>

    <div v-loading="loading" class="list-wrap">
      <table class="member-table">
        <thead>
          <tr>
            <th>成员</th>
            <th style="width: 200px">角色</th>
            <th style="width: 170px">加入时间</th>
            <th v-if="isAdmin" style="width: 100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in members" :key="m.id">
            <td>
              <div class="member-cell">
                <el-avatar :size="32" class="avatar">{{ (m.nickname || m.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
                <div>
                  <div class="mname">{{ m.nickname || m.username }}</div>
                  <div class="musername">@{{ m.username }}</div>
                </div>
              </div>
            </td>
            <td>
              <el-select
                v-if="isAdmin"
                :model-value="m.roleId"
                size="small"
                style="width: 130px"
                @change="(v: number) => changeRole(m, v)"
              >
                <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
              <span v-else class="role-tag">{{ m.roleName }}</span>
            </td>
            <td class="muted">{{ formatTime(m.joinedAt) }}</td>
            <td v-if="isAdmin">
              <el-button link type="danger" size="small" @click="removeMember(m)">移除</el-button>
            </td>
          </tr>
          <tr v-if="!loading && members.length === 0">
            <td colspan="4" class="empty-row"><el-empty description="还没有成员" :image-size="70" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 邀请列表 -->
    <h3 class="section-title">邀请记录</h3>
    <div v-loading="loading" class="list-wrap">
      <table class="member-table">
        <thead>
          <tr>
            <th>邮箱</th>
            <th style="width: 120px">状态</th>
            <th style="width: 170px">过期时间</th>
            <th v-if="isAdmin" style="width: 100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="inv in invitations" :key="inv.id">
            <td>{{ inv.email }}</td>
            <td><el-tag size="small" :type="inviteStatusType(inv.status)">{{ inviteStatusText(inv.status) }}</el-tag></td>
            <td class="muted">{{ formatTime(inv.expiresAt) }}</td>
            <td v-if="isAdmin">
              <template v-if="inv.status === 0">
                <el-button link type="danger" size="small" @click="cancelInvitation(inv)">取消</el-button>
                <el-button link type="primary" size="small" @click="copyInviteLink(inv)">复制链接</el-button>
              </template>
            </td>
          </tr>
          <tr v-if="!loading && invitations.length === 0">
            <td colspan="4" class="empty-row"><el-empty description="还没有邀请记录" :image-size="70" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 邀请对话框 -->
    <el-dialog v-model="inviteVisible" title="邀请成员" width="440px">
      <template v-if="!inviteResult">
        <el-form label-width="64px">
          <el-form-item label="邮箱" required>
            <el-input v-model="inviteForm.email" placeholder="被邀请人邮箱" />
          </el-form-item>
          <el-form-item label="角色" required>
            <el-select v-model="inviteForm.roleId" placeholder="选择角色" style="width: 100%">
              <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </template>
      <template v-else>
        <div class="invite-result">
          <div class="invite-link">
            <span class="invite-label">邀请链接（7 天内有效）</span>
            <el-input :model-value="inviteLink" readonly>
              <template #append><el-button @click="copyInviteLink(inviteResult)">复制</el-button></template>
            </el-input>
          </div>
          <div class="invite-tip">已发送邮件至 {{ inviteResult.email }}，也可直接复制链接发送给对方</div>
        </div>
      </template>
      <template #footer>
        <el-button @click="inviteVisible = false">{{ inviteResult ? '关闭' : '取消' }}</el-button>
        <el-button v-if="!inviteResult" type="primary" :loading="inviting" @click="submitInvite">创建邀请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  workspaceApi,
  type Invitation,
  type WorkspaceMember,
  type WorkspaceRole
} from '../api/workspace'

const currentWorkspaceId = () => localStorage.getItem('novafs_workspace') || '1'

const members = ref<WorkspaceMember[]>([])
const roles = ref<WorkspaceRole[]>([])
const invitations = ref<Invitation[]>([])
const isAdmin = ref(false)
const loading = ref(false)

const inviteVisible = ref(false)
const inviting = ref(false)
const inviteForm = reactive({ email: '', roleId: 0 })
const inviteResult = ref<Invitation | null>(null)

const inviteLink = computed(() =>
  inviteResult.value ? `${location.origin}/join?token=${inviteResult.value.token}` : ''
)

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const detail = await workspaceApi.detail(currentWorkspaceId())
    isAdmin.value = detail.roleCode === 'admin'
    members.value = await workspaceApi.listMembers(currentWorkspaceId())
    roles.value = await workspaceApi.listRoles(currentWorkspaceId())
    if (isAdmin.value) {
      invitations.value = await workspaceApi.listInvitations(currentWorkspaceId())
    }
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function changeRole(m: WorkspaceMember, roleId: number) {
  try {
    await workspaceApi.updateMemberRole(currentWorkspaceId(), m.id, roleId)
    ElMessage.success('角色已更新')
    await loadAll()
  } catch {
    /* 拦截器已提示 */
  }
}

async function removeMember(m: WorkspaceMember) {
  try {
    await ElMessageBox.confirm(`确定移除成员「${m.nickname || m.username}」吗？`, '移除成员')
    await workspaceApi.removeMember(currentWorkspaceId(), m.id)
    ElMessage.success('已移除')
    await loadAll()
  } catch {
    /* 取消或已提示 */
  }
}

function openInvite() {
  inviteForm.email = ''
  inviteForm.roleId = roles.value[0]?.id || 0
  inviteResult.value = null
  inviteVisible.value = true
}

async function submitInvite() {
  if (!inviteForm.email.trim() || !inviteForm.roleId) {
    ElMessage.warning('请填写邮箱并选择角色')
    return
  }
  inviting.value = true
  try {
    inviteResult.value = await workspaceApi.createInvitation(currentWorkspaceId(), {
      email: inviteForm.email.trim(),
      roleId: inviteForm.roleId
    })
    await loadAll()
  } catch {
    /* 拦截器已提示 */
  } finally {
    inviting.value = false
  }
}

async function cancelInvitation(inv: Invitation) {
  try {
    await workspaceApi.cancelInvitation(currentWorkspaceId(), inv.id)
    ElMessage.success('已取消邀请')
    await loadAll()
  } catch {
    /* 拦截器已提示 */
  }
}

async function copyInviteLink(inv: Invitation) {
  const link = `${location.origin}/join?token=${inv.token}`
  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

function inviteStatusType(s: number): 'success' | 'warning' | 'info' | 'danger' {
  return s === 1 ? 'success' : s === 2 ? 'danger' : s === 3 ? 'info' : 'warning'
}
function inviteStatusText(s: number): string {
  return s === 0 ? '待接受' : s === 1 ? '已接受' : s === 2 ? '已过期' : '已取消'
}
function formatTime(t: string): string {
  return t ? t.slice(0, 16).replace('T', ' ') : ''
}
</script>

<style scoped>
.members-page { padding: 20px 28px 28px; }
.members-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 16px;
}
.members-header h2 { margin: 0 0 4px; font-size: 20px; color: var(--novafs-text); }
.members-header .sub { font-size: 13px; color: var(--novafs-text-sub); }
.section-title { font-size: 15px; color: var(--novafs-text); margin: 22px 0 10px; }

.list-wrap {
  background: #fff;
  border: 1px solid var(--novafs-card-border);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(59, 157, 255, 0.06);
  overflow: hidden;
}
.member-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.member-table th {
  text-align: left; padding: 12px 16px; background: #f4f9ff;
  color: var(--novafs-text-sub); font-weight: 600; font-size: 12px;
  border-bottom: 1px solid var(--novafs-card-border);
}
.member-table td { padding: 12px 16px; border-bottom: 1px solid #eef4fc; color: var(--novafs-text); }
.member-table tr:hover td { background: #f7fbff; }
.member-cell { display: flex; align-items: center; gap: 10px; }
.avatar { background: #2f7edb; flex-shrink: 0; }
.mname { font-weight: 600; }
.musername { font-size: 12px; color: var(--novafs-text-muted); }
.muted { color: var(--novafs-text-sub); }
.role-tag { color: var(--novafs-primary-dark); font-weight: 600; }
.empty-row td { padding: 30px; }

.invite-result { padding: 4px 2px; }
.invite-label { display: block; font-size: 12px; color: var(--novafs-text-sub); margin-bottom: 6px; }
.invite-tip { font-size: 12px; color: var(--novafs-text-muted); margin-top: 8px; }
</style>
