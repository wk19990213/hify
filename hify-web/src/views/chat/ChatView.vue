<template>
  <div class="chat-layout">
    <!-- 左侧会话列表 -->
    <aside class="session-sidebar">
      <div class="sidebar-header">
        <h3>{{ agentName || '对话' }}</h3>
        <el-button :icon="Plus" circle size="small" type="primary" @click="handleNewChat" />
      </div>
      <div class="session-list">
        <div
          v-for="s in sessions"
          :key="s.sessionId"
          :class="['session-item', { active: s.sessionId === sessionId }]"
          @click="handleSelectSession(s)"
        >
          <div class="session-title">{{ s.title }}</div>
          <div class="session-meta">
            <span :class="['status-dot', s.status === 'active' ? 'active' : 'ended']" />
            <span>{{ formatTime(s.createdAt) }}</span>
            <el-button :icon="Delete" link size="small" class="delete-btn" @click.stop="handleDeleteSession(s)" />
          </div>
        </div>
        <div v-if="sessions.length === 0" class="empty-sessions">暂无对话历史</div>
      </div>
    </aside>

    <!-- 右侧对话区 -->
    <div class="chat-main">
      <!-- 顶部栏 -->
      <div class="chat-header">
        <el-button :icon="ArrowLeft" text @click="$router.push('/agent')">返回</el-button>
        <div class="header-info">
          <span class="current-title">{{ currentTitle }}</span>
        </div>
        <el-button v-if="loading" :icon="CloseBold" type="danger" plain size="small" @click="handleStop">终止</el-button>
      </div>

      <!-- 消息区域 -->
      <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
        <div v-if="messages.length === 0 && !loading" class="empty-chat">
          <p>向 {{ agentName || 'Agent' }} 发送消息开始对话</p>
        </div>
        <div v-for="(msg, index) in messages" :key="index"
          :class="['message-row', msg.role === 'user' ? 'message-user' : 'message-assistant']">
          <div class="message-avatar">{{ msg.role === 'user' ? 'U' : 'A' }}</div>
          <div :class="['message-bubble', msg.isError && 'message-error']">
            <div v-if="msg.content && msg.role === 'assistant'" class="markdown-body" v-html="renderMarkdown(msg.content)" />
            <span v-else>{{ msg.content }}</span>
            <span v-if="msg.loading" class="typing-cursor">|</span>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="input-row">
          <el-input v-model="inputText" :disabled="loading || !sessionId" placeholder="输入消息... Enter 发送"
            type="textarea" :rows="2" @keydown.enter.exact.prevent="handleSend" resize="none" class="input-field" />
          <div class="input-actions">
            <el-button v-if="loading" :icon="CloseBold" type="danger" @click="handleStop">终止</el-button>
            <el-button v-else :icon="Promotion" type="primary" @click="handleSend" :disabled="loading || !inputText.trim()">发送</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, CloseBold, Plus, Promotion, Delete } from '@element-plus/icons-vue'
import { getAgentDetail } from '@/api/agent'
import { createChatSession, sendMessage, getChatHistory, listSessions, getSessionDetail, deleteSession } from '@/api/chat'
import type { ChatSession, ChatMessage } from '@/api/chat'
import { notifySuccess, notifyError } from '@/utils/notify'
import MarkdownIt from 'markdown-it'

// ── 状态 ──
const route = useRoute()
const md = new MarkdownIt({ breaks: true, linkify: true })
const agentName = ref('')
const agentId = ref<number>(0)
const sessions = ref<ChatSession[]>([])
const sessionId = ref<number | null>(null)
const currentTitle = ref('新对话')
const inputText = ref('')
const loading = ref(false)
const userScrolledUp = ref(false)
const messagesContainer = ref<HTMLElement>()
const messages = ref<(ChatMessage & { loading?: boolean; isError?: boolean })[]>([])
let abortController: AbortController | null = null

onMounted(async () => {
  agentId.value = Number(route.query.agentId)
  if (!agentId.value) return
  try {
    const agent = await getAgentDetail(agentId.value)
    agentName.value = agent.name
    await loadSessions()
    // 尝试恢复最近活跃的会话
    const activeSessions = sessions.value.filter(s => s.status === 'active')
    if (activeSessions.length > 0) {
      await selectSession(activeSessions[0].sessionId)
    } else {
      await createNewSession()
    }
  } catch { notifyError('初始化对话失败') }
})

const loadSessions = async () => {
  try { sessions.value = await listSessions(agentId.value) } catch { /* ignore */ }
}

const createNewSession = async () => {
  const s = await createChatSession(agentId.value)
  sessionId.value = s.sessionId
  currentTitle.value = '新对话'
  messages.value = []
  sessions.value.unshift(s)
}

const selectSession = async (id: number) => {
  sessionId.value = id
  currentTitle.value = '加载中...'
  try {
    const s = await getSessionDetail(id)
    currentTitle.value = s.title || '对话'
    messages.value = s.messages || []
    scrollToBottom(true)
  } catch { notifyError('加载对话失败') }
}

const handleSelectSession = (s: ChatSession) => selectSession(s.sessionId)
const handleNewChat = () => {
  if (!agentId.value) {
    notifyError('请先选择一个 Agent')
    return
  }
  createNewSession()
}

const renderMarkdown = (text: string) => md.render(text)

const handleScroll = () => {
  if (!messagesContainer.value) return
  const el = messagesContainer.value
  userScrolledUp.value = el.scrollHeight - el.scrollTop - el.clientHeight > 60
}

const scrollToBottom = (force = false) => {
  if (!force && userScrolledUp.value) return
  nextTick(() => { if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight })
}

// ── 发送消息 ──
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value || !sessionId.value) return
  inputText.value = ''; loading.value = true; userScrolledUp.value = false
  messages.value.push({ id: 0, role: 'user', content: text, tokenCount: 0, createdAt: '' })
  messages.value.push({ id: 0, role: 'assistant', content: '', tokenCount: 0, createdAt: '', loading: true })
  scrollToBottom(true)
  abortController = new AbortController()

  try {
    const response = await fetch(`/api/v1/chat/sessions/${sessionId.value}/stream`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: text }), signal: abortController.signal
    })
    const reader = response.body?.getReader()
    if (!reader) throw new Error('No reader')
    const decoder = new TextDecoder()
    const lastMsg = messages.value[messages.value.length - 1]
    let buffer = '', done = false
    while (!done) {
      const result = await reader.read()
      if (result.done) break
      buffer += decoder.decode(result.value, { stream: true })
      const lines = buffer.split('\n'); buffer = lines.pop() || ''
      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.substring(5).trim()
        if (data === '[DONE]') { done = true; break }
        if (!data) continue
        lastMsg.content += data; scrollToBottom()
      }
    }
    if (buffer.startsWith('data:')) {
      const data = buffer.substring(5).trim()
      if (data && data !== '[DONE]') lastMsg.content += data
    }
    lastMsg.loading = false
    // 刷新会话列表以更新标题和时间
    currentTitle.value = text.substring(0, 20)
    loadSessions()
  } catch (err: any) {
    const lastMsg = messages.value[messages.value.length - 1]; lastMsg.loading = false
    if (err?.name === 'AbortError') { if (!lastMsg.content) lastMsg.content = '(已终止)' }
    else { lastMsg.isError = true; if (!lastMsg.content) lastMsg.content = '连接失败，请重试' }
  } finally { loading.value = false; abortController = null; scrollToBottom(true) }
}

const handleStop = () => { abortController?.abort(); loading.value = false }

const handleDeleteSession = async (s: ChatSession) => {
  try {
    await deleteSession(s.sessionId)
    sessions.value = sessions.value.filter(x => x.sessionId !== s.sessionId)
    if (sessionId.value === s.sessionId) {
      // 如果删的是当前会话，切到最近的
      const next = sessions.value[0]
      if (next) await selectSession(next.sessionId)
      else await createNewSession()
    }
    notifySuccess('已删除')
  } catch { notifyError('删除失败') }
}

// ── 工具函数 ──
const formatTime = (d: string) => {
  if (!d) return ''
  const dt = new Date(d)
  const now = new Date()
  const diff = now.getTime() - dt.getTime()
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  return dt.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.chat-layout { display: flex; height: calc(100vh - 60px); background: var(--bg-primary); }
.chat-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

/* 左侧会话列表 */
.session-sidebar { width: 260px; border-right: 1px solid var(--border-light); background: var(--bg-secondary); display: flex; flex-direction: column; flex-shrink: 0; }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; padding: 16px; border-bottom: 1px solid var(--border-light); }
.sidebar-header h3 { margin: 0; font-size: 16px; }
.session-list { flex: 1; overflow-y: auto; padding: 8px; }
.session-item { padding: 12px; border-radius: 8px; cursor: pointer; margin-bottom: 4px; transition: background .15s; }
.session-item:hover { background: var(--bg-primary); }
.session-item.active { background: var(--primary-50); border-left: 3px solid var(--primary-500); }
.session-title { font-size: 14px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-meta { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-secondary); margin-top: 4px; }
.status-dot { width: 8px; height: 8px; border-radius: 50%; }
.status-dot.active { background: var(--primary-500); }
.status-dot.ended { background: var(--gray-400); }
.empty-sessions { text-align: center; color: var(--text-secondary); padding: 24px; font-size: 14px; }
.delete-btn { opacity: 0; transition: opacity .15s; margin-left: auto; }
.session-item:hover .delete-btn { opacity: 1; }

/* 对话区 */
.chat-header { display: flex; align-items: center; gap: 12px; padding: 12px 24px; border-bottom: 1px solid var(--border-light); background: var(--bg-secondary); flex-shrink: 0; }
.header-info { flex: 1; }
.current-title { font-size: 15px; font-weight: 500; }

.chat-messages { flex: 1; overflow-y: auto; padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.empty-chat { text-align: center; color: var(--text-secondary); margin-top: 120px; }

.message-row { display: flex; gap: 12px; max-width: 80%; }
.message-user { align-self: flex-end; flex-direction: row-reverse; }
.message-assistant { align-self: flex-start; }
.message-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.message-user .message-avatar { background: var(--primary-500); color: #fff; }
.message-assistant .message-avatar { background: var(--bg-secondary); color: var(--primary-600); }
.message-bubble { padding: 12px 16px; border-radius: 12px; font-size: 15px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.message-user .message-bubble { background: var(--primary-500); color: #fff; border-bottom-right-radius: 4px; }
.message-assistant .message-bubble { background: var(--bg-secondary); border-bottom-left-radius: 4px; }
.message-error { background: #fff2f0 !important; color: #cf1322 !important; border: 1px solid #ffa39e !important; }

.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(code) { background: rgba(0,0,0,.06); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.markdown-body :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: 12px 16px; border-radius: 8px; overflow-x: auto; margin: 8px 0; }
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 4px 0; }
.markdown-body :deep(blockquote) { border-left: 3px solid var(--primary-400); padding-left: 12px; color: var(--text-secondary); margin: 8px 0; }

.typing-cursor { animation: blink .8s infinite; color: var(--primary-500); font-weight: bold; }
@keyframes blink { 50% { opacity: 0; } }

/* 输入区域 */
.chat-input-area { padding: 12px 24px; border-top: 1px solid var(--border-light); background: var(--bg-secondary); flex-shrink: 0; }
.chat-input-area :deep(.el-textarea__inner) { background: var(--bg-primary); }
.input-row { display: flex; align-items: flex-end; gap: 12px; }
.input-field { flex: 1; }
.input-actions { display: flex; gap: 10px; flex-shrink: 0; padding-bottom: 2px; }
</style>
