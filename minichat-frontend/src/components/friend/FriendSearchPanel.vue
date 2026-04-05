<template>
  <div class="search-panel">
    <el-input v-model="keyword" placeholder="输入用户名/昵称搜索" clearable @input="onInput" size="large" class="panel-search">
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <el-skeleton :loading="store.loading" animated :count="3" style="margin-top:12px">
      <template #template>
        <el-skeleton-item variant="text" style="width: 60%" />
        <el-skeleton-item variant="text" style="width: 40%" />
      </template>
      <template #default>
        <div class="result-list">
          <el-card v-for="u in store.searchResults" :key="u.id" class="result-item" shadow="never">
            <div class="left">
              <div class="avatar-wrap">
                <el-avatar :size="36" :src="u.avatar">{{ getInitial(u.nickname || u.username) }}</el-avatar>
              </div>
              <div class="info">
                <div class="name">{{ u.nickname || u.username }}</div>
                <div class="sub">ID: {{ u.id }}</div>
              </div>
            </div>
            <div class="right">
              <el-button size="small" type="primary" @click="openApply(u)" :loading="sending && selectedUser?.id===u.id">加好友</el-button>
            </div>
          </el-card>
          <el-empty v-if="!store.searchResults.length && keyword" description="未搜索到用户" />
        </div>
      </template>
    </el-skeleton>
    <el-dialog v-model="showDialog" title="申请加好友" width="460px" :close-on-click-modal="false">
      <div class="verify-box">
        <div class="label">填写验证信息</div>
        <el-input v-model="verifyMsg" type="textarea" :rows="4" placeholder="请输入验证信息" />
      </div>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="doSend">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useFriendStore } from '../../store/friend'

const store = useFriendStore()
const keyword = ref('')
const loadingId = ref(null)
const showDialog = ref(false)
const verifyMsg = ref('')
const sending = ref(false)
const selectedUser = ref(null)

let timer = null
const onInput = () => {
  clearTimeout(timer)
  timer = setTimeout(() => store.doSearch(keyword.value.trim()), 300)
}

const openApply = (u) => {
  selectedUser.value = u
  verifyMsg.value = ''
  showDialog.value = true
}

const doSend = async () => {
  if (!selectedUser.value) return
  sending.value = true
  try {
    await store.applyFriend(selectedUser.value.id, verifyMsg.value.trim())
    showDialog.value = false
  } finally {
    sending.value = false
  }
}

const getInitial = (name = '') => name.slice(0, 1).toUpperCase()
</script>

<style scoped>
.panel-search :deep(.el-input__wrapper) { height: 40px; border-radius: 10px; }
.panel-search :deep(.el-input__inner) { font-size: 14px; }
.result-item { margin-top: 12px; display: flex; justify-content: space-between; align-items: center; }
.left { display: flex; align-items: center; }
.avatar-wrap { margin-right: 10px; }
.name { font-size: 14px; color: var(--el-text-color-primary); }
.sub { font-size: 12px; color: var(--el-text-color-secondary); }
.verify-box { margin: 8px 0; }
.label { font-size: 13px; color: var(--el-text-color-regular); margin-bottom: 6px; }
</style>
