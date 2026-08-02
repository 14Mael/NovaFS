import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, register as registerApi, type UserInfo } from '../api/auth'

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
      this.user = { username: data.username, nickname: data.nickname }
      localStorage.setItem('novafs_token', data.token)
      localStorage.setItem('novafs_user', JSON.stringify(this.user))
    },
    async register(username: string, password: string, email: string) {
      await registerApi({ username, password, email })
    },
    async logout() {
      try {
        await logoutApi()
      } catch {
        /* 忽略登出接口异常,本地状态照常清理 */
      }
      this.token = ''
      this.user = null
      localStorage.removeItem('novafs_token')
      localStorage.removeItem('novafs_user')
    }
  }
})
