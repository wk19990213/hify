<template>
  <div :class="['workflow-node', `node-${data.type}`, { selected: data.selected }]">
    <Handle type="target" :position="Position.Top" />
    <div class="node-header">
      <span class="node-icon">{{ icon }}</span>
      <span class="node-label">{{ data.label }}</span>
    </div>
    <div class="node-body">
      <el-tag :type="tagType" size="small">{{ typeLabel }}</el-tag>
    </div>
    <Handle type="source" :position="Position.Bottom" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import type { NodeProps } from '@vue-flow/core'

interface NodeData {
  label: string
  type: 'llm' | 'condition' | 'rag' | 'http'
  selected: boolean
}

const props = defineProps<NodeProps<NodeData>>()

const icon = computed(() => {
  const map: Record<string, string> = { llm: '🤖', condition: '◇', rag: '📚', http: '🌐' }
  return map[props.data.type] || '●'
})

const tagType = computed(() => {
  const map: Record<string, string> = { llm: '', condition: 'warning', rag: 'success', http: 'info' }
  return map[props.data.type] || 'info'
})

const typeLabel = computed(() => props.data.type.toUpperCase())
</script>

<style scoped>
.workflow-node {
  background: #fff;
  border: 2px solid #d9d9d9;
  border-radius: 8px;
  min-width: 160px;
  font-size: 13px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s, border-color 0.2s;
  cursor: pointer;
}
.workflow-node:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.workflow-node.selected {
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.3);
}

.node-header {
  padding: 8px 12px 4px;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 6px 6px 0 0;
}
.node-icon { font-size: 16px; }
.node-label { font-weight: 600; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 140px; }

.node-body {
  padding: 0 12px 8px;
}

/* llm - purple */
.node-llm { border-color: #a78bfa; }
.node-llm .node-header { background: #f5f3ff; }
.node-llm.selected { border-color: #7c3aed; }

/* condition - orange */
.node-condition { border-color: #fb923c; }
.node-condition .node-header { background: #fff7ed; }
.node-condition.selected { border-color: #ea580c; }

/* rag - green */
.node-rag { border-color: #4ade80; }
.node-rag .node-header { background: #f0fdf4; }
.node-rag.selected { border-color: #16a34a; }

/* http - blue */
.node-http { border-color: #60a5fa; }
.node-http .node-header { background: #eff6ff; }
.node-http.selected { border-color: #2563eb; }
</style>
