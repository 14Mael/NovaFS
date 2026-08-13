<template>
  <div class="login-page">
    <div class="card">
      <div class="brand">
        <div class="logo">N</div>
        <div class="title">NovaFS</div>
        <div class="sub">AI 文档工作空间 · 检索你的知识库</div>
      </div>
      <el-tabs v-model="mode" class="tabs">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>
      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'">
          <el-input v-model="email" placeholder="邮箱(用于账号找回)" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
          {{ mode === 'login' ? '登 录' : '注 册' }}
        </el-button>
      </el-form>
      <div class="hint">注册成功将自动登录</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const email = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (mode.value === 'register') {
    if (!email.value || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.value)) {
      ElMessage.warning('请输入有效的邮箱地址')
      return
    }
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await auth.login(username.value, password.value)
    } else {
      await auth.register(username.value, password.value, email.value)
      await auth.login(username.value, password.value)
    }
    ElMessage.success(mode.value === 'login' ? '欢迎回来' : '注册成功')
    router.push('/chat')
  } catch { /* 错误已由拦截器提示 */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex; align-items: center; justify-content: center;
  background:
    radial-gradient(1200px 600px at 20% -10%, rgba(59, 157, 255, 0.25), transparent),
    radial-gradient(1000px 500px at 90% 110%, rgba(43, 108, 236, 0.2), transparent),
    var(--novafs-bg);
}
.card {
  width: 380px; padding: 36px 34px 24px;
  background: var(--novafs-card-bg); border-radius: 16px;
  box-shadow: 0 24px 60px rgba(23, 66, 130, 0.16);
}
.brand { text-align: center; margin-bottom: 22px; }
.logo {
  width: 52px; height: 52px; margin: 0 auto 10px; border-radius: 14px;
  background: var(--novafs-gradient);
  color: #fff; font-weight: 800; font-size: 28px;
  display: flex; align-items: center; justify-content: center;
}
.title { font-size: 24px; font-weight: 800; color: var(--novafs-text); }
.sub { font-size: 13px; color: var(--novafs-text-sub); margin-top: 4px; }
.tabs { margin-bottom: 8px; }
.submit { width: 100%; }
.hint { text-align: center; font-size: 12px; color: var(--novafs-text-muted); margin-top: 12px; }
</style>