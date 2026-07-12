import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types'
import { login as loginApi } from '@/api/auth'
import { setToken, clearToken } from '@/api/request'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref<UserInfo | null>(null)

  const isLoggedIn = computed(() => !!userInfo.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ROLE_ADMIN')
  const isStaff = computed(() => userInfo.value?.role === 'ROLE_STAFF')
  const isMember = computed(() => userInfo.value?.role === 'ROLE_MEMBER')

  // 从 localStorage 恢复
  function loadFromStorage() {
    const stored = localStorage.getItem('hotel_user_info')
    if (stored) {
      try {
        userInfo.value = JSON.parse(stored)
      } catch {
        clearToken()
      }
    }
  }

  async function login(username: string, password: string) {
    const res = await loginApi({ username, password })
    if (res.code === 200 && res.data) {
      const info: UserInfo = {
        userId: res.data.userId,
        name: res.data.name,
        role: res.data.role,
        phone: res.data.phone,
        accessToken: res.data.accessToken,
        refreshToken: res.data.refreshToken
      }
      setToken(info.accessToken)
      localStorage.setItem('hotel_user_info', JSON.stringify(info))
      userInfo.value = info
      return info
    }
    throw new Error(res.message || '登录失败')
  }

  function logout() {
    clearToken()
    userInfo.value = null
  }

  return { userInfo, isLoggedIn, isAdmin, isStaff, isMember, loadFromStorage, login, logout }
})