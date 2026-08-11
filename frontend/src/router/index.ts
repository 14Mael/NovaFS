import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
    { path: '/share/:code', component: () => import('../views/ShareView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('../layout/MainLayout.vue'),
      children: [
        { path: '', redirect: '/files' },
        { path: 'files', component: () => import('../views/FilesView.vue'), meta: { title: '我的文件' } },
        { path: 'chat', component: () => import('../views/ChatView.vue'), meta: { title: 'AI 问答' } },
        { path: 'library', component: () => import('../views/LibraryView.vue'), meta: { title: '文档库' } },
        { path: 'recycle', component: () => import('../views/RecycleView.vue'), meta: { title: '回收站' } },
        { path: 'storage', component: () => import('../views/StorageSettingsView.vue'), meta: { title: '存储设置' } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('novafs_token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/chat'
})

export default router
