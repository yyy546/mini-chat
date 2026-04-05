<template>
  <div class="group-detail-card" v-if="group">
    <div class="header">
      <el-avatar :size="80" :src="group.avatar" class="avatar">
        {{ getInitial(group.groupName) }}
      </el-avatar>
      <div class="basic-info">
        <div class="name-row">
          <span class="name">{{ group.groupName }}</span>
        </div>
        <div class="id-row">
          <span class="label">群组 ID：</span>
          <span class="value">{{ group.id }}</span>
        </div>
        <div class="status-row">
          <span class="group-text">
            成员：{{ group.memberCount }} / {{ group.maxMembers }}
          </span>
        </div>
      </div>
    </div>

    <el-divider />

    <div class="section">
      <div class="section-title">群组信息</div>
      <div class="section-body">
        <div class="field">
          <span class="field-label">群名称</span>
          <span class="field-value">{{ group.groupName }}</span>
        </div>
        <div class="field">
          <span class="field-label">群公告</span>
          <span class="field-value">{{ group.announcement || '暂无公告' }}</span>
        </div>
        <div class="field">
          <span class="field-label">创建时间</span>
          <span class="field-value">{{ formatTime(group.createdTime) }}</span>
        </div>
        <div class="field">
            <span class="field-label">群主 ID</span>
            <span class="field-value">{{ group.ownerId }}</span>
        </div>
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" size="large" @click="$emit('send-message', group)">
        发送消息
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  group: {
    type: Object,
    default: null
  }
})

defineEmits(['send-message'])

const getInitial = (name = '') => name ? name.slice(0, 1).toUpperCase() : ''

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}
</script>

<style scoped>
.group-detail-card {
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
  margin-bottom: 24px;
}

.avatar {
  flex-shrink: 0;
  margin-right: 20px;
  background-color: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 32px;
}

.basic-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.name {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-right: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.id-row {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.status-row {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.group-text {
  color: var(--el-text-color-secondary);
}

.section {
  margin-top: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
}

.section-body {
  padding: 0 8px;
}

.field {
  display: flex;
  margin-bottom: 16px;
  line-height: 1.5;
}

.field-label {
  width: 70px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}

.field-value {
  color: var(--el-text-color-primary);
  flex: 1;
}

.actions {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  gap: 16px;
}
</style>
