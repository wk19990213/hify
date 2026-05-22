<template>
  <div class="workflow-editor">
    <div class="editor-header">
      <el-button @click="$router.push('/workflows')">返回列表</el-button>
      <h2>{{ isEdit ? '编辑工作流' : '新建工作流' }}</h2>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>

    <el-card style="margin-bottom: 16px;">
      <template #header>基本信息</template>
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="给工作流起个名字" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="简单描述一下这个工作流做什么" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
    </el-card>

    <WorkflowCanvas
      :nodes="form.nodes"
      :edges="form.edges"
      :selected-node-index="selectedNodeIndex"
      @select-node="selectedNodeIndex = $event"
      @pane-click="selectedNodeIndex = null"
      @update-position="onCanvasUpdatePosition"
      style="margin-bottom: 16px;"
    />

    <div class="editor-body">
      <div class="node-list-panel">
        <div class="panel-header">
          <span>节点列表</span>
          <el-button size="small" :icon="Plus" @click="addNode">添加节点</el-button>
        </div>
        <div
          v-for="(node, idx) in form.nodes"
          :key="idx"
          :class="['node-card', { active: selectedNodeIndex === idx }]"
          @click="selectedNodeIndex = idx"
        >
          <span class="node-type-icon">{{ typeIcon(node.type) }}</span>
          <span class="node-name">{{ node.name || '未命名' }}</span>
          <span class="node-type-label">{{ typeLabel(node.type) }}</span>
          <el-button size="small" type="danger" circle :icon="Delete" @click.stop="removeNode(idx)" />
        </div>
      </div>

      <div class="node-config-panel" v-if="selectedNodeIndex !== null">
        <el-card>
          <template #header>节点配置</template>
          <el-form :model="currentNode" label-width="100px">
            <el-form-item label="名称">
              <el-input v-model="currentNode.name" placeholder="给这个步骤起个名字，如「查订单」" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="currentNode.type">
                <el-option
                  v-for="opt in typeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <div class="type-hint">{{ typeHint(currentNode.type) }}</div>
            </el-form-item>

            <template v-if="currentNode.type === 'llm'">
              <el-form-item label="做什么">
                <el-input v-model="llmPrompt" type="textarea" :rows="5"
                  :placeholder="llmPlaceholder" />
                <div class="type-hint">告诉 AI 要做什么。可以用 <code v-pre>{{节点名.字段}}</code> 引用前面步骤的结果</div>
              </el-form-item>
              <el-form-item label="最多重试">
                <el-input-number v-model="llmMaxRetries" :min="0" :max="5" />
              </el-form-item>
              <el-form-item label="工具调用">
                <el-switch v-model="llmToolsEnabled" size="small" />
                <span class="type-hint">开启后 LLM 可自主判断是否调用 Agent 绑定的全部 MCP 工具</span>
              </el-form-item>
            </template>

            <template v-if="currentNode.type === 'rag'">
              <el-form-item label="知识库">
                <el-input v-model="ragKbId" placeholder="选择要检索的知识库" />
              </el-form-item>
              <el-form-item label="搜什么">
                <el-input v-model="ragQuery" placeholder="比如：{{input.user_message}}" />
              </el-form-item>
            </template>

            <template v-if="currentNode.type === 'http'">
              <el-form-item label="接口地址">
                <el-input v-model="httpUrl" placeholder="https://api.example.com/orders" />
              </el-form-item>
              <el-form-item label="请求方式">
                <el-select v-model="httpMethod">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                </el-select>
              </el-form-item>
              <el-form-item label="请求体">
                <el-input v-model="httpBody" type="textarea" :rows="3" placeholder="POST/PUT 时的 JSON 数据" />
              </el-form-item>
            </template>

            <template v-if="currentNode.type === 'condition'">
              <el-form-item label="条件">
                <el-input v-model="conditionExpression" placeholder="比如：{{查订单.hasOrderId}} == true" />
                <div class="type-hint">
                  支持 <code>==</code>（等于）和 <code>!=</code>（不等于）。结果决定走「条件成立」还是「条件不成立」分支
                </div>
              </el-form-item>
            </template>
          </el-form>

          <!-- 可用变量提示 -->
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
              <div class="var-hints-empty" v-if="selectedNodeIndex !== null && !hasUpstreamEdge()">
                当前节点是第一个节点，没有上游节点。连线到当前节点的节点会自动出现在这里。
              </div>
            </div>
          </div>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>连线配置</template>
          <div class="edge-list">
            <div v-for="(edge, idx) in outEdges" :key="idx" class="edge-item">
              <el-select v-model="edge.edgeType" size="small" style="width: 140px;">
                <el-option label="默认" value="normal" />
                <el-option label="条件成立" value="true" />
                <el-option label="条件不成立" value="false" />
                <el-option label="异常" value="error" />
              </el-select>
              <span class="arrow">→</span>
              <el-select v-model="edge.targetNodeIndex" size="small" style="width: 160px;">
                <el-option
                  v-for="(n, i) in form.nodes"
                  :key="i"
                  :disabled="i === selectedNodeIndex"
                  :label="(n.name || '节点 ' + (i + 1)) + '  [' + typeLabel(n.type) + ']'"
                  :value="i"
                />
              </el-select>
              <el-button size="small" type="danger" circle :icon="Delete" @click="removeEdge(idx)" />
            </div>
            <el-button size="small" :icon="Plus" @click="addEdge">添加连线</el-button>
          </div>
        </el-card>
      </div>

      <div v-else class="empty-hint">点击左侧节点进行配置</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import WorkflowCanvas from './components/WorkflowCanvas.vue'
import {
  createWorkflow, getWorkflowDetail, updateWorkflow,
  type NodeItem, type EdgeItem
} from '@/api/workflow'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = ref({ name: '', description: '', status: 1, nodes: [] as NodeItem[], edges: [] as EdgeItem[] })
const selectedNodeIndex = ref<number | null>(null)

const typeOptions = [
  { value: 'llm', label: 'AI 对话 — 调用大模型生成内容' },
  { value: 'condition', label: '条件判断 — 根据条件走不同分支' },
  { value: 'rag', label: '知识库检索 — 从知识库查找信息' },
  { value: 'http', label: 'HTTP 请求 — 调用外部接口' },
]

function typeLabel(type: string): string {
  const map: Record<string, string> = { llm: 'AI 对话', condition: '条件判断', rag: '知识库检索', http: 'HTTP 请求' }
  return map[type] || type
}

function typeIcon(type: string): string {
  const map: Record<string, string> = { llm: '🤖', condition: '◇', rag: '📚', http: '🌐' }
  return map[type] || '●'
}

function typeHint(type: string): string {
  const map: Record<string, string> = {
    llm: '让 AI 帮你做一件事，比如分析意图、提取信息、生成回复',
    condition: '判断一个条件是否成立，走不同的后续分支',
    rag: '从知识库中检索相关内容',
    http: '调用一个外部 API，获取数据或触发操作',
  }
  return map[type] || ''
}

const llmPlaceholder = `告诉 AI 要做什么，比如：

你是一个客服助手。分析用户消息，提取意图和订单号。
仅返回一行 JSON：
{"intent": "查订单|退款|其他", "orderId": "订单号或null", "hasOrderId": true或false}

用户消息：{{input.user_message}}`

const currentNode = computed(() => {
  if (selectedNodeIndex.value === null || !form.value.nodes[selectedNodeIndex.value]) {
    return { name: '', type: 'llm', configJson: '{}' } as NodeItem
  }
  return form.value.nodes[selectedNodeIndex.value]
})

function parseConfig() {
  if (!currentNode.value.configJson) return {}
  try { return JSON.parse(currentNode.value.configJson) } catch { return {} }
}

function updateConfig(val: any) {
  if (selectedNodeIndex.value !== null) {
    form.value.nodes[selectedNodeIndex.value].configJson = JSON.stringify(val)
  }
}

/** 为 configJson 中的单个字段创建双向绑定 computed */
function configField<T>(key: string, defaultValue: T) {
  return computed<T>({
    get: () => {
      const cfg = parseConfig()
      return (cfg[key] !== undefined ? cfg[key] : defaultValue) as T
    },
    set: (val: T) => {
      const cfg = parseConfig()
      cfg[key] = val
      updateConfig(cfg)
    }
  })
}

const llmPrompt = configField<string>('prompt', '')
const llmMaxRetries = configField<number>('maxRetries', 0)
const ragKbId = configField<string>('kbId', '')
const ragQuery = configField<string>('query', '')
const httpUrl = configField<string>('url', '')
const httpMethod = configField<string>('method', 'GET')
const httpBody = configField<string>('body', '')
const conditionExpression = configField<string>('expression', '')

// 工具调用开关
const llmToolsEnabled = configField<boolean>('toolsEnabled', false)

const outEdges = computed(() => {
  if (selectedNodeIndex.value === null) return []
  return form.value.edges.filter(e => e.sourceNodeIndex === selectedNodeIndex.value)
})

// 当前节点的上游节点列表（通过连线直接连接到当前节点的节点）
const upstreamNodes = computed(() => {
  if (selectedNodeIndex.value === null) return []
  const upstreamIndices = form.value.edges
    .filter(e => e.targetNodeIndex === selectedNodeIndex.value)
    .map(e => e.sourceNodeIndex)
  return [...new Set(upstreamIndices)].map(i => form.value.nodes[i]).filter(Boolean)
})

interface VarHint { key: string; field?: string; desc: string }

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

function hasUpstreamEdge(): boolean {
  if (selectedNodeIndex.value === null) return false
  return form.value.edges.some(e => e.targetNodeIndex === selectedNodeIndex.value)
}

// 从 LLM prompt 中尝试解析预期的 JSON 输出字段
function parseExpectedFields(configJson?: string): string[] {
  if (!configJson) return []
  try {
    const cfg = JSON.parse(configJson)
    const prompt: string = cfg.prompt || ''
    // 匹配 JSON 示例中的字段名
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

function addNode() {
  form.value.nodes.push({ name: '新节点', type: 'llm', configJson: '{}', positionX: 0, positionY: 0 })
}

function removeNode(idx: number) {
  form.value.nodes.splice(idx, 1)
  form.value.edges = form.value.edges.filter(e => e.sourceNodeIndex !== idx && e.targetNodeIndex !== idx)
  form.value.edges.forEach(e => {
    if (e.sourceNodeIndex > idx) e.sourceNodeIndex--
    if (e.targetNodeIndex > idx) e.targetNodeIndex--
  })
  if (selectedNodeIndex.value === idx) selectedNodeIndex.value = null
}

function addEdge() {
  if (selectedNodeIndex.value === null) return
  form.value.edges.push({ sourceNodeIndex: selectedNodeIndex.value, targetNodeIndex: 0, edgeType: 'normal' })
}

function removeEdge(idx: number) {
  const edgeToRemove = outEdges.value[idx]
  const globalIdx = form.value.edges.indexOf(edgeToRemove)
  if (globalIdx >= 0) form.value.edges.splice(globalIdx, 1)
}

function onCanvasUpdatePosition(index: number, x: number, y: number) {
  if (form.value.nodes[index]) {
    form.value.nodes[index].positionX = Math.round(x)
    form.value.nodes[index].positionY = Math.round(y)
  }
}

async function handleSave() {
  try {
    if (isEdit.value) {
      await updateWorkflow(Number(route.params.id), form.value)
    } else {
      await createWorkflow(form.value as any)
    }
    ElMessage.success('保存成功')
    router.push('/workflows')
  } catch {
    ElMessage.error('保存失败')
  }
}

onMounted(async () => {
  if (isEdit.value) {
    const detail = await getWorkflowDetail(Number(route.params.id))
    form.value = {
      name: detail.name,
      description: detail.description || '',
      status: detail.status,
      nodes: detail.nodes || [],
      edges: detail.edges || []
    }
  }
})
</script>

<style scoped>
.workflow-editor { padding: 0; }
.editor-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.editor-body { display: grid; grid-template-columns: 280px 1fr; gap: 16px; }
.node-list-panel { border: 1px solid var(--el-border-color); border-radius: 8px; padding: 12px; }
.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.node-card { display: flex; align-items: center; gap: 8px; padding: 8px; border-radius: 6px; cursor: pointer; margin-bottom: 4px; }
.node-card:hover { background: var(--el-fill-color-light); }
.node-card.active { background: var(--el-color-primary-light-9); border: 1px solid var(--el-color-primary); }
.node-type-icon { font-size: 16px; flex-shrink: 0; }
.node-type-label { font-size: 11px; color: var(--el-text-color-secondary); flex-shrink: 0; }
.node-name { flex: 1; font-size: 14px; }
.node-config-panel { min-height: 400px; }
.empty-hint { display: flex; align-items: center; justify-content: center; color: var(--el-text-color-secondary); min-height: 400px; }

.type-hint {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.type-hint code {
  background: var(--el-fill-color-light);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 12px;
}

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

.edge-list { display: flex; flex-direction: column; gap: 8px; }
.edge-item { display: flex; gap: 8px; align-items: center; }
.arrow { font-size: 14px; color: var(--el-text-color-secondary); flex-shrink: 0; }
</style>
