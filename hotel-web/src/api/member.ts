import request from './request'
import type { ApiResult, MemberResponse } from '@/types'

export function getMember(memberId: string): Promise<ApiResult<MemberResponse>> {
  return request.get(`/member/query/${memberId}`).then(r => r.data)
}

export function getMemberByPhone(phone: string): Promise<ApiResult<MemberResponse>> {
  return request.get(`/member/query/phone/${phone}`).then(r => r.data)
}

export function getMemberLevels(): Promise<ApiResult<Record<string, unknown>[]>> {
  return request.get('/member/levels').then(r => r.data)
}

export function getPointsLogs(memberId: string, limit: number = 50): Promise<ApiResult<Record<string, unknown>[]>> {
  return request.get(`/member/points/logs/${memberId}?limit=${limit}`).then(r => r.data)
}