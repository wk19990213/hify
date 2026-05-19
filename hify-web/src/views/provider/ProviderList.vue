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

    <HifyTable
      ref="tableRef"
      :columns="columns"
      :api="fetchList"
      :show-pagination="true"
      empty-text="暂无提供商数据"
    >
      <template #type="{ row }">
        <el-tag :type="typeTagType(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
      </template>

      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>

      <template #health="{ row }">
        <template v-if="row.health">
          <el-tag :type="healthTagType(row.health.status)" size="small">
            {{ healthLabel(row.health.status) }}
          </el-tag>
          <span v-if="row.health.avgLatencyMs" class="health-latency">{{ row.health.avgLatencyMs }}ms</span>
        </template>
        <el-tag v-else type="info" size="small">未知</el-tag>
      </template>

      <template #modelCount="{ row }">
        {{ row.modelCount }}
      </template>

      <template #createdAt="{ row }">
        {{ formatDateTime(row.createdAt) }}
      </template>

      <template #action="{ row }">
        <div class="action-btns">
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="warning" link :icon="Connection" @click="handleTestConnection(row)">测试</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </div>
      </template>
    </HifyTable>

    <HifyFormDialog
      ref="dialogRef"
      :title="dialogTitle"
      width="560px"
      :rules="formRules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入提供商名称" clearable />
        </el-form-item>

        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="OpenAI" value="OPENAI" />
            <el-option label="Anthropic" value="ANTHROPIC" />
            <el-option label="Ollama" value="OLLAMA" />
            <el-option label="OpenAI Compatible" value="OPENAI_COMPATIBLE" />
          </el-select>
        </el-form-item>

        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.example.com/v1" clearable />
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="form._apiKey"
            type="password"
            placeholder="请输入 API Key"
            show-password
            clearable
          />
        </el-form-item>
      </template>
    </HifyFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Edit, Delete, Connection } from '@element-plus/icons-vue'
import HifyTable, { type TableColumn, type PageResult } from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess, notifyError } from '@/utils/notify'
import {
  getProviderList,
  createProvider,
  updateProvider,
  deleteProvider,
  testConnection,
} from '@/api/provider'
import type { Provider, ProviderRequest, ProviderHealth } from '@/api/provider'

// ── 类型标签映射 ──────────────────────────────────────

const typeMap: Record<string, string> = {
  OPENAI: '',
  ANTHROPIC: 'warning',
  OLLAMA: 'info',
  OPENAI_COMPATIBLE: '',
}

const typeLabelMap: Record<string, string> = {
  OPENAI: 'OpenAI',
  ANTHROPIC: 'Anthropic',
  OLLAMA: 'Ollama',
  OPENAI_COMPATIBLE: 'Compatible',
}

const typeTagType = (t: string) => typeMap[t] || 'info'
const typeLabel = (t: string) => typeLabelMap[t] || t

// ── 健康状态映射 ──────────────────────────────────────

const healthStatusMap: Record<string, { label: string; type: string }> = {
  HEALTHY: { label: '正常', type: 'success' },
  UNHEALTHY: { label: '故障', type: 'danger' },
  DEGRADED: { label: '降级', type: 'warning' },
  UNKNOWN: { label: '未知', type: 'info' },
}

const healthTagType = (status?: string) => healthStatusMap[status || 'UNKNOWN']?.type || 'info'
const healthLabel = (status?: string) => healthStatusMap[status || 'UNKNOWN']?.label || '未知'

// ── 表格列 ──────────────────────────────────────────

const columns: TableColumn<Provider>[] = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'type', label: '类型', width: 120, slot: 'type' },
  { prop: 'baseUrl', label: 'Base URL', minWidth: 240 },
  { prop: 'health', label: '健康状态', width: 140, slot: 'health' },
  { prop: 'modelCount', label: '模型数', width: 80, slot: 'modelCount', align: 'center' },
  { prop: 'status', label: '状态', width: 80, slot: 'status', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 170, slot: 'createdAt' },
  { prop: 'action', label: '操作', width: 200, slot: 'action', fixed: 'right', align: 'center' },
]

// ── 表单校验规则 ─────────────────────────────────────

const formRules = {
  name: [
    { required: true, message: '请输入提供商名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度 2 ~ 50 个字符', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
}

const fetchList = (params: any) => getProviderList(params)

// ── 引用 ────────────────────────────────────────────

const tableRef = ref<any>(null)
const dialogRef = ref<any>(null)
const { confirmDelete } = useConfirm()

const dialogTitle = ref('新增提供商')

// ── 新增 ────────────────────────────────────────────

const handleAdd = () => {
  dialogTitle.value = '新增提供商'
  dialogRef.value?.open()
}

// ── 编辑 ────────────────────────────────────────────

const handleEdit = (row: Provider) => {
  dialogTitle.value = '编辑提供商'
  // 从 authConfig 中提取 apiKey 供表单展示
  const apiKey = row.authConfig?.apiKey || ''
  dialogRef.value?.open({ ...row, _apiKey: apiKey })
}

// ── 删除 ────────────────────────────────────────────

const handleDelete = async (row: Provider) => {
  try {
    await confirmDelete(
      `确定要删除提供商 "${row.name}" 吗？`,
      () => deleteProvider(row.id),
      { title: '删除提供商', successMessage: '删除成功' }
    )
    tableRef.value?.refresh(true)
  } catch {
    // 取消或失败
  }
}

// ── 连通性测试 ───────────────────────────────────────

const handleTestConnection = async (row: Provider) => {
  try {
    const result = await testConnection(row.id)
    if (result.success) {
      notifySuccess(`连接成功，延迟 ${result.latencyMs}ms，${result.modelCount} 个模型可用`)
    } else {
      notifyError('连接失败', result.errorMessage || '未知错误')
    }
  } catch {
    notifyError('测试失败', '请稍后重试')
  }
}

// ── 提交表单 ────────────────────────────────────────

const handleSubmit = async (formData: any, isEdit: boolean) => {
  try {
    dialogRef.value?.setLoading(true)

    // 从 _apiKey 构造 authConfig
    const { _apiKey, ...rest } = formData
    const req: ProviderRequest = {
      ...rest,
      ...(_apiKey ? { authConfig: { apiKey: _apiKey } } : {}),
    }

    if (isEdit) {
      await updateProvider(formData.id, req)
    } else {
      await createProvider(req)
    }

    notifySuccess(isEdit ? '编辑成功' : '新增成功')
    dialogRef.value?.close()
    tableRef.value?.refresh(true)
  } catch {
    notifyError('操作失败', '请稍后重试')
  } finally {
    dialogRef.value?.setLoading(false)
  }
}

// ── 工具函数 ────────────────────────────────────────

const formatDateTime = (datetime: string) => {
  if (!datetime) return '-'
  const d = new Date(datetime)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.provider-list-page {
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

.header-left {
  flex: 1;
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

.header-right {
  flex-shrink: 0;
}

.action-btns {
  display: flex;
  gap: 4px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.action-btns :deep(.el-button) {
  white-space: nowrap;
}

.health-latency {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  .header-right {
    width: 100%;
  }
  .header-right :deep(.el-button) {
    width: 100%;
  }
}
</style>
