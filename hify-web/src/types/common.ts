/** 分页响应类型 */
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}
