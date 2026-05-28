<template>
  <div class="dashboard-page">
    <PageHeader
      title="平台概览"
      description="Hify AI 工作台运行状态与关键指标"
    >
      <template #actions>
        <el-button type="primary" @click="$router.push('/agent')">
          <el-icon><Plus /></el-icon>
          新建 Agent
        </el-button>
      </template>
    </PageHeader>

    <section class="metrics-grid">
      <SummaryMetric label="模型提供商" :value="providerCount" />
      <SummaryMetric label="Agent 数量" :value="agentCount" />
      <SummaryMetric label="知识库文档" :value="kbDocCount" />
      <SummaryMetric label="MCP 工具" :value="mcpToolCount" />
      <SummaryMetric label="工作流" :value="workflowCount" />
      <SummaryMetric label="活跃会话" :value="activeSessionCount" />
    </section>

    <section class="dashboard-panels">
      <div class="panel">
        <div class="panel__header">
          <h3>待处理事项</h3>
        </div>
        <div class="panel__body">
          <div class="todo-item" v-for="item in todoItems" :key="item.text">
            <StatusBadge :text="item.status" :status="item.tone" />
            <span class="todo-text">{{ item.text }}</span>
          </div>
          <el-empty v-if="!todoItems.length" description="暂无待处理事项" />
        </div>
      </div>

      <div class="panel">
        <div class="panel__header">
          <h3>最近活动</h3>
        </div>
        <div class="panel__body">
          <div class="activity-item" v-for="item in recentActivities" :key="item.time">
            <span class="activity-action">{{ item.action }}</span>
            <span class="activity-time">{{ item.time }}</span>
          </div>
          <el-empty v-if="!recentActivities.length" description="暂无最近活动" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import SummaryMetric from '@/components/SummaryMetric.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import { getProviderList } from '@/api/provider'
import { getAgentList } from '@/api/agent'
import { getWorkflowList } from '@/api/workflow'
import { getMcpServerList } from '@/api/mcpServer'

const providerCount = ref(0)
const agentCount = ref(0)
const kbDocCount = ref(0)
const mcpToolCount = ref(0)
const workflowCount = ref(0)
const activeSessionCount = ref(0)
const todoItems = ref<{ text: string; status: string; tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral' }[]>([])
const recentActivities = ref<{ action: string; time: string }[]>([])

onMounted(async () => {
  try {
    const [providerRes, agentRes, workflowRes, mcpRes] = await Promise.allSettled([
      getProviderList({ page: 1, pageSize: 1 }),
      getAgentList({ page: 1, pageSize: 1 }),
      getWorkflowList({ page: 1, pageSize: 1 }),
      getMcpServerList({ page: 1, pageSize: 1 }),
    ])

    if (providerRes.status === 'fulfilled') providerCount.value = providerRes.value.total || 0
    if (agentRes.status === 'fulfilled') agentCount.value = agentRes.value.total || 0
    if (workflowRes.status === 'fulfilled') workflowCount.value = workflowRes.value.total || 0
    if (mcpRes.status === 'fulfilled') {
      mcpToolCount.value = mcpRes.value.total || 0
      activeSessionCount.value = mcpRes.value.list?.filter((s: any) => s.status === 'connected').length || 0
    }
  } catch (e) {
    // 静默处理 - 概览页加载失败不影响 UI 渲染
  }
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.dashboard-panels {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.panel {
  background: var(--bg-primary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.panel__header {
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--border-light);
}

.panel__header h3 {
  margin: 0;
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

.panel__body {
  padding: var(--space-3) var(--space-5);
}

.todo-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) 0;
}

.todo-item + .todo-item {
  border-top: 1px solid var(--border-light);
}

.todo-text {
  font-size: var(--text-sm);
  color: var(--text-primary);
}

.activity-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
}

.activity-item + .activity-item {
  border-top: 1px solid var(--border-light);
}

.activity-action {
  font-size: var(--text-sm);
  color: var(--text-primary);
}

.activity-time {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .dashboard-panels {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
}
</style>
