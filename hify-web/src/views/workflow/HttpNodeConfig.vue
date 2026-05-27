<template>
  <div>
    <el-form-item label="接口地址">
      <el-input :model-value="config.url" placeholder="https://api.example.com/orders" @update:model-value="updateField('url', $event)" />
    </el-form-item>
    <el-form-item label="请求方式">
      <el-select :model-value="config.method" @update:model-value="updateField('method', $event)">
        <el-option label="GET" value="GET" />
        <el-option label="POST" value="POST" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
    </el-form-item>
    <el-form-item label="请求体">
      <el-input :model-value="config.body" type="textarea" :rows="3" placeholder="POST/PUT 时的 JSON 数据" @update:model-value="updateField('body', $event)" />
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
