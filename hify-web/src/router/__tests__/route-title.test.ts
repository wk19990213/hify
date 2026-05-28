import { describe, expect, it } from 'vitest'
import router from '../index'

describe('route titles', () => {
  it('uses readable Chinese titles for main pages', () => {
    const titles = router.getRoutes().map(route => route.meta.title)

    expect(titles).toContain('登录')
    expect(titles).toContain('概览')
    expect(titles).toContain('设计系统')
    expect(titles).toContain('模型管理')
    expect(titles).toContain('Agent 管理')
    expect(titles).toContain('对话')
    expect(titles).toContain('知识库')
    expect(titles).toContain('工作流')
    expect(titles).toContain('MCP 管理')
  })

  it('does not contain mojibake in route titles', () => {
    const joined = router.getRoutes().map(route => route.meta.title || '').join(' ')

    expect(joined).not.toMatch(/[�]|鐧|绠|妯|瀵|宸|姒|璁/)
  })
})
