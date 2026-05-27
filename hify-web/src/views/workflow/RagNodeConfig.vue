<template>
  <div>
    <el-form-item label="知识库">
      <el-input :model-value="config.kbId" placeholder="选择要检索的知识库" @update:model-value="updateField('kbId', $event)" />
    </el-form-item>
    <el-form-item label="搜什么">
      <el-input :model-value="config.query" placeholder="比如：{{input.user_message}}" @update:model-value="updateField('query', $event)" />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ config: Record<string, any> }>()
const emit = defineEmits<{ 'update:config': [value: Record<string, any>] }>()

function updateField(key: string, value: any) {
  emit('update:config', { ...props.config, [key]: value })
}
</script>
