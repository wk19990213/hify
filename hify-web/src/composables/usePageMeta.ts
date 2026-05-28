import { computed, shallowRef } from 'vue'
import { useRoute } from 'vue-router'
import type { Component } from 'vue'

export interface PrimaryAction {
  label: string
  onClick: () => void | Promise<void>
  icon?: Component
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
  disabled?: boolean
  loading?: boolean
}

const DEFAULT_TITLE = 'Hify'

const descriptions: Record<string, string> = {
  Login: '登录后继续使用 Hify 工作台',
  Dashboard: '查看系统运行状态和关键指标',
  DesignSystem: '预览基础组件、色彩和交互规范',
  Provider: '管理模型供应商和调用配置',
  Agent: '配置和维护智能体能力',
  Chat: '与智能体进行实时对话',
  Knowledge: '管理知识库和检索资料',
  WorkflowList: '编排、调试和发布自动化流程',
  WorkflowCreate: '创建新的自动化工作流',
  WorkflowEdit: '编辑已有工作流的节点和连线',
  McpServerList: '管理 MCP 服务连接和健康状态',
}

const primaryAction = shallowRef<PrimaryAction | null>(null)

function readMetaText(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

export function setPrimaryAction(action: PrimaryAction) {
  primaryAction.value = action
}

export function clearPrimaryAction() {
  primaryAction.value = null
}

export function usePageMeta() {
  const route = useRoute()

  const title = computed(() => readMetaText(route.meta.title) || DEFAULT_TITLE)
  const description = computed(() => {
    const metaDescription = readMetaText(route.meta.description)
    if (metaDescription) return metaDescription

    const routeName = typeof route.name === 'string' ? route.name : ''
    return descriptions[routeName] || ''
  })

  return {
    title,
    description,
    primaryAction,
    setPrimaryAction,
    clearPrimaryAction,
  }
}
