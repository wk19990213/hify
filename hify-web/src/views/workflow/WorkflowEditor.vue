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
          <el-input v-model="form.name" placeholder="工作流名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
    </el-card>

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
          <span class="node-name">{{ node.name || '未命名' }}</span>
          <el-tag size="small" :type="nodeTypeTag(node.type) as any">{{ node.type }}</el-tag>
          <el-button size="small" type="danger" circle :icon="Delete" @click.stop="removeNode(idx)" />
        </div>
      </div>

      <div class="node-config-panel" v-if="selectedNodeIndex !== null">
        <el-card>
          <template #header>节点配置</template>
          <el-form :model="currentNode" label-width="100px">
            <el-form-item label="名称">
              <el-input v-model="currentNode.name" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="currentNode.type">
                <el-option label="LLM 调用" value="llm" />
                <el-option label="条件分支" value="condition" />
                <el-option label="RAG 检索" value="rag" />
                <el-option label="HTTP 请求" value="http" />
              </el-select>
            </el-form-item>

            <template v-if="currentNode.type === 'llm'">
              <el-form-item label="模型配置ID">
                <el-input v-model="llmConfig.modelConfigId" placeholder="模型配置 ID" />
              </el-form-item>
              <el-form-item label="Prompt">
                <el-input v-model="llmConfig.prompt" type="textarea" :rows="4" placeholder="提示词，支持 {{node_id.field}} 变量" />
              </el-form-item>
              <el-form-item label="最大重试">
                <el-input-number v-model="llmConfig.maxRetries" :min="0" :max="5" />
              </el-form-item>
            </template>

            <template v-if="currentNode.type === 'rag'">
              <el-form-item label="知识库ID">
                <el-input v-model="ragConfig.kbId" placeholder="知识库 ID" />
              </el-form-item>
              <el-form-item label="查询语句">
                <el-input v-model="ragConfig.query" placeholder="支持 {{input.user_message}} 等变量" />
              </el-form-item>
            </template>

            <template v-if="currentNode.type === 'http'">
              <el-form-item label="URL">
                <el-input v-model="httpConfig.url" placeholder="https://api.example.com" />
              </el-form-item>
              <el-form-item label="方法">
                <el-select v-model="httpConfig.method">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                </el-select>
              </el-form-item>
              <el-form-item label="Body">
                <el-input v-model="httpConfig.body" type="textarea" :rows="3" />
              </el-form-item>
            </template>

            <template v-if="currentNode.type === 'condition'">
              <el-form-item label="表达式">
                <el-input v-model="conditionConfig.expression" placeholder="如 {{节点id.result}} == true" />
              </el-form-item>
            </template>
          </el-form>
        </el-card>

        <el-card style="margin-top: 16px;">
          <template #header>连线配置</template>
          <div class="edge-list">
            <div v-for="(edge, idx) in outEdges" :key="idx" class="edge-item">
              <el-select v-model="edge.edgeType" size="small" style="width: 120px;">
                <el-option label="默认" value="normal" />
                <el-option label="条件-真" value="true" />
                <el-option label="条件-假" value="false" />
                <el-option label="异常" value="error" />
              </el-select>
              <span style="font-size: 12px; color: #999;">→</span>
              <el-select v-model="edge.targetNodeIndex" size="small" style="width: 160px;">
                <el-option
                  v-for="(n, i) in form.nodes"
                  :key="i"
                  :label="n.name || '节点 ' + (i + 1)"
                  :value="i"
                />
              </el-select>
              <el-input v-if="edge.edgeType === 'true' || edge.edgeType === 'false'"
                v-model="edge.conditionExpr" size="small" placeholder="条件表达式" style="width: 160px;" />
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
import {
  createWorkflow, getWorkflowDetail, updateWorkflow,
  type NodeItem, type EdgeItem
} from '@/api/workflow'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const form = ref({ name: '', description: '', status: 1, nodes: [] as NodeItem[], edges: [] as EdgeItem[] })
const selectedNodeIndex = ref<number | null>(null)

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

const llmConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})
const ragConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})
const httpConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})
const conditionConfig = computed({
  get: () => parseConfig(),
  set: (val) => updateConfig(val)
})

function updateConfig(val: any) {
  if (selectedNodeIndex.value !== null) {
    form.value.nodes[selectedNodeIndex.value].configJson = JSON.stringify(val)
  }
}

const outEdges = computed(() => {
  if (selectedNodeIndex.value === null) return []
  return form.value.edges.filter(e => e.sourceNodeIndex === selectedNodeIndex.value)
})

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

function nodeTypeTag(type: string) {
  const map: Record<string, string> = { llm: 'primary', condition: 'warning', rag: 'success', http: 'info' }
  return map[type] || ''
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
.node-name { flex: 1; font-size: 14px; }
.node-config-panel { min-height: 400px; }
.empty-hint { display: flex; align-items: center; justify-content: center; color: var(--el-text-color-secondary); min-height: 400px; }
.edge-list { display: flex; flex-direction: column; gap: 8px; }
.edge-item { display: flex; gap: 8px; align-items: center; }
</style>
