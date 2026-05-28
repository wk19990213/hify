# Hify Frontend Workbench Pro Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Hify 前端升级为 Workbench Pro 风格的内部 AI Agent 工作台，并完成概览页、Provider 列表页、Agent 列表页首批落地。

**Architecture:** 先修复前端中文编码与设计 token，再抽出全局壳层与列表页基础组件，最后让 Dashboard、Provider、Agent 使用统一模板。保持 Vue 3 + TypeScript + Element Plus，不改后端 API，不引入新 UI 框架。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vue Router、Vite、Vitest、CSS variables。

---

## File Structure

- Modify: `hify-web/src/styles/design-system.css`  
  Workbench Pro 设计 token、Element Plus 变量映射、全局工具类。
- Modify: `hify-web/src/styles/global.css`  
  全局 body、链接、滚动条、页面背景。
- Modify: `hify-web/src/main.ts`  
  修复中文注释，继续注入 Element Plus 主题。
- Modify: `hify-web/src/router/index.ts`  
  修复路由标题乱码。
- Modify: `hify-web/src/App.vue`  
  改为只挂载 `AppShell`。
- Create: `hify-web/src/layouts/AppShell.vue`  
  全局壳层：深色窄侧栏、顶部上下文栏、主内容区。
- Create: `hify-web/src/composables/usePageMeta.ts`  
  页面标题、说明、主操作上下文。
- Create: `hify-web/src/components/PageHeader.vue`  
  统一页面标题、说明、右侧操作槽。
- Create: `hify-web/src/components/SummaryMetric.vue`  
  状态摘要卡。
- Create: `hify-web/src/components/StatusBadge.vue`  
  统一状态点 + 文案。
- Create: `hify-web/src/components/IconAction.vue`  
  表格行图标操作按钮。
- Create: `hify-web/src/components/ListToolbar.vue`  
  搜索、筛选、刷新插槽工具条。
- Modify: `hify-web/src/components/HifyTable.vue`  
  表格密度、边框、空状态、分页样式升级。
- Modify: `hify-web/src/views/DashboardLayout.vue`  
  改成运营概览工作台。
- Modify: `hify-web/src/views/provider/ProviderList.vue`  
  使用统一页头、摘要卡、工具条、状态组件。
- Modify: `hify-web/src/views/agent/AgentList.vue`  
  使用统一页头、摘要卡、工具条、状态组件。
- Create tests under `hify-web/src/components/__tests__/`.

---

## Task 1: Fix Chinese Encoding And Route Text

**Files:**
- Modify: `hify-web/src/main.ts`
- Modify: `hify-web/src/router/index.ts`
- Modify: `hify-web/src/App.vue`
- Test: `hify-web/src/router/__tests__/route-title.test.ts`

- [ ] **Step 1: Write failing route title test**

Create `hify-web/src/router/__tests__/route-title.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import router from '../index'

describe('route titles', () => {
  it('uses readable Chinese titles for main pages', () => {
    const titles = router.getRoutes().map(route => route.meta.title)

    expect(titles).toContain('概览')
    expect(titles).toContain('模型管理')
    expect(titles).toContain('Agent 管理')
    expect(titles).toContain('对话')
    expect(titles).toContain('知识库')
    expect(titles).toContain('工作流')
    expect(titles).toContain('MCP 管理')
  })

  it('does not contain mojibake in route titles', () => {
    const joined = router.getRoutes().map(route => route.meta.title || '').join(' ')

    expect(joined).not.toMatch(/[�]|鐧|绠|妯|瀵|宸|姒|璁/)
  })
})
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
cd hify-web
npm run test -- src/router/__tests__/route-title.test.ts
```

Expected: FAIL because current route titles contain mojibake.

- [ ] **Step 3: Replace route titles**

Update `hify-web/src/router/index.ts` route meta:

```ts
meta: { title: '登录' }
meta: { title: '概览' }
meta: { title: '设计系统' }
meta: { title: '模型管理' }
meta: { title: 'Agent 管理' }
meta: { title: '对话' }
meta: { title: '知识库' }
meta: { title: '工作流' }
meta: { title: '新建工作流' }
meta: { title: '编辑工作流' }
meta: { title: 'MCP 管理' }
```

Also replace comments in `hify-web/src/main.ts` with readable Chinese:

```ts
// Hify 设计系统
// 注入 Element Plus 主题覆盖样式
```

- [ ] **Step 4: Simplify App to shell host**

Replace `hify-web/src/App.vue` with:

```vue
<template>
  <AppShell />
</template>

<script setup lang="ts">
import AppShell from '@/layouts/AppShell.vue'
</script>
```

This will fail until Task 3 creates `AppShell.vue`.

- [ ] **Step 5: Run route test**

Run:

```bash
cd hify-web
npm run test -- src/router/__tests__/route-title.test.ts
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add hify-web/src/main.ts hify-web/src/router/index.ts hify-web/src/router/__tests__/route-title.test.ts
git commit -m "fix: 修复前端路由中文标题"
```

Do not commit `App.vue` yet if build is broken before Task 3.

---

## Task 2: Add Workbench Pro Design Tokens

**Files:**
- Modify: `hify-web/src/styles/design-system.css`
- Modify: `hify-web/src/styles/global.css`
- Test: `hify-web/src/styles/__tests__/design-token.test.ts`

- [ ] **Step 1: Write token test**

Create `hify-web/src/styles/__tests__/design-token.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const css = readFileSync(resolve(__dirname, '../design-system.css'), 'utf8')

describe('Workbench Pro design tokens', () => {
  it('defines teal primary tokens and amber warning tokens', () => {
    expect(css).toContain('--primary-600: #0d9488')
    expect(css).toContain('--warning-500: #f59e0b')
    expect(css).toContain('--shell-sidebar-bg: #0f172a')
    expect(css).toContain('--surface-page: #f6f7f9')
  })

  it('does not use large purple gradient as primary theme', () => {
    expect(css).not.toContain('--primary-600: #7c3aed')
    expect(css).not.toContain('linear-gradient(135deg, var(--primary-600), var(--primary-500))')
  })
})
```

- [ ] **Step 2: Run token test and verify failure**

Run:

```bash
cd hify-web
npm run test -- src/styles/__tests__/design-token.test.ts
```

Expected: FAIL because current primary token is purple.

- [ ] **Step 3: Replace root token section**

In `hify-web/src/styles/design-system.css`, replace current `:root` color variables with:

```css
:root {
  --primary-50: #f0fdfa;
  --primary-100: #ccfbf1;
  --primary-200: #99f6e4;
  --primary-300: #5eead4;
  --primary-400: #2dd4bf;
  --primary-500: #14b8a6;
  --primary-600: #0d9488;
  --primary-700: #0f766e;
  --primary-800: #115e59;
  --primary-900: #134e4a;

  --accent-50: #f5f3ff;
  --accent-100: #ede9fe;
  --accent-500: #8b5cf6;
  --accent-600: #7c3aed;

  --gray-50: #f8fafc;
  --gray-100: #f1f5f9;
  --gray-200: #e2e8f0;
  --gray-300: #cbd5e1;
  --gray-400: #94a3b8;
  --gray-500: #64748b;
  --gray-600: #475569;
  --gray-700: #334155;
  --gray-800: #1e293b;
  --gray-900: #0f172a;

  --surface-page: #f6f7f9;
  --surface-panel: #ffffff;
  --surface-subtle: #f8fafc;
  --shell-sidebar-bg: #0f172a;
  --shell-sidebar-hover: #1e293b;
  --shell-sidebar-active: #0d9488;

  --bg-primary: var(--surface-panel);
  --bg-secondary: var(--surface-page);
  --bg-tertiary: var(--surface-subtle);
  --bg-elevated: #ffffff;
  --bg-dark: var(--shell-sidebar-bg);

  --text-primary: #111827;
  --text-secondary: #475569;
  --text-tertiary: #64748b;
  --text-disabled: #94a3b8;
  --text-inverse: #ffffff;

  --border-light: #eef2f7;
  --border-default: #e2e8f0;
  --border-strong: #cbd5e1;
  --border-focus: var(--primary-600);

  --success-50: #ecfdf5;
  --success-500: #22c55e;
  --success-600: #16a34a;
  --warning-50: #fffbeb;
  --warning-500: #f59e0b;
  --warning-600: #d97706;
  --error-50: #fef2f2;
  --error-500: #ef4444;
  --error-600: #dc2626;
  --info-50: #eff6ff;
  --info-500: #3b82f6;
  --info-600: #2563eb;

  --radius-sm: 4px;
  --radius-md: 6px;
  --radius-lg: 8px;
  --radius-xl: 12px;
  --radius-full: 9999px;

  --shadow-sm: 0 1px 2px rgb(15 23 42 / 0.05);
  --shadow-md: 0 8px 20px rgb(15 23 42 / 0.08);
  --shadow-lg: 0 16px 32px rgb(15 23 42 / 0.12);
  --shadow-primary: 0 8px 18px rgb(13 148 136 / 0.22);

  --duration-fast: 150ms;
  --duration-normal: 220ms;
  --ease-default: cubic-bezier(0.4, 0, 0.2, 1);
  --transition-fast: var(--duration-fast) var(--ease-default);
  --transition-normal: var(--duration-normal) var(--ease-default);

  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;

  --font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif;
  --font-mono: 'SF Mono', 'Fira Code', 'JetBrains Mono', monospace;
  --text-xs: 12px;
  --text-sm: 13px;
  --text-base: 14px;
  --text-lg: 16px;
  --text-xl: 18px;
  --text-2xl: 20px;
  --text-3xl: 24px;

  --z-fixed: 300;
  --z-modal: 500;
  --z-tooltip: 700;
}
```

- [ ] **Step 4: Replace button gradient styles**

In `.hify-btn-primary`, use flat primary:

```css
.hify-btn-primary {
  background: var(--primary-600);
  color: var(--text-inverse);
  box-shadow: var(--shadow-primary);
}

.hify-btn-primary:hover {
  background: var(--primary-700);
  transform: translateY(-1px);
}
```

- [ ] **Step 5: Update global background**

Set `hify-web/src/styles/global.css`:

```css
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100vh;
}

body {
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: var(--text-primary);
  background: var(--surface-page);
}

a {
  color: inherit;
  text-decoration: none;
}
```

- [ ] **Step 6: Run token test**

Run:

```bash
cd hify-web
npm run test -- src/styles/__tests__/design-token.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add hify-web/src/styles/design-system.css hify-web/src/styles/global.css hify-web/src/styles/__tests__/design-token.test.ts
git commit -m "style: 更新 Workbench Pro 设计变量"
```

---

## Task 3: Build AppShell And Page Meta

**Files:**
- Create: `hify-web/src/composables/usePageMeta.ts`
- Create: `hify-web/src/layouts/AppShell.vue`
- Modify: `hify-web/src/App.vue`
- Test: `hify-web/src/layouts/__tests__/AppShell.test.ts`

- [ ] **Step 1: Write AppShell test**

Create `hify-web/src/layouts/__tests__/AppShell.test.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'
import AppShell from '../AppShell.vue'

function makeRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', component: { template: '<div>home</div>' }, meta: { title: '概览' } },
      { path: '/agent', component: { template: '<div>agent</div>' }, meta: { title: 'Agent 管理' } },
    ],
  })
}

describe('AppShell', () => {
  it('renders sidebar navigation and page content', async () => {
    const router = makeRouter()
    router.push('/')
    await router.isReady()

    const wrapper = mount(AppShell, {
      global: {
        plugins: [router],
        stubs: {
          AuthDialog: { template: '<div />' },
          ElIcon: { template: '<span><slot /></span>' },
          ElButton: { template: '<button><slot /></button>' },
        },
      },
    })

    expect(wrapper.text()).toContain('Hify')
    expect(wrapper.text()).toContain('模型管理')
    expect(wrapper.text()).toContain('Agent 管理')
    expect(wrapper.text()).toContain('home')
  })
})
```

- [ ] **Step 2: Run test and verify failure**

Run:

```bash
cd hify-web
npm run test -- src/layouts/__tests__/AppShell.test.ts
```

Expected: FAIL because `AppShell.vue` does not exist.

- [ ] **Step 3: Create page meta composable**

Create `hify-web/src/composables/usePageMeta.ts`:

```ts
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const descriptions: Record<string, string> = {
  '/dashboard': '查看平台健康、近期活动与待处理事项',
  '/provider': '管理大模型提供商、连接状态与模型配置',
  '/agent': '创建、配置、调试团队内部 AI Agent',
  '/knowledge': '管理 RAG 知识库与文档',
  '/workflows': '编排节点、变量与执行流程',
  '/mcp-servers': '管理 MCP 工具服务与可用工具',
  '/chat': '与 Agent 对话并调试输出效果',
}

const primaryActionLabel = ref('')
const primaryActionHandler = ref<(() => void) | null>(null)

export function usePageMeta() {
  const route = useRoute()

  const title = computed(() => String(route.meta.title || 'Hify'))
  const description = computed(() => descriptions[route.path] || 'AI Agent 开发平台')

  function setPrimaryAction(label: string, handler: () => void) {
    primaryActionLabel.value = label
    primaryActionHandler.value = handler
  }

  function clearPrimaryAction() {
    primaryActionLabel.value = ''
    primaryActionHandler.value = null
  }

  return {
    title,
    description,
    primaryActionLabel,
    primaryActionHandler,
    setPrimaryAction,
    clearPrimaryAction,
  }
}
```

- [ ] **Step 4: Create AppShell**

Create `hify-web/src/layouts/AppShell.vue` with:

```vue
<template>
  <div class="app-shell">
    <aside class="app-sidebar" :class="{ collapsed: collapsed }">
      <router-link to="/dashboard" class="brand" title="Hify">
        <span class="brand-mark">H</span>
        <span v-if="!collapsed" class="brand-text">Hify</span>
      </router-link>

      <nav class="nav-list">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :title="collapsed ? item.label : ''"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span v-if="!collapsed">{{ item.label }}</span>
        </router-link>
      </nav>

      <button class="collapse-toggle" type="button" @click="collapsed = !collapsed">
        <el-icon :size="16"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
        <span v-if="!collapsed">收起</span>
      </button>
    </aside>

    <section class="app-main" :class="{ expanded: collapsed }">
      <header class="topbar">
        <div>
          <h1>{{ title }}</h1>
          <p>{{ description }}</p>
        </div>
        <div class="topbar-actions">
          <button
            v-if="primaryActionLabel && primaryActionHandler"
            class="primary-action"
            type="button"
            @click="primaryActionHandler"
          >
            {{ primaryActionLabel }}
          </button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </section>

    <AuthDialog ref="authDialog" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import {
  ChatDotRound,
  Document,
  Expand,
  Fold,
  Setting,
  Share,
  Tools,
  User,
} from '@element-plus/icons-vue'
import AuthDialog from '@/components/AuthDialog.vue'
import { usePageMeta } from '@/composables/usePageMeta'

const collapsed = ref(false)
const authDialog = ref<InstanceType<typeof AuthDialog> | null>(null)
const { title, description, primaryActionLabel, primaryActionHandler } = usePageMeta()

const menuItems = [
  { path: '/provider', label: '模型管理', icon: Setting },
  { path: '/agent', label: 'Agent 管理', icon: User },
  { path: '/knowledge', label: '知识库', icon: Document },
  { path: '/workflows', label: '工作流', icon: Share },
  { path: '/mcp-servers', label: 'MCP 管理', icon: Tools },
  { path: '/chat', label: '对话', icon: ChatDotRound },
]

function onAuthRequired() {
  authDialog.value?.open('login')
}

onMounted(() => window.addEventListener('auth:required', onAuthRequired))
onUnmounted(() => window.removeEventListener('auth:required', onAuthRequired))
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--surface-page);
}

.app-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  z-index: var(--z-fixed);
  width: 72px;
  background: var(--shell-sidebar-bg);
  border-right: 1px solid rgb(255 255 255 / 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 10px;
  transition: width var(--transition-normal);
}

.app-sidebar:not(.collapsed) {
  width: 240px;
  align-items: stretch;
}

.brand,
.nav-item,
.collapse-toggle {
  border-radius: var(--radius-lg);
}

.brand {
  height: 44px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  margin-bottom: 12px;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-lg);
  background: var(--primary-600);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
}

.nav-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.nav-item {
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgb(226 232 240 / 0.72);
  padding: 0 11px;
}

.app-sidebar:not(.collapsed) .nav-item {
  justify-content: flex-start;
}

.nav-item:hover {
  background: var(--shell-sidebar-hover);
  color: #fff;
}

.nav-item.router-link-active {
  background: var(--shell-sidebar-active);
  color: #fff;
  box-shadow: inset 3px 0 0 var(--warning-500);
}

.collapse-toggle {
  margin-top: auto;
  min-height: 38px;
  border: 0;
  background: var(--shell-sidebar-hover);
  color: rgb(226 232 240 / 0.78);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
}

.app-main {
  min-height: 100vh;
  margin-left: 240px;
  transition: margin-left var(--transition-normal);
}

.app-main.expanded {
  margin-left: 72px;
}

.topbar {
  height: 64px;
  background: var(--surface-panel);
  border-bottom: 1px solid var(--border-default);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.topbar h1 {
  font-size: var(--text-xl);
  line-height: 1.2;
  margin: 0;
}

.topbar p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.primary-action {
  height: 34px;
  padding: 0 14px;
  border: 0;
  border-radius: var(--radius-md);
  background: var(--primary-600);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.content {
  padding: 20px 24px 28px;
}

@media (max-width: 768px) {
  .app-sidebar {
    transform: translateX(-100%);
  }

  .app-main,
  .app-main.expanded {
    margin-left: 0;
  }

  .topbar {
    height: auto;
    min-height: 64px;
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
    padding: 14px 16px;
  }

  .content {
    padding: 16px;
  }
}
</style>
```

- [ ] **Step 5: Ensure App imports AppShell**

`hify-web/src/App.vue`:

```vue
<template>
  <AppShell />
</template>

<script setup lang="ts">
import AppShell from '@/layouts/AppShell.vue'
</script>
```

- [ ] **Step 6: Run AppShell test**

Run:

```bash
cd hify-web
npm run test -- src/layouts/__tests__/AppShell.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add hify-web/src/App.vue hify-web/src/layouts/AppShell.vue hify-web/src/composables/usePageMeta.ts hify-web/src/layouts/__tests__/AppShell.test.ts
git commit -m "feat: 新增 Workbench 全局壳层"
```

---

## Task 4: Add Shared Workbench Components

**Files:**
- Create: `hify-web/src/components/PageHeader.vue`
- Create: `hify-web/src/components/SummaryMetric.vue`
- Create: `hify-web/src/components/StatusBadge.vue`
- Create: `hify-web/src/components/IconAction.vue`
- Create: `hify-web/src/components/ListToolbar.vue`
- Modify: `hify-web/src/components/HifyTable.vue`
- Test: `hify-web/src/components/__tests__/workbench-components.test.ts`

- [ ] **Step 1: Write shared component tests**

Create `hify-web/src/components/__tests__/workbench-components.test.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import PageHeader from '../PageHeader.vue'
import SummaryMetric from '../SummaryMetric.vue'
import StatusBadge from '../StatusBadge.vue'
import IconAction from '../IconAction.vue'

describe('Workbench shared components', () => {
  it('renders page header action slot', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '模型管理', description: '管理大模型提供商' },
      slots: { actions: '<button>新增</button>' },
    })

    expect(wrapper.text()).toContain('模型管理')
    expect(wrapper.text()).toContain('管理大模型提供商')
    expect(wrapper.text()).toContain('新增')
  })

  it('renders summary metric value and label', () => {
    const wrapper = mount(SummaryMetric, {
      props: { label: '可用提供商', value: 3, tone: 'success' },
    })

    expect(wrapper.text()).toContain('可用提供商')
    expect(wrapper.text()).toContain('3')
  })

  it('renders status badge text', () => {
    const wrapper = mount(StatusBadge, {
      props: { status: 'success', text: '启用' },
    })

    expect(wrapper.text()).toContain('启用')
    expect(wrapper.classes()).toContain('status-success')
  })

  it('emits click from icon action', async () => {
    const onClick = vi.fn()
    const wrapper = mount(IconAction, {
      props: { label: '编辑', onClick },
      global: { stubs: { ElTooltip: { template: '<span><slot /></span>' } } },
    })

    await wrapper.find('button').trigger('click')
    expect(onClick).toHaveBeenCalledTimes(1)
  })
})
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
cd hify-web
npm run test -- src/components/__tests__/workbench-components.test.ts
```

Expected: FAIL because new components do not exist.

- [ ] **Step 3: Create PageHeader**

Create `hify-web/src/components/PageHeader.vue`:

```vue
<template>
  <section class="page-header">
    <div>
      <h2>{{ title }}</h2>
      <p v-if="description">{{ description }}</p>
    </div>
    <div class="page-actions">
      <slot name="actions" />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: var(--text-2xl);
  line-height: 1.25;
}

.page-header p {
  margin: 5px 0 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.page-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 768px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
```

- [ ] **Step 4: Create SummaryMetric**

Create `hify-web/src/components/SummaryMetric.vue`:

```vue
<template>
  <article class="summary-metric" :class="`tone-${tone}`">
    <span class="metric-label">{{ label }}</span>
    <strong>{{ value }}</strong>
    <span v-if="hint" class="metric-hint">{{ hint }}</span>
  </article>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  label: string
  value: string | number
  hint?: string
  tone?: 'default' | 'success' | 'warning' | 'danger' | 'info'
}>(), {
  tone: 'default',
})
</script>

<style scoped>
.summary-metric {
  min-height: 76px;
  background: var(--surface-panel);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 12px 14px;
}

.metric-label,
.metric-hint {
  display: block;
  color: var(--text-secondary);
  font-size: var(--text-xs);
}

strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  line-height: 1;
}

.tone-success strong { color: var(--success-600); }
.tone-warning strong { color: var(--warning-600); }
.tone-danger strong { color: var(--error-600); }
.tone-info strong { color: var(--info-600); }
</style>
```

- [ ] **Step 5: Create StatusBadge**

Create `hify-web/src/components/StatusBadge.vue`:

```vue
<template>
  <span class="status-badge" :class="`status-${status}`">
    <span class="status-dot" />
    <span>{{ text }}</span>
  </span>
</template>

<script setup lang="ts">
defineProps<{
  status: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
  text: string
}>()
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 24px;
  padding: 0 9px;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 600;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: currentColor;
}

.status-success { color: var(--success-600); background: var(--success-50); }
.status-warning { color: var(--warning-600); background: var(--warning-50); }
.status-danger { color: var(--error-600); background: var(--error-50); }
.status-info { color: var(--info-600); background: var(--info-50); }
.status-neutral { color: var(--gray-600); background: var(--gray-100); }
</style>
```

- [ ] **Step 6: Create IconAction**

Create `hify-web/src/components/IconAction.vue`:

```vue
<template>
  <el-tooltip :content="label" placement="top">
    <button class="icon-action" type="button" :aria-label="label" @click="onClick">
      <slot />
    </button>
  </el-tooltip>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  onClick: (event: MouseEvent) => void
}>()
</script>

<style scoped>
.icon-action {
  width: 30px;
  height: 30px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: var(--surface-panel);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.icon-action:hover {
  color: var(--primary-700);
  border-color: var(--primary-300);
  background: var(--primary-50);
}
</style>
```

- [ ] **Step 7: Create ListToolbar**

Create `hify-web/src/components/ListToolbar.vue`:

```vue
<template>
  <section class="list-toolbar">
    <div class="toolbar-left">
      <slot name="left" />
    </div>
    <div class="toolbar-right">
      <slot name="right" />
    </div>
  </section>
</template>

<style scoped>
.list-toolbar {
  min-height: 48px;
  background: var(--surface-panel);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.toolbar-left,
.toolbar-right {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

@media (max-width: 768px) {
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
```

- [ ] **Step 8: Tighten HifyTable styles**

In `hify-web/src/components/HifyTable.vue`, set wrapper:

```css
.hify-table-wrapper {
  background: var(--surface-panel);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
```

Set cell padding:

```css
.hify-table :deep(th.el-table__cell) {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--text-secondary);
  padding: 10px 12px;
  background: var(--surface-subtle);
}

.hify-table :deep(td.el-table__cell) {
  padding: 10px 12px;
  font-size: var(--text-sm);
  color: var(--text-primary);
}
```

- [ ] **Step 9: Run component tests**

Run:

```bash
cd hify-web
npm run test -- src/components/__tests__/workbench-components.test.ts
```

Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add hify-web/src/components/PageHeader.vue hify-web/src/components/SummaryMetric.vue hify-web/src/components/StatusBadge.vue hify-web/src/components/IconAction.vue hify-web/src/components/ListToolbar.vue hify-web/src/components/HifyTable.vue hify-web/src/components/__tests__/workbench-components.test.ts
git commit -m "feat: 新增工作台通用组件"
```

---

## Task 5: Apply Workbench Layout To Dashboard, Provider, Agent

**Files:**
- Modify: `hify-web/src/views/DashboardLayout.vue`
- Modify: `hify-web/src/views/provider/ProviderList.vue`
- Modify: `hify-web/src/views/agent/AgentList.vue`
- Test: `hify-web/src/views/__tests__/workbench-pages.test.ts`

- [ ] **Step 1: Write page rendering tests**

Create `hify-web/src/views/__tests__/workbench-pages.test.ts`:

```ts
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import DashboardLayout from '../DashboardLayout.vue'
import ProviderList from '../provider/ProviderList.vue'
import AgentList from '../agent/AgentList.vue'

vi.mock('@/api/provider', () => ({
  getProviderList: vi.fn().mockResolvedValue({ list: [], total: 0 }),
  createProvider: vi.fn(),
  updateProvider: vi.fn(),
  deleteProvider: vi.fn(),
  testConnection: vi.fn(),
}))

vi.mock('@/api/agent', () => ({
  getAgentList: vi.fn().mockResolvedValue({ list: [], total: 0 }),
  createAgent: vi.fn(),
  updateAgent: vi.fn(),
  deleteAgent: vi.fn(),
}))

vi.mock('@/api/workflow', () => ({
  getWorkflowList: vi.fn().mockResolvedValue({ list: [], total: 0 }),
}))

vi.mock('@/api/mcpServer', () => ({
  getAllMcpTools: vi.fn().mockResolvedValue([]),
}))

vi.mock('@/utils/request', () => ({
  get: vi.fn().mockResolvedValue([]),
}))

const global = {
  stubs: {
    RouterLink: { template: '<a><slot /></a>' },
    HifyTable: { template: '<div class="table-stub"><slot name="status" :row="{ status: 1 }" /></div>' },
    ProviderFormDialog: { template: '<div />' },
    AgentFormDialog: { template: '<div />' },
    HealthStatusCell: { template: '<div>健康</div>' },
    ElButton: { template: '<button><slot /></button>' },
    ElInput: { template: '<input />' },
    ElSelect: { template: '<select><slot /></select>' },
    ElOption: { template: '<option />' },
    ElIcon: { template: '<span><slot /></span>' },
    ElTooltip: { template: '<span><slot /></span>' },
  },
}

describe('Workbench pages', () => {
  it('renders dashboard workbench sections', () => {
    const wrapper = mount(DashboardLayout, { global })
    expect(wrapper.text()).toContain('平台概览')
    expect(wrapper.text()).toContain('待处理事项')
  })

  it('renders provider workbench sections', () => {
    const wrapper = mount(ProviderList, { global })
    expect(wrapper.text()).toContain('模型提供商')
    expect(wrapper.text()).toContain('新增提供商')
  })

  it('renders agent workbench sections', () => {
    const wrapper = mount(AgentList, { global })
    expect(wrapper.text()).toContain('Agent 管理')
    expect(wrapper.text()).toContain('新增 Agent')
  })
})
```

- [ ] **Step 2: Run page tests and verify failure**

Run:

```bash
cd hify-web
npm run test -- src/views/__tests__/workbench-pages.test.ts
```

Expected: FAIL because current pages contain mojibake and old structure.

- [ ] **Step 3: Replace DashboardLayout**

Replace `hify-web/src/views/DashboardLayout.vue` with a static workbench overview using new components:

```vue
<template>
  <div class="dashboard-page">
    <PageHeader title="平台概览" description="查看平台健康、近期活动与待处理事项" />

    <section class="metric-grid">
      <SummaryMetric label="模型提供商" value="4" hint="3 个可用" tone="success" />
      <SummaryMetric label="Agent" value="12" hint="9 个启用" tone="info" />
      <SummaryMetric label="知识库文档" value="286" hint="本周新增 18" />
      <SummaryMetric label="MCP 工具" value="31" hint="2 个异常" tone="warning" />
    </section>

    <section class="dashboard-grid">
      <article class="panel">
        <header>
          <h3>最近活动</h3>
          <span>过去 24 小时</span>
        </header>
        <ul>
          <li><StatusBadge status="success" text="Provider 正常" /> OpenAI Compatible 连接检测通过</li>
          <li><StatusBadge status="info" text="Agent 更新" /> 客服助手调整系统提示词</li>
          <li><StatusBadge status="warning" text="Workflow" /> 订单查询流程等待配置 MCP</li>
        </ul>
      </article>

      <article class="panel">
        <header>
          <h3>待处理事项</h3>
          <span>建议优先处理</span>
        </header>
        <ul>
          <li>2 个模型提供商最近检测失败</li>
          <li>1 个 Agent 未绑定模型配置</li>
          <li>3 个知识库文档等待向量化</li>
        </ul>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import SummaryMetric from '@/components/SummaryMetric.vue'
import StatusBadge from '@/components/StatusBadge.vue'
</script>

<style scoped>
.dashboard-page {
  max-width: 1280px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}

.panel {
  background: var(--surface-panel);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.panel header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.panel h3 {
  margin: 0;
  font-size: var(--text-lg);
}

.panel header span {
  color: var(--text-secondary);
  font-size: var(--text-xs);
}

.panel ul {
  list-style: none;
  display: grid;
  gap: 10px;
  color: var(--text-secondary);
}

@media (max-width: 900px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
```

- [ ] **Step 4: Refactor ProviderList text and structure**

In `ProviderList.vue`:

- Use `PageHeader`, `SummaryMetric`, `ListToolbar`, `StatusBadge`, `IconAction`.
- Replace mojibake labels with:

```ts
const columns: TableColumn<Provider>[] = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'type', label: '类型', width: 120, slot: 'type' },
  { prop: 'baseUrl', label: 'Base URL', minWidth: 240 },
  { prop: 'health', label: '健康状态', width: 140, slot: 'health' },
  { prop: 'modelCount', label: '模型数', width: 90, slot: 'modelCount', align: 'center' },
  { prop: 'status', label: '状态', width: 90, slot: 'status', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 170, type: 'datetime' },
  { prop: 'action', label: '操作', width: 130, slot: 'action', fixed: 'right', align: 'center' },
]
```

Use status slot:

```vue
<StatusBadge
  :status="row.status === 1 ? 'success' : 'neutral'"
  :text="row.status === 1 ? '启用' : '禁用'"
/>
```

Use action slot:

```vue
<div class="action-btns">
  <IconAction label="编辑" :on-click="() => handleEdit(row)"><el-icon><Edit /></el-icon></IconAction>
  <IconAction label="测试连接" :on-click="() => handleTestConnection(row)"><el-icon><Connection /></el-icon></IconAction>
  <IconAction label="删除" :on-click="() => handleDelete(row)"><el-icon><Delete /></el-icon></IconAction>
</div>
```

- [ ] **Step 5: Refactor AgentList text and structure**

In `AgentList.vue`:

- Use same component pattern as Provider.
- Replace column labels with:

```ts
const columns: TableColumn<Agent>[] = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'code', label: '编码', minWidth: 140 },
  { prop: 'status', label: '状态', width: 90, slot: 'status', align: 'center' },
  { prop: 'modelConfigName', label: '模型配置', width: 150, slot: 'modelConfigName' },
  { prop: 'toolCount', label: '工具数', width: 90, slot: 'toolCount', align: 'center' },
  { prop: 'temperature', label: '温度', width: 90, slot: 'temperature', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 170, type: 'datetime' },
  { prop: 'action', label: '操作', width: 130, slot: 'action', fixed: 'right', align: 'center' },
]
```

Replace visible strings:

```ts
notifySuccess(isEdit ? '编辑成功' : '新增成功')
notifyError('操作失败', e?.response?.data?.message || e?.message || '请稍后重试')
```

- [ ] **Step 6: Run page tests**

Run:

```bash
cd hify-web
npm run test -- src/views/__tests__/workbench-pages.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add hify-web/src/views/DashboardLayout.vue hify-web/src/views/provider/ProviderList.vue hify-web/src/views/agent/AgentList.vue hify-web/src/views/__tests__/workbench-pages.test.ts
git commit -m "feat: 落地工作台首页和核心列表页"
```

---

## Task 6: Final Build And Visual QA

**Files:**
- Modify only if verification finds issues.

- [ ] **Step 1: Run full frontend tests**

Run:

```bash
cd hify-web
npm run test
```

Expected: PASS.

- [ ] **Step 2: Run typecheck and build**

Run:

```bash
cd hify-web
npm run build
```

Expected: PASS.

- [ ] **Step 3: Search for mojibake in touched frontend files**

Run:

```bash
rg "鐧|绠|妯|瀵|宸|姒|璁|�" hify-web/src/App.vue hify-web/src/layouts hify-web/src/components hify-web/src/views/DashboardLayout.vue hify-web/src/views/provider/ProviderList.vue hify-web/src/views/agent/AgentList.vue hify-web/src/router/index.ts hify-web/src/main.ts
```

Expected: no output.

- [ ] **Step 4: Manual browser QA**

User explicitly allowed browser review during design, but project instruction says do not start services unless explicitly requested. Ask user before starting dev server:

```text
需要启动前端 dev server 做浏览器验证吗？
```

If approved, run:

```bash
cd hify-web
npm run dev -- --host 127.0.0.1
```

Then inspect:

- `/dashboard`: summary cards, activity panel, pending panel.
- `/provider`: topbar, metrics, toolbar, table, row actions.
- `/agent`: topbar, metrics, toolbar, table, row actions.
- 375px width: no text overlap, topbar wraps cleanly.
- 1366px width: title, metrics, toolbar, table visible in first viewport.

- [ ] **Step 5: Fix verification defects**

For each defect, make smallest scoped fix, then rerun:

```bash
cd hify-web
npm run test
npm run build
```

Expected: PASS.

- [ ] **Step 6: Final commit**

```bash
git status --short
git add hify-web docs/superpowers/specs/2026-05-28-hify-frontend-workbench-pro-design.md docs/superpowers/plans/2026-05-28-hify-frontend-workbench-pro.md
git commit -m "feat: 升级前端 Workbench Pro 视觉"
```

Before committing, follow project rule: if recent 5 commits use Chinese messages, confirm commit message with user.

---

## Self-Review

- Spec coverage: covered encoding fix, shell, token update, overview page, Provider list, Agent list, reusable components, verification. Chat and Workflow are intentionally out of first implementation phase.
- Placeholder scan: no `TBD`, no incomplete test command, no vague implementation step without concrete code target.
- Type consistency: component names and paths stay consistent across tasks.

