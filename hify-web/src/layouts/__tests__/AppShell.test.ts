import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createApp, h, nextTick, ref } from 'vue'
import { usePageMeta, setPrimaryAction, clearPrimaryAction } from '@/composables/usePageMeta'
import ElementPlus from 'element-plus'

// Mock icons
vi.mock('@element-plus/icons-vue', () => {
  const icon = { template: '<span class="mock-icon" />' }
  return {
    ChatDotRound: icon,
    Connection: icon,
    DataAnalysis: icon,
    Document: icon,
    Fold: icon,
    Grid: icon,
    MagicStick: icon,
    Monitor: icon,
    Operation: icon,
    Expand: icon,
    User: icon,
    ChatLineSquare: icon,
  }
})

// Mock useAuth
vi.mock('@/composables/useAuth', () => ({
  useAuth: () => ({
    user: ref({ username: 'test' }),
    logout: vi.fn(),
  }),
}))

describe('usePageMeta composable', () => {
  it('reads route.meta.title', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', component: { template: '<div />' }, meta: { title: '概览', description: '查看系统状态' } },
      ],
    })
    router.push('/')
    await router.isReady()

    const app = createApp({
      setup() {
        const meta = usePageMeta()
        return () => h('div', [
          h('span', { 'data-testid': 'title' }, meta.title.value),
          h('span', { 'data-testid': 'desc' }, meta.description.value),
        ])
      },
    })
    app.use(router)
    const el = document.createElement('div')
    document.body.appendChild(el)
    app.mount(el)
    await nextTick()

    expect(el.querySelector('[data-testid="title"]')?.textContent).toBe('概览')
    expect(el.querySelector('[data-testid="desc"]')?.textContent).toBe('查看系统状态')
    app.unmount()
    document.body.removeChild(el)
  })

  it('returns fallback title when meta is missing', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/test', component: { template: '<div />' }, name: 'TestPage' },
      ],
    })
    router.push('/test')
    await router.isReady()

    const app = createApp({
      setup() {
        const meta = usePageMeta()
        return () => h('span', { 'data-testid': 'title' }, meta.title.value)
      },
    })
    app.use(router)
    const el = document.createElement('div')
    document.body.appendChild(el)
    app.mount(el)
    await nextTick()

    expect(el.querySelector('[data-testid="title"]')?.textContent).toBe('Hify')
    app.unmount()
    document.body.removeChild(el)
  })

  it('supports setPrimaryAction and clearPrimaryAction', () => {
    const action = { label: '新增', onClick: vi.fn() }

    setPrimaryAction(action)
    const { primaryAction } = usePageMeta()
    expect(primaryAction.value).toEqual(action)

    clearPrimaryAction()
    expect(primaryAction.value).toBeNull()
  })

  it('falls back to description map for known route names', async () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/dashboard', component: { template: '<div />' }, name: 'Dashboard', meta: { title: '概览' } },
      ],
    })
    router.push('/dashboard')
    await router.isReady()

    const app = createApp({
      setup() {
        const meta = usePageMeta()
        return () => h('span', { 'data-testid': 'desc' }, meta.description.value)
      },
    })
    app.use(router)
    const el = document.createElement('div')
    document.body.appendChild(el)
    app.mount(el)
    await nextTick()

    expect(el.querySelector('[data-testid="desc"]')?.textContent).toBe('查看系统运行状态和关键指标')
    app.unmount()
    document.body.removeChild(el)
  })
})

describe('AppShell component', () => {
  function buildRouter() {
    return createRouter({
      history: createWebHistory(),
      routes: [
        {
          path: '/dashboard',
          name: 'Dashboard',
          component: { template: '<div data-testid="dashboard-view">Dashboard Content</div>' },
          meta: { title: '概览', description: '查看系统运行状态和关键指标' },
        },
        {
          path: '/provider',
          name: 'Provider',
          component: { template: '<div data-testid="provider-view">Provider Content</div>' },
          meta: { title: '模型管理' },
        },
      ],
    })
  }

  it('renders brand mark', async () => {
    const router = buildRouter()
    router.push('/dashboard')
    await router.isReady()

    const mountEl = document.createElement('div')
    document.body.appendChild(mountEl)

    // Access the default export of AppShell lazily
    const AppShell = (await import('@/layouts/AppShell.vue')).default
    const wrapper = mount(AppShell, {
      global: { plugins: [ElementPlus, router] },
      attachTo: mountEl,
    })

    expect(wrapper.find('[data-testid="app-shell"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Hify')
    expect(wrapper.text()).toContain('AI 工作台')

    wrapper.unmount()
    document.body.removeChild(mountEl)
  })

  it('renders Chinese navigation items', async () => {
    const router = buildRouter()
    router.push('/dashboard')
    await router.isReady()

    const mountEl = document.createElement('div')
    document.body.appendChild(mountEl)

    const AppShell = (await import('@/layouts/AppShell.vue')).default
    const wrapper = mount(AppShell, {
      global: { plugins: [ElementPlus, router] },
      attachTo: mountEl,
    })

    const nav = wrapper.find('[data-testid="sidebar-nav"]')
    expect(nav.exists()).toBe(true)

    const navText = nav.text()
    expect(navText).toContain('概览')
    expect(navText).toContain('模型管理')
    expect(navText).toContain('Agent 管理')
    expect(navText).toContain('对话')
    expect(navText).toContain('知识库')
    expect(navText).toContain('工作流')
    expect(navText).toContain('MCP 管理')

    wrapper.unmount()
    document.body.removeChild(mountEl)
  })

  it('renders page title and description', async () => {
    const router = buildRouter()
    router.push('/dashboard')
    await router.isReady()

    const mountEl = document.createElement('div')
    document.body.appendChild(mountEl)

    const AppShell = (await import('@/layouts/AppShell.vue')).default
    const wrapper = mount(AppShell, {
      global: { plugins: [ElementPlus, router] },
      attachTo: mountEl,
    })

    // Wait for async rendering
    await nextTick()

    const titleEl = wrapper.find('[data-testid="page-title"]')
    expect(titleEl.exists()).toBe(true)
    expect(titleEl.text()).toBe('概览')

    wrapper.unmount()
    document.body.removeChild(mountEl)
  })

  it('renders router-view content', async () => {
    const router = buildRouter()
    router.push('/dashboard')
    await router.isReady()

    const mountEl = document.createElement('div')
    document.body.appendChild(mountEl)

    const AppShell = (await import('@/layouts/AppShell.vue')).default
    const wrapper = mount(AppShell, {
      global: { plugins: [ElementPlus, router] },
      attachTo: mountEl,
    })

    await nextTick()

    const content = wrapper.find('[data-testid="page-content"]')
    expect(content.exists()).toBe(true)

    wrapper.unmount()
    document.body.removeChild(mountEl)
  })
})
