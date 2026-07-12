<template>
  <div class="page-container" style="display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#1989fa,#07c160)">
    <div style="width:90%;max-width:360px;background:white;border-radius:12px;padding:32px 24px;box-shadow:0 4px 20px rgba(0,0,0,0.15)">
      <h2 style="text-align:center;margin-bottom:24px;font-size:22px">酒店自助入住系统</h2>
      <van-form @submit="onSubmit">
        <van-cell-group inset>
          <van-field v-model="username" name="username" label="用户名" placeholder="请输入用户名"
            :rules="[{ required: true, message: '请输入用户名' }]" />
          <van-field v-model="password" type="password" name="password" label="密码" placeholder="请输入密码"
            :rules="[{ required: true, message: '请输入密码' }]" />
        </van-cell-group>
        <div style="margin:24px 16px">
          <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
        </div>
      </van-form>
      <div style="text-align:center;color:#999;font-size:12px;margin-top:8px">
        测试账号: admin/admin123 | member/member123
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    const user = await auth.login(username.value, password.value)
    showToast(`欢迎回来，${user.name}`)
    if (user.role === 'ROLE_ADMIN' || user.role === 'ROLE_STAFF') {
      router.push('/admin/dashboard')
    } else {
      router.push('/guest/home')
    }
  } catch (e: any) {
    showToast(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>