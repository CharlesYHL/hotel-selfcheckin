<template>
  <div class="page-container">
    <div class="page-header">
      <van-icon name="arrow-left" @click="router.back()" style="margin-right:8px" />
      房间管理
    </div>
    <div class="page-body">
      <van-loading v-if="loading" size="24px" style="display:block;margin:40px auto" />
      <div v-for="room in rooms" :key="room.roomId" class="card-item">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <div>
            <span style="font-weight:600;font-size:16px">{{ room.roomNo }}</span>
            <span style="color:#999;font-size:12px;margin-left:8px">{{ room.floorNo }}楼</span>
          </div>
          <span :class="'status-tag status-' + (room.roomStatus === 1 ? 'assigned' : 'pending')">
            {{ room.roomStatusDesc || '空房' }}
          </span>
        </div>
        <div style="font-size:12px;color:#999;margin-top:4px">
          最大入住: {{ room.maxGuest }}人 · {{ room.isSmokeFree ? '禁烟' : '可吸烟' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRoomsByStatus } from '@/api/room'
import type { RoomDTO } from '@/types'

const router = useRouter()
const rooms = ref<RoomDTO[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getRoomsByStatus('H001', 1) // 空房
    if (res.code === 200 && res.data) {
      rooms.value = res.data
    }
  } catch (_) {
  } finally {
    loading.value = false
  }
})
</script>