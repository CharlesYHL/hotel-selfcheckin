<template>
  <div class="page-container">
    <div class="page-header">支付订单</div>
    <div class="page-body">
      <van-cell-group inset>
        <van-cell title="订单编号" :value="orderData?.orderNo || '--'" />
        <van-cell title="订单金额" value="¥299.00" />
      </van-cell-group>

      <div style="text-align:center;margin:32px 0">
        <div style="font-size:40px;font-weight:700;color:#ee0a24">¥299.00</div>
        <div style="color:#999;margin-top:8px">微信支付</div>
      </div>

      <div style="text-align:center">
        <van-loading v-if="paying" size="24px" />
        <van-button v-else round block type="primary" @click="doPay" style="margin:0 16px">
          立即支付 ¥299.00
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getOrder } from '@/api/order'
import { createPayment, callbackPayment } from '@/api/payment'
import type { OrderResponse } from '@/types'

const route = useRoute()
const router = useRouter()
const orderData = ref<OrderResponse | null>(null)
const paying = ref(false)

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

async function doPay() {
  if (!orderData.value) return
  paying.value = true
  try {
    // 创建支付
    const payRes = await createPayment({
      orderId: orderData.value.orderId,
      orderNo: orderData.value.orderNo,
      hotelId: orderData.value.hotelId,
      amount: 299,
      payChannel: 'WX_PAY',
      paymentType: 1,
      businessType: 'ORDER'
    })
    if (payRes.code === 200 && payRes.data) {
      // 模拟支付回调
      const cbRes = await callbackPayment({
        paymentNo: payRes.data.paymentNo,
        tradeNo: 'MOCK' + Date.now(),
        status: 3
      })
      if (cbRes.code === 200) {
        showToast('支付成功')
        router.push(`/guest/checkin/${orderData.value.orderId}`)
      } else {
        showToast('支付回调失败')
      }
    } else {
      showToast(payRes.message || '支付失败')
    }
  } catch (e: any) {
    showToast(e.message || '支付失败')
  } finally {
    paying.value = false
  }
}
</script>