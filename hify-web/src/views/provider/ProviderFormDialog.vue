<template>
  <HifyFormDialog
    ref="dialogRef"
    :title="title"
    width="560px"
    :rules="formRules"
    @submit="(fd: any, ie: boolean) => $emit('submit', fd, ie)"
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
          :placeholder="form._apiKey ? '****（已设置，留空不修改）' : '请输入 API Key'"
          show-password
          clearable
        />
      </el-form-item>
    </template>
  </HifyFormDialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import HifyFormDialog from '@/components/HifyFormDialog.vue'

const emit = defineEmits<{
  submit: [formData: Record<string, any>, isEdit: boolean]
}>()

const dialogRef = ref<any>(null)
const title = ref('新增提供商')

const formRules = {
  name: [
    { required: true, message: '请输入提供商名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度 2 ~ 50 个字符', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
}

const open = (data?: Record<string, any>) => {
  title.value = data ? '编辑提供商' : '新增提供商'
  dialogRef.value?.open(data)
}

const close = () => dialogRef.value?.close()
const setLoading = (val: boolean) => dialogRef.value?.setLoading(val)

defineExpose({ open, close, setLoading })
</script>
