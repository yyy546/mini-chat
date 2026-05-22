<template>
  <!-- 撤回/系统消息 -->
  <template v-if="message.type === 4 || message.type === 5">
    <div class="system-message-content">
      <span>{{ message.content }}</span>
    </div>
  </template>

  <!-- 对方消息：头像在左侧 -->
  <template v-else-if="!isSelf">
    <el-avatar :size="36" :src="message.senderAvatar || friendAvatar" class="msg-avatar">
      {{ otherInitial }}
    </el-avatar>
    <div class="msg-content">
      <div v-if="isGroup && message.senderNickname" class="sender-name">
        <span>{{ message.senderNickname }}</span>
        <el-tag v-if="memberRoleMap[message.fromId] === 2" size="small" type="warning" effect="dark" class="role-tag">群主</el-tag>
        <el-tag v-else-if="memberRoleMap[message.fromId] === 1" size="small" type="success" effect="dark" class="role-tag">管理员</el-tag>
      </div>
      <div class="bubble">
        <template v-if="message.type === 1">{{ message.content }}</template>
        <div v-else-if="message.type === 2" class="image-container">
          <el-image
            v-if="message.fileUrl"
            :src="message.fileUrl"
            class="msg-image"
            :preview-src-list="[message.fileUrl]"
            :preview-teleported="true"
            hide-on-click-modal
          >
            <template #error>
              <div class="image-placeholder">图片加载失败</div>
            </template>
          </el-image>
          <div v-else class="image-placeholder">图片加载失败</div>
        </div>
        <div v-else-if="message.type === 3" class="file-message">
          <div class="file-info">
            <el-icon class="file-icon"><Paperclip /></el-icon>
            <a v-if="message.fileUrl" :href="message.fileUrl" target="_blank" download class="file-name">{{ message.fileName || '文件' }}</a>
            <span v-else class="file-name">{{ message.fileName || '文件' }}</span>
          </div>
          <div class="file-meta">
            <span class="size">{{ formatSize(message.fileSize) }}</span>
          </div>
        </div>
        <div v-else class="system">{{ message.content }}</div>
      </div>
      <div class="time">{{ formatTime(message.timestamp) }}</div>
    </div>
  </template>

  <!-- 自己消息：头像在右侧 -->
  <template v-else>
    <div class="msg-content">
      <div class="bubble-row">
        <el-icon v-if="message.sendError" class="error-icon"><WarningFilled /></el-icon>
        <div class="bubble" @contextmenu.prevent="$emit('contextmenu', $event, message)">
          <template v-if="message.type === 1">{{ message.content }}</template>
          <div v-else-if="message.type === 2" class="image-container">
            <el-image
              v-if="message.fileUrl"
              :src="message.fileUrl"
              class="msg-image"
              :preview-src-list="[message.fileUrl]"
              :preview-teleported="true"
              hide-on-click-modal
            >
              <template #error>
                <div class="image-placeholder">图片加载失败</div>
              </template>
            </el-image>
            <div v-else class="image-placeholder">图片加载失败</div>
          </div>
          <div v-else-if="message.type === 3" class="file-message">
            <div class="file-info">
              <el-icon class="file-icon"><Paperclip /></el-icon>
              <a v-if="message.fileUrl" :href="message.fileUrl" target="_blank" download class="file-name">{{ message.fileName || '文件' }}</a>
              <span v-else class="file-name">{{ message.fileName || '文件' }}</span>
            </div>
            <div class="file-meta">
              <span class="size">{{ formatSize(message.fileSize) }}</span>
              <span class="sent-status">已发送</span>
            </div>
          </div>
          <div v-else class="system">{{ message.content }}</div>
        </div>
      </div>
      <div v-if="message.sendError" class="error-text">
        <span v-if="message.errorMessage && message.errorMessage.includes('添加对方为好友')">
          发送失败，请先<span class="link-text" @click="$emit('readd-friend')">添加对方为好友</span>
        </span>
        <span v-else>{{ message.errorMessage || '发送失败' }}</span>
      </div>
      <div class="time">{{ formatTime(message.timestamp) }}</div>
    </div>
    <el-avatar :size="36" :src="selfAvatar" class="msg-avatar">
      {{ selfInitial }}
    </el-avatar>
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import type { UIMessage } from '@/types/message'

const props = defineProps<{
  message: UIMessage
  isSelf: boolean
  isGroup: boolean
  friendAvatar?: string
  friendName?: string
  selfAvatar?: string
  selfInitial?: string
  memberRoleMap: Record<string | number, number>
}>()

defineEmits<{
  contextmenu: [event: MouseEvent, message: UIMessage]
  'readd-friend': []
}>()

const otherInitial = computed(() => {
  const name = props.message.senderNickname || props.friendName || ''
  return name.slice(0, 1).toUpperCase()
})

function formatTime(ts: number) {
  try {
    const date = new Date(ts)
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${year}/${month}/${day} ${hours}:${minutes}`
  } catch {
    return ''
  }
}

function formatSize(s: number | undefined) {
  if (!s || isNaN(s)) return ''
  if (s < 1024) return `${s}B`
  if (s < 1024 * 1024) return `${(s / 1024).toFixed(1)}KB`
  return `${(s / 1024 / 1024).toFixed(1)}MB`
}
</script>

<style scoped>
.system-message-content {
  background-color: #f2f2f2;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #999;
}
.msg-avatar { flex-shrink: 0; }
.msg-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  max-width: 100%;
}
.sender-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.role-tag { transform: scale(0.9); height: 18px; padding: 0 4px; }
.bubble {
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 8px 12px;
  line-height: 1.6;
  word-break: break-word;
  max-width: 100%;
  color: var(--el-text-color-primary);
}
:global(.dark) .bubble { /* placeholder */ }
.time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.image-container { position: relative; }
.msg-image { max-width: 360px; max-height: 400px; border-radius: 8px; display: inline-block; vertical-align: top; }
.image-placeholder { padding: 20px; text-align: center; color: var(--el-text-color-secondary); }
.file-message { display: flex; flex-direction: column; gap: 4px; }
.file-info { display: flex; align-items: center; gap: 8px; }
.file-icon { color: var(--el-text-color-secondary); flex-shrink: 0; }
.file-name { color: inherit; text-decoration: none; }
.file-name:hover { text-decoration: underline; }
.file-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--el-text-color-secondary); }
.size { font-size: 12px; color: var(--el-text-color-secondary); }
.sent-status { font-size: 12px; color: var(--el-text-color-secondary); }
.system { text-align: center; display: block; }
.bubble-row { display: flex; align-items: center; gap: 8px; }
.error-icon { color: var(--el-color-danger); font-size: 18px; cursor: pointer; }
.error-text { font-size: 12px; color: var(--el-color-danger); margin-top: 4px; text-align: right; }
.link-text { color: var(--el-color-primary); cursor: pointer; text-decoration: none; }
.link-text:hover { text-decoration: underline; }
</style>
