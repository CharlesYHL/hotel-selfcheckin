<template>
  <div class="page-container">
    <div class="page-header">
      <van-icon name="arrow-left" @click="router.back()" style="margin-right:8px" />
      入住管理
    </div>
    <div class="page-body">
      <van-empty description="暂无入住记录" />
      <div style="text-align:center;color:#999;font-size:12px">
        今天暂无入住记录
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>