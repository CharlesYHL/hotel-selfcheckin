import request from './request'
import type { ApiResult, CardResponse } from '@/types'

export function getCard(cardId: string): Promise<ApiResult<CardResponse>> {
  return request.get(`/card/query/${cardId}`).then(r => r.data)
}

export function getCardsByCheckin(checkinId: string): Promise<ApiResult<CardResponse[]>> {
  return request.get(`/card/checkin/${checkinId}`).then(r => r.data)
}

export function createCard(data: {
  checkinId: string
  hotelId: string
  roomId: string
  roomNo: string
  guestId?: string
  checkInTime?: string
  checkOutTime?: string
}): Promise<ApiResult<CardResponse>> {
  return request.post('/card/create', data).then(r => r.data)
}

export function openDoor(cardId: string, device: string = 'APP'): Promise<ApiResult<Record<string, unknown>>> {
  return request.post(`/card/open/${cardId}?device=${device}`).then(r => r.data)
}

export function getCardLogs(cardId: string): Promise<ApiResult<Record<string, unknown>[]>> {
  return request.get(`/card/logs/${cardId}`).then(r => r.data)
}