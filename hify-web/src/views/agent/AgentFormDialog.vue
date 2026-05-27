<template>
  <HifyFormDialog
    ref="dialogRef"
    :title="title"
    width="600px"
    :rules="formRules"
    @submit="(fd: any, ie: boolean) => $emit('submit', fd, ie)"
  >
    <template #default="{ form }">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入 Agent 名称" clearable />
      </el-form-item>

      <el-form-item label="编码" prop="code">
        <el-input v-model="form.code" placeholder="唯一编码，如 customer-service" clearable />
      </el-form-item>

      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="描述这个 Agent 的用途" clearable />
      </el-form-item>

      <el-form-item label="模型配置" prop="modelConfigId" required>
        <el-select v-model="form.modelConfigId" placeholder="请选择模型" style="width: 100%">
          <el-option v-for="item in modelOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="知识库">
        <el-select v-model="form.kbId" placeholder="请选择知识库（可选）" style="width: 100%" clearable>
          <el-option v-for="item in kbOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="工作流">
        <el-select v-model="form.workflowId" placeholder="请选择工作流（可选）" style="width: 100%" clearable>
          <el-option v-for="item in workflowOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>

      <el-form-item label="MCP 服务绑定">
        <McpServerSelector v-model="form._selectedMcpServers" :options="mcpServerOptions" />
      </el-form-item>

      <el-form-item label="系统提示词">
        <el-input v-model="form.systemPrompt" type="textarea" :rows="4" placeholder="设置系统提示词，定义 Agent 的角色和行为" clearable />
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
</template>

<script setup lang="ts">
import { ref } from 'vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'
import McpServerSelector from './McpServerSelector.vue'
import type { McpServerOption } from './McpServerSelector.vue'

interface SelectOption {
  id: number
  name: string
}

withDefaults(defineProps<{
  modelOptions?: SelectOption[]
  kbOptions?: SelectOption[]
  workflowOptions?: SelectOption[]
  mcpServerOptions?: McpServerOption[]
}>(), {
  modelOptions: () => [],
  kbOptions: () => [],
  workflowOptions: () => [],
  mcpServerOptions: () => [],
})

const emit = defineEmits<{
  submit: [formData: Record<string, any>, isEdit: boolean]
}>()

const dialogRef = ref<any>(null)
const title = ref('新增 Agent')

const formRules = {
  name: [
    { required: true, message: '请输入 Agent 名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度 2 ~ 50 个字符', trigger: 'blur' },
  ],
  code: [
    { min: 2, max: 64, message: '长度 2 ~ 64 个字符', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只允许小写字母、数字和连字符', trigger: 'blur' },
  ],
  modelConfigId: [{ required: true, message: '必须选择模型配置', trigger: 'change' }],
}

const open = (data?: Record<string, any>) => {
  title.value = data ? '编辑 Agent' : '新增 Agent'
  dialogRef.value?.open(data)
}

const close = () => dialogRef.value?.close()
const setLoading = (val: boolean) => dialogRef.value?.setLoading(val)
const setDefaultData = (data: Record<string, any>) => dialogRef.value?.setDefaultData(data)

defineExpose({ open, close, setLoading, setDefaultData })
</script>
