<template>
  <div class="provider-list-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">模型提供商管理</h1>
        <p class="page-desc">管理接入的大模型提供商，包括 OpenAI、Claude、Ollama 等</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增提供商</el-button>
      </div>
    </div>

    <HifyTable ref="tableRef" :columns="columns" :api="fetchList" :show-pagination="true" empty-text="暂无提供商数据">
      <template #type="{ row }">
        <el-tag :type="typeTagType(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
      </template>
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>
      <template #health="{ row }"><HealthStatusCell :health="row.health" /></template>
      <template #modelCount="{ row }">{{ row.modelCount }}</template>
      <template #action="{ row }">
        <div class="action-btns">
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="warning" link :icon="Connection" @click="handleTestConnection(row)">测试</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </div>
      </template>
    </HifyTable>

    <ProviderFormDialog ref="dialogRef" @submit="handleSubmit" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Edit, Delete, Connection } from '@element-plus/icons-vue'
import HifyTable, { type TableColumn } from '@/components/HifyTable.vue'
import ProviderFormDialog from './ProviderFormDialog.vue'
import HealthStatusCell from '@/components/HealthStatusCell.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess, notifyError } from '@/utils/notify'
import { getProviderList, createProvider, updateProvider, deleteProvider, testConnection } from '@/api/provider'
import type { Provider, ProviderRequest } from '@/api/provider'

// 类型标签映射
const typeMap: Record<string, string> = { OPENAI: '', ANTHROPIC: 'warning', OLLAMA: 'info', OPENAI_COMPATIBLE: '' }
const typeLabelMap: Record<string, string> = { OPENAI: 'OpenAI', ANTHROPIC: 'Anthropic', OLLAMA: 'Ollama', OPENAI_COMPATIBLE: 'Compatible' }
const typeTagType = (t: string) => typeMap[t] || 'info'
const typeLabel = (t: string) => typeLabelMap[t] || t

const columns: TableColumn<Provider>[] = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'type', label: '类型', width: 120, slot: 'type' },
  { prop: 'baseUrl', label: 'Base URL', minWidth: 240 },
  { prop: 'health', label: '健康状态', width: 140, slot: 'health' },
  { prop: 'modelCount', label: '模型数', width: 80, slot: 'modelCount', align: 'center' },
  { prop: 'status', label: '状态', width: 80, slot: 'status', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 170, type: 'datetime' },
  { prop: 'action', label: '操作', width: 200, slot: 'action', fixed: 'right', align: 'center' },
]

const fetchList = (params: any) => getProviderList(params)
const tableRef = ref<any>(null)
const dialogRef = ref<any>(null)
const { confirmDelete } = useConfirm()

// 新增
const handleAdd = () => { dialogRef.value?.open() }

// 编辑
const handleEdit = (row: Provider) => {
  dialogRef.value?.open({ ...row, _apiKey: row.authConfig?.apiKey || '' })
}

// 删除
const handleDelete = async (row: Provider) => {
  try {
    await confirmDelete(`确定要删除提供商 "${row.name}" 吗？`, () => deleteProvider(row.id),
      { title: '删除提供商', successMessage: '删除成功' })
    tableRef.value?.refresh(true)
  } catch { /* 取消或失败 */ }
}

// 连通性测试
const handleTestConnection = async (row: Provider) => {
  try {
    const result = await testConnection(row.id)
    if (result.success) { notifySuccess(`连接成功，延迟 ${result.latencyMs}ms，${result.modelCount} 个模型可用`) }
    else { notifyError('连接失败', result.errorMessage || '未知错误') }
    tableRef.value?.refresh()
  } catch { notifyError('测试失败', '请稍后重试') }
}

// 提交表单
const handleSubmit = async (formData: any, isEdit: boolean) => {
  try {
    dialogRef.value?.setLoading(true)
    const { _apiKey, ...rest } = formData
    const req: ProviderRequest = { ...rest, ...(_apiKey ? { authConfig: { apiKey: _apiKey } } : {}) }
    if (isEdit) { await updateProvider(formData.id, req) }
    else { await createProvider(req) }
    notifySuccess(isEdit ? '编辑成功' : '新增成功')
    dialogRef.value?.close()
    tableRef.value?.refresh(true)
  } catch { notifyError('操作失败', '请稍后重试') }
  finally { dialogRef.value?.setLoading(false) }
}
</script>

<style scoped>
.provider-list-page { padding: 24px; max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; padding-bottom: 24px; border-bottom: 1px solid var(--border-light); }
.header-left { flex: 1; }
.page-title { font-size: 24px; font-weight: 600; color: var(--text-primary); margin: 0 0 8px 0; }
.page-desc { font-size: 14px; color: var(--text-secondary); margin: 0; }
.header-right { flex-shrink: 0; }
.action-btns { display: flex; gap: 4px; flex-wrap: nowrap; white-space: nowrap; }

@media (max-width: 768px) {
  .page-header { flex-direction: column; align-items: flex-start; gap: 16px; }
  .header-right { width: 100%; }
  .header-right :deep(.el-button) { width: 100%; }
}
</style>
