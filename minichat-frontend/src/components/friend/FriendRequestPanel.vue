<template>
  <div class="request-panel">
    <template v-if="mode === 'incoming'">
      <el-card v-for="r in requests" :key="idOf(r)" class="req-item" shadow="hover">
        <div class="req-main">
          <div class="avatar-wrap">
            <el-avatar :size="36" :src="r.fromUserAvatar || r.fromAvatar || r.avatar">{{
              getInitial(r.fromUserNickname || r.fromNickname || r.nickname || r.username)
            }}</el-avatar>
          </div>
          <div class="texts">
            <div class="line1">{{ r.fromUserNickname || r.fromNickname || r.nickname || r.username }}</div>
            <div class="line2">{{ r.message || r.remark || r.verifyMessage || '请求添加为好友' }}</div>
          </div>
        </div>
        <div class="req-ops">
          <el-button size="small" type="primary" @click="accept(idOf(r))" :loading="loadingId === idOf(r)"
            >同意</el-button
          >
          <el-button size="small" @click="reject(idOf(r))" :loading="loadingId === idOf(r)">拒绝</el-button>
        </div>
      </el-card>
      <el-empty v-if="!requests.length" description="暂无好友申请" />
    </template>

    <template v-else>
      <el-card v-for="s in sent" :key="idOf(s)" class="sent-item" shadow="never">
        <div class="sent-main">
          <div class="avatar-wrap">
            <el-avatar :size="36" :src="s.toUserAvatar || s.toAvatar || s.avatar">{{
              getInitial(s.toUserNickname || s.nickname || s.username)
            }}</el-avatar>
          </div>
          <div class="texts">
            <div class="line1">{{ s.toUserNickname || s.nickname || s.username }}</div>
            <div class="line2">{{ s.message }}</div>
          </div>
          <div class="status">
            <el-tag :type="statusType(s.status)">{{ statusText(s.status) }}</el-tag>
          </div>
        </div>
      </el-card>
      <el-empty v-if="!sent.length" description="暂无我发出的申请" />
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useFriendStore } from '../../store/friend'

const store = useFriendStore()
const props = defineProps({ mode: { type: String, default: 'incoming' } })
const requests = computed(() => store.incomingRequests)
const sent = computed(() => store.sentRequests)
const loadingId = ref(null)

const idOf = (r) => r?.requestId ?? r?.id ?? r?.applyId ?? r?.reqId

const accept = async (id) => {
  loadingId.value = id
  try {
    await store.processRequest(id, 'accept')
  } finally {
    loadingId.value = null
  }
}

const reject = async (id) => {
  loadingId.value = id
  try {
    await store.processRequest(id, 'reject')
  } finally {
    loadingId.value = null
  }
}

const getInitial = (name = '') => name?.slice(0, 1)?.toUpperCase()

const statusText = (s) => (s === 1 ? '已同意' : s === 2 ? '已拒绝' : '待处理')
const statusType = (s) => (s === 1 ? 'success' : s === 2 ? 'danger' : 'info')
</script>

<style scoped>
.req-item {
  margin-bottom: 12px;
}
.req-main {
  display: flex;
  align-items: center;
}
.avatar-wrap {
  margin-right: 10px;
}
.texts .line1 {
  font-size: 14px;
  color: var(--el-text-color-primary);
}
.texts .line2 {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.req-ops {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.sent-header {
  margin: 12px 0 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}
.sent-item {
  margin-bottom: 12px;
}
.sent-main {
  display: flex;
  align-items: center;
}
.status {
  margin-left: auto;
}
</style>
