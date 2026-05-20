import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardLayout.vue'),
    meta: { title: '概览' }
  },
  {
    path: '/design',
    name: 'DesignSystem',
    component: () => import('@/views/DesignSystem.vue'),
    meta: { title: '设计系统' }
  },
  {
    path: '/provider',
    name: 'Provider',
    component: () => import('@/views/provider/ProviderList.vue'),
    meta: { title: '模型管理' }
  },
  {
    path: '/agent',
    name: 'Agent',
    component: () => import('@/views/agent/AgentList.vue'),
    meta: { title: 'Agent 管理' }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/chat/ChatView.vue'),
    meta: { title: '对话' }
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('@/views/knowledge/KnowledgeList.vue'),
    meta: { title: '知识库' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
