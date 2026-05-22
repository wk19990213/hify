<template>
  <div class="workflow-canvas">
    <VueFlow
      :nodes="flowNodes"
      :edges="flowEdges"
      :node-types="nodeTypes"
      :default-viewport="{ x: 0, y: 0, zoom: 1 }"
      :min-zoom="0.3"
      :max-zoom="2"
      :connection-line-style="{ stroke: '#9ca3af', strokeWidth: 1 }"
      @node-click="onNodeClick"
      @node-drag-stop="onNodeDragStop"
      @pane-click="() => emit('paneClick')"
      fit-view-on-init
    >
      <Background :gap="20" />
    </VueFlow>
  </div>
</template>

<script setup lang="ts">
import { computed, markRaw } from 'vue'
import { VueFlow, Position, MarkerType } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import type { NodeItem, EdgeItem } from '@/api/workflow'
import WorkflowNode from './WorkflowNode.vue'
import { autoLayout } from './autoLayout'

const props = defineProps<{
  nodes: NodeItem[]
  edges: EdgeItem[]
  selectedNodeIndex: number | null
}>()

const emit = defineEmits<{
  selectNode: [index: number]
  paneClick: []
  updatePosition: [index: number, x: number, y: number]
}>()

const nodeTypes = { custom: markRaw(WorkflowNode) }

const flowNodes = computed(() => {
  const layouts = autoLayout(
    props.nodes.map((n, i) => ({ index: i, positionX: n.positionX, positionY: n.positionY })),
    props.edges
  )

  return props.nodes.map((n, index) => {
    const pos = layouts.get(index) || { x: 0, y: 0 }
    const label = n.name || '未命名'
    return {
      id: `node-${index}`,
      type: 'custom',
      position: { x: pos.x, y: pos.y },
      sourcePosition: Position.Bottom,
      targetPosition: Position.Top,
      data: {
        label: label.length > 12 ? label.substring(0, 12) + '...' : label,
        type: n.type,
        selected: index === props.selectedNodeIndex
      },
      draggable: true,
    }
  })
})

function extractConditionExpr(node: NodeItem): string | null {
  if (!node.configJson) return null
  try {
    const cfg = JSON.parse(node.configJson)
    return cfg.expression || null
  } catch { return null }
}

function buildEdgeLabel(edge: EdgeItem): string | undefined {
  const srcNode = props.nodes[edge.sourceNodeIndex]
  if (!srcNode) return undefined
  if (edge.conditionExpr) return edge.conditionExpr

  if (srcNode.type === 'condition') {
    const expr = extractConditionExpr(srcNode) || '条件'
    if (edge.edgeType === 'true') return expr
    if (edge.edgeType === 'false') return '否则'
  }
  return undefined
}

const flowEdges = computed(() => {
  return props.edges.map((e, i) => {
    const edgeType = e.edgeType || 'normal'
    const isCondition = edgeType === 'true' || edgeType === 'false'
    const color = edgeType === 'true' ? '#22c55e'
      : edgeType === 'false' ? '#ef4444'
      : edgeType === 'error' ? '#f97316'
      : '#9ca3af'

    return {
      id: `edge-${i}`,
      source: `node-${e.sourceNodeIndex}`,
      target: `node-${e.targetNodeIndex}`,
      type: 'smoothstep',
      style: {
        stroke: color,
        strokeWidth: isCondition ? 2 : 1.5,
      },
      animated: isCondition,
      label: buildEdgeLabel(e),
      labelStyle: { fill: color, fontWeight: '700', fontSize: '13px' },
      labelBgStyle: { fill: '#ffffff', fillOpacity: 0.85 },
      labelBgPadding: [6, 3] as [number, number],
      labelBgBorderRadius: 4,
      markerEnd: { type: MarkerType.ArrowClosed, color, width: 18, height: 18 },
    }
  })
})

function onNodeClick({ node }: { node: { id: string } }) {
  const idx = parseInt(node.id.replace('node-', ''))
  emit('selectNode', idx)
}

function onNodeDragStop({ node }: { node: { id: string; position: { x: number; y: number } } }) {
  const idx = parseInt(node.id.replace('node-', ''))
  emit('updatePosition', idx, node.position.x, node.position.y)
}
</script>

<style scoped>
.workflow-canvas {
  width: 100%;
  height: 460px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  background: #fafbfc;
}
</style>
