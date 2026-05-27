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

            <component
              :is="configComponent"
              v-if="configComponent"
              :config="nodeConfig"
              @update:config="onConfigUpdate"
            />
          </el-form>

          <VarHintsPanel
            :nodes="form.nodes"
            :edges="form.edges"
            :selected-node-index="selectedNodeIndex!"
          />
        </el-card>

        <EdgeConfigPanel
          :edges="form.edges"
          :nodes="form.nodes"
          :selected-node-index="selectedNodeIndex!"
          @update:edges="form.edges = $event"
        />
      </div>

      <div v-else class="empty-hint">点击左侧节点进行配置</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Plus, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import WorkflowCanvas from './components/WorkflowCanvas.vue'
import LlmNodeConfig from './LlmNodeConfig.vue'
import HttpNodeConfig from './HttpNodeConfig.vue'
import RagNodeConfig from './RagNodeConfig.vue'
import ConditionNodeConfig from './ConditionNodeConfig.vue'
import EdgeConfigPanel from './EdgeConfigPanel.vue'
import VarHintsPanel from './VarHintsPanel.vue'
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

const configComponentMap: Record<string, any> = {
  llm: LlmNodeConfig,
  condition: ConditionNodeConfig,
  rag: RagNodeConfig,
  http: HttpNodeConfig,
}

const configComponent = computed(() => configComponentMap[currentNode.value.type] || null)

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

/** 统一配置对象 */
const nodeConfig = reactive<Record<string, any>>({
  prompt: '', maxRetries: 0, kbId: '', query: '',
  url: '', method: 'GET', body: '', expression: '', toolsEnabled: false,
})

// 当选中的节点变化时，从 configJson 同步到 reactive 对象
watch(currentNode, () => {
  const cfg = parseConfig()
  for (const key of Object.keys(nodeConfig)) {
    nodeConfig[key] = cfg[key] !== undefined ? cfg[key] : nodeConfig[key]
  }
}, { immediate: true })

// 当 reactive 对象变化时，回写到 configJson
watch(nodeConfig, (val) => {
  updateConfig({ ...val })
}, { deep: true })

function onConfigUpdate(newConfig: Record<string, any>) {
  Object.assign(nodeConfig, newConfig)
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
</style>
