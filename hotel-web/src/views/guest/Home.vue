<template>
  <div class="page-container">
    <div class="page-header">
      <div style="font-size:14px;opacity:0.8">杭州西湖希尔顿酒店</div>
      <div style="font-size:22px;margin-top:4px">选择房型</div>
    </div>
    <div class="page-body">
      <!-- 日期选择 -->
      <van-cell-group inset style="margin-bottom:16px">
        <van-field v-model="checkInDate" label="入住日期" readonly is-link @click="showCheckInPicker = true" />
        <van-field v-model="checkOutDate" label="退房日期" readonly is-link @click="showCheckOutPicker = true" />
      </van-cell-group>
      <van-calendar v-model:show="showCheckInPicker" @confirm="onCheckInConfirm" :min-date="today" />
      <van-calendar v-model:show="showCheckOutPicker" @confirm="onCheckOutConfirm" :min-date="checkInDateObj" />

      <!-- 房型列表 -->
      <div v-if="loading" style="text-align:center;padding:40px">
        <van-loading size="24px" />
        <p style="margin-top:8px;color:#999">加载中...</p>
      </div>
      <div v-for="item in roomTypes" :key="item.roomTypeId" class="card-item room-type-card" @click="goOrder(item)">
        <div>
          <div style="font-size:16px;font-weight:600;margin-bottom:4px">{{ item.roomTypeName }}</div>
          <div style="font-size:12px;color:#999">
            {{ item.bedType }} · 可住{{ item.maxCapacity }}人 · 余{{ item.availableCount }}间
          </div>
        </div>
        <div class="price">¥299/晚</div>
      </div>
    </div>

    <!-- 底部导航 -->
    <div class="tab-bar">
      <router-link to="/guest/home" class="tab-bar-item active">
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
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getRoomTypes } from '@/api/room'
import type { RoomType } from '@/types'

const router = useRouter()
const roomTypes = ref<RoomType[]>([])
const loading = ref(true)
const checkInDate = ref('')
const checkOutDate = ref('')
const showCheckInPicker = ref(false)
const showCheckOutPicker = ref(false)
const today = new Date()
const checkInDateObj = ref(today)
const checkOutDateObj = ref(new Date(today.getTime() + 86400000))

const formatDate = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

checkInDate.value = formatDate(today)
checkOutDate.value = formatDate(new Date(today.getTime() + 86400000))

function onCheckInConfirm(v: Date) {
  checkInDateObj.value = v
  checkInDate.value = formatDate(v)
  showCheckInPicker.value = false
  if (checkOutDateObj.value <= v) {
    checkOutDateObj.value = new Date(v.getTime() + 86400000)
    checkOutDate.value = formatDate(checkOutDateObj.value)
  }
}

function onCheckOutConfirm(v: Date) {
  checkOutDateObj.value = v
  checkOutDate.value = formatDate(v)
  showCheckOutPicker.value = false
}

function goOrder(item: RoomType) {
  router.push({
    path: '/guest/order',
    query: {
      roomTypeId: item.roomTypeId,
      roomTypeName: item.roomTypeName,
      hotelId: item.hotelId,
      checkInDate: checkInDate.value,
      checkOutDate: checkOutDate.value,
      maxCapacity: String(item.maxCapacity)
    }
  })
}

onMounted(async () => {
  try {
    const res = await getRoomTypes('H001')
    if (res.code === 200 && res.data) {
      roomTypes.value = res.data
    }
  } catch (e) {
    showToast('加载房型失败')
  } finally {
    loading.value = false
  }
})
</script>