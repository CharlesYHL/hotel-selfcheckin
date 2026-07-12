<template>
  <div class="page-container">
    <div class="page-header">
      <van-icon name="arrow-left" @click="router.back()" style="margin-right:8px" />
      订单管理
    </div>
    <div class="page-body">
      <van-empty description="暂无订单数据" />
      <div style="text-align:center;color:#999;font-size:12px">
        订单数据将通过后端 API 加载
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>