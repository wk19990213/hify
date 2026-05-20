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

    <HifyTable
      ref="tableRef"
      :columns="columns"
      :api="fetchList"
      :show-pagination="true"
      empty-text="暂无 Agent 数据"
    >
      <template #status="{ row }">
        <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" effect="light">
          {{ row.status === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>

      <template #modelConfigName="{ row }">
        {{ row.modelConfigName || '-' }}
      </template>

      <template #toolCount="{ row }">
        <el-tag v-if="row.toolCount > 0" type="primary" size="small">{{ row.toolCount }} 个</el-tag>
        <span v-else class="text-gray">-</span>
      </template>

      <template #temperature="{ row }">
        {{ row.temperature !== null && row.temperature !== undefined ? row.temperature : '-' }}
      </template>

      <template #createdAt="{ row }">
        {{ formatDateTime(row.createdAt) }}
      </template>

      <template #action="{ row }">
        <div class="action-btns">
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button type="warning" link :icon="ChatDotRound" @click="handleChat(row)">对话</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </div>
      </template>
    </HifyTable>

    <HifyFormDialog
      ref="dialogRef"
      :title="dialogTitle"
      width="600px"
      :rules="formRules"
      @submit="handleSubmit"
    >
      <template #default="{ form }">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入 Agent 名称" clearable />
        </el-form-item>

        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="唯一编码，如 customer-service" clearable />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="描述这个 Agent 的用途"
            clearable
          />
        </el-form-item>

        <el-form-item label="模型配置">
          <el-select v-model="form.modelConfigId" placeholder="请选择模型" style="width: 100%" clearable>
            <el-option
              v-for="item in modelOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="知识库">
          <el-select v-model="form.kbId" placeholder="请选择知识库（可选）" style="width: 100%" clearable>
            <el-option
              v-for="item in kbOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="系统提示词">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :rows="4"
            placeholder="设置系统提示词，定义 Agent 的角色和行为"
            clearable
          />
        </el-form-item>

        <el-form-item label="温度参数">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>

        <el-form-item label="最大轮数">
          <el-input-number v-model="form.conversationMaxRounds" :min="1" :max="100" style="width: 100%" />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
        </el-form-item>
      </template>
    </HifyFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus, Edit, Delete, ChatDotRound } from '@element-plus/icons-vue'
import HifyTable, { type TableColumn } from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess, notifyError } from '@/utils/notify'
import { get } from '@/utils/request'
import {
  getAgentList,
  createAgent,
  updateAgent,
  deleteAgent,
} from '@/api/agent'
import type { Agent, AgentRequest } from '@/api/agent'

// ── 表格列 ──────────────────────────────────────────

const columns: TableColumn<Agent>[] = [
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'code', label: '编码', minWidth: 140 },
  { prop: 'status', label: '状态', width: 80, slot: 'status', align: 'center' },
  { prop: 'modelConfigName', label: '模型配置', width: 150, slot: 'modelConfigName' },
  { prop: 'toolCount', label: '工具数', width: 80, slot: 'toolCount', align: 'center' },
  { prop: 'temperature', label: '温度', width: 80, slot: 'temperature', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 170, slot: 'createdAt' },
  { prop: 'action', label: '操作', width: 200, slot: 'action', fixed: 'right', align: 'center' },
]

// ── 表单校验规则 ─────────────────────────────────────

const formRules = {
  name: [
    { required: true, message: '请输入 Agent 名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度 2 ~ 50 个字符', trigger: 'blur' },
  ],
  code: [
    { min: 2, max: 64, message: '长度 2 ~ 64 个字符', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只允许小写字母、数字和连字符', trigger: 'blur' },
  ],
}

const fetchList = (params: any) => getAgentList(params)

// ── 引用 ────────────────────────────────────────────

const tableRef = ref<any>(null)
const dialogRef = ref<any>(null)
const { confirmDelete } = useConfirm()

const dialogTitle = ref('新增 Agent')

// ── 模型选项（从 /v1/model-configs 获取）──
const modelOptions = ref<{ id: number; name: string }[]>([])
const kbOptions = ref<{ id: number; name: string }[]>([])

onMounted(async () => {
  try {
    modelOptions.value = await get<{ id: number; name: string }[]>('/v1/providers/model-configs')
    const kbRes = await get<{ list: { id: number; name: string }[] }>('/v1/knowledge/bases')
    kbOptions.value = kbRes.list || []
  } catch {
    // 获取选项失败，下拉框为空
  }
})

// ── 新增 ────────────────────────────────────────────

const handleAdd = () => {
  dialogTitle.value = '新增 Agent'
  dialogRef.value?.setDefaultData({
    status: 1,
    temperature: 0.7,
    conversationMaxRounds: 20,
    sortOrder: 0,
  })
  dialogRef.value?.open()
}

// ── 编辑 ────────────────────────────────────────────

const handleEdit = (row: Agent) => {
  dialogTitle.value = '编辑 Agent'
  dialogRef.value?.open({
    id: row.id,
    name: row.name,
    code: row.code,
    description: row.description,
    modelConfigId: row.modelConfigId,
    kbId: row.kbId,
    systemPrompt: row.systemPrompt,
    temperature: row.temperature,
    conversationMaxRounds: row.conversationMaxRounds,
    status: row.status,
    sortOrder: row.sortOrder,
  })
}

// ── 删除 ────────────────────────────────────────────

const handleDelete = async (row: Agent) => {
  try {
    await confirmDelete(
      `确定要删除 Agent "${row.name}" 吗？`,
      () => deleteAgent(row.id),
      { title: '删除 Agent', successMessage: '删除成功' }
    )
    tableRef.value?.refresh(true)
  } catch {
    // 取消或失败
  }
}

// ── 对话 ────────────────────────────────────────────

const handleChat = (row: Agent) => {
  // 跳转到对话页面，传入 agentId
  window.location.href = `/chat?agentId=${row.id}`
}

// ── 提交表单 ────────────────────────────────────────

const handleSubmit = async (formData: any, isEdit: boolean) => {
  try {
    dialogRef.value?.setLoading(true)

    const req: AgentRequest = {
      name: formData.name,
      code: formData.code,
      description: formData.description,
      modelConfigId: formData.modelConfigId,
      kbId: formData.kbId,
      systemPrompt: formData.systemPrompt,
      temperature: formData.temperature,
      conversationMaxRounds: formData.conversationMaxRounds,
      status: formData.status,
      sortOrder: formData.sortOrder,
    }

    if (isEdit) {
      await updateAgent(formData.id, req)
    } else {
      await createAgent(req)
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
.agent-list-page {
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

.text-gray {
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
