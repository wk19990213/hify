<template>
  <div v-if="options.length === 0" class="text-gray">
    暂无可用 MCP 服务，请先在
    <router-link to="/mcp-servers">MCP 管理</router-link>
    中添加服务器
  </div>
  <el-checkbox-group
    v-else
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-for="opt in options" :key="opt.serverId" style="margin-bottom: 4px">
      <el-checkbox :label="opt.serverId" :value="opt.serverId" :disabled="opt.hasError">
        <span style="font-weight:500" :style="opt.hasError ? 'color: var(--el-color-danger)' : ''">{{ opt.serverName }}</span>
        <span class="tool-desc" :style="opt.hasError ? 'color: var(--el-color-danger)' : ''">
          {{ opt.hasError ? opt.description : `${opt.toolCount} 个工具 — ${opt.description}` }}
        </span>
      </el-checkbox>
    </div>
  </el-checkbox-group>
</template>

<script setup lang="ts">
export interface McpServerOption {
  serverId: number
  serverName: string
  toolCount: number
  description: string
  hasError?: boolean
}

defineProps<{
  modelValue: number[]
  options: McpServerOption[]
}>()

defineEmits<{
  'update:modelValue': [value: number[]]
}>()
</script>

<style scoped>
.text-gray {
  color: var(--text-secondary);
}
.tool-desc {
  color: var(--text-secondary);
  font-size: 12px;
  margin-left: 8px;
}
</style>
