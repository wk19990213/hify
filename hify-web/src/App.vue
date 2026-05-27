<template>
  <div class="layout">
    <!-- 深色侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: isCollapsed }">
      <!-- Logo 区域 -->
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">
            <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#8b5cf6"/>
                  <stop offset="100%" stop-color="#06b6d4"/>
                </linearGradient>
              </defs>
              <rect width="32" height="32" rx="8" fill="url(#logoGradient)"/>
              <path d="M8 10h16M8 16h12M8 22h8" stroke="white" stroke-width="2.5" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="logo-text" v-show="!isCollapsed">
            <div class="brand-name">Hify</div>
            <div class="brand-tagline">AI Agent Platform</div>
          </div>
        </div>
      </div>

      <!-- 菜单导航 -->
      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          :class="['sidebar-item', { active: activeMenu === item.path }]"
          :title="isCollapsed ? item.label : ''"
        >
          <el-icon :size="18" class="menu-icon">
            <component :is="item.icon" />
          </el-icon>
          <span class="menu-label" v-show="!isCollapsed">{{ item.label }}</span>
        </router-link>
      </nav>

      <!-- 底部区域 -->
      <div class="sidebar-footer">
        <!-- 用户区域 -->
        <div v-if="isLoggedIn" class="user-area" :class="{ collapsed: isCollapsed }">
          <div class="user-info" v-show="!isCollapsed">
            <el-icon :size="16"><UserFilled /></el-icon>
            <span class="user-name">{{ user?.username }}</span>
          </div>
          <el-button link class="logout-btn" @click="logout" :title="isCollapsed ? '退出登录' : ''">
            <el-icon :size="16"><SwitchButton /></el-icon>
            <span v-show="!isCollapsed">退出</span>
          </el-button>
        </div>
        <div v-else class="user-area" :class="{ collapsed: isCollapsed }">
          <button class="login-btn" @click="authDialog?.open('login')">
            <el-icon :size="16"><User /></el-icon>
            <span v-show="!isCollapsed">登录 / 注册</span>
          </button>
        </div>

        <!-- 折叠按钮 -->
        <button class="collapse-btn" @click="toggleCollapse" :title="isCollapsed ? '展开' : '收起'">
          <el-icon :size="16">
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <span v-show="!isCollapsed">收起菜单</span>
        </button>

        <!-- 版本号 -->
        <div class="version" v-show="!isCollapsed">
          <span class="version-dot"></span>
          <span>v1.0.0</span>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main" :class="{ 'main-expanded': isCollapsed }">
      <router-view />
    </main>

    <!-- 登录/注册弹窗 -->
    <AuthDialog ref="authDialog" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  Setting,
  User,
  Document,
  ChatDotRound,
  Share,
  Fold,
  Expand,
  Tools,
  UserFilled,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useAuth } from '@/composables/useAuth'
import AuthDialog from '@/components/AuthDialog.vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
const { isLoggedIn, user, logout } = useAuth()
const authDialog = ref<InstanceType<typeof AuthDialog> | null>(null)

// 折叠状态
const isCollapsed = ref(false)
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

// 监听全局 401 → 弹出登录框
function onAuthRequired() {
  authDialog.value?.open('login')
}
onMounted(() => window.addEventListener('auth:required', onAuthRequired))
onUnmounted(() => window.removeEventListener('auth:required', onAuthRequired))

const menuItems = [
  { path: '/provider', label: '模型管理', icon: Setting },
  { path: '/agent', label: 'Agent 管理', icon: User },
  { path: '/knowledge', label: '知识库', icon: Document },
  { path: '/workflows', label: '工作流', icon: Share },
  { path: '/mcp-servers', label: 'MCP 管理', icon: Tools },
  { path: '/chat', label: '对话', icon: ChatDotRound },
]
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* ========== 侧边栏 ========== */
.sidebar {
  width: 240px;
  height: 100vh;
  background: var(--bg-dark);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  z-index: var(--z-fixed);
  transition: width 0.3s var(--ease-spring);
  overflow: hidden;
}

/* 折叠状态 */
.sidebar.collapsed {
  width: 64px;
}

/* ========== Logo 区域 ========== */
.sidebar-header {
  padding: 24px 20px;
  flex-shrink: 0;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 2px 8px rgba(139, 92, 246, 0.4));
}

.logo-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.brand-name {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #a78bfa 0%, #67e8f9 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.brand-tagline {
  font-size: 11px;
  color: var(--gray-500);
  letter-spacing: 0.5px;
  text-transform: uppercase;
  line-height: 1.2;
}

/* 折叠时隐藏文字 */
.sidebar.collapsed .logo-text {
  opacity: 0;
  width: 0;
}

/* ========== 菜单导航 ========== */
.sidebar-nav {
  flex: 1;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 菜单项基础样式 */
.sidebar-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  color: rgba(255, 255, 255, 0.75);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

/* hover 状态 - 背景微亮 */
.sidebar-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.95);
}

/* 选中状态 - 左边3px主色竖线 + 背景微亮 */
.sidebar-item.active {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.sidebar-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: linear-gradient(180deg, var(--primary-400), var(--accent-400));
  border-radius: 0 2px 2px 0;
  box-shadow: 0 0 8px rgba(139, 92, 246, 0.5);
}

/* 图标样式 */
.menu-icon {
  flex-shrink: 0;
  transition: transform 0.2s ease;
}

.sidebar-item:hover .menu-icon {
  transform: scale(1.05);
}

.sidebar-item.active .menu-icon {
  color: var(--primary-400);
}

/* 菜单标签 */
.menu-label {
  white-space: nowrap;
  overflow: hidden;
  transition: opacity 0.2s ease;
}

/* 折叠时居中 */
.sidebar.collapsed .sidebar-item {
  justify-content: center;
  padding: 14px;
}

.sidebar.collapsed .sidebar-item.active::before {
  height: 32px;
}

/* ========== 底部区域 ========== */
.sidebar-footer {
  padding: 16px 14px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 用户区域 */
.user-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  margin-bottom: 4px;
}

.user-area.collapsed {
  justify-content: center;
  padding: 10px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
}

.user-name {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.logout-btn {
  color: rgba(255, 255, 255, 0.5) !important;
  font-size: 12px;
  padding: 4px 8px;
}

.logout-btn:hover {
  color: rgba(255, 100, 100, 0.8) !important;
}

.login-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(6, 182, 212, 0.1));
  border: 1px solid rgba(139, 92, 246, 0.25);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.login-btn:hover {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.25), rgba(6, 182, 212, 0.15));
  border-color: rgba(139, 92, 246, 0.4);
  color: #fff;
}

/* 折叠按钮 */
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.9);
}

/* 折叠状态下的按钮 */
.sidebar.collapsed .collapse-btn {
  justify-content: center;
  padding: 12px;
}

/* 版本号 */
.version {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: var(--gray-600);
}

.version-dot {
  width: 6px;
  height: 6px;
  background: linear-gradient(135deg, var(--success-500), var(--accent-500));
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(34, 197, 94, 0.4);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(0.95); }
}

/* ========== 主内容区 ========== */
.main {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
  background: var(--bg-secondary);
  transition: margin-left 0.3s var(--ease-spring);
}

.main.main-expanded {
  margin-left: 64px;
}

/* ========== 滚动条美化 ========== */
.sidebar-nav::-webkit-scrollbar {
  width: 4px;
}

.sidebar-nav::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
}

.sidebar-nav::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.2);
}
</style>
