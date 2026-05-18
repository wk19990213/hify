/**
 * Element Plus 主题覆盖配置
 * 导入 design-system.css 后，Element 组件会自动应用这些样式
 */

import type { ConfigProviderProps } from 'element-plus'

export const elementPlusTheme: Partial<ConfigProviderProps> = {
  // 尺寸（保持紧凑）
  size: 'default',
  zIndex: 3000,

  // 按钮自定义
  button: {
    autoInsertSpace: true,
  },
}

// Element 组件 CSS 变量覆盖
export const elementPlusCSSVars = `
  /* 按钮 */
  .el-button {
    font-weight: 500;
    border-radius: var(--radius-md);
    transition: all var(--transition-fast);
  }

  .el-button--primary {
    background: linear-gradient(135deg, var(--primary-600), var(--primary-500));
    border: none;
    box-shadow: var(--shadow-primary);
  }

  .el-button--primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgb(139 92 246 / 0.5);
  }

  .el-button--primary:active {
    transform: translateY(0);
  }

  /* 次要按钮 */
  .el-button--default {
    border-color: var(--border-default);
    background: var(--bg-primary);
  }

  .el-button--default:hover {
    border-color: var(--primary-400);
    color: var(--primary-600);
  }

  /* 输入框 */
  .el-input__wrapper {
    border-radius: var(--radius-md);
    box-shadow: 0 0 0 1px var(--border-default) inset;
    transition: all var(--transition-fast);
  }

  .el-input__wrapper:hover {
    box-shadow: 0 0 0 1px var(--border-strong) inset;
  }

  .el-input__wrapper.is-focus {
    box-shadow: 0 0 0 1px var(--primary-500) inset, 0 0 0 3px var(--primary-100);
  }

  /* 卡片 */
  .el-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--border-light);
    box-shadow: var(--shadow-sm);
    transition: box-shadow var(--transition-fast);
  }

  .el-card:hover {
    box-shadow: var(--shadow-md);
  }

  /* 表格 */
  .el-table {
    border-radius: var(--radius-lg);
    overflow: hidden;
  }

  .el-table th.el-table__cell {
    background: var(--bg-secondary);
    color: var(--text-secondary);
    font-weight: 600;
    font-size: var(--text-xs);
    text-transform: uppercase;
    letter-spacing: 0.05em;
    padding: var(--space-3) var(--space-4);
  }

  .el-table td.el-table__cell {
    padding: var(--space-4);
  }

  .el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell {
    background: var(--bg-secondary);
  }

  /* 菜单 */
  .el-menu {
    border-right: none;
  }

  .el-menu-item {
    border-radius: var(--radius-md);
    margin: var(--space-1) var(--space-2);
    transition: all var(--transition-fast);
  }

  .el-menu-item:hover {
    background: rgba(255, 255, 255, 0.05);
  }

  .el-menu-item.is-active {
    background: linear-gradient(135deg, var(--primary-700), var(--primary-600));
    box-shadow: var(--shadow-primary);
  }

  /* 对话框 */
  .el-dialog {
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-xl);
  }

  .el-dialog__header {
    border-bottom: 1px solid var(--border-light);
    padding: var(--space-4) var(--space-6);
  }

  .el-dialog__body {
    padding: var(--space-6);
  }

  /* 下拉菜单 */
  .el-dropdown-menu {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    border: 1px solid var(--border-light);
    padding: var(--space-2);
  }

  .el-dropdown-menu__item {
    border-radius: var(--radius-md);
    transition: all var(--transition-fast);
  }

  /* 标签 */
  .el-tag {
    border-radius: var(--radius-sm);
    font-weight: 500;
    padding: var(--space-1) var(--space-2);
  }

  .el-tag--primary {
    background: var(--primary-50);
    border-color: var(--primary-200);
    color: var(--primary-700);
  }

  .el-tag--success {
    background: var(--success-50);
    border-color: transparent;
    color: var(--success-600);
  }

  .el-tag--warning {
    background: var(--warning-50);
    border-color: transparent;
    color: var(--warning-600);
  }

  .el-tag--danger {
    background: var(--error-50);
    border-color: transparent;
    color: var(--error-600);
  }

  .el-tag--info {
    background: var(--gray-100);
    border-color: transparent;
    color: var(--gray-700);
  }

  /* 分页 */
  .el-pagination {
    font-weight: 500;
  }

  .el-pagination .el-pager li {
    border-radius: var(--radius-md);
    transition: all var(--transition-fast);
  }

  .el-pagination .el-pager li.is-active {
    background: linear-gradient(135deg, var(--primary-600), var(--primary-500));
    box-shadow: var(--shadow-primary);
  }

  /* 开关 */
  .el-switch__core {
    border-radius: var(--radius-full);
  }

  .el-switch.is-checked .el-switch__core {
    border-color: var(--primary-500);
    background: var(--primary-500);
  }

  /* 单选/复选 */
  .el-radio__input.is-checked .el-radio__inner {
    border-color: var(--primary-500);
    background: var(--primary-500);
  }

  .el-checkbox__input.is-checked .el-checkbox__inner {
    border-color: var(--primary-500);
    background: var(--primary-500);
  }

  /* 选择器 */
  .el-select .el-input.is-focus .el-input__wrapper {
    box-shadow: 0 0 0 1px var(--primary-500) inset, 0 0 0 3px var(--primary-100);
  }

  /* 日期选择器 */
  .el-date-editor {
    border-radius: var(--radius-md);
  }

  /* 消息提示 */
  .el-message {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    padding: var(--space-4) var(--space-6);
  }

  .el-message--success {
    background: var(--success-50);
    border-color: var(--success-500);
  }

  .el-message--warning {
    background: var(--warning-50);
    border-color: var(--warning-500);
  }

  .el-message--error {
    background: var(--error-50);
    border-color: var(--error-500);
  }

  /* 通知 */
  .el-notification {
    border-radius: var(--radius-xl);
    box-shadow: var(--shadow-xl);
    border: 1px solid var(--border-light);
  }

  /* 抽屉 */
  .el-drawer {
    box-shadow: var(--shadow-xl);
  }

  .el-drawer__header {
    border-bottom: 1px solid var(--border-light);
    padding: var(--space-4) var(--space-6);
    font-weight: 600;
  }

  /* 步骤条 */
  .el-step__head.is-success {
    color: var(--success-500);
    border-color: var(--success-500);
  }

  .el-step__head.is-process {
    color: var(--primary-500);
    border-color: var(--primary-500);
  }

  /* 进度条 */
  .el-progress-bar__inner {
    background: linear-gradient(90deg, var(--primary-500), var(--accent-500));
    border-radius: var(--radius-full);
  }

  /* 滑块 */
  .el-slider__bar {
    background: linear-gradient(90deg, var(--primary-500), var(--accent-500));
  }

  .el-slider__button {
    border-color: var(--primary-500);
    box-shadow: var(--shadow-sm);
  }

  /* 加载动画 */
  .el-loading-mask {
    background: rgba(255, 255, 255, 0.8);
  }

  .el-loading-spinner .path {
    stroke: var(--primary-500);
  }

  /* 徽章 */
  .el-badge__content {
    border-radius: var(--radius-full);
    border: 2px solid var(--bg-primary);
  }

  .el-badge__content--primary {
    background: var(--primary-500);
  }

  .el-badge__content--success {
    background: var(--accent-500);
  }

  /* 分割线 */
  .el-divider {
    background: var(--border-light);
  }

  /* 面包屑 */
  .el-breadcrumb {
    font-size: var(--text-sm);
  }

  .el-breadcrumb__item {
    font-weight: 500;
  }

  .el-breadcrumb__inner.is-link {
    color: var(--text-tertiary);
    transition: color var(--transition-fast);
  }

  .el-breadcrumb__inner.is-link:hover {
    color: var(--primary-600);
  }

  /* 文字提示 */
  .el-tooltip__popper {
    border-radius: var(--radius-md);
    padding: var(--space-2) var(--space-3);
  }

  /*  Popover */
  .el-popover {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    border: 1px solid var(--border-light);
  }

  /* Popconfirm */
  .el-popconfirm {
    border-radius: var(--radius-lg);
  }

  /* 颜色选择器 */
  .el-color-picker__trigger {
    border-radius: var(--radius-md);
  }

  /* 上传 */
  .el-upload--picture-card {
    border-radius: var(--radius-lg);
    border-color: var(--border-default);
    transition: all var(--transition-fast);
  }

  .el-upload--picture-card:hover {
    border-color: var(--primary-400);
    color: var(--primary-600);
  }

  /* 时间轴 */
  .el-timeline-item__node {
    background: var(--primary-500);
  }

  .el-timeline-item__node--success {
    background: var(--success-500);
  }

  .el-timeline-item__node--warning {
    background: var(--warning-500);
  }

  .el-timeline-item__node--danger {
    background: var(--error-500);
  }

  /* 骨架屏 */
  .el-skeleton {
    border-radius: var(--radius-md);
  }

  /* 空状态 */
  .el-empty {
    padding: var(--space-10) var(--space-6);
  }

  /* 回到顶部 */
  .el-backtop {
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-lg);
    background: var(--bg-primary);
    color: var(--primary-600);
    border: 1px solid var(--border-light);
    transition: all var(--transition-fast);
  }

  .el-backtop:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-primary);
  }

  /* 图片预览 */
  .el-image-viewer__btn {
    color: white;
    border-radius: var(--radius-full);
  }

  /* 虚拟滚动条 */
  .el-scrollbar__thumb {
    background: var(--gray-300);
    border-radius: var(--radius-full);
  }

  .el-scrollbar__thumb:hover {
    background: var(--gray-400);
  }

  /* 统计数字 */
  .el-statistic__content {
    font-feature-settings: 'tnum';
    font-variant-numeric: tabular-nums;
  }

  /* 描述列表 */
  .el-descriptions__label {
    color: var(--text-secondary);
    font-weight: 500;
  }

  /* 结果页 */
  .el-result {
    padding: var(--space-10) var(--space-8);
  }

  .el-result__icon svg {
    filter: drop-shadow(var(--shadow-sm));
  }
`
