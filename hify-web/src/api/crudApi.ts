import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

/**
 * 泛型 CRUD API 工厂 — 减少模块重复的 get/post/put/del 样板代码。
 *
 * @example
 * const agentApi = crudApi<AgentRequest, AgentResponse>('/v1/agents')
 * agentApi.list({ page: 1, pageSize: 20 })  // → PageResult<AgentResponse>
 * agentApi.create({ name: '...' })           // → { id: 1 }
 * agentApi.getDetail(1)                      // → AgentResponse
 */
export function crudApi<Req, Resp>(basePath: string) {
  return {
    list: (params?: Record<string, any>): Promise<PageResult<Resp>> =>
      get(`${basePath}`, params),

    create: (data: Req): Promise<{ id: number }> =>
      post(`${basePath}`, data),

    update: (id: number, data: Partial<Req>): Promise<void> =>
      put(`${basePath}/${id}`, data),

    delete: (id: number): Promise<void> =>
      del(`${basePath}/${id}`),

    getDetail: (id: number): Promise<Resp> =>
      get(`${basePath}/${id}`),
  }
}
