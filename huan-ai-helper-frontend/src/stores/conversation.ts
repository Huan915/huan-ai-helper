import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useRouter } from 'vue-router'
import type { ChatConversation, ChatMessage, AttachmentInfo } from '@/api/types'
import * as conversationApi from '@/api/conversation'
import { useUserStore } from './user'

export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref<ChatConversation[]>([])
  const currentConversation = ref<ChatConversation | null>(null)
  const messages = ref<ChatMessage[]>([])
  const sending = ref(false)
  const hasMoreMessages = ref(true)
  const loadingMore = ref(false)

  const router = useRouter()

  function syncUrl() {
    const id = currentConversation.value?.id
    const currentQuery = { ...router.currentRoute.value.query }
    if (id) {
      router.replace({ path: '/chat', query: { ...currentQuery, c: String(id), tab: undefined } })
    } else {
      router.replace({ path: '/chat', query: { ...currentQuery, c: undefined } })
    }
  }

  async function fetchConversations() {
    const userStore = useUserStore()
    if (!userStore.userInfo?.id) return
    const res = await conversationApi.listConversations(userStore.userInfo.id)
    conversations.value = res.data.data
  }

  async function createConversation(title?: string, systemPrompt?: string, conversationType?: string) {
    const userStore = useUserStore()
    const res = await conversationApi.createConversation({
      userId: userStore.userInfo?.id,
      title: title || '新会话',
      systemPrompt,
      conversationType: conversationType || 'NORMAL',
    })
    const conversation = res.data.data
    conversations.value.unshift(conversation)
    currentConversation.value = conversation
    messages.value = []
    hasMoreMessages.value = false
    syncUrl()
    return conversation
  }

  async function selectConversation(id: number) {
    const conv = conversations.value.find((c) => c.id === id)
    if (!conv) return
    currentConversation.value = conv
    syncUrl()
    const res = await conversationApi.listMessagesPage(id, undefined, 10)
    messages.value = res.data.data
    hasMoreMessages.value = messages.value.length >= 10
  }

  async function loadMoreMessages(): Promise<number> {
    if (!currentConversation.value || loadingMore.value || !hasMoreMessages.value) return 0
    if (messages.value.length === 0) return 0

    const firstMsgId = messages.value[0]?.id
    if (!firstMsgId || firstMsgId < 0) return 0

    loadingMore.value = true
    try {
      const res = await conversationApi.listMessagesPage(
        currentConversation.value.id,
        firstMsgId,
        20,
      )
      const olderMessages = res.data.data
      if (olderMessages.length === 0) {
        hasMoreMessages.value = false
        return 0
      }
      messages.value = [...olderMessages, ...messages.value]
      hasMoreMessages.value = olderMessages.length >= 10
      return olderMessages.length
    } finally {
      loadingMore.value = false
    }
  }

  async function deleteConversation(id: number) {
    await conversationApi.deleteConversation(id)
    conversations.value = conversations.value.filter((c) => c.id !== id)
    if (currentConversation.value?.id === id) {
      currentConversation.value = null
      messages.value = []
      syncUrl()
    }
  }

  async function renameConversation(id: number, title: string) {
    const res = await conversationApi.renameConversation(id, title)
    const updated = res.data.data
    const idx = conversations.value.findIndex((c) => c.id === id)
    if (idx > -1) {
      conversations.value[idx] = updated
    }
    if (currentConversation.value?.id === id) {
      currentConversation.value = updated
    }
  }

  async function sendMessage(content: string, gain?: boolean, attachments?: AttachmentInfo[]) {
    if (!currentConversation.value) {
      const conv = await createConversation()
      if (!conv) return
    }
    const convId = currentConversation.value!.id
    sending.value = true

    const attachmentsJson = attachments && attachments.length > 0 ? JSON.stringify(attachments.map(a => ({ name: a.name, type: a.type, url: a.url, size: a.size }))) : null

    const tempUserMsg: ChatMessage = {
      id: -Date.now(),
      conversationId: convId,
      parentMessageId: null,
      role: 'user',
      content,
      modelName: null,
      inputTokens: null,
      outputTokens: null,
      totalTokens: null,
      finishReason: null,
      toolCalls: null,
      toolCallId: null,
      contextIncluded: null,
      seqNo: messages.value.length,
      attachments: attachmentsJson,
      createdAt: new Date().toISOString(),
    }
    messages.value.push(tempUserMsg)

    const aiMsgId = -Date.now() - 1
    const tempAiMsg: ChatMessage = {
      id: aiMsgId,
      conversationId: convId,
      parentMessageId: null,
      role: 'assistant',
      content: '',
      modelName: null,
      inputTokens: null,
      outputTokens: null,
      totalTokens: null,
      finishReason: null,
      toolCalls: null,
      toolCallId: null,
      contextIncluded: null,
      seqNo: messages.value.length,
      attachments: null,
      createdAt: new Date().toISOString(),
    }
    messages.value.push(tempAiMsg)

    await conversationApi.sendMessageStream(
      convId,
      content,
      (chunk) => {
        const aiMsg = messages.value.find((m) => m.id === aiMsgId)
        if (aiMsg) {
          aiMsg.content += chunk
        }
      },
      (error) => {
        const aiMsg = messages.value.find((m) => m.id === aiMsgId)
        if (aiMsg && !aiMsg.content) {
          aiMsg.content = error
        }
        sending.value = false
      },
      () => {
        sending.value = false
      },
      gain,
      attachments,
    )
  }

  function clearCurrent() {
    currentConversation.value = null
    messages.value = []
    hasMoreMessages.value = true
    syncUrl()
  }

  return {
    conversations,
    currentConversation,
    messages,
    sending,
    hasMoreMessages,
    loadingMore,
    fetchConversations,
    createConversation,
    selectConversation,
    loadMoreMessages,
    deleteConversation,
    renameConversation,
    sendMessage,
    clearCurrent,
  }
})
