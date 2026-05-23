<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h1 class="page-title">概览</h1>
      <p class="page-desc">Hify AI Agent 开发平台</p>
    </div>

    <div class="quick-links">
      <router-link v-for="link in links" :key="link.path" :to="link.path" class="quick-card">
        <el-icon :size="24">
          <component :is="link.icon" />
        </el-icon>
        <div class="quick-card-text">
          <div class="quick-card-title">{{ link.title }}</div>
          <div class="quick-card-desc">{{ link.desc }}</div>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Setting, User, Document, Share, Tools, ChatDotRound } from '@element-plus/icons-vue'

const links = [
  { path: '/agent', title: 'Agent 管理', desc: '创建和配置 AI Agent', icon: User },
  { path: '/provider', title: '模型管理', desc: '管理模型提供商和配置', icon: Setting },
  { path: '/knowledge', title: '知识库', desc: '管理 RAG 知识库文档', icon: Document },
  { path: '/workflows', title: '工作流', desc: '编排节点与连线', icon: Share },
  { path: '/mcp-servers', title: 'MCP 管理', desc: '管理 MCP 工具服务', icon: Tools },
  { path: '/chat', title: '对话', desc: '与 Agent 开始对话', icon: ChatDotRound },
]
</script>

<style scoped>
.dashboard-page {
  padding: 32px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--bg-primary);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  color: var(--text-primary);
  text-decoration: none;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.quick-card:hover {
  border-color: var(--primary-400);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.quick-card-text {
  flex: 1;
}

.quick-card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.quick-card-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

@media (max-width: 640px) {
  .quick-links {
    grid-template-columns: 1fr;
  }
}
</style>
