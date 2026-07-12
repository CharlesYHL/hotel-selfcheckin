import request from './request'
import type { ApiResult, RoomType, RoomDTO } from '@/types'

export function getRoomTypes(hotelId: string): Promise<ApiResult<RoomType[]>> {
  return request.get(`/room/types/${hotelId}`).then(r => r.data)
}

export function getRoomType(hotelId: string, roomTypeId: string): Promise<ApiResult<RoomType>> {
  return request.get(`/room/types/${hotelId}/${roomTypeId}`).then(r => r.data)
}

export function getRoomsByType(hotelId: string, roomTypeId: string): Promise<ApiResult<RoomDTO[]>> {
  return request.get(`/room/list/${hotelId}/${roomTypeId}`).then(r => r.data)
}

export function getRoomsByStatus(hotelId: string, status: number): Promise<ApiResult<RoomDTO[]>> {
  return request.get(`/room/status/${hotelId}/${status}`).then(r => r.data)
}

export function getRoom(roomId: string): Promise<ApiResult<RoomDTO>> {
  return request.get(`/room/${roomId}`).then(r => r.data)
}

export function getInventory(hotelId: string, roomTypeId: string): Promise<ApiResult<number>> {
  return request.get(`/room/inventory/${hotelId}/${roomTypeId}`).then(r => r.data)
}