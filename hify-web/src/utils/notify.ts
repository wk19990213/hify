import { ElMessage, ElNotification } from 'element-plus'

const DEFAULT_DURATION = 3000
const DEFAULT_OFFSET = 20

export const notifySuccess = (message: string, customDuration?: number): void => {
  ElMessage.success({
    message,
    duration: customDuration || DEFAULT_DURATION,
    offset: DEFAULT_OFFSET
  })
}

export const notifyError = (message: string, description?: string, customDuration?: number): void => {
  if (description) {
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
