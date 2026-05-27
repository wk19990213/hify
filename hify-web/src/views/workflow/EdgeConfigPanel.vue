<template>
  <el-card style="margin-top: 16px;">
    <template #header>连线配置</template>
    <div class="edge-list">
      <div v-for="(edge, idx) in outEdges" :key="idx" class="edge-item">
        <el-select :model-value="edge.edgeType" size="small" style="width: 140px;" @update:model-value="updateEdge(idx, 'edgeType', $event)">
          <el-option label="默认" value="normal" />
          <el-option label="条件成立" value="true" />
          <el-option label="条件不成立" value="false" />
          <el-option label="异常" value="error" />
        </el-select>
        <span class="arrow">&rarr;</span>
        <el-select :model-value="edge.targetNodeIndex" size="small" style="width: 160px;" @update:model-value="updateEdge(idx, 'targetNodeIndex', $event)">
          <el-option
            v-for="(n, i) in nodes"
            :key="i"
            :disabled="i === selectedNodeIndex"
            :label="(n.name || '节点 ' + (i + 1)) + '  [' + typeLabel(n.type) + ']'"
            :value="i"
          />
        </el-select>
        <el-button size="small" type="danger" circle :icon="Delete" @click="handleRemoveEdge(idx)" />
      </div>
      <el-button size="small" :icon="Plus" @click="handleAddEdge">添加连线</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { EdgeItem, NodeItem } from '@/api/workflow'

const props = defineProps<{
  edges: EdgeItem[]
  nodes: NodeItem[]
  selectedNodeIndex: number
}>()

const emit = defineEmits<{
  'update:edges': [value: EdgeItem[]]
}>()

function typeLabel(type: string): string {
  const map: Record<string, string> = { llm: 'AI 对话', condition: '条件判断', rag: '知识库检索', http: 'HTTP 请求' }
  return map[type] || type
}

const outEdges = computed(() =>
  props.edges.filter(e => e.sourceNodeIndex === props.selectedNodeIndex)
)

function handleAddEdge() {
  const newEdges = [...props.edges, {
    sourceNodeIndex: props.selectedNodeIndex,
    targetNodeIndex: 0,
    edgeType: 'normal' as const
  }]
  emit('update:edges', newEdges)
}

function handleRemoveEdge(idx: number) {
  const edgeToRemove = outEdges.value[idx]
  const globalIdx = props.edges.indexOf(edgeToRemove)
  if (globalIdx >= 0) {
    const newEdges = [...props.edges]
    newEdges.splice(globalIdx, 1)
    emit('update:edges', newEdges)
  }
}

function updateEdge(idx: number, field: string, value: any) {
  const edgeToUpdate = outEdges.value[idx]
  const globalIdx = props.edges.indexOf(edgeToUpdate)
  if (globalIdx >= 0) {
    const newEdges = [...props.edges]
    newEdges[globalIdx] = { ...newEdges[globalIdx], [field]: value }
    emit('update:edges', newEdges)
  }
}
</script>

<style scoped>
.edge-list { display: flex; flex-direction: column; gap: 8px; }
.edge-item { display: flex; gap: 8px; align-items: center; }
.arrow { font-size: 14px; color: var(--el-text-color-secondary); flex-shrink: 0; }
</style>
