<template>
  <div class="page-container">
    <div class="page-header">管理后台</div>
    <div class="page-body">
      <van-grid :column-num="3" :border="false">
        <van-grid-item v-for="item in menuItems" :key="item.path" :icon="item.icon" :text="item.name"
          @click="router.push(item.path)" />
      </van-grid>

      <van-cell-group inset style="margin-top:16px">
        <van-cell title="酒店名称" value="杭州西湖希尔顿酒店" />
        <van-cell title="今日入住" value="8 间" />
        <van-cell title="今日退房" value="3 间" />
        <van-cell title="空房率" value="65%" />
        <van-cell title="当前用户" :value="auth.userInfo?.name || '--'" />
        <van-cell title="角色" :value="auth.userInfo?.role === 'ROLE_ADMIN' ? '管理员' : '前台' " />
      </van-cell-group>

      <div style="margin:24px 16px">
        <van-button round block type="danger" @click="doLogout">退出登录</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const menuItems = [
  { name: '订单管理', path: '/admin/orders', icon: 'orders-o' },
  { name: '房间管理', path: '/admin/rooms', icon: 'home-o' },
  { name: '入住管理', path: '/admin/checkins', icon: 'logistics' },
  { name: '会员管理', path: '/admin/members', icon: 'friends-o' }
]

function doLogout() {
  auth.logout()
  router.push('/login')
}
</script>