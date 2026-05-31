export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface User {
  id: number
  userName: string
  avatarUrl: string | null
  email: string | null
  phone: string | null
  status: number
  lastLoginTime: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface UserLoginRequest {
  userName: string
  password: string
}

export interface UserRegisterRequest {
  userName: string
  password: string
  email?: string
  phone?: string
}

export interface UserUpdateRequest {
  userName?: string
  avatarUrl?: string
  email?: string
  phone?: string
}

export interface UserChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

export interface ChatConversation {
  id: number
  userId: number
  title: string
  systemPrompt: string | null
  conversationType: string
  modelName: string
  contextWindowSize: number
  contextStrategy: string
  temperature: number
  topP: number
  status: number
  totalTokens: number
  totalMessages: number
  lastMessageTime: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface ChatConversationCreateRequest {
  userId?: number
  title?: string
  systemPrompt?: string
  conversationType?: string
  modelName?: string
  contextWindowSize?: number
  contextStrategy?: string
  temperature?: number
  topP?: number
}

export interface AttachmentInfo {
  name: string
  type: string
  url: string
  size: number
  parsedContent?: string
}

export interface ChatMessage {
  id: number
  conversationId: number
  parentMessageId: number | null
  role: string
  content: string
  modelName: string | null
  inputTokens: number | null
  outputTokens: number | null
  totalTokens: number | null
  finishReason: string | null
  toolCalls: string | null
  toolCallId: string | null
  contextIncluded: boolean | null
  seqNo: number
  attachments: string | null
  createdAt: string | null
}

export interface ChatMessageSendRequest {
  content: string
  gain?: boolean
  deepThinking?: boolean
  attachments?: AttachmentInfo[]
}

export interface RAGAgent {
  id: number
  agentId: string
  agentName: string
  description: string | null
  avatarUrl: string | null
  systemPrompt: string
  knowledgeBaseId: string | null
  maxSteps: number | null
  status: number
  createdAt: string | null
  updatedAt: string | null
}
