import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
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
  },
  {
    path: '/workflows',
    name: 'WorkflowList',
    component: () => import('@/views/workflow/WorkflowList.vue'),
    meta: { title: '工作流' }
  },
  {
    path: '/workflows/create',
    name: 'WorkflowCreate',
    component: () => import('@/views/workflow/WorkflowEditor.vue'),
    meta: { title: '新建工作流' }
  },
  {
    path: '/workflows/:id/edit',
    name: 'WorkflowEdit',
    component: () => import('@/views/workflow/WorkflowEditor.vue'),
    meta: { title: '编辑工作流' }
  },
  {
    path: '/mcp-servers',
    name: 'McpServerList',
    component: () => import('@/views/mcp/McpServerList.vue'),
    meta: { title: 'MCP 管理' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('hify_token')

  if (!token && to.path !== '/login') {
    next('/login')
  } else if (token && to.path === '/login') {
    next('/')
  } else {
    next()
  }
})

export default router
