import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { getSignHeaders } from '@/utils/sign'
import type { ApiResult } from '@/types'

const TOKEN_KEY = 'hotel_access_token'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 添加 JWT Token
    const token = localStorage.getItem(TOKEN_KEY)
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加请求签名（API 路径需要去掉 baseURL 前缀）
    if (config.url) {
      const path = '/api/' + config.url.replace(/^\//, '')
      const signHeaders = getSignHeaders(path)
      if (config.headers) {
        Object.assign(config.headers, signHeaders)
      }
    }

    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const data = response.data
    if (data.code === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('hotel_user_info')
      window.location.href = '/login'
      return Promise.reject(new Error(data.message || '认证失败'))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('hotel_user_info')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem('hotel_user_info')
}

export default request