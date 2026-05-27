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

const upstreamNodes = computed(() => {
  const upstreamIndices = props.edges
    .filter(e => e.targetNodeIndex === props.selectedNodeIndex)
    .map(e => e.sourceNodeIndex)
  return [...new Set(upstreamIndices)].map(i => props.nodes[i]).filter(Boolean)
})

const hasUpstream = computed(() =>
  props.edges.some(e => e.targetNodeIndex === props.selectedNodeIndex)
)

const upstreamVars = computed((): VarHint[] => {
  const vars: VarHint[] = []
  for (const node of upstreamNodes.value) {
    if (!node.name) continue
    const name = node.name
    if (node.type === 'llm') {
      vars.push({ key: name, field: 'content', desc: name + ' 的文本回复' })
      const fields = parseExpectedFields(node.configJson)
      for (const f of fields) {
        vars.push({ key: name, field: f, desc: name + ' 输出的 ' + f + ' 字段' })
      }
    } else if (node.type === 'http') {
      vars.push({ key: name, field: 'body', desc: name + ' 返回的响应体' })
      vars.push({ key: name, field: 'status', desc: name + ' 返回的状态码' })
    } else if (node.type === 'condition') {
      vars.push({ key: name, field: 'result', desc: name + ' 的判断结果（true/false）' })
    } else if (node.type === 'rag') {
      vars.push({ key: name, field: 'sources', desc: name + ' 检索到的文档' })
    }
  }
  return vars
})

function parseExpectedFields(configJson?: string): string[] {
  if (!configJson) return []
  try {
    const cfg = JSON.parse(configJson)
    const prompt: string = cfg.prompt || ''
    const jsonMatch = prompt.match(/\{[^}]+\}/)
    if (!jsonMatch) return []
    const fields: string[] = []
    const fieldRegex = /"(\w+)"\s*:/g
    let m
    while ((m = fieldRegex.exec(jsonMatch[0])) !== null) {
      if (!['intent', 'result', 'content'].includes(m[1]) && !fields.includes(m[1])) {
        fields.push(m[1])
      }
    }
    return fields
  } catch { return [] }
}

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
