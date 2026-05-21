<template>
  <el-container class="friend-page">
    <el-aside width="280px" class="sidebar">
      <div class="sidebar-top">
        <div class="back-icon" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="title">好友</div>
      </div>
      <FriendList :friends="store.friends" :showSearch="true" @select="onSelect" />
    </el-aside>

    <el-main class="main">
      <div class="main-top">
        <el-tabs v-model="active">
          <el-tab-pane label="搜索用户" name="search" />
          <el-tab-pane label="好友申请" name="requests" />
          <el-tab-pane label="我发出的申请" name="sent" />
        </el-tabs>
      </div>
      <div class="main-body">
        <FriendSearchPanel v-if="active === 'search'" />
        <FriendRequestPanel v-else-if="active === 'requests'" mode="incoming" />
        <FriendRequestPanel v-else mode="sent" />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
defineOptions({ name: 'Friend' })
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useFriendStore } from '../store/friend'
import FriendList from '../components/friend/FriendList.vue'
import FriendRequestPanel from '../components/friend/FriendRequestPanel.vue'
import FriendSearchPanel from '../components/friend/FriendSearchPanel.vue'
import { ArrowLeft } from '@element-plus/icons-vue'

const store = useFriendStore()
const active = ref('search')
const router = useRouter()

onMounted(async () => {
  await Promise.all([store.fetchFriends(), store.fetchIncomingRequests(), store.fetchSentRequests()])
})

const onSelect = (friend) => {
  // 当前版本仅基础展示，后续可在右侧显示聊天或资料
}

const goBack = () => {
  router.push('/')
}
</script>

<style scoped>
.friend-page {
  height: calc(100vh - 20px);
  background: var(--el-bg-color-page);
}
.sidebar {
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
}
.sidebar-top {
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color-light);
  display: flex;
  align-items: center;
  gap: 8px;
}
.back-btn {
  padding: 4px 8px;
}
.title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.main {
  background: var(--el-bg-color);
}
.main-top {
  border-bottom: 1px solid var(--el-border-color-light);
}
.main-body {
  padding: 12px;
}
.back-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 10px;
}
.back-icon:hover {
  background: var(--el-fill-color-light);
}
.back-icon :deep(svg) {
  font-size: 18px;
}
</style>
