import { ElMessageBox, ElMessage } from 'element-plus'

/**
 * 删除确认 composable
 * 一行代码完成：确认删除 → 调接口 → 提示成功
 *
 * @example
 * const { confirmDelete } = useConfirm()
 *
 * // 在方法中使用
 * const handleDelete = (id: string) => {
 *   confirmDelete('确定要删除该 Agent 吗？', () => deleteAgentApi(id))
 * }
 */

export interface ConfirmOptions {
  title?: string
  message?: string
  confirmButtonText?: string
  cancelButtonText?: string
  type?: 'warning' | 'error' | 'info' | 'success'
  successMessage?: string
  showClose?: boolean
}

export const useConfirm = () => {
  /**
   * 确认删除
   * @param message 确认文案
   * @param apiFn API 方法（返回 Promise）
   * @param options 额外配置
   * @returns Promise<void>
   */
  const confirmDelete = <T = any>(
    message: string,
    apiFn: () => Promise<T>,
    options?: Partial<ConfirmOptions>
  ): Promise<T> => {
    const opts: ConfirmOptions = {
      title: '确认删除',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
      successMessage: '删除成功',
      showClose: false,
      ...options
    }

    return new Promise((resolve, reject) => {
      ElMessageBox.confirm(message, opts.title!, {
        confirmButtonText: opts.confirmButtonText,
        cancelButtonText: opts.cancelButtonText,
        type: opts.type,
        showClose: opts.showClose,
        customClass: 'hify-confirm-box'
      })
        .then(() => {
          // 用户确认，调用 API
          apiFn()
            .then((res) => {
              ElMessage.success(opts.successMessage!)
              resolve(res)
            })
            .catch((err) => {
              reject(err)
            })
        })
        .catch(() => {
          // 用户取消
          reject(new Error('User cancelled'))
        })
    })
  }

  /**
   * 通用确认
   * @param message 确认文案
   * @param apiFn API 方法
   * @param options 配置
   * @returns Promise<void>
   */
  const confirm = <T = any>(
    message: string,
    apiFn: () => Promise<T>,
    options?: Partial<ConfirmOptions>
  ): Promise<T> => {
    const opts: ConfirmOptions = {
      title: '确认操作',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
      successMessage: '操作成功',
      showClose: false,
      ...options
    }

    return new Promise((resolve, reject) => {
      ElMessageBox.confirm(message, opts.title!, {
        confirmButtonText: opts.confirmButtonText,
        cancelButtonText: opts.cancelButtonText,
        type: opts.type,
        showClose: opts.showClose,
        customClass: 'hify-confirm-box'
      })
        .then(() => {
          apiFn()
            .then((res) => {
              ElMessage.success(opts.successMessage!)
              resolve(res)
            })
            .catch((err) => {
              reject(err)
            })
        })
        .catch(() => {
          reject(new Error('User cancelled'))
        })
    })
  }

  return {
    confirmDelete,
    confirm
  }
}

export default useConfirm
