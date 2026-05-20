<template>
  <div class="chat-container">
    <!-- 顶部栏 -->
    <div class="chat-header">
      <el-button :icon="ArrowLeft" text @click="$router.push('/agent')">返回</el-button>
      <div class="header-info">
        <h2>{{ agentName || '对话' }}</h2>
        <span class="session-title">{{ sessionTitle }}</span>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll">
      <div v-if="messages.length === 0 && !loading" class="empty-chat">
        <p>向 {{ agentName || 'Agent' }} 发送消息开始对话</p>
      </div>

      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message-row', msg.role === 'user' ? 'message-user' : 'message-assistant']"
      >
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
        <el-input
          v-model="inputText"
          :disabled="loading || sessionEnded"
          placeholder="输入消息... Enter 发送，Shift+Enter 换行"
          type="textarea"
          :rows="2"
          @keydown.enter.exact.prevent="handleSend"
          resize="none"
          class="input-field"
        />
        <div class="input-actions">
          <el-button v-if="loading" :icon="CloseBold" type="danger" @click="handleStop">终止</el-button>
          <el-button v-else :icon="Promotion" type="primary" @click="handleSend" :disabled="loading || !inputText.trim()">发送</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, CloseBold, Promotion } from '@element-plus/icons-vue'
import { getAgentDetail } from '@/api/agent'
import { createChatSession, endChatSession } from '@/api/chat'
import { notifyError } from '@/utils/notify'
import MarkdownIt from 'markdown-it'

// ── 状态 ──
const route = useRoute()
const md = new MarkdownIt({ breaks: true, linkify: true })
const agentName = ref('')
const sessionTitle = ref('新对话')
const sessionId = ref<number | null>(null)
const sessionEnded = ref(false)
const inputText = ref('')
const loading = ref(false)
const messagesContainer = ref<HTMLElement>()
const userScrolledUp = ref(false)
let abortController: AbortController | null = null

interface ChatMsg {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
  isError?: boolean
}

const messages = ref<ChatMsg[]>([])

// ── 初始化 ──
onMounted(async () => {
  const agentId = route.query.agentId as string
  if (!agentId) return
  try {
    const agent = await getAgentDetail(Number(agentId))
    agentName.value = agent.name
    const session = await createChatSession(agent.id)
    sessionId.value = session.sessionId
  } catch {
    notifyError('初始化对话失败')
  }
})

// ── Markdown 渲染 ──
const renderMarkdown = (text: string) => md.render(text)

// ── 滚动策略 —— 用户手动上滚时不断到底部 ──
const handleScroll = () => {
  if (!messagesContainer.value) return
  const el = messagesContainer.value
  const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 60
  userScrolledUp.value = !atBottom
}

const scrollToBottom = () => {
  if (userScrolledUp.value) return
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// ── 发送消息 —— 完整时间线 ──
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || loading.value || !sessionId.value) return

  // 1. 清空输入框，禁用发送按钮
  inputText.value = ''
  loading.value = true
  userScrolledUp.value = false

  // 2. 用户消息气泡
  messages.value.push({ role: 'user', content: text })
  scrollToBottom()

  // 3. AI 消息气泡（空内容 + 加载动画）
  messages.value.push({ role: 'assistant', content: '', loading: true })
  scrollToBottom()

  abortController = new AbortController()

  try {
    const response = await fetch(`/api/v1/chat/sessions/${sessionId.value}/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: text }),
      signal: abortController.signal
    })

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    if (!reader) throw new Error('No reader')

    const lastMsg = messages.value[messages.value.length - 1]
    let buffer = ''
    let done = false

    while (!done) {
      const result = await reader.read()
      if (result.done) break

      const text = decoder.decode(result.value, { stream: true })
      buffer += text
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('data:')) continue
        const data = line.substring(5).trim()
        if (data === '[DONE]') { done = true; break }
        if (!data) continue
        // 4. 追加大 AI 气泡
        lastMsg.content += data
        scrollToBottom()
      }
    }

    // 缓冲残留
    if (buffer.startsWith('data:')) {
      const data = buffer.substring(5).trim()
      if (data && data !== '[DONE]') lastMsg.content += data
    }

    // 5. 完成 —— 加载动画消失
    lastMsg.loading = false
    scrollToBottom()
  } catch (err: any) {
    const lastMsg = messages.value[messages.value.length - 1]
    lastMsg.loading = false
    if (err?.name === 'AbortError') {
      // 用户主动终止
      if (!lastMsg.content) lastMsg.content = '(已终止)'
    } else {
      // 6. 错误 —— 红色提示
      lastMsg.isError = true
      if (!lastMsg.content) lastMsg.content = '连接失败，请重试'
    }
  } finally {
    // 7. 恢复发送按钮
    loading.value = false
    abortController = null
  }
}

// ── 终止回答 ──
const handleStop = () => {
  abortController?.abort()
  loading.value = false
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg && lastMsg.loading) {
    lastMsg.loading = false
    if (!lastMsg.content) lastMsg.content = '(已终止)'
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  max-width: 900px;
  margin: 0 auto;
  background: var(--bg-primary);
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-secondary);
  flex-shrink: 0;
}
.header-info { flex: 1; }
.header-info h2 { margin: 0; font-size: 18px; }
.session-title { font-size: 13px; color: var(--text-secondary); }

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.empty-chat {
  text-align: center;
  color: var(--text-secondary);
  margin-top: 120px;
}

.message-row { display: flex; gap: 12px; max-width: 80%; }
.message-user { align-self: flex-end; flex-direction: row-reverse; }
.message-assistant { align-self: flex-start; }

.message-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 600; flex-shrink: 0;
}
.message-user .message-avatar { background: var(--primary-500); color: #fff; }
.message-assistant .message-avatar { background: var(--bg-secondary); color: var(--primary-600); }

.message-bubble {
  padding: 12px 16px; border-radius: 12px;
  font-size: 15px; line-height: 1.6;
  white-space: pre-wrap; word-break: break-word;
}
.message-user .message-bubble { background: var(--primary-500); color: #fff; border-bottom-right-radius: 4px; }
.message-assistant .message-bubble { background: var(--bg-secondary); border-bottom-left-radius: 4px; }
.message-error { background: #fff2f0 !important; color: #cf1322 !important; border: 1px solid #ffa39e !important; }

/* Markdown 内容样式 */
.markdown-body :deep(p) { margin: 0 0 8px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(code) {
  background: rgba(0,0,0,.06); padding: 2px 6px; border-radius: 4px; font-size: 13px;
}
.markdown-body :deep(pre) {
  background: #1e1e1e; color: #d4d4d4; padding: 12px 16px; border-radius: 8px;
  overflow-x: auto; margin: 8px 0;
}
.markdown-body :deep(pre code) { background: none; padding: 0; color: inherit; }
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin: 4px 0; }
.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--primary-400); padding-left: 12px; color: var(--text-secondary); margin: 8px 0;
}

/* 打字光标闪烁 */
.typing-cursor { animation: blink 0.8s infinite; color: var(--primary-500); font-weight: bold; }
@keyframes blink { 50% { opacity: 0; } }

/* 输入区域 */
.chat-input-area { padding: 16px 24px; border-top: 1px solid var(--border-light); background: var(--bg-secondary); flex-shrink: 0; }
.chat-input-area :deep(.el-textarea__inner) { background: var(--bg-primary); }
.input-row { display: flex; align-items: flex-end; gap: 12px; }
.input-field { flex: 1; }
.input-actions { display: flex; gap: 10px; flex-shrink: 0; padding-bottom: 2px; }
</style>
