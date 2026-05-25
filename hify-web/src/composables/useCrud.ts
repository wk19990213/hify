import { ref } from 'vue'
import { useConfirm } from './useConfirm'
import { notifySuccess } from '@/utils/notify'
import type { PageResult } from '@/types/common'

/**
 * 通用 CRUD 操作 composable
 * 封装列表页面的分页加载、删除确认等常见模式
 *
 * @example
 * const { list, total, loading, loadList, handleDelete, handlePageChange } = useCrud()
 * loadList(() => knowledgeApi.list(params))
 */
export function useCrud<T>() {
  const list = ref<T[]>([])
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(10)
  const loading = ref(false)
  const { confirmDelete } = useConfirm()

  type FetchFn = (page: number, pageSize: number) => Promise<PageResult<T>>

  /** 加载列表数据 */
  const loadList = async (fetchFn: FetchFn) => {
    loading.value = true
    try {
      const result = await fetchFn(page.value, pageSize.value)
      list.value = result.list
      total.value = result.total
    } catch { /* 错误由 useRequest 拦截处理 */ }
    finally { loading.value = false }
  }

  /** 删除确认 */
  const handleDelete = async (message: string, deleteFn: () => Promise<any>) => {
    await confirmDelete(message, deleteFn)
  }

  /** 页码变化 */
  const handlePageChange = (newPage: number) => {
    page.value = newPage
  }

  /** 每页条数变化 */
  const handleSizeChange = (newSize: number) => {
    pageSize.value = newSize
    page.value = 1
  }

  /** 搜索/筛选后重新加载 */
  const search = (fetchFn: FetchFn) => {
    page.value = 1
    return loadList(fetchFn)
  }

  return {
    list,
    total,
    page,
    pageSize,
    loading,
    loadList,
    handleDelete,
    handlePageChange,
    handleSizeChange,
    search
  }
}

export default useCrud
