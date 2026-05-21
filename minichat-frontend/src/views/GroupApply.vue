<template>
  <el-container class="group-page">
    <el-aside width="280px" class="sidebar">
      <div class="sidebar-top">
        <div class="back-icon" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="title">群聊</div>
      </div>
      <div class="sidebar-search">
        <el-input v-model="groupFilter" placeholder="搜索群聊" clearable size="large" class="list-search">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <GroupList :active-id="null" :filter-text="groupFilter" @select="noop" />
    </el-aside>

    <el-main class="main">
      <div class="main-top">
        <el-tabs v-model="active" @tab-change="handleTabChange">
          <el-tab-pane label="搜索群组" name="search" />
          <el-tab-pane label="加入申请" name="requests" />
          <el-tab-pane label="我发出的申请" name="sent" />
        </el-tabs>
      </div>
      <div class="main-body">
        <div v-if="active === 'search'" class="search-panel">
          <el-input
            v-model="keyword"
            placeholder="输入群ID/群名称搜索"
            clearable
            @input="onSearchInput"
            size="large"
            class="panel-search"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-skeleton :loading="loading" animated :count="3" style="margin-top: 12px">
            <template #template>
              <el-skeleton-item variant="text" style="width: 60%" />
              <el-skeleton-item variant="text" style="width: 40%" />
            </template>
            <template #default>
              <div class="result-list">
                <el-card v-for="g in results" :key="g.id" class="result-item" shadow="never">
                  <div class="left">
                    <div class="avatar-wrap">
                      <el-avatar :size="36" :src="g.avatar">{{ getInitial(g.groupName) }}</el-avatar>
                    </div>
                    <div class="info">
                      <div class="name">{{ g.groupName }}</div>
                      <div class="sub">ID: {{ g.id }} | 成员: {{ g.memberCount }}/{{ g.maxMembers }}</div>
                    </div>
                  </div>
                  <div class="right">
                    <el-button
                      size="small"
                      type="primary"
                      @click="openApply(g)"
                      :loading="sending && selectedGroup?.id === g.id"
                      >申请加入</el-button
                    >
                  </div>
                </el-card>
                <el-empty v-if="!results.length && keyword" description="未搜索到群组" />
              </div>
            </template>
          </el-skeleton>
        </div>

        <div v-else-if="active === 'requests'" class="requests-panel">
          <el-skeleton :loading="receivedLoading" animated :count="3">
            <template #default>
              <div v-if="receivedList.length" class="result-list">
                <el-card v-for="req in receivedList" :key="req.requestId" class="request-card" shadow="hover">
                  <div class="request-content">
                    <div class="request-left">
                      <el-avatar :size="48" :src="req.applicantAvatar" shape="circle">{{
                        getInitial(req.applicantName)
                      }}</el-avatar>
                    </div>
                    <div class="request-center">
                      <div class="request-header">
                        <span class="user-name">{{ req.applicantName }}</span>
                        <span class="action-text">申请加入</span>
                        <span class="group-name">{{ req.groupName }}</span>
                        <span class="time">{{ formatTime(req.createdTime) }}</span>
                      </div>
                      <div class="request-reason" v-if="req.message">
                        <span class="reason-label">理由：</span>{{ req.message }}
                      </div>
                    </div>
                    <div class="request-right">
                      <div v-if="req.status === 0" class="action-buttons">
                        <el-button type="primary" size="small" @click="handleRequestAction(req, 1)" :loading="handling"
                          >同意</el-button
                        >
                        <el-button
                          type="danger"
                          size="small"
                          @click="handleRequestAction(req, 2)"
                          :loading="handling"
                          plain
                          >拒绝</el-button
                        >
                      </div>
                      <div v-else class="status-tag">
                        <el-tag :type="getStatusType(req.status)" effect="light">{{
                          getStatusText(req.status)
                        }}</el-tag>
                      </div>
                    </div>
                  </div>
                </el-card>
              </div>
              <el-empty v-else description="暂无收到的申请" />
            </template>
          </el-skeleton>
        </div>

        <div v-else class="sent-panel">
          <el-skeleton :loading="sentLoading" animated :count="3">
            <template #default>
              <div v-if="sentList.length" class="result-list">
                <el-card v-for="req in sentList" :key="req.groupId" class="request-card" shadow="hover">
                  <div class="request-content">
                    <div class="request-left">
                      <el-avatar :size="48" :src="req.avatar" shape="circle">{{ getInitial(req.groupName) }}</el-avatar>
                    </div>
                    <div class="request-center">
                      <div class="request-header">
                        <span class="user-name">{{ req.groupName }}</span>
                        <span class="action-text">（群ID: {{ req.groupId }}）</span>
                        <span class="time">{{ formatTime(req.createdTime) }}</span>
                      </div>
                      <div class="request-reason" v-if="req.message">
                        <span class="reason-label">理由：</span>{{ req.message }}
                      </div>
                    </div>
                    <div class="request-right">
                      <el-tag :type="getStatusType(req.status)" effect="light">{{ getStatusText(req.status) }}</el-tag>
                    </div>
                  </div>
                </el-card>
              </div>
              <el-empty v-else description="暂无发出的申请" />
            </template>
          </el-skeleton>
        </div>
      </div>
    </el-main>
  </el-container>

  <!-- 自定义申请弹窗 -->
  <el-dialog v-model="applyDialogVisible" title="申请加入群组" width="400px" destroy-on-close>
    <div style="margin-bottom: 12px">填写验证信息</div>
    <el-input v-model="applyReason" type="textarea" :rows="4" placeholder="请输入验证信息" />
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApply" :loading="sending">发送</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
defineOptions({ name: 'GroupApply' })
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import GroupList from '../components/group/GroupList.vue'
import {
  searchGroups,
  sendGroupRequest,
  getSentGroupRequests,
  handleGroupRequest,
  getReceivedGroupRequests
} from '../api/group'
import { ArrowLeft, Search } from '@element-plus/icons-vue'

const router = useRouter()
const active = ref('search')
const keyword = ref('')
const groupFilter = ref('')
const results = ref([])
const loading = ref(false)
const sending = ref(false)
const selectedGroup = ref(null)

// 申请弹窗相关
const applyDialogVisible = ref(false)
const applyReason = ref('')

// 已发送申请列表
const sentList = ref([])
const sentLoading = ref(false)

// 收到的申请列表
const receivedList = ref([])
const receivedLoading = ref(false)
const handling = ref(false)

const goBack = () => {
  router.push('/')
}

const noop = () => {}

const getInitial = (name = '') => (name ? name.slice(0, 1).toUpperCase() : '#')

const onSearchInput = async () => {
  const k = keyword.value.trim()
  if (!k) {
    results.value = []
    return
  }
  loading.value = true
  try {
    const res = await searchGroups(k)
    if (res.code === 1) {
      results.value = Array.isArray(res.data) ? res.data : []
    } else {
      ElMessage.error(res.msg || '搜索群组失败')
    }
  } catch (e) {
    ElMessage.error('搜索群组出错')
  } finally {
    loading.value = false
  }
}

const openApply = (group) => {
  selectedGroup.value = group
  applyReason.value = ''
  applyDialogVisible.value = true
}

const confirmApply = async () => {
  if (!selectedGroup.value) return

  sending.value = true
  try {
    const res = await sendGroupRequest({
      groupId: selectedGroup.value.id,
      message: applyReason.value
    })
    if (res.code === 1) {
      ElMessage.success('申请已提交，等待群主/管理员审核')
      applyDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '申请提交失败')
    }
  } catch (e) {
    ElMessage.error('申请提交出错')
  } finally {
    sending.value = false
  }
}

const handleTabChange = (name) => {
  if (name === 'sent') {
    fetchSentRequests()
  } else if (name === 'requests') {
    fetchReceivedRequests()
  }
}

const fetchReceivedRequests = async () => {
  receivedLoading.value = true
  try {
    const res = await getReceivedGroupRequests()
    if (res.code === 1) {
      receivedList.value = res.data || []
    } else {
      ElMessage.error(res.msg || '获取申请列表失败')
    }
  } catch (e) {
    ElMessage.error('获取申请列表出错')
  } finally {
    receivedLoading.value = false
  }
}

const handleRequestAction = async (req, status) => {
  if (handling.value) return
  handling.value = true
  try {
    const res = await handleGroupRequest({
      groupId: req.groupId,
      applicantId: req.applicantId,
      status: status
    })
    if (res.code === 1) {
      ElMessage.success('操作成功')
      fetchReceivedRequests()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作出错')
  } finally {
    handling.value = false
  }
}

const fetchSentRequests = async () => {
  sentLoading.value = true
  try {
    const res = await getSentGroupRequests()
    if (res.code === 1) {
      sentList.value = res.data || []
    } else {
      ElMessage.error(res.msg || '获取申请列表失败')
    }
  } catch (e) {
    ElMessage.error('获取申请列表出错')
  } finally {
    sentLoading.value = false
  }
}

const getStatusType = (status) => {
  // 0-待处理、1-已同意、2-已拒绝
  const map = { 0: 'warning', 1: 'success', 2: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { 0: '待处理', 1: '已同意', 2: '已拒绝' }
  return map[status] || '未知'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.group-page {
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
.back-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 10px;
}
.back-icon :deep(svg) {
  font-size: 18px;
}
.back-icon:hover {
  background: var(--el-fill-color-light);
}
.title {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.sidebar-search {
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}
.list-search :deep(.el-input__wrapper) {
  height: 38px;
  border-radius: 10px;
}
.list-search :deep(.el-input__inner) {
  font-size: 14px;
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
.search-panel {
}
.panel-search :deep(.el-input__wrapper) {
  height: 38px;
  border-radius: 10px;
}
.panel-search :deep(.el-input__inner) {
  font-size: 14px;
}
.result-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.result-item {
  display: flex;
  align-items: center;
}
.request-card :deep(.el-card__body) {
  padding: 16px;
}
.request-content {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.request-left {
  flex-shrink: 0;
}
.request-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}
.request-header {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px;
  line-height: 1.4;
}
.user-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
  font-size: 15px;
}
.action-text {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.group-name {
  font-weight: 600;
  color: var(--el-color-primary);
  font-size: 14px;
}
.time {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.request-reason {
  background: var(--el-fill-color-lighter);
  padding: 8px 12px;
  border-radius: 4px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.5;
}
.reason-label {
  color: var(--el-text-color-secondary);
}
.request-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  align-self: center;
}
.action-buttons {
  display: flex;
  gap: 8px;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar-wrap {
}
.info .name {
  font-size: 14px;
  color: var(--el-text-color-primary);
}
.info .sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.right {
  margin-left: auto;
}
.placeholder {
  padding: 24px;
}
</style>
