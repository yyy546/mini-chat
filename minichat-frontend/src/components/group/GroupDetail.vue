<template>
  <div class="group-detail-container">
    <div v-if="group" class="group-detail">
      <div class="detail-card">
        <div class="profile-header">
          <div class="header-left">
            <el-avatar :size="80" :src="group.avatar" class="big-avatar">
              {{ getInitial(group.groupName) }}
            </el-avatar>
          </div>
          <div class="header-right">
            <div class="name-row">
              <span class="display-name">{{ group.groupName }}</span>
            </div>
            <div class="sub-row">
              <span class="id-text">ID {{ group.id }}</span>
              <span class="member-count">成员: {{ group.memberCount || 0 }} / {{ group.maxMembers || 200 }}</span>
            </div>
          </div>
        </div>

        <div class="info-section">
          <div class="info-item">
            <span class="label">群公告</span>
            <div class="value">{{ group.announcement || '暂无公告' }}</div>
          </div>
          <div class="info-item">
            <span class="label">创建时间</span>
            <span class="value">{{ formatTime(group.createdTime) }}</span>
          </div>
          <div class="info-item">
            <span class="label">群主ID</span>
            <span class="value">{{ group.ownerId }}</span>
          </div>
        </div>

        <div class="action-buttons">
          <el-button class="action-btn primary" type="primary" @click="sendMessage">发消息</el-button>
          <el-tooltip :content="canEdit ? '修改群组资料' : '仅群主或管理员可修改'" placement="top">
            <el-button class="action-btn" :type="canEdit ? 'success' : 'info'" :disabled="!canEdit" @click="handleEdit"
              >修改资料</el-button
            >
          </el-tooltip>
          <el-button class="action-btn" type="danger" @click="handleExitGroup">退出群聊</el-button>
          <el-button v-if="isOwner" class="action-btn" type="danger" @click="handleDismissGroup">解散群聊</el-button>
        </div>
      </div>

      <!-- 编辑资料弹窗 -->
      <el-dialog
        v-model="showEditDialog"
        title="修改群组资料"
        width="600px"
        append-to-body
        destroy-on-close
        class="edit-dialog"
      >
        <el-form :model="editForm" label-width="100px" class="edit-form" label-position="right">
          <div class="form-header">
            <div class="avatar-section">
              <el-upload class="avatar-uploader" action="#" :show-file-list="false" :http-request="handleAvatarUpload">
                <div v-if="editForm.avatar" class="avatar-wrapper">
                  <img :src="editForm.avatar" class="avatar" />
                  <div class="avatar-mask">
                    <el-icon><Plus /></el-icon>
                  </div>
                </div>
                <div v-else class="avatar-placeholder">
                  <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                  <div class="upload-tip">点击上传头像</div>
                </div>
              </el-upload>
            </div>
            <div class="basic-info">
              <el-form-item label="群名称" required>
                <el-input v-model="editForm.groupName" maxlength="20" show-word-limit placeholder="请输入群名称" />
              </el-form-item>
              <el-form-item label="群ID">
                <span class="readonly-text">{{ group.id }}</span>
              </el-form-item>
            </div>
          </div>

          <el-divider content-position="center">基本设置</el-divider>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="最大成员数">
                <el-input-number v-model="editForm.maxMembers" :min="1" :max="500" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="加入方式">
                <el-select v-model="editForm.joinPolicy" placeholder="请选择" style="width: 100%">
                  <el-option label="自由加入" :value="0" />
                  <el-option label="需审批" :value="1" />
                  <el-option label="仅邀请" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="邀请权限">
                <el-select v-model="editForm.invitePolicy" placeholder="请选择" style="width: 100%">
                  <el-option label="所有成员" :value="0" />
                  <el-option label="管理员" :value="1" />
                  <el-option label="群主" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-divider content-position="center">群公告</el-divider>

          <el-form-item label-width="0">
            <el-input
              v-model="editForm.announcement"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="请输入群公告..."
              class="announcement-input"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="showEditDialog = false">取消</el-button>
            <el-button type="primary" :loading="editLoading" @click="saveGroupInfo">保存修改</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
    <div v-else class="empty-state">
      <el-empty description="请选择一个群组查看详情" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { getGroupProfile, updateGroupProfile, exitGroup, dismissGroup } from '../../api/group'
import { uploadFileUnified } from '../../api/upload'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'
import { Plus } from '@element-plus/icons-vue'
import logger from '../../utils/logger'

const props = defineProps({
  groupId: { type: [Number, String], default: null }
})

const emit = defineEmits(['message'])
const userStore = useUserStore()
const group = ref(null)
const showEditDialog = ref(false)
const editLoading = ref(false)
const editForm = ref({
  groupName: '',
  announcement: '',
  avatar: '',
  maxMembers: 200,
  joinPolicy: 0,
  invitePolicy: 0
})

const canEdit = computed(() => {
  if (!group.value || !userStore.userInfo) return false
  const currentUserId = userStore.userInfo.id

  // 1. 判断是否是群主
  const isOwner = group.value.ownerId === currentUserId

  // 2. 判断是否是管理员
  const isAdmin = group.value.adminIds && group.value.adminIds.includes(currentUserId)

  return isOwner || isAdmin
})

const isOwner = computed(() => {
  if (!group.value || !userStore.userInfo) return false
  return group.value.ownerId === userStore.userInfo.id
})

const fetchGroupDetail = async (id) => {
  if (!id) {
    group.value = null
    return
  }
  try {
    const res = await getGroupProfile(id)
    if (res.code === 1) {
      group.value = res.data
    } else {
      ElMessage.error(res.msg || '获取群组信息失败')
    }
  } catch (e) {
    logger.error(e)
    ElMessage.error('获取群组信息失败')
  }
}

watch(
  () => props.groupId,
  (newId) => {
    fetchGroupDetail(newId)
  },
  { immediate: true }
)

const getInitial = (name) => (name || '').slice(0, 1).toUpperCase()

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleDateString()
}

const sendMessage = () => {
  if (!group.value) return
  const user = {
    id: group.value.id,
    name: group.value.groupName,
    nickname: group.value.groupName,
    avatar: group.value.avatar,
    type: 1 // Group
  }
  emit('message', user)
}

const handleEdit = () => {
  if (!group.value) return
  editForm.value = {
    groupName: group.value.groupName,
    announcement: group.value.announcement,
    avatar: group.value.avatar,
    maxMembers: group.value.maxMembers || 200,
    joinPolicy: group.value.joinPolicy || 0,
    invitePolicy: group.value.invitePolicy || 0
  }
  showEditDialog.value = true
}

const handleAvatarUpload = async (options) => {
  try {
    if (!group.value) {
      ElMessage.error('未找到群组信息')
      return
    }
    const result = await uploadFileUnified({
      file: options.file,
      scene: 'groupAvatar',
      groupId: group.value.id
    })
    editForm.value.avatar = result.fileUrl
    ElMessage.success('头像上传成功')
    fetchGroupDetail(group.value.id)
  } catch (e) {
    logger.error(e)
    ElMessage.error(e.message || '头像上传失败')
  }
}

const saveGroupInfo = async () => {
  if (!editForm.value.groupName) {
    ElMessage.warning('群名称不能为空')
    return
  }

  editLoading.value = true
  try {
    const res = await updateGroupProfile(group.value.id, {
      groupName: editForm.value.groupName,
      announcement: editForm.value.announcement,
      avatar: editForm.value.avatar,
      maxMembers: editForm.value.maxMembers,
      joinPolicy: editForm.value.joinPolicy,
      invitePolicy: editForm.value.invitePolicy
    })

    if (res.code === 1) {
      ElMessage.success('群资料修改成功')
      showEditDialog.value = false
      fetchGroupDetail(group.value.id)
    } else {
      ElMessage.error(res.msg || '修改失败')
    }
  } catch (e) {
    logger.error(e)
    ElMessage.error('修改失败')
  } finally {
    editLoading.value = false
  }
}

const handleExitGroup = async () => {
  if (!group.value) return

  try {
    await ElMessageBox.confirm('确定要退出该群聊吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return // 用户取消
  }

  try {
    const res = await exitGroup(group.value.id)
    if (res.code === 1) {
      ElMessage.success('退出群组成功')
      // 退出成功后，清空当前群组信息，并触发外部更新
      group.value = null
      emit('message', null) // 通知父组件清除选中状态
      window.location.reload() // 简单刷新页面
    } else {
      ElMessage.error(res.msg || '退出群组失败')
    }
  } catch (e) {
    ElMessage.error('退出群组出错')
  }
}

const handleDismissGroup = async () => {
  if (!group.value || !isOwner.value) return
  try {
    await ElMessageBox.confirm('确定要解散该群聊吗？解散后不可恢复', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    const res = await dismissGroup(group.value.id)
    if (res.code === 1) {
      ElMessage.success('解散群组成功')
      group.value = null
      emit('message', null)
      window.location.reload()
    } else {
      ElMessage.error(res.msg || '解散群组失败')
    }
  } catch (e) {
    ElMessage.error('解散群组出错')
  }
}
</script>

<style scoped>
.group-detail-container {
  height: 100%;
  background: var(--el-bg-color-page);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.group-detail {
  flex: 1;
  overflow-y: auto;
  padding: 40px;
  background: var(--el-bg-color-page);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-card {
  width: 100%;
  max-width: 600px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.profile-header {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  align-items: flex-start;
}
.big-avatar {
  border: 2px solid var(--el-bg-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.name-row {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.sub-row {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}

.info-section {
  margin-bottom: 30px;
  padding: 0 10px;
}
.info-item {
  display: flex;
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.5;
}
.label {
  color: var(--el-text-color-secondary);
  width: 80px;
  flex-shrink: 0;
}
.value {
  color: var(--el-text-color-primary);
  flex: 1;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  justify-content: center;
}
.action-btn {
  width: 140px;
  height: 40px;
  border-radius: 20px;
  font-size: 15px;
}
.action-btn.primary {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

.avatar-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 50%;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: var(--el-fill-color-lighter);
  width: 100px;
  height: 100px;
}

.avatar-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
  background-color: var(--el-fill-color-light);
}

.avatar-uploader-icon {
  font-size: 24px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.upload-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.2;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
  border-radius: 50%;
}

.form-header {
  display: flex;
  gap: 24px;
  margin-bottom: 10px;
  align-items: center;
}

.avatar-section {
  flex-shrink: 0;
}

.basic-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.readonly-text {
  color: var(--el-text-color-secondary);
  font-family: monospace;
}

.announcement-input :deep(.el-textarea__inner) {
  background-color: var(--el-fill-color-lighter);
  border: none;
  padding: 12px;
  font-size: 14px;
}

.announcement-input :deep(.el-textarea__inner:focus) {
  background-color: var(--el-bg-color);
  box-shadow: none;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 50%;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}
</style>
