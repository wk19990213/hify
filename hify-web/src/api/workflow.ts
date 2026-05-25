import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/common'

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
