import { get, post, put, del } from '@/utils/request'

export interface ChatMessage {
  id: number
  role: string
  content: string
  tokenCount: number
  createdAt: string
}

export interface ChatSession {
  sessionId: number
  sessionUuid: string
  agentId: number
  title: string
  status: string
  messages: ChatMessage[]
  createdAt: string
}

export const listSessions = (agentId: number) =>
  get<ChatSession[]>(`/v1/chat/sessions?agentId=${agentId}`)

export const getSessionDetail = (sessionId: number) =>
  get<ChatSession>(`/v1/chat/sessions/${sessionId}`)

export const createChatSession = (agentId: number, title?: string) =>
  post<ChatSession>(`/v1/chat/sessions?agentId=${agentId}&title=${encodeURIComponent(title || '新对话')}`)

export const sendMessage = (sessionId: number, content: string) =>
  post<ChatMessage>(`/v1/chat/sessions/${sessionId}/messages`, { content })

export const getChatHistory = (sessionId: number) =>
  get<ChatMessage[]>(`/v1/chat/sessions/${sessionId}/messages`)

export const deleteSession = (sessionId: number) =>
  del<void>(`/v1/chat/sessions/${sessionId}`)
