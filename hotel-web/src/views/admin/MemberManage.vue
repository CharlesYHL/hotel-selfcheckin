<template>
  <div class="page-container">
    <div class="page-header">
      <van-icon name="arrow-left" @click="router.back()" style="margin-right:8px" />
      会员管理
    </div>
    <div class="page-body">
      <van-loading v-if="loading" size="24px" style="display:block;margin:40px auto" />
      <div v-for="level in levels" :key="level.levelId" class="card-item">
        <div style="font-weight:600">{{ level.levelName }}</div>
        <div style="font-size:12px;color:#999;margin-top:4px">
          最低积分: {{ level.minPoints }} · 折扣: {{ level.discount }}折
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMemberLevels } from '@/api/member'

const router = useRouter()
const levels = ref<Array<Record<string, any>>>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await getMemberLevels()
    if (res.code === 200 && res.data) {
      levels.value = res.data
    }
  } catch (_) {
  } finally {
    loading.value = false
  }
})
</script>