import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// NovaFS 前端:开发时代理 /api 到后端,构建产物直接输出到 Spring Boot static 目录
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../fs-admin/src/main/resources/static',
    emptyOutDir: true
  }
})