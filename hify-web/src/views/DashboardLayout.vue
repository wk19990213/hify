<template>
  <div class="hify-layout">
    <!-- 深色侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-icon">H</span>
          <span class="logo-text">Hify</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <a
          v-for="item in menuItems"
          :key="item.path"
          :class="['sidebar-item', { active: currentPath === item.path }]"
          @click="navigate(item.path)"
        >
          <el-icon :size="18">
            <component :is="item.icon" />
          </el-icon>
          <span>{{ item.label }}</span>
        </a>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <el-avatar :size="32" class="user-avatar">
            {{ userInitials }}
          </el-avatar>
          <div class="user-meta">
            <div class="user-name">{{ userName }}</div>
            <div class="user-role">管理员</div>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <header class="page-header">
        <div class="breadcrumb">
          <el-breadcrumb>
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-actions">
          <el-button type="primary" :icon="Plus">新建 Agent</el-button>
        </div>
      </header>

      <div class="page-content">
        <!-- 统计卡片 -->
        <div class="stats-row">
          <div v-for="stat in stats" :key="stat.label" class="stat-card">
            <div class="stat-icon" :style="{ background: stat.bg }">
              <el-icon :size="24" :color="stat.color">
                <component :is="stat.icon" />
              </el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
            <div class="stat-trend" :class="{ up: stat.trend > 0 }">
              {{ stat.trend > 0 ? '+' : '' }}{{ stat.trend }}%
            </div>
          </div>
        </div>

        <!-- 内容区 -->
        <div class="content-grid">
          <div class="content-main">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span style="font-weight: 600;">Agent 列表</span>
                  <el-input
                    v-model="searchQuery"
                    placeholder="搜索 Agent..."
                    style="width: 240px"
                    :prefix-icon="Search"
                    clearable
                  />
                </div>
              </template>
              <el-table :data="agents" stripe>
                <el-table-column prop="name" label="名称">
                  <template #default="{ row }">
                    <div style="display: flex; align-items: center; gap: 12px;">
                      <div class="agent-icon" :style="{ background: row.color }">
                        <el-icon :size="16"><Cpu /></el-icon>
                      </div>
                      <div>
                        <div style="font-weight: 500;">{{ row.name }}</div>
                        <div style="font-size: 12px; color: var(--text-tertiary);">{{ row.desc }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="model" label="模型" width="140">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.modelType">{{ row.model }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <span :class="['status-badge', row.status]">
                      <span class="status-dot"></span>
                      {{ row.statusText }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="tokens" label="Token 消耗" width="120" align="right">
                  <template #default="{ row }">
                    <span style="font-family: var(--font-mono); font-feature-settings: 'tnum';">{{ row.tokens }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default>
                    <el-button type="primary" link>编辑</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div style="margin-top: 16px; display: flex; justify-content: flex-end;">
                <el-pagination
                  :total="100"
                  :page-size="10"
                  layout="prev, pager, next"
                />
              </div>
            </el-card>
          </div>

          <div class="content-sidebar">
            <el-card>
              <template #header>
                <span style="font-weight: 600;">系统状态</span>
              </template>
              <div class="status-list">
                <div v-for="service in services" :key="service.name" class="status-item">
                  <div class="status-item-info">
                    <span class="status-item-name">{{ service.name }}</span>
                    <span class="status-item-latency">{{ service.latency }}ms</span>
                  </div>
                  <div class="status-item-bar">
                    <div
                      class="status-item-fill"
                      :style="{ width: service.health + '%', background: service.color }"
                    ></div>
                  </div>
                </div>
              </div>
            </el-card>

            <el-card style="margin-top: 16px;">
              <template #header>
                <span style="font-weight: 600;">快速操作</span>
              </template>
              <div class="quick-actions">
                <el-button type="primary" style="width: 100%; justify-content: flex-start;">
                  <el-icon><Plus /></el-icon>
                  <span>新建 Agent</span>
                </el-button>
                <el-button style="width: 100%; justify-content: flex-start; margin: 8px 0 0 0;">
                  <el-icon><Upload /></el-icon>
                  <span>导入配置</span>
                </el-button>
                <el-button style="width: 100%; justify-content: flex-start; margin: 8px 0 0 0;">
                  <el-icon><Document /></el-icon>
                  <span>查看文档</span>
                </el-button>
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  HomeFilled,
  Cpu,
  ChatDotRound,
  Folder,
  Setting,
  Plus,
  Search,
  Upload,
  Document,
  DataLine,
  Monitor
} from '@element-plus/icons-vue'

const currentPath = ref('/dashboard')
const searchQuery = ref('')
const userName = ref('张三')

const userInitials = computed(() => userName.value.slice(0, 2))
const pageTitle = computed(() => {
  const item = menuItems.find(i => i.path === currentPath.value)
  return item?.label || '首页'
})

const menuItems = [
  { path: '/dashboard', label: '概览', icon: HomeFilled },
  { path: '/agents', label: 'Agents', icon: Cpu },
  { path: '/chat', label: '对话', icon: ChatDotRound },
  { path: '/knowledge', label: '知识库', icon: Folder },
  { path: '/settings', label: '设置', icon: Setting },
]

const stats = [
  { label: 'Agents', value: '12', trend: 8, icon: Cpu, bg: 'var(--primary-50)', color: 'var(--primary-500)' },
  { label: '对话数', value: '3,456', trend: 12, icon: ChatDotRound, bg: 'var(--accent-50)', color: 'var(--accent-500)' },
  { label: 'Token 消耗', value: '2.1M', trend: -3, icon: DataLine, bg: 'var(--success-50)', color: 'var(--success-500)' },
  { label: '平均延迟', value: '245ms', trend: -15, icon: Monitor, bg: 'var(--warning-50)', color: 'var(--warning-500)' },
]

const agents = [
  { name: '客服助手', desc: '处理客户咨询和售后问题', model: 'GPT-4', modelType: 'primary', status: 'running', statusText: '运行中', tokens: '1.2M', color: 'var(--primary-500)' },
  { name: '代码审查', desc: '自动审查代码质量和规范', model: 'Claude 3', modelType: 'success', status: 'paused', statusText: '已暂停', tokens: '856K', color: 'var(--accent-500)' },
  { name: '文档生成', desc: '根据代码自动生成文档', model: 'Gemini Pro', modelType: 'warning', status: 'error', statusText: '错误', tokens: '234K', color: 'var(--warning-500)' },
  { name: '数据分析', desc: '分析业务数据并生成报告', model: 'GPT-3.5', modelType: '', status: 'running', statusText: '运行中', tokens: '2.1M', color: 'var(--success-500)' },
]

const services = [
  { name: 'OpenAI API', latency: 245, health: 95, color: 'var(--primary-500)' },
  { name: 'Claude API', latency: 320, health: 88, color: 'var(--accent-500)' },
  { name: '本地 Ollama', latency: 89, health: 100, color: 'var(--success-500)' },
  { name: 'Redis', latency: 12, health: 99, color: 'var(--warning-500)' },
]

const navigate = (path: string) => {
  currentPath.value = path
}
</script>

<style scoped>
.hify-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* 侧边栏 - 深色 */
.sidebar {
  width: 240px;
  background: linear-gradient(180deg, var(--bg-dark) 0%, var(--bg-dark-elevated) 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
}

.sidebar-header {
  padding: var(--space-5);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--primary-500), var(--accent-500));
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: var(--font-bold);
  font-size: 14px;
  color: white;
}

.logo-text {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--text-inverse);
}

.sidebar-nav {
  flex: 1;
  padding: var(--space-2);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.sidebar-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  color: var(--gray-400);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
}

.sidebar-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--gray-200);
}

.sidebar-item.active {
  background: linear-gradient(135deg, var(--primary-700), var(--primary-600));
  color: white;
  box-shadow: var(--shadow-primary);
}

.sidebar-footer {
  padding: var(--space-4);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.user-avatar {
  background: linear-gradient(135deg, var(--primary-500), var(--accent-500));
  color: white;
  font-weight: var(--font-medium);
}

.user-meta {
  flex: 1;
}

.user-name {
  color: var(--gray-200);
  font-weight: var(--font-medium);
  font-size: var(--text-sm);
}

.user-role {
  color: var(--gray-500);
  font-size: var(--text-xs);
}

/* 主内容区 */
.main-content {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
}

.page-header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-light);
  padding: var(--space-4) var(--space-6);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.breadcrumb {
  flex: 1;
}

.page-content {
  padding: var(--space-6);
}

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-light);
  transition: box-shadow var(--transition-fast);
}

.stat-card:hover {
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  font-feature-settings: 'tnum';
  font-variant-numeric: tabular-nums;
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.stat-trend {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--error-500);
}

.stat-trend.up {
  color: var(--success-500);
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--space-6);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.agent-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
}

.status-badge.running .status-dot {
  background: var(--success-500);
  box-shadow: 0 0 0 2px var(--success-50);
}

.status-badge.paused .status-dot {
  background: var(--warning-500);
  box-shadow: 0 0 0 2px var(--warning-50);
}

.status-badge.error .status-dot {
  background: var(--error-500);
  box-shadow: 0 0 0 2px var(--error-50);
}

/* 系统状态 */
.status-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.status-item-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.status-item-name {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.status-item-latency {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.status-item-bar {
  height: 4px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.status-item-fill {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width var(--transition-normal);
}

/* 快速操作 */
.quick-actions {
  display: flex;
  flex-direction: column;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .content-sidebar {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-4);
  }
}

@media (max-width: 768px) {
  .sidebar {
    width: 64px;
  }

  .logo-text,
  .sidebar-item span,
  .user-meta {
    display: none;
  }

  .sidebar-item {
    justify-content: center;
    padding: var(--space-3);
  }

  .main-content {
    margin-left: 64px;
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .content-sidebar {
    grid-template-columns: 1fr;
  }
}
</style>
