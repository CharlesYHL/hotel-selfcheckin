<template>
  <div class="page-container">
    <div class="page-header">办理入住</div>
    <div class="page-body">
      <van-cell-group inset>
        <van-cell title="订单编号" :value="orderData?.orderNo || '--'" />
        <van-cell title="房型" :value="orderData?.roomTypeName || '--'" />
        <van-cell title="入住日期" :value="orderData?.checkInDate || '--'" />
        <van-cell title="退房日期" :value="orderData?.checkOutDate || '--'" />
      </van-cell-group>

      <van-cell-group inset style="margin-top:16px">
        <van-field v-model="guestName" label="姓名" placeholder="请输入姓名" />
        <van-field v-model="idCardNo" label="身份证号" placeholder="请输入身份证号" />
        <van-field v-model="guestPhone" label="手机号" placeholder="请输入手机号" type="tel" />
      </van-cell-group>

      <div style="margin:24px 16px">
        <van-button round block type="primary" :loading="submitting" @click="doCheckIn">确认入住</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getOrder } from '@/api/order'
import { checkIn } from '@/api/checkin'
import type { OrderResponse } from '@/types'

const route = useRoute()
const router = useRouter()
const orderData = ref<OrderResponse | null>(null)
const guestName = ref('')
const idCardNo = ref('')
const guestPhone = ref('')
const submitting = ref(false)

onMounted(async () => {
  try {
    const res = await getOrder(route.params.orderId as string)
    if (res.code === 200 && res.data) {
      orderData.value = res.data
    }
  } catch (e) {
    showToast('加载订单失败')
  }
})

async function doCheckIn() {
  if (!guestName.value || !idCardNo.value) {
    showToast('请填写入住人信息')
    return
  }
  if (!orderData.value) return
  submitting.value = true
  try {
    // 先获取可用房间
    const roomRes = await import('@/api/room').then(m => m.getRoomsByType(orderData.value!.hotelId, orderData.value!.roomTypeId))
    if (roomRes.code !== 200 || !roomRes.data || roomRes.data.length === 0) {
      showToast('暂无可用房间')
      return
    }
    const room = roomRes.data[0]

    const res = await checkIn({
      orderId: orderData.value.orderId,
      hotelId: orderData.value.hotelId,
      roomId: room.roomId,
      guests: [{
        guestName: guestName.value,
        guestType: 1,
        idCardType: 1,
        idCardNo: idCardNo.value,
        phone: guestPhone.value
      }]
    })
    if (res.code === 200 && res.data) {
      showToast('入住成功')
      router.push(`/guest/card/${res.data.checkinId}`)
    } else {
      showToast(res.message || '入住失败')
    }
  } catch (e: any) {
    showToast(e.message || '入住失败')
  } finally {
    submitting.value = false
  }
}
</script>