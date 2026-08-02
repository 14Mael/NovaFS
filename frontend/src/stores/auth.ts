import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi, type UserInfo } from '../api/auth'

// 安全读取 localStorage:脏数据(如字符串 "undefined"、损坏 JSON)一律按未登录处理并清理
function loadToken(): string {
  const t = localStorage.getItem('novafs_token')
  if (!t || t === 'undefined') {
    localStorage.removeItem('novafs_token')
    return ''
  }
  return t
}

function loadUser(): UserInfo | null {
  const raw = localStorage.getItem('novafs_user')
  if (!raw || raw === 'undefined') {
    localStorage.removeItem('novafs_user')
    return null
  }
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    localStorage.removeItem('novafs_user')
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: loadToken(),
    user: loadUser()
  }),
  actions: {
    async login(username: string, password: string) {
      const data = await loginApi({ username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('novafs_token', data.token)
      localStorage.setItem('novafs_user', JSON.stringify(data.user))
    },
    async register(username: string, password: string, email: string) {
      await registerApi({ username, password, email })
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('novafs_token')
      localStorage.removeItem('novafs_user')
    }
  }
})