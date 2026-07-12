import request from './request'
import type { ApiResult, LoginRequest, LoginResponse } from '@/types'

export function login(data: LoginRequest): Promise<ApiResult<LoginResponse>> {
  return request.post('/auth/login', data).then(r => r.data)
}

export function refreshToken(refreshToken: string): Promise<ApiResult<{ accessToken: string; refreshToken: string }>> {
  return request.post('/auth/refresh', { refreshToken }).then(r => r.data)
}

export function logout(): Promise<ApiResult<null>> {
  return request.post('/auth/logout').then(r => r.data)
}