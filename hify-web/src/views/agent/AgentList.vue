<template>
  <div class="agent-list-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">Agent 管理</h1>
        <p class="page-desc">配置 AI Agent，绑定模型和工具</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增 Agent</el-button>
      </div>
    </div>

    <HifyTable ref="tableRef" :columns="columns" :api="fetchList" :show-pagination="true" empty-text="暂无 Agent 数据">
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>
      <template #modelConfigName="{ row }">{{ row.modelConfigName || '-' }}</template>
      <template #toolCount="{ row }">
        <el-tag v-if="row.toolCount > 0" type="primary" size="small">{{ row.toolCount }} 个</el-tag>
        <span v-else class="text-gray">-</span>
      </template>
      <template #temperature="{ row }">{{ row.temperature != null ? row.temperature : '-' }}</template>
      <template #action="{ row }">
        <div class="action-btns">
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="warning" link :icon="ChatDotRound" @click="handleChat(row)">对话</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </div>
      </template>
    </HifyTable>

    <AgentFormDialog ref="dialogRef" :model-options="modelOptions" :kb-options="kbOptions"
      :workflow-options="workflowOptions" :mcp-server-options="mcpServerOptions" @submit="handleSubmit" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Edit, Delete, ChatDotRound } from '@element-plus/icons-vue'
import HifyTable, { type TableColumn } from '@/components/HifyTable.vue'
import AgentFormDialog from './AgentFormDialog.vue'
import type { McpServerOption } from './McpServerSelector.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess, notifyError } from '@/utils/notify'
import { get } from '@/utils/request'
import { getAgentList, createAgent, updateAgent, deleteAgent } from '@/api/agent'
import type { Agent, AgentRequest } from '@/api/agent'
import { getWorkflowList } from '@/api/workflow'
import { getAllMcpTools } from '@/api/mcpServer'

const columns: TableColumn<Agent>[] = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'code', label: '编码', minWidth: 140 },
  { prop: 'status', label: '状态', width: 80, slot: 'status', align: 'center' },
  { prop: 'modelConfigName', label: '模型配置', width: 150, slot: 'modelConfigName' },
  { prop: 'toolCount', label: '工具数', width: 80, slot: 'toolCount', align: 'center' },
  { prop: 'temperature', label: '温度', width: 80, slot: 'temperature', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 170, type: 'datetime' },
  { prop: 'action', label: '操作', width: 200, slot: 'action', fixed: 'right', align: 'center' },
]

const fetchList = (params: any) => getAgentList(params)
const tableRef = ref<any>(null)
const dialogRef = ref<any>(null)
const { confirmDelete } = useConfirm()

// 下拉选项
const modelOptions = ref<{ id: number; name: string }[]>([])
const kbOptions = ref<{ id: number; name: string }[]>([])
const workflowOptions = ref<{ id: number; name: string }[]>([])
const mcpServerOptions = ref<McpServerOption[]>([])

onMounted(async () => {
  try {
    modelOptions.value = await get<{ id: number; name: string }[]>('/v1/providers/model-configs')
    const kbRes = await get<{ list: { id: number; name: string }[] }>('/v1/knowledge/bases')
    kbOptions.value = kbRes.list || []
    const wfRes = await getWorkflowList({ pageSize: 100 })
    workflowOptions.value = (wfRes.list || []).map((w: any) => ({ id: w.id, name: w.name }))
    const servers = await getAllMcpTools()
    mcpServerOptions.value = servers.map(s => ({
      serverId: s.serverId, serverName: s.serverName,
      toolCount: s.errorMsg ? 0 : s.tools.length,
      description: s.errorMsg || s.tools.map(t => t.name).join(', '),
      hasError: !!s.errorMsg,
    }))
  } catch { /* 获取选项失败，下拉框为空 */ }
})

// 新增
const handleAdd = () => {
  dialogRef.value?.setDefaultData({ status: 1, temperature: 0.7, conversationMaxRounds: 20, sortOrder: 0 })
  dialogRef.value?.open()
}

// 编辑
const handleEdit = (row: Agent) => {
  dialogRef.value?.open({
    id: row.id, name: row.name, code: row.code, description: row.description,
    modelConfigId: row.modelConfigId, kbId: row.kbId, systemPrompt: row.systemPrompt,
    temperature: row.temperature, conversationMaxRounds: row.conversationMaxRounds,
    status: row.status, sortOrder: row.sortOrder, workflowId: row.workflowId,
    _selectedMcpServers: row.mcpServerIds || [],
  })
}

// 删除
const handleDelete = async (row: Agent) => {
  try {
    await confirmDelete(`确定要删除 Agent "${row.name}" 吗？`, () => deleteAgent(row.id),
      { title: '删除 Agent', successMessage: '删除成功' })
    tableRef.value?.refresh(true)
  } catch { /* 取消或失败 */ }
}

// 对话
const handleChat = (row: Agent) => { window.location.href = `/chat?agentId=${row.id}` }

// 提交表单
const handleSubmit = async (formData: any, isEdit: boolean) => {
  try {
    dialogRef.value?.setLoading(true)
    const selected = (formData._selectedMcpServers || []) as any[]
    const cleanIds: number[] = selected.map((v: any) => Number(v)).filter((n: number) => Number.isFinite(n) && n > 0)
    const req: AgentRequest = {
      name: formData.name, code: formData.code, description: formData.description,
      modelConfigId: formData.modelConfigId, kbId: formData.kbId,
      systemPrompt: formData.systemPrompt, temperature: formData.temperature,
      conversationMaxRounds: formData.conversationMaxRounds,
      status: formData.status, sortOrder: formData.sortOrder,
      workflowId: formData.workflowId,
      mcpServerIds: cleanIds.length > 0 ? cleanIds : undefined,
    }
    if (isEdit) { await updateAgent(formData.id, req) }
    else { await createAgent(req) }
    notifySuccess(isEdit ? '编辑成功' : '新增成功')
    dialogRef.value?.close()
    tableRef.value?.refresh(true)
  } catch (e: any) {
    console.error('Agent save failed', e)
    notifyError('操作失败', e?.response?.data?.message || e?.message || '请稍后重试')
  } finally { dialogRef.value?.setLoading(false) }
}
</script>

<style scoped>
.agent-list-page { padding: 24px; max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 24px; border-bottom: 1px solid var(--border-light); }
.header-left { flex: 1; }
.page-title { font-size: 24px; font-weight: 600; color: var(--text-primary); margin: 0 0 8px 0; }
.page-desc { font-size: 14px; color: var(--text-secondary); margin: 0; }
.header-right { flex-shrink: 0; }
.action-btns { display: flex; gap: 4px; flex-wrap: nowrap; white-space: nowrap; }
.text-gray { color: var(--text-secondary); }

@media (max-width: 768px) {
  .page-header { flex-direction: column; align-items: flex-start; gap: 16px; }
  .header-right { width: 100%; }
  .header-right :deep(.el-button) { width: 100%; }
}
</style>
