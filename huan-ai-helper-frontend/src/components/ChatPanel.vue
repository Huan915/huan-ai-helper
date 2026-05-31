<template>
  <div class="chat-panel">
    <div v-if="props.initializing" class="loading-state"></div>
    <div v-else-if="props.activeTab === 'agent'" class="agent-state">
      <div class="agent-header">
        <h2 class="agent-title">选择智能体</h2>
        <button class="agent-create-btn" @click="handleCreateChatbot">
          <PlusOutlined />
          <span>创建智能体</span>
        </button>
      </div>
      <div v-if="ragAgentsLoading" class="agent-loading">加载中...</div>
      <div v-else class="agent-grid">
        <div
          v-for="agent in ragAgents"
          :key="agent.agentId"
          class="agent-card"
          @click="handleAgentClick(agent)"
        >
          <a-avatar :size="48" :src="agent.avatarUrl || undefined" class="agent-card-avatar">
            <template #icon><RobotOutlined /></template>
          </a-avatar>
          <div class="agent-card-name">{{ agent.agentName }}</div>
          <div class="agent-card-desc">{{ agent.description || '暂无描述' }}</div>
        </div>
      </div>
    </div>
    <div v-else-if="!conversationStore.currentConversation" class="home-state">
      <img :src="logoUrl" alt="Hper" class="home-logo" />
      <h2 class="home-title">想知道什么，请告诉我</h2>
      <div class="home-input-area">
        <div class="input-wrapper">
          <div v-if="pendingAttachments.length > 0" class="pending-attachments">
            <div v-for="(att, idx) in pendingAttachments" :key="idx" class="pending-attachment-card">
              <span class="pending-attachment-icon">{{ getFileIcon(att.type) }}</span>
              <div class="pending-attachment-info">
                <div class="pending-attachment-name">{{ att.name }}</div>
                <div class="pending-attachment-size">{{ formatFileSize(att.size) }}</div>
              </div>
              <button class="pending-attachment-remove" @click="removeAttachment(idx)">
                <CloseOutlined />
              </button>
            </div>
          </div>
          <textarea
            v-model="inputText"
            @keydown.enter.exact="handleHomeSend"
            @input="autoResize"
            @paste="handlePaste"
            placeholder="输入你的问题..."
            rows="1"
            ref="inputRef"
          ></textarea>
          <div class="input-bottom">
            <div class="input-bottom-left">
              <button
                class="attach-btn"
                :disabled="pendingAttachments.length >= 3"
                @click="triggerFileInput"
              >
                <PaperClipOutlined />
              </button>
              <button
                class="web-search-btn"
                :class="{ active: webSearchEnabled }"
                @click="webSearchEnabled = !webSearchEnabled"
              >
                <SearchOutlined />
                <span>联网搜索</span>
              </button>
            </div>
            <button
              class="send-btn"
              :disabled="(!inputText.trim() && pendingAttachments.length === 0) || homeSending"
              @click="handleHomeSend"
            >
              <ArrowUpOutlined />
            </button>
          </div>
        </div>
        <div class="input-hint">Enter 发送，Shift + Enter 换行</div>
      </div>
    </div>
    <template v-else>
      <div
        class="messages-area"
        ref="messagesRef"
        @scroll="handleMessagesScroll"
        :class="{ 'messages-hidden': !initialScrollDone }"
      >
        <div v-if="conversationStore.hasMoreMessages" class="load-more-indicator">
          <template v-if="conversationStore.loadingMore">
            <span class="load-more-spinner"></span>
            <span>加载中...</span>
          </template>
          <template v-else>
            <span class="load-more-hint">上拉加载更多</span>
          </template>
        </div>
        <div
          v-for="msg in conversationStore.messages"
          :key="msg.id"
          class="message-row"
          :class="msg.role"
        >
          <template v-if="msg.role === 'user'">
            <div class="user-bubble-wrap">
              <div v-if="parseAttachments(msg.attachments).length > 0" class="user-attachments">
                <div
                  v-for="(att, idx) in parseAttachments(msg.attachments)"
                  :key="idx"
                  class="user-attachment-card"
                  @click="previewAttachment(att)"
                >
                  <span class="user-attachment-icon">{{ getFileIcon(att.type) }}</span>
                  <div class="user-attachment-info">
                    <div class="user-attachment-name">{{ att.name }}</div>
                    <div class="user-attachment-size">{{ formatFileSize(att.size) }}</div>
                  </div>
                </div>
              </div>
              <div class="user-bubble">{{ msg.content }}</div>
            </div>
          </template>
          <template v-else>
            <div class="message-content">
              <div class="message-text markdown-body">
                <div v-if="conversationStore.sending && isLastAssistantMsg(msg.id) && !msg.content" class="thinking-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
                <template v-else>
                  <div v-html="renderMarkdown(msg.content)"></div>
                  <span
                    v-if="conversationStore.sending && isLastAssistantMsg(msg.id)"
                    class="cursor-blink"
                  >|</span>
                </template>
              </div>
            </div>
          </template>
        </div>
      </div>
      <transition name="scroll-btn-fade">
        <button v-if="showScrollBtn" class="scroll-bottom-btn" @click="scrollToBottom">
          <ArrowDownOutlined />
        </button>
      </transition>
      <div class="input-area">
        <div class="input-wrapper">
          <div v-if="pendingAttachments.length > 0" class="pending-attachments">
            <div v-for="(att, idx) in pendingAttachments" :key="idx" class="pending-attachment-card">
              <span class="pending-attachment-icon">{{ getFileIcon(att.type) }}</span>
              <div class="pending-attachment-info">
                <div class="pending-attachment-name">{{ att.name }}</div>
                <div class="pending-attachment-size">{{ formatFileSize(att.size) }}</div>
              </div>
              <button class="pending-attachment-remove" @click="removeAttachment(idx)">
                <CloseOutlined />
              </button>
            </div>
          </div>
          <textarea
            v-model="inputText"
            @keydown.enter.exact="handleSend"
            @input="autoResize"
            @paste="handlePaste"
            placeholder="发送消息..."
            rows="1"
            ref="inputRef"
          ></textarea>
          <div class="input-bottom">
            <div class="input-bottom-left">
              <button
                class="attach-btn"
                :disabled="pendingAttachments.length >= 3"
                @click="triggerFileInput"
              >
                <PaperClipOutlined />
              </button>
              <button
                v-if="conversationStore.currentConversation?.conversationType !== 'CHATBOT'"
                class="web-search-btn"
                :class="{ active: webSearchEnabled }"
                @click="webSearchEnabled = !webSearchEnabled"
              >
                <SearchOutlined />
                <span>联网搜索</span>
              </button>
            </div>
            <button
              class="send-btn"
              :disabled="(!inputText.trim() && pendingAttachments.length === 0) || conversationStore.sending"
              @click="handleSend"
            >
              <ArrowUpOutlined />
            </button>
          </div>
        </div>
        <div class="input-hint">Enter 发送，Shift + Enter 换行</div>
      </div>
    </template>

    <div v-if="agentDetailVisible" class="agent-detail-overlay" @click="agentDetailVisible = false">
      <div class="agent-detail-modal" @click.stop>
        <div class="agent-detail-header">
          <a-avatar :size="64" :src="selectedAgent?.avatarUrl || undefined">
            <template #icon><RobotOutlined /></template>
          </a-avatar>
          <div class="agent-detail-name">{{ selectedAgent?.agentName }}</div>
        </div>
        <div class="agent-detail-content">
          <div class="agent-detail-section">
            <div class="agent-detail-label">描述</div>
            <div class="agent-detail-value">{{ selectedAgent?.description || '暂无描述' }}</div>
          </div>
        </div>
        <div class="agent-detail-actions">
          <button class="agent-detail-btn cancel" @click="agentDetailVisible = false">取消</button>
          <button class="agent-detail-btn confirm" @click="handleStartConversation">开始对话</button>
        </div>
      </div>
    </div>

    <div v-if="previewVisible" class="preview-overlay" @click="previewVisible = false">
      <div class="preview-modal" @click.stop>
        <div class="preview-header">
          <div class="preview-title">
            <span class="preview-file-icon">{{ getFileIcon(previewFile?.type || '') }}</span>
            {{ previewFile?.name }}
          </div>
          <button class="preview-close" @click="previewVisible = false">×</button>
        </div>
        <div class="preview-body">
          <div v-if="previewLoading" class="preview-loading">加载中...</div>
          <div v-else-if="previewContent" class="preview-content">
            <pre>{{ previewContent }}</pre>
          </div>
          <div v-else class="preview-empty">无法预览该文件</div>
        </div>
      </div>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      accept=".pdf,.txt,.md"
      multiple
      style="display: none"
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, onMounted } from 'vue'
import { SearchOutlined, ArrowUpOutlined, ArrowDownOutlined, RobotOutlined, PlusOutlined, PaperClipOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { useConversationStore } from '@/stores/conversation'
import { renderMarkdown } from '@/utils/markdown'
import { listRagAgents } from '@/api/ragAgent'
import { uploadFile } from '@/api/file'
import type { RAGAgent, AttachmentInfo } from '@/api/types'
import logoUrl from '@/assets/images/hper-logo.svg'

const props = defineProps<{
  initializing?: boolean
  activeTab?: 'chat' | 'agent' | 'more'
}>()

const emit = defineEmits<{
  (e: 'createChatbot'): void
  (e: 'switchToChat'): void
}>()

const conversationStore = useConversationStore()
const inputText = ref('')
const webSearchEnabled = ref(false)
const homeSending = ref(false)
const messagesRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const showScrollBtn = ref(false)
const isScrollingToBottom = ref(false)
const initialScrollDone = ref(false)

const ragAgents = ref<RAGAgent[]>([])
const ragAgentsLoading = ref(false)

const agentDetailVisible = ref(false)
const selectedAgent = ref<RAGAgent | null>(null)

const pendingAttachments = ref<AttachmentInfo[]>([])
const fileInputRef = ref<HTMLInputElement | null>(null)
const uploadingFiles = ref(false)

const previewVisible = ref(false)
const previewFile = ref<AttachmentInfo | null>(null)
const previewContent = ref<string>('')
const previewLoading = ref(false)

onMounted(() => {
  if (props.activeTab === 'agent') {
    fetchRagAgents()
  }
})

watch(() => props.activeTab, (val) => {
  if (val === 'agent' && ragAgents.value.length === 0) {
    fetchRagAgents()
  }
})

async function fetchRagAgents() {
  ragAgentsLoading.value = true
  try {
    const res = await listRagAgents()
    ragAgents.value = res.data.data || []
  } catch {
    ragAgents.value = []
  } finally {
    ragAgentsLoading.value = false
  }
}

function handleAgentClick(agent: RAGAgent) {
  selectedAgent.value = agent
  agentDetailVisible.value = true
}

async function handleStartConversation() {
  if (!selectedAgent.value) return
  agentDetailVisible.value = false
  await conversationStore.createConversation(
    selectedAgent.value.agentName,
    selectedAgent.value.systemPrompt || undefined,
    'AGENT'
  )
  emit('switchToChat')
}

function handleCreateChatbot() {
  emit('createChatbot')
}

function isLastAssistantMsg(id: number): boolean {
  const assistantMsgs = conversationStore.messages.filter((m) => m.role === 'assistant')
  if (assistantMsgs.length === 0) return false
  return assistantMsgs[assistantMsgs.length - 1].id === id
}

let prevMsgCount = 0

watch(
  () => props.initializing,
  (val) => {
    if (!val && conversationStore.messages.length > 0) {
      nextTick(() => {
        scrollToBottom()
      })
    }
  },
)

watch(
  () => conversationStore.currentConversation?.id,
  () => {
    prevMsgCount = 0
    pendingAttachments.value = []
    initialScrollDone.value = false
  },
)

watch(
  () => conversationStore.messages.length,
  (len) => {
    nextTick(() => {
      if (!initialScrollDone.value) {
        const el = messagesRef.value
        if (el) {
          el.scrollTop = el.scrollHeight
        }
        requestAnimationFrame(() => {
          initialScrollDone.value = true
          isScrollingToBottom.value = false
        })
        prevMsgCount = len
        return
      }
      if (prevMsgCount === 0 || isNearBottom()) {
        scrollToBottom()
      }
      prevMsgCount = len
    })
  },
)

watch(
  () => conversationStore.messages.map((m) => m.content).join(''),
  () => {
    nextTick(() => {
      if (isNearBottom()) {
        scrollToBottom()
      }
    })
  },
)

function isNearBottom(): boolean {
  const el = messagesRef.value
  if (!el) return true
  const threshold = 120
  return el.scrollHeight - el.scrollTop - el.clientHeight < threshold
}

function scrollToBottom() {
  if (messagesRef.value) {
    isScrollingToBottom.value = true
    showScrollBtn.value = false
    messagesRef.value.scrollTo({
      top: messagesRef.value.scrollHeight,
      behavior: 'smooth',
    })
    const checkDone = () => {
      const el = messagesRef.value
      if (!el) {
        isScrollingToBottom.value = false
        return
      }
      if (Math.abs(el.scrollHeight - el.scrollTop - el.clientHeight) < 2) {
        isScrollingToBottom.value = false
      } else {
        requestAnimationFrame(checkDone)
      }
    }
    requestAnimationFrame(checkDone)
  }
}

function scrollToBottomImmediate() {
  const el = messagesRef.value
  if (el) {
    isScrollingToBottom.value = true
    showScrollBtn.value = false
    el.scrollTop = el.scrollHeight
    requestAnimationFrame(() => {
      isScrollingToBottom.value = false
    })
  }
}

function handleMessagesScroll() {
  if (isScrollingToBottom.value) return
  const el = messagesRef.value
  if (!el) return
  showScrollBtn.value = !isNearBottom()

  if (el.scrollTop < 100 && conversationStore.hasMoreMessages && !conversationStore.loadingMore) {
    handleLoadMore()
  }
}

async function handleLoadMore() {
  const el = messagesRef.value
  if (!el) return
  const prevScrollHeight = el.scrollHeight
  const prevScrollTop = el.scrollTop

  const count = await conversationStore.loadMoreMessages()
  if (count > 0) {
    await nextTick()
    const newScrollHeight = el.scrollHeight
    el.scrollTop = newScrollHeight - prevScrollHeight + prevScrollTop
  }
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

async function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files) return
  const files = Array.from(input.files)
  const remaining = 3 - pendingAttachments.value.length
  const toUpload = files.slice(0, remaining)
  input.value = ''

  if (toUpload.length === 0) return

  uploadingFiles.value = true
  try {
    for (const file of toUpload) {
      const ext = file.name.split('.').pop()?.toLowerCase() || ''
      if (!['pdf', 'txt', 'md'].includes(ext)) {
        continue
      }
      if (file.size > 10 * 1024 * 1024) {
        continue
      }
      const res = await uploadFile(file)
      if (res.data.data) {
        pendingAttachments.value.push(res.data.data)
      }
    }
  } finally {
    uploadingFiles.value = false
  }
}

function removeAttachment(idx: number) {
  pendingAttachments.value.splice(idx, 1)
}

async function handlePaste(e: ClipboardEvent) {
  const files = e.clipboardData?.files
  if (!files || files.length === 0) return
  e.preventDefault()
  const remaining = 3 - pendingAttachments.value.length
  if (remaining <= 0) return
  const toUpload = Array.from(files).slice(0, remaining)
  uploadingFiles.value = true
  try {
    for (const file of toUpload) {
      const ext = file.name.split('.').pop()?.toLowerCase() || ''
      if (!['pdf', 'txt', 'md'].includes(ext)) continue
      if (file.size > 10 * 1024 * 1024) continue
      const res = await uploadFile(file)
      if (res.data.data) {
        pendingAttachments.value.push(res.data.data)
      }
    }
  } finally {
    uploadingFiles.value = false
  }
}

function getFileIcon(type: string): string {
  switch (type) {
    case 'pdf': return '📄'
    case 'txt': return '📝'
    case 'md': return '📋'
    default: return '📎'
  }
}

function formatFileSize(size: number): string {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  return (size / (1024 * 1024)).toFixed(1) + ' MB'
}

function parseAttachments(attachments: string | null): AttachmentInfo[] {
  if (!attachments) return []
  try {
    return JSON.parse(attachments)
  } catch {
    return []
  }
}

async function previewAttachment(att: AttachmentInfo) {
  previewFile.value = att
  previewVisible.value = true
  previewLoading.value = true
  previewContent.value = ''

  try {
    if (att.type === 'txt' || att.type === 'md') {
      const response = await fetch(att.url)
      previewContent.value = await response.text()
    } else if (att.type === 'pdf') {
      previewContent.value = 'PDF 文件请下载后查看\n\n文件地址：' + att.url
    } else {
      previewContent.value = ''
    }
  } catch {
    previewContent.value = ''
  } finally {
    previewLoading.value = false
  }
}

async function handleHomeSend(e?: KeyboardEvent) {
  if (e && e.shiftKey) return
  e?.preventDefault()
  const text = inputText.value.trim()
  const attachments = [...pendingAttachments.value]
  if (!text && attachments.length === 0) return
  if (homeSending.value) return
  inputText.value = ''
  pendingAttachments.value = []
  homeSending.value = true
  try {
    await conversationStore.createConversation(text || '文件分享')
    nextTick(() => {
      scrollToBottom()
    })
    await conversationStore.sendMessage(text || '请查看我分享的文件', webSearchEnabled.value || undefined, attachments.length > 0 ? attachments : undefined)
  } finally {
    homeSending.value = false
  }
}

function autoResize() {
  const el = inputRef.value
  if (!el) return
  el.style.height = 'auto'
  const maxHeight = window.innerHeight / 3
  el.style.height = Math.min(el.scrollHeight, maxHeight) + 'px'
}

async function handleSend(e?: KeyboardEvent) {
  if (e && e.shiftKey) return
  e?.preventDefault()
  const text = inputText.value.trim()
  const attachments = [...pendingAttachments.value]
  if (!text && attachments.length === 0) return
  if (conversationStore.sending) return
  inputText.value = ''
  pendingAttachments.value = []
  nextTick(() => {
    autoResize()
    scrollToBottom()
  })
  await conversationStore.sendMessage(text || '请查看我分享的文件', webSearchEnabled.value || undefined, attachments.length > 0 ? attachments : undefined)
  nextTick(() => {
    inputRef.value?.focus()
  })
}
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  background: var(--bg-primary);
  position: relative;
}

.loading-state {
  flex: 1;
  background: var(--bg-primary);
}

.home-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
}

.agent-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 24px;
  overflow-y: auto;
}

.agent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 900px;
  margin-bottom: 32px;
}

.agent-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.agent-create-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--accent);
  border: none;
  border-radius: 20px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.agent-create-btn:hover {
  background: var(--accent-hover);
}

.agent-loading {
  color: var(--text-muted);
  font-size: 15px;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  width: 100%;
  max-width: 900px;
}

.agent-card {
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s;
}

.agent-card:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.agent-card-avatar {
  margin-bottom: 12px;
}

.agent-card-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 6px;
  text-align: center;
}

.agent-card-desc {
  font-size: 13px;
  color: var(--text-muted);
  text-align: center;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.agent-detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: overlay-fade-in 0.2s ease;
}

@keyframes overlay-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.agent-detail-modal {
  background: var(--bg-primary);
  border-radius: 24px;
  padding: 28px;
  min-width: 400px;
  max-width: 500px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  animation: modal-pop-in 0.25s ease;
}

@keyframes modal-pop-in {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.agent-detail-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}

.agent-detail-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: 12px;
}

.agent-detail-content {
  margin-bottom: 24px;
}

.agent-detail-section {
  margin-bottom: 16px;
}

.agent-detail-section:last-child {
  margin-bottom: 0;
}

.agent-detail-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.agent-detail-value {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  background: var(--bg-hover);
  padding: 12px 16px;
  border-radius: 12px;
  max-height: 120px;
  overflow-y: auto;
}

.agent-detail-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.agent-detail-btn {
  padding: 10px 28px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  border: none;
  transition: background 0.15s, transform 0.1s;
}

.agent-detail-btn.cancel {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.agent-detail-btn.cancel:hover {
  background: var(--bg-active);
}

.agent-detail-btn.confirm {
  background: #fff;
  color: black;
}

.agent-detail-btn.confirm:hover {
  background: #e0e0e0;
}

.agent-detail-btn:active {
  transform: scale(0.96);
}

.home-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
}

.home-title {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 32px;
}

.home-input-area {
  width: 100%;
  max-width: 680px;
}

.messages-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 0;
  position: relative;
}

.messages-hidden {
  visibility: hidden;
}

.load-more-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  color: var(--text-muted);
  font-size: 13px;
}

.load-more-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid var(--border-color);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.load-more-hint {
  font-size: 12px;
  color: var(--text-muted);
  opacity: 0.6;
}

.message-row {
  display: flex;
  gap: 16px;
  padding: 12px 24px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.message-row.user {
  justify-content: flex-end;
}

.message-row.assistant {
  background: var(--bg-message-ai);
}

.user-bubble-wrap {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.user-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  justify-content: flex-end;
}

.user-attachment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  max-width: 220px;
}

.user-attachment-card:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
}

.user-attachment-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.user-attachment-info {
  min-width: 0;
}

.user-attachment-name {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
}

.user-attachment-size {
  font-size: 11px;
  color: var(--text-muted);
}

.user-bubble {
  max-width: 100%;
  padding: 10px 16px;
  border-radius: 18px 18px 4px 18px;
  background: #2a2a2a;
  color: #e0e0e0;
  font-size: 15px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-text {
  font-size: 16px;
  line-height: 1.7;
  color: var(--text-secondary);
  word-break: break-word;
}

.cursor-blink {
  color: var(--text-muted);
  animation: cursor-blink 1s step-end infinite;
  font-weight: 300;
}

@keyframes cursor-blink {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

.thinking-dots {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0;
}

.thinking-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-muted);
  animation: dot-bounce 1.4s ease-in-out infinite;
}

.thinking-dots .dot:nth-child(1) {
  animation-delay: 0s;
}

.thinking-dots .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.thinking-dots .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes dot-bounce {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1.2);
  }
}

.markdown-body :deep(p) {
  margin: 0 0 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(strong) {
  color: var(--text-primary);
  font-weight: 600;
}

.markdown-body :deep(em) {
  font-style: italic;
}

.markdown-body :deep(a) {
  color: var(--accent);
  text-decoration: underline;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--border-color);
  padding-left: 12px;
  margin: 8px 0;
  color: var(--text-muted);
}

.markdown-body :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 12px 0;
}

.markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
  width: 100%;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--border-color);
  padding: 6px 12px;
  text-align: left;
}

.markdown-body :deep(th) {
  background: var(--bg-hover);
  font-weight: 600;
}

.markdown-body :deep(.code-block) {
  background: #0d0d0d;
  border-radius: 8px;
  margin: 8px 0;
  overflow: hidden;
}

.markdown-body :deep(.code-header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  background: #1a1a1a;
  border-bottom: 1px solid #2a2a2a;
}

.markdown-body :deep(.code-lang) {
  font-size: 12px;
  color: var(--text-muted);
}

.markdown-body :deep(.code-copy) {
  font-size: 12px;
  color: var(--text-muted);
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: background 0.15s, color 0.15s;
}

.markdown-body :deep(.code-copy:hover) {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.markdown-body :deep(.code-block code) {
  display: block;
  padding: 12px 16px;
  overflow-x: auto;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.markdown-body :deep(code:not(.hljs)) {
  background: var(--bg-hover);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  color: var(--text-primary);
  margin: 16px 0 8px;
  font-weight: 600;
}

.markdown-body :deep(h1) { font-size: 20px; }
.markdown-body :deep(h2) { font-size: 18px; }
.markdown-body :deep(h3) { font-size: 16px; }
.markdown-body :deep(h4) { font-size: 15px; }

.input-area {
  padding: 16px 24px 20px;
  max-width: 800px;
  margin: 0 auto;
  width: 100%;
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 12px 14px;
  transition: border-color 0.15s;
}

.input-wrapper:focus-within {
  border-color: var(--text-muted);
}

.pending-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
  padding: 2px 0;
}

.pending-attachment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: rgba(0, 122, 255, 0.08);
  border: 1px solid rgba(0, 122, 255, 0.2);
  border-radius: 12px;
  max-width: 200px;
  transition: all 0.2s;
}

.pending-attachment-card:hover {
  background: rgba(0, 122, 255, 0.12);
  border-color: rgba(0, 122, 255, 0.35);
}

.pending-attachment-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.pending-attachment-info {
  min-width: 0;
  flex: 1;
}

.pending-attachment-name {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.pending-attachment-size {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 1px;
}

.pending-attachment-remove {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-muted);
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s;
  padding: 0;
}

.pending-attachment-remove:hover {
  background: rgba(255, 80, 80, 0.2);
  color: #ff6b6b;
}

.input-wrapper textarea {
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 16px;
  line-height: 1.5;
  resize: none;
  padding: 4px 4px;
  min-height: 28px;
  max-height: 33vh;
  overflow-y: auto;
}

.input-wrapper textarea::placeholder {
  color: var(--text-muted);
}

.input-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 6px;
}

.input-bottom-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.attach-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  -webkit-backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  color: var(--text-muted);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    inset 0 1px 1px rgba(255, 255, 255, 0.2),
    inset 0 -1px 1px rgba(255, 255, 255, 0.05),
    inset 1px 0 1px rgba(255, 255, 255, 0.08),
    inset -1px 0 1px rgba(255, 255, 255, 0.08);
}

.attach-btn:hover:not(:disabled) {
  border-color: rgba(255, 255, 255, 0.35);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.06);
  box-shadow:
    inset 0 1px 1px rgba(255, 255, 255, 0.25),
    inset 0 -1px 1px rgba(255, 255, 255, 0.08),
    inset 1px 0 1px rgba(255, 255, 255, 0.12),
    inset -1px 0 1px rgba(255, 255, 255, 0.12);
}

.attach-btn:active:not(:disabled) {
  transform: scale(1.06);
}

.attach-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.web-search-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  -webkit-backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  color: var(--text-muted);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    inset 0 1px 1px rgba(255, 255, 255, 0.2),
    inset 0 -1px 1px rgba(255, 255, 255, 0.05),
    inset 1px 0 1px rgba(255, 255, 255, 0.08),
    inset -1px 0 1px rgba(255, 255, 255, 0.08);
}

.web-search-btn:hover {
  border-color: rgba(255, 255, 255, 0.35);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.06);
  box-shadow:
    inset 0 1px 1px rgba(255, 255, 255, 0.25),
    inset 0 -1px 1px rgba(255, 255, 255, 0.08),
    inset 1px 0 1px rgba(255, 255, 255, 0.12),
    inset -1px 0 1px rgba(255, 255, 255, 0.12);
}

.web-search-btn:active {
  transform: scale(1.06);
}

.web-search-btn.active {
  border-color: rgba(0, 122, 255, 0.4);
  color: var(--accent);
  background: rgba(0, 122, 255, 0.06);
  backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  -webkit-backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  box-shadow:
    inset 0 1px 1px rgba(0, 122, 255, 0.2),
    inset 0 -1px 1px rgba(255, 255, 255, 0.05),
    inset 1px 0 1px rgba(0, 122, 255, 0.1),
    inset -1px 0 1px rgba(0, 122, 255, 0.1);
}

.send-btn {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(0, 122, 255, 0.15);
  backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  -webkit-backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 1px 4px rgba(0, 0, 0, 0.08),
    0 4px 16px rgba(0, 122, 255, 0.2),
    inset 0 1px 1px rgba(255, 255, 255, 0.25),
    inset 0 -1px 1px rgba(255, 255, 255, 0.06),
    inset 1px 0 1px rgba(255, 255, 255, 0.1),
    inset -1px 0 1px rgba(255, 255, 255, 0.1);
}

.send-btn:hover:not(:disabled) {
  background: rgba(0, 122, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow:
    0 1px 4px rgba(0, 0, 0, 0.08),
    0 6px 24px rgba(0, 122, 255, 0.3),
    inset 0 1px 1px rgba(255, 255, 255, 0.3),
    inset 0 -1px 1px rgba(255, 255, 255, 0.1),
    inset 1px 0 1px rgba(255, 255, 255, 0.15),
    inset -1px 0 1px rgba(255, 255, 255, 0.15);
  transform: scale(1.08);
}

.send-btn:active:not(:disabled) {
  transform: scale(1.15);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.input-hint {
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

.scroll-bottom-btn {
  position: absolute;
  bottom: 158px;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  -webkit-backdrop-filter: blur(40px) saturate(220%) brightness(1.15);
  color: var(--text-secondary);
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 1px 4px rgba(0, 0, 0, 0.08),
    0 8px 32px rgba(0, 0, 0, 0.14),
    inset 0 2px 2px rgba(255, 255, 255, 0.35),
    inset 0 -1px 1px rgba(255, 255, 255, 0.08),
    inset 1px 0 1px rgba(255, 255, 255, 0.12),
    inset -1px 0 1px rgba(255, 255, 255, 0.12);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 10;
}

.scroll-bottom-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.4);
  box-shadow:
    0 1px 4px rgba(0, 0, 0, 0.08),
    0 12px 40px rgba(0, 0, 0, 0.2),
    inset 0 2px 2px rgba(255, 255, 255, 0.4),
    inset 0 -1px 1px rgba(255, 255, 255, 0.12),
    inset 1px 0 1px rgba(255, 255, 255, 0.15),
    inset -1px 0 1px rgba(255, 255, 255, 0.15);
  transform: translateX(-50%) scale(1.1);
}

.scroll-bottom-btn:active {
  transform: translateX(-50%) scale(0.92);
}

.scroll-btn-fade-enter-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.scroll-btn-fade-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.scroll-btn-fade-enter-from {
  opacity: 0;
  transform: translateX(-50%) translateY(12px) scale(0.8);
}

.scroll-btn-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px) scale(0.8);
}

.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: overlay-fade-in 0.2s ease;
}

.preview-modal {
  background: var(--bg-primary);
  border-radius: 20px;
  width: 90%;
  max-width: 700px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.5);
  animation: modal-pop-in 0.25s ease;
  overflow: hidden;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.preview-file-icon {
  font-size: 18px;
}

.preview-close {
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 22px;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  transition: color 0.15s;
}

.preview-close:hover {
  color: var(--text-primary);
}

.preview-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.preview-loading {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 0;
}

.preview-content {
  min-height: 100px;
}

.preview-content pre {
  margin: 0;
  padding: 16px;
  background: var(--bg-hover);
  border-radius: 12px;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  overflow-x: auto;
}

.preview-empty {
  text-align: center;
  color: var(--text-muted);
  padding: 40px 0;
}
</style>
