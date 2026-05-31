import request from './request'
import type { Result, RAGAgent } from './types'

export function listRagAgents() {
  return request.get<Result<RAGAgent[]>>('/admin/rag-agent/list')
}

export function getRagAgent(agentId: string) {
  return request.get<Result<RAGAgent>>(`/admin/rag-agent/${agentId}`)
}
