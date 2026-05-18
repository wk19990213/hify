# Hify Design System

> 浅底 + 科技感 / 蓝紫主色 / 青绿辅色
> 参考：Linear、Supabase、Vercel

---

## 设计原则

1. **浅色背景优先**：管理后台表格多，深色底长时间看眼睛累
2. **科技感点缀**：侧边栏深色、按钮彩色渐变、微动效
3. **清晰层次**：通过阴影、圆角、边框区分层级
4. **数据友好**：等宽数字、状态指示、进度可视化

---

## 色彩系统

### 主色阶（蓝紫系 Violet-Blue）

| Token | Hex | 用途 |
|-------|-----|------|
| `--primary-50` | #f5f3ff | 浅色背景 |
| `--primary-100` | #ede9fe | 聚焦环 |
| `--primary-200` | #ddd6fe | 边框 |
| `--primary-300` | #c4b5fd | 浅色装饰 |
| `--primary-400` | #a78bfa | 浅色文字 |
| `--primary-500` | #8b5cf6 | 主色（渐变起点） |
| `--primary-600` | #7c3aed | **主按钮** |
| `--primary-700` | #6d28d9 | 深色按钮 |
| `--primary-800` | #5b21b6 | 深色装饰 |
| `--primary-900` | #4c1d95 | 最深色 |

### 辅色阶（青色 Cyan-Mint）

| Token | Hex | 用途 |
|-------|-----|------|
| `--accent-50` | #ecfeff | 浅色背景 |
| `--accent-100` | #cffafe | 聚焦环 |
| `--accent-500` | #06b6d4 | **辅按钮**、状态指示 |
| `--accent-600` | #0891b2 | 深色按钮 |
| `--accent-700` | #0e7490 | 深色装饰 |

### 中性色阶

| Token | Hex | 用途 |
|-------|-----|------|
| `--gray-50` | #fafafa | 页面背景 |
| `--gray-100` | #f4f4f5 | 卡片背景 |
| `--gray-200` | #e4e4e7 | 边框浅色 |
| `--gray-300` | #d4d4d8 | 边框默认 |
| `--gray-400` | #a1a1aa | 禁用文字 |
| `--gray-500` | #71717a | 三级文字 |
| `--gray-600` | #52525b | 二级文字 |
| `--gray-700` | #3f3f46 | 深色装饰 |
| `--gray-800` | #27272a | 深色侧边栏 |
| `--gray-900` | #18181b | 主文字、深色背景 |

### 深色背景

| Token | Hex | 用途 |
|-------|-----|------|
| `--bg-dark` | #0f0f11 | 侧边栏背景 |
| `--bg-dark-elevated` | #1a1a1e | 侧边栏次级 |
| `--bg-dark-sunken` | #0a0a0c | 最深色 |

---

## 阴影系统

| Token | 效果 | 用途 |
|-------|------|------|
| `--shadow-sm` | 0 1px 2px rgba(0,0,0,0.05) | 微提升、输入框 |
| `--shadow-md` | 0 4px 6px rgba(0,0,0,0.1) | 卡片、按钮 |
| `--shadow-lg` | 0 10px 15px rgba(0,0,0,0.1) | 下拉菜单、弹窗 |
| `--shadow-xl` | 0 20px 25px rgba(0,0,0,0.1) | 模态框、抽屉 |
| `--shadow-primary` | 0 4px 14px rgba(139,92,246,0.39) | 主按钮悬浮 |
| `--shadow-accent` | 0 4px 14px rgba(6,182,212,0.39) | 辅按钮悬浮 |

---

## 圆角系统

| Token | 值 | 用途 |
|-------|-----|------|
| `--radius-sm` | 4px | 小标签、输入框 |
| `--radius-md` | 6px | **默认**、按钮、菜单项 |
| `--radius-lg` | 8px | 卡片、表格 |
| `--radius-xl` | 12px | 大卡片、弹窗 |
| `--radius-2xl` | 16px | 特殊大元素 |
| `--radius-full` | 9999px | 圆点、头像、状态指示 |

---

## 动效系统

### 时长

| Token | 值 | 用途 |
|-------|-----|------|
| `--duration-fast` | 150ms | 按钮悬浮、链接 |
| `--duration-normal` | 250ms | 开关、展开、切换 |
| `--duration-slow` | 350ms | 弹窗、抽屉出现 |

### 缓动曲线

| Token | 曲线 | 用途 |
|-------|------|------|
| `--ease-default` | cubic-bezier(0.4, 0, 0.2, 1) | 标准过渡 |
| `--ease-out` | cubic-bezier(0, 0, 0.2, 1) | 出现动画 |
| `--ease-bounce` | cubic-bezier(0.34, 1.56, 0.64, 1) | 弹性效果 |
| `--ease-spring` | cubic-bezier(0.175, 0.885, 0.32, 1.275) | 弹簧效果 |

### 组合

```css
--transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
--transition-normal: 250ms cubic-bezier(0.4, 0, 0.2, 1);
--transition-slow: 350ms cubic-bezier(0, 0, 0.2, 1);
--transition-bounce: 250ms cubic-bezier(0.34, 1.56, 0.64, 1);
```

---

## 间距系统（4px 基数）

| Token | 值 | 用途 |
|-------|-----|------|
| `--space-1` | 4px | 最小间距 |
| `--space-2` | 8px | 紧凑间距 |
| `--space-3` | 12px | 组件内间距 |
| `--space-4` | 16px | **默认**、标准间距 |
| `--space-5` | 20px | 卡片内边距 |
| `--space-6` | 24px | 区域间距 |
| `--space-8` | 32px | 大间距 |
| `--space-10` | 40px | 页面内边距 |

---

## 字体系统

### 字号

| Token | 值 | 用途 |
|-------|-----|------|
| `--text-xs` | 12px | 辅助信息、时间 |
| `--text-sm` | 13px | 小标签、说明文字 |
| `--text-base` | 14px | **正文**、默认文字 |
| `--text-lg` | 16px | 小标题 |
| `--text-xl` | 18px | 中等标题 |
| `--text-2xl` | 20px | 大标题 |
| `--text-3xl` | 24px | 页面标题 |
| `--text-4xl` | 30px | 展示标题 |

### 字重

| Token | 值 | 用途 |
|-------|-----|------|
| `--font-normal` | 400 | 正文 |
| `--font-medium` | 500 | **默认**、按钮、标签 |
| `--font-semibold` | 600 | 标题、强调 |
| `--font-bold` | 700 | 大标题、数字 |

---

## 组件规范

### 按钮

**主按钮**
- 背景：渐变 `linear-gradient(135deg, #7c3aed, #8b5cf6)`
- 阴影：`var(--shadow-primary)`
- 悬浮：上移 1px，阴影增强
- 点击：复位

**次要按钮**
- 背景：白色
- 边框：`var(--border-default)`
- 悬浮：边框变主色，文字变主色

### 卡片

- 背景：白色
- 圆角：`var(--radius-lg)` (8px)
- 阴影：`var(--shadow-sm)`
- 边框：`1px solid var(--border-light)`
- 悬浮：阴影增强到 `var(--shadow-md)`

### 表格

- 表头：背景 `var(--bg-secondary)`，文字 `var(--text-secondary)`
- 表头字体：12px，大写，字间距 0.05em
- 行高亮：背景 `var(--bg-secondary)`
- 斑马纹：交替行背景色

### 状态指示

- 运行中：绿色 `#22c55e`，外环 `#f0fdf4`
- 警告：黄色 `#f59e0b`，外环 `#fffbeb`
- 错误：红色 `#ef4444`，外环 `#fef2f2`
- 处理中：青色 `#06b6d4`，外环 `#ecfeff`，脉冲动画

---

## 布局规范

### 侧边栏（深色）

- 宽度：220px（桌面），64px（移动）
- 背景：`linear-gradient(180deg, #0f0f11, #1a1a1e)`
- Logo 渐变：主色到辅色
- 菜单项：灰色默认，紫色渐变激活

### 主内容区

- 背景：`var(--bg-secondary)` (#fafafa)
- 卡片背景：白色
- 内边距：24px

---

## 使用方式

### 1. 引入设计系统

```ts
// main.ts
import './styles/design-system.css'
```

### 2. 使用 CSS 变量

```css
.my-component {
  background: var(--bg-primary);
  color: var(--text-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: var(--space-4);
}
```

### 3. 使用预设类

```html
<button class="hify-btn hify-btn-primary">主按钮</button>
<div class="hify-card">
  <span class="hify-tag hify-tag-primary">标签</span>
</div>
```

### 4. Element Plus 自动主题

设计系统已映射到 Element Plus 主题变量，组件会自动应用样式。

---

## 文件结构

```
src/styles/
├── design-system.css      # 设计系统变量和工具类
├── element-plus-theme.ts  # Element Plus 主题覆盖
└── global.css             # 全局基础样式
```

---

## 预览

访问 `/design` 路径查看完整设计系统展示。
