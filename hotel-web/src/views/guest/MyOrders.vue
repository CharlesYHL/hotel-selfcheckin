<template>
  <div class="page-container">
    <div class="page-header">我的订单</div>
    <div class="page-body">
      <van-empty v-if="orders.length === 0" description="暂无订单" />
      <div v-for="o in orders" :key="o.orderId" class="card-item" @click="viewOrder(o)">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
          <span style="font-weight:600">{{ o.roomTypeName }}</span>
          <span :class="'status-tag status-' + getStatusClass(o.orderStatus)">
            {{ getStatusText(o.orderStatus) }}
          </span>
        </div>
        <div style="font-size:12px;color:#999">
          {{ o.checkInDate }} ~ {{ o.checkOutDate }} · {{ o.nights }}晚
        </div>
        <div style="font-size:12px;color:#999;margin-top:4px">
          订单号: {{ o.orderNo }}
        </div>
        <div style="margin-top:8px;text-align:right">
          <van-button v-if="o.orderStatus === 1" size="small" type="primary" @click.stop="goPay(o)">去支付</van-button>
          <van-button v-if="o.orderStatus === 2" size="small" type="success" @click.stop="goCheckIn(o)">办理入住</van-button>
          <van-button v-if="o.orderStatus === 4" size="small" type="warning" @click.stop="goCard(o)">查看房卡</van-button>
        </div>
      </div>
    </div>

    <div class="tab-bar">
      <router-link to="/guest/home" class="tab-bar-item">
        <span class="icon">🏠</span>选房
      </router-link>
      <router-link to="/guest/orders" class="tab-bar-item active">
        <span class="icon">📋</span>我的订单
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getOrder } from '@/api/order'
import type { OrderResponse } from '@/types'

const router = useRouter()
const orders = ref<OrderResponse[]>([])

// 模拟从 localStorage 获取订单列表
const savedOrderIds = ref<string[]>([])

function getStatusText(s: number) {
  const map: Record<number, string> = { 1: '待支付', 2: '已支付', 3: '已排房', 4: '已入住', 5: '已完成', 6: '已取消', 7: '已退款' }
  return map[s] || '未知'
}

function getStatusClass(s: number) {
  const map: Record<number, string> = { 1: 'pending', 2: 'paid', 3: 'assigned', 4: 'checked-in', 5: 'completed', 6: 'cancelled', 7: 'cancelled' }
  return map[s] || 'pending'
}

function goPay(o: OrderResponse) { router.push(`/guest/payment/${o.orderId}`) }
function goCheckIn(o: OrderResponse) { router.push(`/guest/checkin/${o.orderId}`) }
function goCard(o: OrderResponse) {
  // 根据订单号查询入住信息获取房卡
  router.push(`/guest/card/${o.orderId}`)
}

function viewOrder(o: OrderResponse) {
  showToast(`订单状态: ${getStatusText(o.orderStatus)}`)
}

onMounted(() => {
  // 从 localStorage 读取保存的订单ID
  const stored = localStorage.getItem('hotel_order_ids')
  if (stored) {
    try {
      savedOrderIds.value = JSON.parse(stored)
      savedOrderIds.value.forEach(async (id) => {
        try {
          const res = await getOrder(id)
          if (res.code === 200 && res.data) {
            orders.value.push(res.data)
          }
        } catch (_) {}
      })
    } catch (_) {}
  }
})
</script>