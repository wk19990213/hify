<template>
  <div class="provider-list-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">模型提供商管理</h1>
        <p class="page-desc">管理接入的大模型提供商，包括 OpenAI、Claude、Gemini、Ollama 等</p>
      </div>
      <div class="header-right">
        <el-button type="primary" size="large" :icon="Plus" @click="handleAdd">
          新增提供商
        </el-button>
      </div>
    </div>

    <!-- 列表区 -->
    <HifyTable
      ref="tableRef"
      :columns="columns"
      :api="fetchProviderList"
      :show-pagination="true"
      :default-page-size="10"
      empty-text="暂无提供商数据"
    >
      <!-- 类型列 -->
      <template #type="{ value }">
        <el-tag :type="getTypeTagType(value)" size="small">
          {{ value }}
        </el-tag>
      </template>

      <!-- 状态列 -->
      <template #status="{ value }">
        <el-tag :type="value === 1 ? 'success' : 'info'" size="small" effect="light">
          {{ value === 1 ? '启用' : '禁用' }}
        </el-tag>
      </template>

      <!-- 创建时间列 -->
      <template #createdAt="{ value }">
        {{ formatDateTime(value) }}
      </template>

      <!-- 操作列 -->
      <template #action="{ row }">
        <div class="action-btns">
          <el-button type="primary" link :icon="Edit" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">
            删除
          </el-button>
        </div>
      </template>
    </HifyTable>

    <!-- 新增/编辑弹窗 -->
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
            <el-option label="OpenAI" value="OpenAI" />
            <el-option label="Claude" value="Claude" />
            <el-option label="Gemini" value="Gemini" />
            <el-option label="Ollama" value="Ollama" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key" prop="apiKey">
          <el-input
            v-model="form.apiKey"
            type="password"
            placeholder="请输入 API Key"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item label="Base URL" prop="baseUrl">
          <el-input v-model="form.baseUrl" placeholder="https://api.example.com/v1" clearable />
        </el-form-item>
      </template>
    </HifyFormDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import HifyTable, { type TableColumn, type PageResult } from '@/components/HifyTable.vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import { useConfirm } from '@/composables/useConfirm'
import { notifySuccess, notifyError } from '@/utils/notify'

// 提供商类型
interface Provider {
  id: number
  name: string
  type: 'OpenAI' | 'Claude' | 'Gemini' | 'Ollama'
  apiKey: string
  baseUrl: string
  status: 0 | 1
  createdAt: string
  updatedAt: string
}

// Mock 数据（5条）
const mockProviders: Provider[] = [
  {
    id: 1,
    name: 'OpenAI 官方',
    type: 'OpenAI',
    apiKey: 'sk-***********************',
    baseUrl: 'https://api.openai.com/v1',
    status: 1,
    createdAt: '2024-01-15 09:30:00',
    updatedAt: '2024-01-15 09:30:00'
  },
  {
    id: 2,
    name: 'Claude Anthropic',
    type: 'Claude',
    apiKey: 'sk-ant-********************',
    baseUrl: 'https://api.anthropic.com',
    status: 1,
    createdAt: '2024-01-18 14:20:00',
    updatedAt: '2024-01-18 14:20:00'
  },
  {
    id: 3,
    name: '本地 Ollama',
    type: 'Ollama',
    apiKey: '',
    baseUrl: 'http://localhost:11434',
    status: 1,
    createdAt: '2024-02-01 10:00:00',
    updatedAt: '2024-02-01 10:00:00'
  },
  {
    id: 4,
    name: 'Google Gemini',
    type: 'Gemini',
    apiKey: 'AIzaSy*******************',
    baseUrl: 'https://generativelanguage.googleapis.com',
    status: 0,
    createdAt: '2024-02-10 16:45:00',
    updatedAt: '2024-02-10 16:45:00'
  },
  {
    id: 5,
    name: 'Azure OpenAI',
    type: 'OpenAI',
    apiKey: '***********************',
    baseUrl: 'https://my-resource.openai.azure.com',
    status: 1,
    createdAt: '2024-03-05 08:15:00',
    updatedAt: '2024-03-05 08:15:00'
  }
]

// 表格列配置
const columns: TableColumn<Provider>[] = [
  { prop: 'name', label: '名称', minWidth: 180 },
  { prop: 'type', label: '类型', width: 120, slot: 'type' },
  { prop: 'baseUrl', label: 'Base URL', minWidth: 280 },
  { prop: 'status', label: '状态', width: 100, slot: 'status', align: 'center' },
  { prop: 'createdAt', label: '创建时间', width: 180, slot: 'createdAt' },
  { prop: 'action', label: '操作', width: 150, slot: 'action', fixed: 'right', align: 'center' }
]

// 表单校验规则
const formRules = {
  name: [
    { required: true, message: '请输入提供商名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择类型', trigger: 'change' }
  ],
  baseUrl: [
    { required: true, message: '请输入 Base URL', trigger: 'blur' },
    { type: 'url', message: '请输入正确的 URL', trigger: 'blur' }
  ]
}

// 获取类型对应的标签样式
const getTypeTagType = (type: string) => {
  const typeMap: Record<string, string> = {
    'OpenAI': 'success',
    'Claude': 'warning',
    'Gemini': 'primary',
    'Ollama': 'info'
  }
  return typeMap[type] || 'info'
}

// 格式化日期时间
const formatDateTime = (datetime: string) => {
  if (!datetime) return '-'
  const date = new Date(datetime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).replace(/\//g, '-')
}

// 模拟 API 请求
const fetchProviderList = (params: any): Promise<PageResult<Provider>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const { pageNum = 1, pageSize = 10 } = params
      const start = (pageNum - 1) * pageSize
      const end = start + pageSize
      const list = mockProviders.slice(start, end)

      resolve({
        list,
        total: mockProviders.length,
        pageNum,
        pageSize
      })
    }, 500) // 模拟网络延迟
  })
}

// 引用
const tableRef = ref<any>(null)
const dialogRef = ref<any>(null)
const { confirmDelete } = useConfirm()

// 弹窗标题
const dialogTitle = ref('新增提供商')

// 编辑模式时更新标题
const updateDialogTitle = (isEdit: boolean) => {
  dialogTitle.value = isEdit ? '编辑提供商' : '新增提供商'
}

// 新增
const handleAdd = () => {
  updateDialogTitle(false)
  dialogRef.value?.open()
}

// 编辑
const handleEdit = (row: Provider) => {
  updateDialogTitle(true)
  dialogRef.value?.open({ ...row })
}

// 删除
const handleDelete = async (row: Provider) => {
  try {
    await confirmDelete(
      `确定要删除提供商 "${row.name}" 吗？删除后该提供商下的所有模型将无法使用。`,
      () => {
        // 模拟删除 API
        return new Promise<void>((resolve) => {
          setTimeout(() => {
            const index = mockProviders.findIndex(p => p.id === row.id)
            if (index > -1) {
              mockProviders.splice(index, 1)
            }
            resolve()
          }, 500)
        })
      },
      {
        title: '删除提供商',
        successMessage: '删除成功'
      }
    )
    // 刷新列表
    tableRef.value?.refresh(true)
  } catch {
    // 用户取消或删除失败
  }
}

// 提交表单
const handleSubmit = async (formData: Partial<Provider>, isEdit: boolean) => {
  try {
    dialogRef.value?.setLoading(true)

    // 模拟提交 API
    await new Promise<void>((resolve) => {
      setTimeout(() => {
        if (isEdit && formData.id) {
          // 编辑
          const index = mockProviders.findIndex(p => p.id === formData.id)
          if (index > -1) {
            mockProviders[index] = {
              ...mockProviders[index],
              ...formData,
              updatedAt: new Date().toLocaleString('zh-CN').replace(/\//g, '-')
            } as Provider
          }
        } else {
          // 新增
          const newProvider: Provider = {
            ...formData,
            id: mockProviders.length + 1,
            status: 1,
            createdAt: new Date().toLocaleString('zh-CN').replace(/\//g, '-'),
            updatedAt: new Date().toLocaleString('zh-CN').replace(/\//g, '-')
          } as Provider
          mockProviders.unshift(newProvider)
        }
        resolve()
      }, 800)
    })

    notifySuccess(isEdit ? '编辑成功' : '新增成功')
    dialogRef.value?.close()
    tableRef.value?.refresh(true)
  } catch (error) {
    notifyError('操作失败', '请稍后重试')
  } finally {
    dialogRef.value?.setLoading(false)
  }
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
  gap: 8px;
}

/* 响应式 */
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
