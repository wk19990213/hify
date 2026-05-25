<template>
  <div class="workflow-list-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">工作流</h1>
        <p class="page-desc">编排节点与连线，绑定到 Agent 后自动执行</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="$router.push('/workflows/create')">新建工作流</el-button>
      </div>
    </div>

    <HifyTable
      ref="tableRef"
      :columns="columns"
      :api="fetchList"
      :show-pagination="true"
      empty-text="暂无工作流数据"
    >
      <template #nodeCount="{ row }">
        <el-tag v-if="row.nodes?.length" size="small">{{ row.nodes.length }} 个</el-tag>
        <span v-else class="text-gray">-</span>
      </template>

      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>

      <template #createdAt="{ row }">
        {{ formatDateTime(row.createdAt) }}
      </template>

      <template #action="{ row }">
        <div class="action-btns">
          <el-button type="primary" link :icon="Edit" @click="$router.push(`/workflows/${row.id}/edit`)">编辑</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </div>
      </template>
    </HifyTable>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HifyTable, { type TableColumn } from '@/components/HifyTable.vue'
import { getWorkflowList, deleteWorkflow, type Workflow, type WorkflowListParams } from '@/api/workflow'
import { formatDateTime } from '@/utils/date'

const columns: TableColumn<Workflow>[] = [
  { prop: 'name', label: '名称', minWidth: 180 },
  { prop: 'description', label: '描述', minWidth: 200 },
  { prop: 'nodeCount', label: '节点数', width: 100, slot: 'nodeCount', align: 'center' },
  { prop: 'status', label: '状态', width: 80, slot: 'status', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 180, slot: 'createdAt' },
  { prop: 'action', label: '操作', width: 160, slot: 'action', fixed: 'right', align: 'center' },
]

const fetchList = (params: WorkflowListParams) => getWorkflowList(params)

const tableRef = ref<any>(null)

async function handleDelete(row: Workflow) {
  try {
    await ElMessageBox.confirm(`确定要删除工作流「${row.name}」吗？`, '删除工作流', { type: 'warning' })
    await deleteWorkflow(row.id)
    ElMessage.success('删除成功')
    tableRef.value?.refresh(true)
  } catch {
    // 取消或失败
  }
}


</script>

<style scoped>
.workflow-list-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-light);
}

.header-left { flex: 1; }

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

.header-right { flex-shrink: 0; }

.action-btns {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
}

.text-gray { color: var(--text-secondary); }
</style>
