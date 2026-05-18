<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    class="hify-form-dialog"
    destroy-on-close
    @close="handleClose"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-width="labelWidth"
      :label-position="labelPosition"
      class="hify-form"
    >
      <slot :form="formData" :loading="loading" />
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button
          :disabled="loading"
          @click="handleCancel"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSubmit"
        >
          {{ submitText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts" generic="T extends Record<string, any> = Record<string, any>">
import { ref, computed, nextTick } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

// Props
interface Props {
  title?: string
  width?: string | number
  labelWidth?: string | number
  labelPosition?: 'left' | 'right' | 'top'
  rules?: FormRules
  submitText?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '表单',
  width: '560px',
  labelWidth: '100px',
  labelPosition: 'right',
  rules: () => ({}),
  submitText: '确定'
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [data: T, isEdit: boolean]
  close: []
  'closed': []
}>()

// 内部状态
const visible = defineModel<boolean>({ default: false })
const loading = ref(false)
const isEdit = ref(false)
const originalId = ref<string | number | null>(null)

// 表单
const formRef = ref<FormInstance>()
const formData = ref<Partial<T>>({})

// 表单默认值（用于重置）
const defaultFormData = ref<Partial<T>>({})

// 打开弹窗（暴露给外部）
const open = (data?: T) => {
  isEdit.value = !!data
  // 支持多种 id 字段命名
  const idValue = data && ('id' in data ? data.id : 'Id' in data ? (data as any).Id : undefined)
  originalId.value = idValue != null ? String(idValue) : null

  if (data) {
    formData.value = { ...data }
  } else {
    formData.value = { ...defaultFormData.value }
  }

  visible.value = true

  // 等待 DOM 更新后清除校验
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

// 关闭弹窗
const close = () => {
  visible.value = false
}

// 设置表单默认值
const setDefaultData = (data: T) => {
  defaultFormData.value = { ...data }
}

// 取消
const handleCancel = () => {
  emit('close')
  close()
}

// 关闭时
const handleClose = () => {
  emit('close')
}

// 关闭后重置
const handleClosed = () => {
  emit('closed')
  // 延迟重置，确保关闭动画完成
  setTimeout(() => {
    formData.value = { ...defaultFormData.value }
    isEdit.value = false
    originalId.value = null
    loading.value = false
  }, 200)
}

// 提交
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    loading.value = true
    // 触发 submit 事件，由父组件处理 API 调用
    emit('submit', formData.value as T, isEdit.value)
  } catch {
    // 校验失败
  }
}

// 设置 loading 状态（供父组件调用）
const setLoading = (val: boolean) => {
  loading.value = val
}

// 获取表单数据
const getFormData = () => ({ ...formData.value })

// 设置表单字段
const setFieldValue = (field: keyof T, value: any) => {
  formData.value[field] = value
}

// 暴露方法
defineExpose({
  open,
  close,
  setDefaultData,
  setLoading,
  getFormData,
  setFieldValue,
  formData,
  isEdit,
  originalId
})
</script>

<style scoped>
.hify-form-dialog :deep(.el-dialog) {
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-xl);
}

.hify-form-dialog :deep(.el-dialog__header) {
  padding: var(--space-5) var(--space-6);
  border-bottom: 1px solid var(--border-light);
  margin: 0;
}

.hify-form-dialog :deep(.el-dialog__title) {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

.hify-form-dialog :deep(.el-dialog__headerbtn) {
  top: var(--space-5);
  right: var(--space-6);
}

.hify-form-dialog :deep(.el-dialog__body) {
  padding: var(--space-6);
}

.hify-form-dialog :deep(.el-dialog__footer) {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--border-light);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
}

.hify-form :deep(.el-form-item__label) {
  font-weight: var(--font-medium);
  color: var(--text-secondary);
}

.hify-form :deep(.el-input__wrapper),
.hify-form :deep(.el-textarea__inner) {
  border-radius: var(--radius-md);
}

.hify-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--primary-500) inset, 0 0 0 3px var(--primary-100);
}
</style>
