<template>
  <div class="mcp-list-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">MCP 服务器管理</h1>
        <p class="page-desc">管理 MCP (Model Context Protocol) 工具服务器，为 Agent 提供外部工具调用能力</p>
      </div>
      <div class="header-right">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增服务器</el-button>
      </div>
    </div>

    <HifyTable
      ref="tableRef"
      :columns="columns"
      :api="fetchList"
      :show-pagination="true"
      empty-text="暂无 MCP 服务器"
    >
      <template #transportType="{ row }">
        <el-tag :type="row.transportType === 'sse' ? 'success' : ''" size="small">
          {{ row.transportType === 'sse' ? 'SSE' : 'Stdio' }}
        </el-tag>
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
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
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
          <el-input v-model="form.name" placeholder="例如：filesystem-server" clearable />
        </el-form-item>

        <el-form-item label="传输类型" prop="transportType">
          <el-select v-model="form.transportType" placeholder="请选择传输类型" style="width: 100%">
            <el-option label="Stdio（子进程通信）" value="stdio" />
            <el-option label="SSE（HTTP 服务）" value="sse" />
          </el-select>
        </el-form-item>

        <template v-if="form.transportType !== 'sse'">
          <el-form-item label="Command" prop="command">
            <el-input v-model="form.command" placeholder="例如：npx 或 uvx" clearable />
          </el-form-item>

          <el-form-item label="参数 (JSON 数组)">
            <el-input
              v-model="form.argsJson"
              placeholder='例如：["-y", "@modelcontextprotocol/server-filesystem"]'
              clearable
            />
          </el-form-item>

          <el-form-item label="环境变量 (JSON 对象)">
            <el-input
              v-model="form.envVarsJson"
              placeholder='例如：{"HOME": "/tmp"}'
              clearable
            />
          </el-form-item>
        </template>

        <template v-if="form.transportType === 'sse'">
          <el-form-item label="URL" prop="url">
            <el-input v-model="form.url" placeholder="例如：http://localhost:8080/sse" clearable />
          </el-form-item>
        </template>

        <el-form-item label="状态">
          <el-switch
            :model-value="form.status === 1"
            active-text="启用"
            inactive-text="禁用"
            @change="(val: boolean) => form.status = val ? 1 : 0"
          />
        </el-form-item>
      </template>
    </HifyFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import HifyTable, { type TableColumn } from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess, notifyError } from '@/utils/notify'
import {
  getMcpServerList,
  createMcpServer,
  updateMcpServer,
  deleteMcpServer,
} from '@/api/mcpServer'
import type { McpServer, McpServerRequest } from '@/api/mcpServer'

// ── 表格列 ──────────────────────────────────────────

const columns: TableColumn<McpServer>[] = [
  { prop: 'name', label: '名称', minWidth: 180 },
  { prop: 'transportType', label: '传输类型', width: 100, slot: 'transportType', align: 'center' },
  { prop: 'command', label: 'Command', minWidth: 160 },
  { prop: 'url', label: 'URL', minWidth: 200 },
  { prop: 'status', label: '状态', width: 80, slot: 'status', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 170, slot: 'createdAt' },
  { prop: 'action', label: '操作', width: 160, slot: 'action', fixed: 'right', align: 'center' },
]

// ── 表单校验规则 ─────────────────────────────────────

const formRules = {
  name: [
    { required: true, message: '请输入服务器名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度 2 ~ 50 个字符', trigger: 'blur' },
  ],
  transportType: [{ required: true, message: '请选择传输类型', trigger: 'change' }],
}

const fetchList = (params: any) => getMcpServerList(params)

// ── 引用 ────────────────────────────────────────────

const tableRef = ref<any>(null)
const dialogRef = ref<any>(null)
const { confirmDelete } = useConfirm()

const dialogTitle = ref('新增 MCP 服务器')

// ── 新增 ────────────────────────────────────────────

const handleAdd = () => {
  dialogTitle.value = '新增 MCP 服务器'
  dialogRef.value?.open({ status: 1, transportType: 'stdio' })
}

// ── 编辑 ────────────────────────────────────────────

const handleEdit = (row: McpServer) => {
  dialogTitle.value = '编辑 MCP 服务器'
  dialogRef.value?.open({ ...row })
}

// ── 删除 ────────────────────────────────────────────

const handleDelete = async (row: McpServer) => {
  try {
    await confirmDelete(
      `确定要删除 MCP 服务器 "${row.name}" 吗？`,
      () => deleteMcpServer(row.id),
      { title: '删除 MCP 服务器', successMessage: '删除成功' }
    )
    tableRef.value?.refresh(true)
  } catch {
    // 取消或失败
  }
}

// ── 提交表单 ────────────────────────────────────────

const handleSubmit = async (formData: any, isEdit: boolean) => {
  try {
    dialogRef.value?.setLoading(true)

    const req: McpServerRequest = {
      name: formData.name,
      command: formData.command || undefined,
      argsJson: formData.argsJson || undefined,
      envVarsJson: formData.envVarsJson || undefined,
      url: formData.url || undefined,
      transportType: formData.transportType || 'stdio',
      status: formData.status ?? 1,
    }

    if (isEdit) {
      await updateMcpServer(formData.id, req)
    } else {
      await createMcpServer(req)
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
.mcp-list-page {
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
