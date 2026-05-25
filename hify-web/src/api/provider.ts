import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

// ── 类型定义 ───────────────────────────────────────────

/** 模型配置 */
export interface ModelConfig {
  id: number
  providerId: number
  modelId: string
  name: string
  code: string
  capabilities: Record<string, any> | null
  priceConfig: Record<string, any> | null
  status: number
  isDefault: number
  sortOrder: number
  createdAt: string
  updatedAt: string
}

/** 健康状态 */
export interface ProviderHealth {
  providerId: number
  status: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY' | 'UNKNOWN'
  consecutiveFailures: number
  avgLatencyMs: number | null
  successRate: number | null
  lastCheckTime: string | null
  lastSuccessTime: string | null
  lastErrorMsg: string | null
}

/** 提供商（列表/详情共用） */
export interface Provider {
  id: number
  name: string
  code: string
  type: 'OPENAI' | 'ANTHROPIC' | 'OLLAMA' | 'OPENAI_COMPATIBLE'
  baseUrl: string
  authConfig: Record<string, any> | null
  timeoutMs: number
  maxRetries: number
  retryIntervalMs: number
  status: number
  sortOrder: number
  extraConfig: Record<string, any> | null
  createdAt: string
  updatedAt: string
  /** 已启用模型数（列表接口填充） */
  modelCount: number
  /** 模型配置（仅详情接口填充） */
  modelConfigs?: ModelConfig[]
  /** 健康状态（仅详情接口填充） */
  health?: ProviderHealth | null
}

/** 提供商请求（创建/更新共用） */
export interface ProviderRequest {
  name: string
  code?: string
  type: string
  baseUrl: string
  authConfig?: Record<string, any>
  timeoutMs?: number
  maxRetries?: number
  retryIntervalMs?: number
  status?: number
  sortOrder?: number
  extraConfig?: Record<string, any>
}

/** 连通性测试结果 */
export interface ConnectionTestResult {
  success: boolean
  latencyMs: number
  modelCount: number
  errorMessage: string | null
}

/** 分页结果（request.ts 拦截器已解包 Result.data，字段对齐 CLAUDE.md） */

/** 列表查询参数 */
export interface ProviderListParams {
  page?: number
  pageSize?: number
  type?: string
  enabled?: boolean
}

// ── API 方法 ───────────────────────────────────────────

/** 分页列表 → request.ts 解包后直接返回 PageResult<Provider> */
export const getProviderList = (params?: ProviderListParams) =>
  get<PageResult<Provider>>('/v1/providers', params)

/** 创建 → 返回新 ID */
export const createProvider = (data: ProviderRequest) =>
  post<number>('/v1/providers', data)

/** 详情 → 含 modelConfigs + health */
export const getProviderDetail = (id: number) =>
  get<Provider>(`/v1/providers/${id}`)

/** 更新 */
export const updateProvider = (id: number, data: ProviderRequest) =>
  put<void>(`/v1/providers/${id}`, data)

/** 删除 */
export const deleteProvider = (id: number) =>
  del<void>(`/v1/providers/${id}`)

/** 连通性测试 */
export const testConnection = (id: number) =>
  post<ConnectionTestResult>(`/v1/providers/${id}/test-connection`)
