import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// Sa-Token:Authorization 头直接携带 token(不带 Bearer)
const instance = axios.create({ baseURL: '/api', timeout: 60000 })

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('novafs_token')
  if (token) config.headers.Authorization = token
  return config
})

instance.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && body.code !== 200) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return body.data
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('novafs_token')
      localStorage.removeItem('novafs_user')
      router.push('/login')
    }
    ElMessage.error(err.response?.data?.msg || err.message || '网络错误')
    return Promise.reject(err)
  }
)

// 拦截器已把响应解包为 body.data,这里直接返回 Promise<T>
const request = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    instance.get(url, config) as unknown as Promise<T>,
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    instance.post(url, data, config) as unknown as Promise<T>,
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) =>
    instance.put(url, data, config) as unknown as Promise<T>,
  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    instance.delete(url, config) as unknown as Promise<T>
}

export default request