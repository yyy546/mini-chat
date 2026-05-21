<template>
  <div style="padding: 20px">
    <h1>MiniChat 首页</h1>
    <div v-if="userStore.userInfo">
      <p>欢迎，{{ userStore.userInfo.nickname || userStore.userInfo.username }}！</p>
      <p>用户名：{{ userStore.userInfo.username }}</p>
    </div>
    <p v-else>欢迎使用 MiniChat 聊天系统</p>
    <el-button @click="handleLogout">退出登录</el-button>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '../store/user'
import { useRouter } from 'vue-router'
import logger from '../utils/logger'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
}

// 组件加载时获取当前用户信息
onMounted(async () => {
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.fetchCurrentUser()
    } catch (error) {
      logger.error('获取用户信息失败:', error)
    }
  }
})
</script>
