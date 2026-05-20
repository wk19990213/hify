import { get, post, del } from '@/utils/request'

export interface KnowledgeBase {
  id: number
  name: string
  description: string
  embeddingModel: string
  chunkSize: number
  chunkOverlap: number
  status: number
  documentCount: number
  createdAt: string
}

export interface DocumentResp {
  id: number
  kbId: number
  name: string
  fileType: string
  fileSize: number
  status: string
  chunkCount: number
  createdAt: string
}

export interface RagResp {
  answer: string
  sources: string[]
  tokenCount: number
  latencyMs: number
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export const createKB = (name: string, description?: string) =>
  post<KnowledgeBase>(`/v1/knowledge/bases?name=${encodeURIComponent(name)}&description=${encodeURIComponent(description || '')}`)

export const listKB = (page = 1, pageSize = 20) =>
  get<PageResult<KnowledgeBase>>(`/v1/knowledge/bases?page=${page}&pageSize=${pageSize}`)

export const deleteKB = (id: number) =>
  del<void>(`/v1/knowledge/bases/${id}`)

export const uploadDocument = (kbId: number, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return post<DocumentResp>(`/v1/knowledge/bases/${kbId}/documents`, formData)
}

export const queryKB = (kbId: number, question: string) =>
  post<RagResp>(`/v1/knowledge/bases/${kbId}/query?question=${encodeURIComponent(question)}`)
