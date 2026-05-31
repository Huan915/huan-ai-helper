import request from './request'
import type {
  Result,
  User,
  UserLoginRequest,
  UserRegisterRequest,
  UserUpdateRequest,
  UserChangePasswordRequest,
} from './types'

export function login(data: UserLoginRequest) {
  return request.post<Result<User>>('/user/login', data)
}

export function register(data: UserRegisterRequest) {
  return request.post<Result<User>>('/user/register', data)
}

export function getUserInfo(userId: number) {
  return request.get<Result<User>>(`/user/${userId}`)
}

export function updateUser(userId: number, data: UserUpdateRequest) {
  return request.put<Result<User>>(`/user/${userId}`, data)
}

export function changePassword(userId: number, data: UserChangePasswordRequest) {
  return request.put<Result<boolean>>(`/user/${userId}/password`, data)
}

export function uploadAvatar(userId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<Result<string>>(`/user/${userId}/avatar`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
