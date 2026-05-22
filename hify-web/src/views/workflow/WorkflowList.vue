<template>
  <div class="workflow-list-page">
    <div class="page-toolbar">
      <h2>工作流</h2>
      <el-button type="primary" :icon="Plus" @click="$router.push('/workflows/create')">新建工作流</el-button>
    </div>

    <el-table :data="workflows" stripe v-loading="loading">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column label="节点数" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.nodes?.length || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/workflows/${row.id}/edit`)">编辑</el-button>
          <el-button size="small" type="warning" @click="handleRun(row)">运行</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkflowList, deleteWorkflow, runWorkflow, type Workflow } from '@/api/workflow'

const workflows = ref<Workflow[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getWorkflowList({ page: page.value, pageSize: pageSize.value })
    workflows.value = res.list
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function handleRun(row: Workflow) {
  try {
    await runWorkflow(row.id)
    ElMessage.success('执行完成')
  } catch {
    ElMessage.error('执行失败')
  }
}

async function handleDelete(row: Workflow) {
  await ElMessageBox.confirm('确定删除该工作流吗？', '提示', { type: 'warning' })
  await deleteWorkflow(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function formatDate(s: string) {
  return s ? s.replace('T', ' ').substring(0, 19) : ''
}
</script>

<style scoped>
.workflow-list-page { padding: 0; }
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
