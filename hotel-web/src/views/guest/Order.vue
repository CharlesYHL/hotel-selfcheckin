<template>
  <div class="page-container">
    <div class="page-header">确认订单</div>
    <div class="page-body">
      <van-cell-group inset>
        <van-cell title="房型" :value="route.query.roomTypeName as string" />
        <van-cell title="入住日期" :value="route.query.checkInDate as string" />
        <van-cell title="退房日期" :value="route.query.checkOutDate as string" />
        <van-cell title="预计费用" value="¥299.00" />
      </van-cell-group>

      <van-cell-group inset style="margin-top:16px">
        <van-field v-model="contactName" label="入住人" placeholder="请输入姓名" />
        <van-field v-model="contactPhone" label="手机号" placeholder="请输入手机号" type="tel" />
        <van-field v-model="specialRequest" label="特殊要求" placeholder="如无烟房、高楼层等" />
      </van-cell-group>

      <div style="margin:24px 16px">
        <van-button round block type="primary" :loading="submitting" @click="submitOrder">提交订单</van-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from 'vant'
import { createOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()
const contactName = ref('')
const contactPhone = ref('')
const specialRequest = ref('')
const submitting = ref(false)

async function submitOrder() {
  if (!contactName.value || !contactPhone.value) {
    showToast('请填写入住人信息')
    return
  }
  submitting.value = true
  try {
    const res = await createOrder({
      hotelId: route.query.hotelId as string,
      roomTypeId: route.query.roomTypeId as string,
      checkInDate: route.query.checkInDate as string,
      checkOutDate: route.query.checkOutDate as string,
      contactName: contactName.value,
      contactPhone: contactPhone.value,
      specialRequest: specialRequest.value,
      orderAmount: 299,
      sourceChannel: 'H5'
    })
    if (res.code === 200 && res.data) {
      showToast('下单成功')
      router.push(`/guest/payment/${res.data.orderId}`)
    } else {
      showToast(res.message || '下单失败')
    }
  } catch (e: any) {
    showToast(e.message || '下单失败')
  } finally {
    submitting.value = false
  }
}
</script>