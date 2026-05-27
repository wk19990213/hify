<template>
  <div class="var-hints">
    <div class="var-hints-title">可用变量</div>
    <div class="var-group">
      <div class="var-group-title">系统变量（工作流触发时自动注入）</div>
      <div class="var-hints-list">
        <span class="var-tag sys-var">
          <code v-pre>{{input.user_message}}</code>
          <span class="var-desc">用户发送的消息内容</span>
        </span>
        <span class="var-tag sys-var">
          <code v-pre>{{input.session_id}}</code>
          <span class="var-desc">当前对话的会话 ID</span>
        </span>
      </div>
    </div>
    <div class="var-group" v-if="upstreamNodes.length">
      <div class="var-group-title">上游节点输出</div>
      <div class="var-hints-list">
        <span v-for="v in upstreamVars" :key="v.key + '.' + (v.field || '')" class="var-tag">
          <code>{{ varRef(v) }}</code>
          <span class="var-desc">{{ v.desc }}</span>
        </span>
      </div>
    </div>
    <div class="var-group" v-else>
      <div class="var-group-title">上游节点输出</div>
      <div class="var-hints-empty" v-if="!hasUpstream">
        当前节点是第一个节点，没有上游节点。连线到当前节点的节点会自动出现在这里。
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { NodeItem, EdgeItem } from '@/api/workflow'

const props = defineProps<{
  nodes: NodeItem[]
  edges: EdgeItem[]
  selectedNodeIndex: number
}>()

interface VarHint { key: string; field?: string; desc: string }

/** 输出字段 Schema 定义（JSON Schema 风格，按节点类型声明输出字段） */
interface OutputFieldDef { field: string; desc: string }

const NODE_OUTPUT_FIELDS: Record<string, OutputFieldDef[]> = {
  llm: [{ field: 'content', desc: '文本回复' }],
  http: [
    { field: 'body', desc: '返回的响应体' },
    { field: 'status', desc: '返回的状态码' },
  ],
  condition: [{ field: 'result', desc: '判断结果（true/false）' }],
  rag: [{ field: 'sources', desc: '检索到的文档' }],
}

const upstreamNodes = computed(() => {
  const upstreamIndices = props.edges
    .filter(e => e.targetNodeIndex === props.selectedNodeIndex)
    .map(e => e.sourceNodeIndex)
  return [...new Set(upstreamIndices)].map(i => props.nodes[i]).filter(Boolean)
})

const hasUpstream = computed(() =>
  props.edges.some(e => e.targetNodeIndex === props.selectedNodeIndex)
)

/** 从节点 config.outputSchema 读取自定义输出字段（JSON Schema 驱动，替代正则解析） */
function readOutputSchema(configJson?: string): OutputFieldDef[] {
  if (!configJson) return []
  try {
    const cfg = JSON.parse(configJson)
    const schema = cfg.outputSchema
    if (!Array.isArray(schema)) return []
    return schema
      .map((item: any) => {
        if (typeof item === 'string') return { field: item, desc: ' 输出的 ' + item + ' 字段' }
        if (item && typeof item.field === 'string') return { field: item.field, desc: item.desc || ' 输出的 ' + item.field + ' 字段' }
        return null
      })
      .filter(Boolean) as OutputFieldDef[]
  } catch { return [] }
}

const upstreamVars = computed((): VarHint[] => {
  const vars: VarHint[] = []
  for (const node of upstreamNodes.value) {
    if (!node.name) continue
    const name = node.name
    // 标准输出字段（按节点类型的 JSON Schema 定义）
    const standardFields = NODE_OUTPUT_FIELDS[node.type] || []
    for (const f of standardFields) {
      vars.push({ key: name, field: f.field, desc: name + ' 的' + f.desc })
    }
    // LLM 节点：额外从 outputSchema 读取自定义输出字段
    if (node.type === 'llm') {
      const customFields = readOutputSchema(node.configJson)
      for (const f of customFields) {
        vars.push({ key: name, field: f.field, desc: name + f.desc })
      }
    }
  }
  return vars
})

function varRef(v: VarHint): string {
  return '{{' + v.key + (v.field ? '.' + v.field : '') + '}}'
}
</script>

<style scoped>
.var-hints {
  margin-top: 12px;
  padding: 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}
.var-hints-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 10px;
}
.var-group {
  margin-bottom: 10px;
}
.var-group:last-child {
  margin-bottom: 0;
}
.var-group-title {
  font-size: 11px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.var-hints-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.var-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #fff;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  font-size: 12px;
  cursor: default;
}
.var-tag.sys-var {
  border-color: var(--el-color-success-light-3);
  background: var(--el-color-success-light-9);
}
.var-tag code {
  color: var(--el-color-primary);
  font-weight: 600;
}
.var-tag.sys-var code {
  color: var(--el-color-success);
}
.var-desc {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}
.var-hints-empty {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  font-style: italic;
}
</style>
