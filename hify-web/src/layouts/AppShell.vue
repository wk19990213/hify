<template>
  <div data-testid="app-shell" class="app-shell" :class="{ 'app-shell--collapsed': collapsed }">
    <aside class="app-shell__sidebar" aria-label="主导航">
      <div class="app-shell__brand">
        <div class="app-shell__brand-mark">H</div>
        <div v-if="!collapsed" class="app-shell__brand-text">
          <strong>Hify</strong>
          <span>AI 工作台</span>
        </div>
      </div>

      <nav data-testid="sidebar-nav" class="app-shell__nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          class="app-shell__nav-item"
          active-class="app-shell__nav-item--active"
          :to="item.path"
          :title="collapsed ? item.label : undefined"
        >
          <component :is="item.icon" class="app-shell__nav-icon" />
          <span v-if="!collapsed">{{ item.label }}</span>
        </RouterLink>
      </nav>

      <button class="app-shell__collapse" type="button" :aria-label="collapsed ? '展开侧栏' : '折叠侧栏'" @click="collapsed = !collapsed">
        <el-icon>
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
      </button>
    </aside>

    <div class="app-shell__main">
      <header class="app-shell__topbar">
        <div class="app-shell__heading">
          <h1 data-testid="page-title">{{ title }}</h1>
          <p v-if="description" data-testid="page-description">{{ description }}</p>
        </div>

        <div class="app-shell__actions">
          <el-button
            v-if="primaryAction"
            :type="primaryAction.type || 'primary'"
            :loading="primaryAction.loading"
            :disabled="primaryAction.disabled"
            @click="primaryAction.onClick"
          >
            <el-icon v-if="primaryAction.icon">
              <component :is="primaryAction.icon" />
            </el-icon>
            <span>{{ primaryAction.label }}</span>
          </el-button>
          <el-button v-if="user" plain @click="logout">退出</el-button>
        </div>
      </header>

      <main data-testid="page-content" class="app-shell__content">
        <RouterView />
      </main>
    </div>

    <AuthDialog ref="authDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import {
  ChatDotRound,
  Connection,
  DataAnalysis,
  Document,
  Fold,
  Grid,
  MagicStick,
  Monitor,
  Operation,
  Expand,
} from '@element-plus/icons-vue'
import AuthDialog from '@/components/AuthDialog.vue'
import { clearPrimaryAction, usePageMeta } from '@/composables/usePageMeta'
import { useAuth } from '@/composables/useAuth'

interface AuthDialogExpose {
  open: (mode?: 'login' | 'register') => void
}

const navItems = [
  { path: '/dashboard', label: '概览', icon: DataAnalysis },
  { path: '/provider', label: '模型管理', icon: Operation },
  { path: '/agent', label: 'Agent 管理', icon: MagicStick },
  { path: '/chat', label: '对话', icon: ChatDotRound },
  { path: '/knowledge', label: '知识库', icon: Document },
  { path: '/workflows', label: '工作流', icon: Connection },
  { path: '/mcp-servers', label: 'MCP 管理', icon: Monitor },
  { path: '/design', label: '设计系统', icon: Grid },
]

const collapsed = ref(false)
const authDialogRef = ref<AuthDialogExpose>()
const route = useRoute()
const { title, description, primaryAction } = usePageMeta()
const { user, logout } = useAuth()

function openAuthDialog() {
  authDialogRef.value?.open('login')
}

watch(
  () => route.fullPath,
  () => clearPrimaryAction()
)

onMounted(() => {
  window.addEventListener('auth:required', openAuthDialog)
})

onBeforeUnmount(() => {
  window.removeEventListener('auth:required', openAuthDialog)
})
</script>

<style scoped>
.app-shell {
  display: flex;
  min-height: 100vh;
  background: var(--surface-page, #f6f7f9);
  color: var(--text-primary, #18181b);
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex: 0 0 240px;
  flex-direction: column;
  width: 240px;
  height: 100vh;
  padding: 18px 14px;
  color: #e5e7eb;
  background: #0f172a;
  transition: width 180ms ease, flex-basis 180ms ease;
}

.app-shell--collapsed .app-shell__sidebar {
  flex-basis: 76px;
  width: 76px;
}

.app-shell__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 44px;
  padding: 0 8px 18px;
}

.app-shell__brand-mark {
  display: grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 8px;
  color: #042f2e;
  font-weight: 800;
  background: #5eead4;
}

.app-shell__brand-text {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.app-shell__brand-text strong {
  font-size: 16px;
  line-height: 1.2;
}

.app-shell__brand-text span {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.app-shell__nav {
  display: grid;
  gap: 6px;
}

.app-shell__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 0 11px;
  border-radius: 8px;
  color: #cbd5e1;
  font-size: 14px;
  font-weight: 500;
  transition: color 150ms ease, background 150ms ease;
}

.app-shell__nav-item:hover,
.app-shell__nav-item--active {
  color: #ffffff;
  background: rgb(20 184 166 / 0.18);
}

.app-shell__nav-icon {
  flex: 0 0 18px;
  width: 18px;
  height: 18px;
}

.app-shell__collapse {
  display: grid;
  width: 40px;
  height: 40px;
  margin: auto auto 0;
  place-items: center;
  border: 1px solid rgb(148 163 184 / 0.22);
  border-radius: 8px;
  color: #cbd5e1;
  background: rgb(15 23 42 / 0.8);
  cursor: pointer;
}

.app-shell__collapse:hover {
  color: #ffffff;
  border-color: rgb(94 234 212 / 0.5);
}

.app-shell__main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.app-shell__topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 76px;
  padding: 16px 28px;
  border-bottom: 1px solid var(--border-default, #e4e4e7);
  background: rgb(255 255 255 / 0.92);
  backdrop-filter: blur(12px);
}

.app-shell__heading {
  min-width: 0;
}

.app-shell__heading h1 {
  margin: 0;
  color: var(--text-primary, #18181b);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.25;
}

.app-shell__heading p {
  margin: 4px 0 0;
  color: var(--text-secondary, #52525b);
  font-size: 13px;
  line-height: 1.5;
}

.app-shell__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
}

.app-shell__content {
  min-width: 0;
  flex: 1;
  padding: 24px 28px 32px;
}

@media (max-width: 768px) {
  .app-shell__sidebar {
    flex-basis: 76px;
    width: 76px;
  }

  .app-shell__brand-text,
  .app-shell__nav-item span {
    display: none;
  }

  .app-shell__topbar {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    padding: 14px 18px;
  }

  .app-shell__content {
    padding: 18px;
  }
}
</style>
