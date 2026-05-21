<template>
  <div class="friend-detail-card" v-if="friend">
    <div class="header">
      <el-avatar :size="80" :src="friend.friendAvatar || friend.avatar" class="avatar">
        {{ getInitial(realNickname) }}
      </el-avatar>
      <div class="basic-info">
        <div class="name-row">
          <span class="name">{{ realNickname }}</span>
          <el-tag v-if="friend.remarkName" size="small" effect="plain" class="remark-tag">
            备注：{{ friend.remarkName }}
          </el-tag>
        </div>
        <div class="id-row">
          <span class="label">账号 ID：</span>
          <span class="value">{{ friend.friendUserId || friend.friendId || friend.id }}</span>
        </div>
        <div class="status-row">
          <span class="status-dot" :class="{ online: friend.online }"></span>
          <span class="status-text">{{ friend.online ? '在线' : '离线' }}</span>
          <span class="divider">|</span>
          <span class="group-text"> 分组：{{ friend.groupName || '我的好友' }} </span>
        </div>
      </div>
    </div>

    <el-divider />

    <div class="section">
      <div class="section-title">好友信息</div>
      <div class="section-body">
        <div class="field">
          <span class="field-label">昵称</span>
          <span class="field-value">{{ friend.friendNickname || friend.nickname || '-' }}</span>
        </div>
        <div class="field">
          <span class="field-label">备注名</span>
          <span class="field-value">{{ friend.remarkName || '未设置' }}</span>
        </div>
        <div class="field">
          <span class="field-label">分组</span>
          <span class="field-value">{{ friend.groupName || '我的好友' }}</span>
        </div>
        <div class="field">
          <span class="field-label">性别</span>
          <span class="field-value">{{ friend.gender || '未知' }}</span>
        </div>
        <div class="field">
          <span class="field-label">个性签名</span>
          <span class="field-value">{{ friend.signature || '这个人很懒，还没有写签名~' }}</span>
        </div>
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" size="large" @click="$emit('send-message', friend)"> 发送消息 </el-button>
      <el-button type="danger" size="large" plain @click="$emit('delete-friend', friend)"> 删除好友 </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  friend: {
    type: Object,
    default: null
  }
})

defineEmits(['send-message', 'delete-friend'])

// 真实昵称（不包含备注名）
const realNickname = computed(() => {
  const f = props.friend || {}
  return f.friendNickname || f.nickname || f.username || ''
})

const getInitial = (name = '') => name.slice(0, 1).toUpperCase()
</script>

<style scoped>
.friend-detail-card {
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 24px 28px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  height: 100%;
  box-sizing: border-box;
}

.header {
  display: flex;
  align-items: center;
}

.avatar {
  margin-right: 20px;
}

.basic-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.name {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.remark-tag {
  border-radius: 999px;
}

.id-row {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.status-row {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: var(--el-text-color-regular);
  gap: 4px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #c0c4cc;
}

.status-dot.online {
  background: #67c23a;
}

.status-text {
  margin-right: 4px;
}

.divider {
  color: var(--el-border-color);
  padding: 0 4px;
}

.group-text {
  color: var(--el-text-color-secondary);
}

.section {
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.section-body {
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  padding: 12px 14px;
}

.field {
  display: flex;
  margin-bottom: 6px;
}

.field:last-child {
  margin-bottom: 0;
}

.field-label {
  width: 60px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.field-value {
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.actions .el-button {
  min-width: 120px;
}
</style>
