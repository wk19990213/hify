import { get, post, put, del } from '@/utils/request'

export interface NodeItem {
  id?: number
  name: string
  type: 'llm' | 'condition' | 'rag' | 'http'
  configJson?: string
  positionX?: number
  positionY?: number
}

export interface EdgeItem {
  sourceNodeIndex: number
  targetNodeIndex: number
  edgeType?: 'normal' | 'true' | 'false' | 'error'
  conditionExpr?: string
  sortOrder?: number
}

export interface Workflow {
  id: number
  name: string
  description: string
  status: number
  nodes: NodeItem[]
  edges: EdgeItem[]
  createdAt: string
  updatedAt: string
}

export interface WorkflowCreateReq {
  name: string
  description?: string
  status?: number
  nodes: NodeItem[]
  edges: EdgeItem[]
}

export interface WorkflowUpdateReq {
  name?: string
  description?: string
  status?: number
  nodes?: NodeItem[]
  edges?: EdgeItem[]
}

export interface WorkflowListParams {
  page?: number
  pageSize?: number
  name?: string
  status?: number
}

export interface NodeExecutionResp {
  id: number
  nodeId: number
  nodeName: string
  nodeType: string
  status: string
  inputJson: string
  outputJson: string
  errorMsg: string
  retryCount: number
  startedAt: string
  finishedAt: string
}

export interface WorkflowInstanceResp {
  id: number
  workflowId: number
  workflowName: string
  sessionId: number
  triggerType: string
  status: string
  inputJson: string
  outputJson: string
  errorMsg: string
  startedAt: string
  finishedAt: string
  createdAt: string
  nodeExecutions?: NodeExecutionResp[]
}

export interface WorkflowRunReq {
  input?: Record<string, any>
  sessionId?: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export const getWorkflowList = (params?: WorkflowListParams) =>
  get<PageResult<Workflow>>('/v1/workflows', params)

export const createWorkflow = (data: WorkflowCreateReq) =>
  post<number>('/v1/workflows', data)

export const getWorkflowDetail = (id: number) =>
  get<Workflow>(`/v1/workflows/${id}`)

export const updateWorkflow = (id: number, data: WorkflowUpdateReq) =>
  put<void>(`/v1/workflows/${id}`, data)

export const deleteWorkflow = (id: number) =>
  del<void>(`/v1/workflows/${id}`)

export const runWorkflow = (id: number, data?: WorkflowRunReq) =>
  post<WorkflowInstanceResp>(`/v1/workflows/${id}/run`, data)

export const getRunHistory = (workflowId?: number, page?: number, pageSize?: number) =>
  get<PageResult<WorkflowInstanceResp>>('/v1/workflows/runs', { workflowId, page, pageSize })

export const getRunDetail = (id: number) =>
  get<WorkflowInstanceResp>(`/v1/workflows/runs/${id}`)
