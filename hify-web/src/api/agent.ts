import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

// ── 类型定义 ───────────────────────────────────────────

/** 工具绑定请求 */
export interface AgentToolRequest {
  toolName: string
  toolType: 'mcp' | 'builtin'
  mcpServerId?: number
  configJson?: string
  sortOrder?: number
}

/** 工具绑定响应 */
export interface AgentToolResponse {
  id: number
  agentId: number
  toolName: string
  toolType: 'mcp' | 'builtin'
  mcpServerId?: number
  mcpServerName?: string
  configJson?: string
  sortOrder: number
}

/** Agent */
export interface Agent {
  id: number
  name: string
  code: string
  description: string
  modelConfigId?: number
  kbId?: number
  modelConfigName?: string
  workflowId?: number
  workflowName?: string
  systemPrompt?: string
  conversationMaxRounds: number
  temperature: number
  status: number
  sortOrder: number
  toolCount: number
  tools?: AgentToolResponse[]
  mcpServerIds?: number[]
  createdAt: string
  updatedAt: string
}

/** Agent 请求（创建/更新共用） */
export interface AgentRequest {
  name: string
  code?: string
  description?: string
  modelConfigId?: number
  kbId?: number
  workflowId?: number
  systemPrompt?: string
  conversationMaxRounds?: number
  temperature?: number
  status?: number
  sortOrder?: number
  tools?: AgentToolRequest[]
  mcpServerIds?: number[]
}

/** 列表查询参数 */
export interface AgentListParams {
  page?: number
  pageSize?: number
  sortField?: string
  sortOrder?: string
  name?: string
  status?: number
  modelConfigId?: number
}

// ── API 方法 ───────────────────────────────────────────

/** 分页列表 → 返回 PageResult<Agent> */
export const getAgentList = (params?: AgentListParams) =>
  get<PageResult<Agent>>('/v1/agents', params)

/** 创建 → 返回新 ID */
export const createAgent = (data: AgentRequest) =>
  post<number>('/v1/agents', data)

/** 详情 → 含 tools */
export const getAgentDetail = (id: number) =>
  get<Agent>(`/v1/agents/${id}`)

/** 更新 */
export const updateAgent = (id: number, data: AgentRequest) =>
  put<void>(`/v1/agents/${id}`, data)

/** 删除 */
export const deleteAgent = (id: number) =>
  del<void>(`/v1/agents/${id}`)
