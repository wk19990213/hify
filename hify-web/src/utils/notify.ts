import { ElMessage, ElNotification } from 'element-plus'
import type { NotificationProps } from 'element-plus'

/**
 * 统一通知封装
 * 底层调用 ElMessage，统一配置 duration 和样式
 *
 * @example
 * import { notifySuccess, notifyError, notifyWarning } from '@/utils/notify'
 *
 * notifySuccess('创建成功')
 * notifyError('请求失败', '请稍后重试')
 * notifyWarning('表单校验失败')
 */

// 默认配置
const DEFAULT_DURATION = 3000
const DEFAULT_OFFSET = 20

/**
 * 成功提示
 * @param message 主消息
 * @param customDuration 显示时长（毫秒）
 */
export const notifySuccess = (message: string, customDuration?: number): void => {
  ElMessage.success({
    message,
    duration: customDuration || DEFAULT_DURATION,
    offset: DEFAULT_OFFSET
  })
}

/**
 * 错误提示
 * @param message 主消息
 * @param description 详细描述（可选）
 * @param customDuration 显示时长（毫秒）
 */
export const notifyError = (message: string, description?: string, customDuration?: number): void => {
  if (description) {
    // 使用 Notification 显示更详细的错误
    ElNotification.error({
      title: message,
      message: description,
      duration: customDuration || DEFAULT_DURATION * 1.5,
      position: 'top-right'
    })
  } else {
    ElMessage.error({
      message,
      duration: customDuration || DEFAULT_DURATION * 1.5,
      offset: DEFAULT_OFFSET
    })
  }
}

/**
 * 警告提示
 * @param message 主消息
 * @param description 详细描述（可选）
 * @param customDuration 显示时长（毫秒）
 */
export const notifyWarning = (message: string, description?: string, customDuration?: number): void => {
  if (description) {
    ElNotification.warning({
      title: message,
      message: description,
      duration: customDuration || DEFAULT_DURATION,
      position: 'top-right'
    })
  } else {
    ElMessage.warning({
      message,
      duration: customDuration || DEFAULT_DURATION,
      offset: DEFAULT_OFFSET
    })
  }
}

/**
 * 信息提示
 * @param message 主消息
 * @param description 详细描述（可选）
 * @param customDuration 显示时长（毫秒）
 */
export const notifyInfo = (message: string, description?: string, customDuration?: number): void => {
  if (description) {
    ElNotification.info({
      title: message,
      message: description,
      duration: customDuration || DEFAULT_DURATION,
      position: 'top-right'
    })
  } else {
    ElMessage.info({
      message,
      duration: customDuration || DEFAULT_DURATION,
      offset: DEFAULT_OFFSET
    })
  }
}

/**
 * 自定义通知（底层）
 * @param options ElNotification 配置
 */
export const notifyCustom = (options: NotificationProps): void => {
  ElNotification(options)
}

/**
 * 关闭所有消息
 */
export const closeAllMessages = (): void => {
  ElMessage.closeAll()
}

/**
 * 关闭所有通知
 */
export const closeAllNotifications = (): void => {
  ElNotification.closeAll()
}

// 导出别名，方便不同习惯使用
export const success = notifySuccess
export const error = notifyError
export const warning = notifyWarning
export const info = notifyInfo

// 默认导出
export default {
  success: notifySuccess,
  error: notifyError,
  warning: notifyWarning,
  info: notifyInfo,
  custom: notifyCustom,
  closeAllMessages,
  closeAllNotifications
}
