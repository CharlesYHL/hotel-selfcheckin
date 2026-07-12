<template>
  <div class="page-container">
    <div class="page-header">我的房卡</div>
    <div class="page-body">
      <div v-if="loading" style="text-align:center;padding:40px">
        <van-loading size="24px" />
      </div>

      <div v-if="cardData" class="card-item" style="text-align:center">
        <div style="font-size:14px;color:#999;margin-bottom:8px">{{ cardData.hotelId === 'H001' ? '杭州西湖希尔顿酒店' : '' }}</div>
        <div style="font-size:24px;font-weight:700;margin-bottom:4px">{{ cardData.roomNo }}</div>
        <div style="font-size:12px;color:#999;margin-bottom:16px">
          有效期至 {{ cardData.validTo?.substring(0, 16) }}
        </div>

        <!-- 模拟二维码 -->
        <div style="width:160px;height:160px;background:#f0f0f0;margin:0 auto 16px;display:flex;align-items:center;justify-content:center;border:2px dashed #ccc;border-radius:8px">
          <span style="color:#999;font-size:14px">QR Code</span>
        </div>

        <van-button round block type="primary" :loading="opening" @click="openDoorAction" style="margin:16px 0">
          🔑 一键开门
        </van-button>
        <div style="color:#999;font-size:12px">卡号: {{ cardData.cardNo }}</div>
      </div>
    </div>

    <div class="tab-bar">
      <router-link to="/guest/home" class="tab-bar-item">
        <span class="icon">🏠</span>选房
      </router-link>
      <router-link to="/guest/orders" class="tab-bar-item">
        <span class="icon">📋</span>我的订单
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getCardsByCheckin, openDoor } from '@/api/card'
import type { CardResponse } from '@/types'

const route = useRoute()
const cardData = ref<CardResponse | null>(null)
const loading = ref(true)
const opening = ref(false)

onMounted(async () => {
  try {
    const res = await getCardsByCheckin(route.params.checkinId as string)
    if (res.code === 200 && res.data && res.data.length > 0) {
      cardData.value = res.data[0]
    } else {
      showToast('未找到房卡')
    }
  } catch (e) {
    showToast('加载房卡失败')
  } finally {
    loading.value = false
  }
})

async function openDoorAction() {
  if (!cardData.value) return
  opening.value = true
  try {
    const res = await openDoor(cardData.value.cardId)
    if (res.code === 200) {
      showToast('开门成功！')
    } else {
      showToast('开门失败')
    }
  } catch (e) {
    showToast('开门失败')
  } finally {
    opening.value = false
  }
}
</script>