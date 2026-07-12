import request from './request'
import type { ApiResult, OrderCreateRequest, OrderResponse } from '@/types'

export function createOrder(data: OrderCreateRequest): Promise<ApiResult<OrderResponse>> {
  return request.post('/order/create', data).then(r => r.data)
}

export function getOrder(orderId: string): Promise<ApiResult<OrderResponse>> {
  return request.get(`/order/query/${orderId}`).then(r => r.data)
}

export function cancelOrder(orderId: string): Promise<ApiResult<OrderResponse>> {
  return request.post(`/order/cancel/${orderId}`).then(r => r.data)
}