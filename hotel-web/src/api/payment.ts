import request from './request'
import type { ApiResult, PaymentResponse } from '@/types'

export function createPayment(data: {
  orderId: string
  orderNo: string
  hotelId: string
  memberId?: string
  amount: number
  paymentType?: number
  payChannel?: string
  businessType?: string
}): Promise<ApiResult<PaymentResponse>> {
  return request.post('/pay/create', data).then(r => r.data)
}

export function callbackPayment(data: {
  paymentNo: string
  tradeNo: string
  status: number
  message?: string
}): Promise<ApiResult<PaymentResponse>> {
  return request.post('/pay/callback', data).then(r => r.data)
}

export function getPayment(paymentId: string): Promise<ApiResult<PaymentResponse>> {
  return request.get(`/pay/query/${paymentId}`).then(r => r.data)
}

export function getPaymentByOrder(orderId: string): Promise<ApiResult<PaymentResponse>> {
  return request.get(`/pay/queryByOrder/${orderId}`).then(r => r.data)
}