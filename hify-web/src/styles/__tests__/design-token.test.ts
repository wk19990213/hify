import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const css = readFileSync(resolve(__dirname, '../design-system.css'), 'utf8')
const elementPlusTheme = readFileSync(resolve(__dirname, '../element-plus-theme.ts'), 'utf8')

describe('Workbench Pro design tokens', () => {
  it('defines teal primary tokens and amber warning tokens', () => {
    expect(css).toContain('--primary-600: #0d9488')
    expect(css).toContain('--warning-500: #f59e0b')
    expect(css).toContain('--shell-sidebar-bg: #0f172a')
    expect(css).toContain('--surface-page: #f6f7f9')
  })

  it('does not use large purple gradient as primary theme', () => {
    expect(css).not.toContain('--primary-600: #7c3aed')
    expect(css).not.toContain('linear-gradient(135deg, var(--primary-600), var(--primary-500))')
    expect(elementPlusTheme).not.toContain(
      'linear-gradient(135deg, var(--primary-600), var(--primary-500))',
    )
  })
})
