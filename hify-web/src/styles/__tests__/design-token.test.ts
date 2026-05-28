import { describe, expect, it } from 'vitest'
import fs from 'fs'
import path from 'path'

function loadCSS(filename: string): string {
  return fs.readFileSync(path.resolve(__dirname, '..', filename), 'utf-8')
}

describe('design-system.css tokens', () => {
  const css = loadCSS('design-system.css')

  it('has teal primary color', () => {
    expect(css).toMatch(/--primary-600:\s*#0d9488/)
  })

  it('has amber warning color', () => {
    expect(css).toMatch(/--warning-500:\s*#f59e0b/)
  })

  it('has dark shell sidebar background', () => {
    expect(css).toMatch(/--shell-sidebar-bg:\s*#0f172a/)
  })

  it('has surface page background', () => {
    expect(css).toMatch(/--surface-page:\s*#f6f7f9/)
  })

  it('does not use purple primary', () => {
    expect(css).not.toMatch(/#7c3aed|#8b5cf6|#a855f7|#c084fc|#6366f1/)
  })

  it('primary button does not use large gradient', () => {
    const match = css.match(/\.hify-btn-primary\s*\{([^}]+)\}/)
    if (match) {
      expect(match[1]).not.toMatch(/linear-gradient/)
    }
  })
})

describe('element-plus-theme.ts', () => {
  const theme = loadCSS('element-plus-theme.ts')

  it('does not use primary large gradient pattern', () => {
    expect(theme).not.toMatch(
      /linear-gradient\(135deg,\s*var\(--primary-600\),\s*var\(--primary-500\)\)/
    )
  })

  it('primary hover uses --primary-700', () => {
    expect(theme).toMatch(/--primary-700/)
  })

  it('does not contain hardcoded purple', () => {
    expect(theme).not.toMatch(/#7c3aed|#8b5cf6|#a855f7|#c084fc|#6366f1|rgb\(139\s*92\s*246/)
  })
})

describe('global.css', () => {
  const css = loadCSS('global.css')

  it('html, body, #app have min-height 100vh', () => {
    expect(css).toMatch(/html,\s*body,\s*#app\s*\{\s*min-height:\s*100vh/)
  })

  it('body uses var(--surface-page) background', () => {
    expect(css).toMatch(/body\s*\{[^}]*background:\s*var\(--surface-page\)/)
  })

  it('links inherit color with no default underline', () => {
    expect(css).toMatch(/a\s*\{[^}]*color:\s*inherit/)
    expect(css).toMatch(/a\s*\{[^}]*text-decoration:\s*none/)
  })
})
