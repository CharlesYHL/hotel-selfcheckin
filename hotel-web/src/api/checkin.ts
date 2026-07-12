import request from './request'
import type { ApiResult, CheckInRequest, CheckInResponse } from '@/types'

export function checkIn(data: CheckInRequest): Promise<ApiResult<CheckInResponse>> {
  return request.post('/checkin/checkin', data).then(r => r.data)
}

export function getCheckIn(checkinId: string): Promise<ApiResult<CheckInResponse>> {
  return request.get(`/checkin/query/${checkinId}`).then(r => r.data)
}

export function getCheckInByOrder(orderId: string): Promise<ApiResult<CheckInResponse>> {
  return request.get(`/checkin/query/order/${orderId}`).then(r => r.data)
}

export function getGuests(checkinId: string): Promise<ApiResult<Record<string, unknown>[]>> {
  return request.get(`/checkin/guests/${checkinId}`).then(r => r.data)
}

export function checkout(checkinId: string, checkoutType: number = 1): Promise<ApiResult<unknown>> {
  return request.post(`/checkin/checkout/${checkinId}?checkoutType=${checkoutType}`).then(r => r.data)
}

export function extendStay(data: { checkinId: string; extendDays: number }): Promise<ApiResult<CheckInResponse>> {
  return request.post('/checkin/extend', data).then(r => r.data)
}