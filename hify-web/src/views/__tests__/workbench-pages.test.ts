import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'

// Mock icons
vi.mock('@element-plus/icons-vue', () => {
  const icon = { template: '<span class="mock-icon" />' }
  return {
    Setting: icon, User: icon, Document: icon, Share: icon, Tools: icon,
    ChatDotRound: icon, Plus: icon, Search: icon, Edit: icon, Delete: icon, List: icon,
    Connection: icon, ChatLineSquare: icon, Monitor: icon, Operation: icon,
    DataAnalysis: icon, MagicStick: icon, Clock: icon, WarningFilled: icon,
    SuccessFilled: icon, CircleCheckFilled: icon, CircleCloseFilled: icon,
    InfoFilled: icon, Refresh: icon,
  }
})

// Mock API calls
vi.mock('@/api/provider', () => ({
  getProviderList: vi.fn().mockResolvedValue({ list: [], total: 3 }),
  deleteProvider: vi.fn(),
}))

vi.mock('@/api/agent', () => ({
  getAgentList: vi.fn().mockResolvedValue({ list: [], total: 5 }),
  deleteAgent: vi.fn(),
}))

vi.mock('@/api/workflow', () => ({
  getWorkflowList: vi.fn().mockResolvedValue({ list: [], total: 2 }),
}))

vi.mock('@/api/mcpServer', () => ({
  getMcpServerList: vi.fn().mockResolvedValue({ list: [], total: 4 }),
  getAllMcpTools: vi.fn().mockResolvedValue([]),
}))

describe('Dashboard', () => {
  it('renders workbench overview with metrics', async () => {
    const DashboardLayout = (await import('@/views/DashboardLayout.vue')).default
    const wrapper = mount(DashboardLayout, {
      global: {
        stubs: {
          'router-link': { template: '<a><slot /></a>' },
          'el-icon': true,
          'el-button': { template: '<button><slot /></button>' },
          'el-empty': { template: '<div class="el-empty" />' },
        },
      },
    })

    const text = wrapper.text()
    expect(text).toContain('平台概览')
    expect(text).toContain('Hify')
    expect(text).toContain('模型提供商')
    expect(text).toContain('Agent 数量')
    expect(text).toContain('知识库文档')
    expect(text).toContain('MCP 工具')
    expect(text).toContain('待处理事项')
    expect(text).toContain('最近活动')
  })
})

describe('Provider page', () => {
  it('renders provider page content', async () => {
    const ProviderList = (await import('@/views/provider/ProviderList.vue')).default
    const wrapper = mount(ProviderList, {
      global: {
        stubs: {
          'router-link': { template: '<a><slot /></a>' },
          'el-icon': true,
          'el-button': true,
          'el-table': true,
          'el-table-column': true,
          'el-tag': true,
          'el-dialog': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-select': true,
          'el-option': true,
          'el-empty': true,
          'el-pagination': true,
          'el-popconfirm': true,
        },
      },
    })

    // Should have Chinese page title
    expect(wrapper.text()).toContain('模型提供商管理')
    expect(wrapper.text()).toContain('提供商')
  })
})

describe('Agent page', () => {
  it('renders agent page content', async () => {
    const AgentList = (await import('@/views/agent/AgentList.vue')).default
    const wrapper = mount(AgentList, {
      global: {
        stubs: {
          'router-link': { template: '<a><slot /></a>' },
          'el-icon': true,
          'el-button': true,
          'el-table': true,
          'el-table-column': true,
          'el-tag': true,
          'el-dialog': true,
          'el-form': true,
          'el-form-item': true,
          'el-input': true,
          'el-select': true,
          'el-option': true,
          'el-empty': true,
          'el-pagination': true,
          'el-popconfirm': true,
          'el-slider': true,
          'el-input-number': true,
          'el-radio': true,
          'el-radio-group': true,
        },
      },
    })

    expect(wrapper.text()).toContain('Agent 管理')
    expect(wrapper.text()).toContain('配置')
  })
})
