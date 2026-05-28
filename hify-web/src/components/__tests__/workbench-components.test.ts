import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PageHeader from '@/components/PageHeader.vue'

describe('PageHeader', () => {
  it('renders title and description', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '模型管理', description: '管理模型供应商和调用配置' },
    })

    expect(wrapper.find('[data-testid="page-header-title"]').text()).toBe('模型管理')
    expect(wrapper.find('[data-testid="page-header-description"]').text()).toBe('管理模型供应商和调用配置')
  })

  it('renders without description', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '概览' },
    })

    expect(wrapper.find('[data-testid="page-header-title"]').text()).toBe('概览')
    expect(wrapper.find('[data-testid="page-header-description"]').exists()).toBe(false)
  })

  it('renders actions slot', () => {
    const wrapper = mount(PageHeader, {
      props: { title: 'Agent 管理' },
      slots: { actions: '<button data-testid="action-btn">新增 Agent</button>' },
    })

    expect(wrapper.find('[data-testid="page-header-actions"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="action-btn"]').text()).toBe('新增 Agent')
  })

  it('does not render actions when slot is empty', () => {
    const wrapper = mount(PageHeader, {
      props: { title: '对话' },
    })

    expect(wrapper.find('[data-testid="page-header-actions"]').exists()).toBe(false)
  })
})

describe('SummaryMetric', () => {
  it('renders label and value', async () => {
    const SummaryMetric = (await import('@/components/SummaryMetric.vue')).default
    const wrapper = mount(SummaryMetric, {
      props: { label: '模型提供商', value: 12 },
    })

    expect(wrapper.find('[data-testid="summary-metric-label"]').text()).toBe('模型提供商')
    expect(wrapper.find('[data-testid="summary-metric-value"]').text()).toBe('12')
  })

  it('renders numeric value', async () => {
    const SummaryMetric = (await import('@/components/SummaryMetric.vue')).default
    const wrapper = mount(SummaryMetric, {
      props: { label: 'Active Agents', value: 5 },
    })

    expect(wrapper.find('[data-testid="summary-metric-value"]').text()).toBe('5')
  })
})

describe('StatusBadge', () => {
  async function mountBadge(text: string, status: string) {
    const StatusBadge = (await import('@/components/StatusBadge.vue')).default
    return mount(StatusBadge, {
      props: { text, status } as any,
    })
  }

  it('renders text and status class', async () => {
    const wrapper = await mountBadge('运行中', 'success')
    const badge = wrapper.find('[data-testid="status-badge"]')
    expect(badge.text()).toBe('运行中')
    expect(badge.classes()).toContain('status-badge--success')
  })

  it('supports warning status', async () => {
    const wrapper = await mountBadge('异常', 'warning')
    expect(wrapper.find('[data-testid="status-badge"]').classes()).toContain('status-badge--warning')
  })

  it('supports danger status', async () => {
    const wrapper = await mountBadge('离线', 'danger')
    expect(wrapper.find('[data-testid="status-badge"]').classes()).toContain('status-badge--danger')
  })

  it('supports info status', async () => {
    const wrapper = await mountBadge('新建', 'info')
    expect(wrapper.find('[data-testid="status-badge"]').classes()).toContain('status-badge--info')
  })

  it('defaults to neutral status', async () => {
    const StatusBadge = (await import('@/components/StatusBadge.vue')).default
    const wrapper = mount(StatusBadge, {
      props: { text: '未知' },
    })
    expect(wrapper.find('[data-testid="status-badge"]').classes()).toContain('status-badge--neutral')
  })
})

describe('IconAction', () => {
  it('emits click event', async () => {
    const IconAction = (await import('@/components/IconAction.vue')).default
    const wrapper = mount(IconAction, {
      props: { label: '编辑' },
      slots: { default: '<span>icon</span>' },
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toHaveLength(1)
  })

  it('has aria-label', async () => {
    const IconAction = (await import('@/components/IconAction.vue')).default
    const wrapper = mount(IconAction, {
      props: { label: '删除' },
      slots: { default: '<span>icon</span>' },
    })

    expect(wrapper.attributes('aria-label')).toBe('删除')
  })

  it('does not emit click when disabled', async () => {
    const IconAction = (await import('@/components/IconAction.vue')).default
    const wrapper = mount(IconAction, {
      props: { label: '删除', disabled: true },
      slots: { default: '<span>icon</span>' },
    })

    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeFalsy()
  })
})

describe('ListToolbar', () => {
  it('renders filters slot', async () => {
    const ListToolbar = (await import('@/components/ListToolbar.vue')).default
    const wrapper = mount(ListToolbar, {
      slots: { filters: '<select data-testid="filter-select"><option>All</option></select>' },
    })

    expect(wrapper.find('[data-testid="filter-select"]').exists()).toBe(true)
  })

  it('renders actions slot', async () => {
    const ListToolbar = (await import('@/components/ListToolbar.vue')).default
    const wrapper = mount(ListToolbar, {
      slots: { actions: '<button data-testid="create-btn">新增</button>' },
    })

    expect(wrapper.find('[data-testid="create-btn"]').text()).toBe('新增')
  })

  it('renders both slots', async () => {
    const ListToolbar = (await import('@/components/ListToolbar.vue')).default
    const wrapper = mount(ListToolbar, {
      slots: {
        filters: '<span data-testid="f">F</span>',
        actions: '<span data-testid="a">A</span>',
      },
    })

    expect(wrapper.find('[data-testid="f"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="a"]').exists()).toBe(true)
  })
})

describe('HifyTable', () => {
  it('renders wrapper with table', async () => {
    const HifyTable = (await import('@/components/HifyTable.vue')).default
    const mockApi = async () => ({ list: [], total: 0, page: 1, pageSize: 20 })
    const wrapper = mount(HifyTable, {
      props: {
        columns: [{ prop: 'name', label: '名称' }],
        api: mockApi,
        showPagination: false,
      },
      global: {
        stubs: { 'el-table': true, 'el-table-column': true, 'el-empty': true, 'el-pagination': true },
      },
    })

    expect(wrapper.find('.hify-table-wrapper').exists()).toBe(true)
    expect(wrapper.find('.hify-table').exists()).toBe(true)
  })
})
