/**
 * 格式化日期时间字符串为中文显示
 * @param datetime ISO 日期字符串
 * @returns 格式化后的日期时间，如 "2026/05/25 14:30"
 */
export function formatDateTime(datetime: string): string {
  if (!datetime) return '-'
  const d = new Date(datetime)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
