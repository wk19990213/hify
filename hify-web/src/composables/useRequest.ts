import { ref, computed, type Ref, type ComputedRef } from 'vue'

/**
 * 请求状态管理 composable
 * 自动管理 loading/error/data，避免每个页面写 try-catch-finally
 *
 * @example
 * const { data, loading, error, execute } = useRequest(fetchUserList)
 *
 * // 执行请求
 * await execute({ page: 1 })
 *
 * // 在模板中使用
 * <div v-if="loading">加载中...</div>
 * <div v-else-if="error">出错了</div>
 * <div v-else>{{ data }}</div>
 */

export interface UseRequestOptions<T, P extends any[]> {
  /** 默认数据 */
  defaultData?: T
  /** 是否立即执行 */
  immediate?: boolean
  /** 请求前钩子 */
  onBefore?: (...args: P) => void
  /** 成功回调 */
  onSuccess?: (data: T, ...args: P) => void
  /** 失败回调 */
  onError?: (error: any, ...args: P) => void
  /** 完成回调 */
  onFinally?: (...args: P) => void
}

export interface UseRequestReturn<T, P extends any[]> {
  data: { value: T | null }
  loading: { value: boolean }
  error: { value: any }
  isSuccess: { value: boolean }
  isError: { value: boolean }
  execute: (...args: P) => Promise<T | null>
  reset: () => void
  setData: (data: T) => void
}

export function useRequest<T, P extends any[] = any[]>(
  apiFn: (...args: P) => Promise<T>,
  options?: UseRequestOptions<T, P>
): UseRequestReturn<T, P> {
  const {
    defaultData = null,
    immediate = false,
    onBefore,
    onSuccess,
    onError,
    onFinally
  } = options || {}

  // 状态
  const data = ref<T | null>(defaultData)
  const loading = ref(false)
  const error = ref<any>(null)

  // 计算属性
  const isSuccess = computed(() => !loading.value && !error.value && data.value !== null)
  const isError = computed(() => !loading.value && error.value !== null)

  // 执行请求
  const execute = async (...args: P): Promise<T | null> => {
    // 请求前
    loading.value = true
    error.value = null

    try {
      onBefore?.(...args)

      // 执行 API
      const result = await apiFn(...args)

      // 请求成功
      data.value = result
      onSuccess?.(result, ...args)

      return result
    } catch (err) {
      // 请求失败
      error.value = err
      onError?.(err, ...args)
      return null
    } finally {
      // 请求完成
      loading.value = false
      onFinally?.(...args)
    }
  }

  // 重置状态
  const reset = () => {
    data.value = defaultData
    loading.value = false
    error.value = null
  }

  // 设置数据
  const setData = (newData: T) => {
    data.value = newData
  }

  // 立即执行
  if (immediate) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    execute(...([] as any))
  }

// 返回时转换为 any 避免类型问题
  return {
    data: data as any,
    loading: loading as any,
    error: error as any,
    isSuccess: isSuccess as any,
    isError: isError as any,
    execute,
    reset,
    setData
  }
}

/**
 * 分页请求 composable
 * 封装分页逻辑，自动管理分页参数
 *
 * @example
 * const { data, loading, page, pageSize, total, refresh, loadMore } = usePagination(
 *   (page, pageSize) => fetchList({ page, pageSize })
 * )
 */
export interface PaginationData<T> {
  list: T[]
  total: number
}

export interface UsePaginationOptions<T> {
  defaultPage?: number
  defaultPageSize?: number
  immediate?: boolean
  onSuccess?: (data: PaginationData<T>) => void
  onError?: (error: any) => void
}

export interface UsePaginationReturn<T> {
  data: { value: T[] }
  loading: { value: boolean }
  error: { value: any }
  page: { value: number }
  pageSize: { value: number }
  total: { value: number }
  totalPages: { value: number }
  hasMore: { value: boolean }
  isEmpty: { value: boolean }
  refresh: () => Promise<void>
  changePage: (newPage: number) => Promise<void>
  changePageSize: (newSize: number) => Promise<void>
  loadMore: () => Promise<void>
  reset: () => void
}

export function usePagination<T>(
  apiFn: (page: number, pageSize: number) => Promise<PaginationData<T>>,
  options?: UsePaginationOptions<T>
): UsePaginationReturn<T> {
  const {
    defaultPage = 1,
    defaultPageSize = 20,
    immediate = false,
    onSuccess,
    onError
  } = options || {}

  const data = ref<T[]>([])
  const loading = ref(false)
  const error = ref<any>(null)
  const page = ref(defaultPage)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)

  // 计算属性
  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
  const hasMore = computed(() => page.value < totalPages.value)
  const isEmpty = computed(() => !loading.value && data.value.length === 0)

  // 获取数据
  const fetch = async (): Promise<void> => {
    loading.value = true
    error.value = null

    try {
      const result = await apiFn(page.value, pageSize.value)
      data.value = result.list
      total.value = result.total
      onSuccess?.(result)
    } catch (err) {
      error.value = err
      onError?.(err)
    } finally {
      loading.value = false
    }
  }

  // 刷新
  const refresh = async (): Promise<void> => {
    page.value = defaultPage
    await fetch()
  }

  // 切换页码
  const changePage = async (newPage: number): Promise<void> => {
    page.value = newPage
    await fetch()
  }

  // 切换每页条数
  const changePageSize = async (newSize: number): Promise<void> => {
    pageSize.value = newSize
    page.value = defaultPage
    await fetch()
  }

  // 加载更多
  const loadMore = async (): Promise<void> => {
    if (!hasMore.value || loading.value) return
    page.value++
    loading.value = true

    try {
      const result = await apiFn(page.value, pageSize.value)
      const currentData = data.value as T[]
      data.value = [...currentData, ...result.list]
      total.value = result.total
    } catch (err) {
      error.value = err
      page.value--
    } finally {
      loading.value = false
    }
  }

  // 重置
  const reset = (): void => {
    data.value = []
    loading.value = false
    error.value = null
    page.value = defaultPage
    pageSize.value = defaultPageSize
    total.value = 0
  }

  // 立即执行
  if (immediate) {
    fetch()
  }

  return {
    data: data as any,
    loading: loading as any,
    error: error as any,
    page: page as any,
    pageSize: pageSize as any,
    total: total as any,
    totalPages: totalPages as any,
    hasMore: hasMore as any,
    isEmpty: isEmpty as any,
    refresh,
    changePage,
    changePageSize,
    loadMore,
    reset
  }
}

export default useRequest
