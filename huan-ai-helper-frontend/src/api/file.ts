import request from './request'
import type { Result, AttachmentInfo } from './types'

export function uploadFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<Result<AttachmentInfo>>('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
}
