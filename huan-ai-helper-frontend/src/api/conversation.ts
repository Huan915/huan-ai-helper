import request from './request'
import type {
  Result,
  ChatConversation,
  ChatConversationCreateRequest,
  ChatMessage,
  ChatMessageSendRequest,
} from './types'
export function createConversation(data: ChatConversationCreateRequest) {
  return request.post<Result<ChatConversation>>('/conversation/create', data)
}

export function getConversation(id: number) {
  return request.get<Result<ChatConversation>>(`/conversation/${id}`)
}

export function listConversations(userId: number) {
  return request.get<Result<ChatConversation[]>>('/conversation/list', { params: { userId } })
}

export function deleteConversation(id: number) {
  return request.delete<Result<boolean>>(`/conversation/${id}`)
}

export function renameConversation(id: number, title: string) {
  return request.put<Result<ChatConversation>>(`/conversation/${id}/rename`, { title })
}

export function listMessages(conversationId: number) {
  return request.get<Result<ChatMessage[]>>(`/conversation/${conversationId}/messages`)
}

export function listMessagesPage(conversationId: number, beforeId?: number, limit: number = 20) {
  const params: Record<string, string | number> = { limit }
  if (beforeId !== undefined) {
    params.beforeId = beforeId
  }
  return request.get<Result<ChatMessage[]>>(`/conversation/${conversationId}/messages/page`, { params })
}

export async function sendMessageStream(
  conversationId: number,
  content: string,
  onChunk: (chunk: string) => void,
  onError: (error: string) => void,
  onDone: () => void,
  gain?: boolean,
  attachments?: ChatMessageSendRequest['attachments'],
) {
  const body: ChatMessageSendRequest = { content, gain }
  if (attachments && attachments.length > 0) {
    body.attachments = attachments
  }
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  const token = localStorage.getItem('token')
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const response = await fetch(`/api/conversation/${conversationId}/message`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    onError(`请求失败: ${response.status}`)
    return
  }

  const reader = response.body!.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''

    for (const part of parts) {
      if (!part.trim()) continue
      let eventName = ''
      let dataLines: string[] = []
      for (const line of part.split('\n')) {
        if (line.startsWith('event:')) eventName = line.slice(6).trim()
        if (line.startsWith('data:')) {
          let val = line.slice(5)
          if (val.startsWith(' ')) val = val.slice(1)
          dataLines.push(val)
        }
      }
      const data = dataLines.join('\n')

      if (eventName === 'error') {
        onError(data || '未知错误')
        return
      } else if (eventName === 'done') {
        onDone()
        return
      } else {
        onChunk(data)
      }
    }
  }

  onDone()
}
