import { get, post, put, del } from '@/utils/request'

// ── 类型定义 ───────────────────────────────────────────

export interface McpServer {
  id: number
  name: string
  command: string | null
  argsJson: string | null
  envVarsJson: string | null
  url: string | null
  transportType: string
  status: number
  createdAt: string
  updatedAt: string
}

export interface McpServerRequest {
  name: string
  command?: string
  argsJson?: string
  envVarsJson?: string
  url?: string
  transportType?: string
  status?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface McpServerListParams {
  page?: number
  pageSize?: number
  name?: string
  status?: number
}

// ── API 方法 ───────────────────────────────────────────

export const getMcpServerList = (params?: McpServerListParams) =>
  get<PageResult<McpServer>>('/v1/mcp-servers', params)

export const createMcpServer = (data: McpServerRequest) =>
  post<number>('/v1/mcp-servers', data)

export const getMcpServerDetail = (id: number) =>
  get<McpServer>(`/v1/mcp-servers/${id}`)

export const updateMcpServer = (id: number, data: McpServerRequest) =>
  put<void>(`/v1/mcp-servers/${id}`, data)

export const deleteMcpServer = (id: number) =>
  del<void>(`/v1/mcp-servers/${id}`)

/** 所有 MCP Server 及其工具（用于 Agent 工具绑定选择） */
export interface McpServerWithTools {
  serverId: number
  serverName: string
  tools: { name: string; description: string }[]
}

/** 获取所有 MCP Server 的工具列表（用于 Agent 绑定 UI） */
export const getAllMcpTools = () =>
  get<McpServerWithTools[]>('/v1/mcp-servers/tools')
