<template>
  <div class="friend-list">
    <div class="list-header" v-if="showSearch">
      <el-input v-model="keyword" placeholder="搜索好友" clearable @input="onFilter" size="large" class="list-search">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <el-scrollbar class="list-scroll">
      <div
        v-for="item in filtered"
        :key="item.id"
        :class="['list-item', { active: activeId == item.id && (item.type || 0) === (activeType || 0) }]"
        @click="emit('select', item)"
      >
        <div class="avatar-wrap">
          <el-avatar :size="36" :src="item.avatar">{{
            getInitial(item.name || item.nickname || item.username)
          }}</el-avatar>
          <!-- 只有私聊（type === 0）才显示在线状态 -->
          <span
            v-if="item.type === 0 || item.type === undefined"
            class="status-dot"
            :class="{ online: item.online }"
          ></span>
          <!-- 群聊标识 -->
          <span v-if="item.type === 1" class="group-badge">群</span>
        </div>
        <div class="info">
          <div class="name">{{ item.name || item.remark || item.nickname || item.username }}</div>
        </div>
        <div class="ops">
          <!-- 只有私聊才显示设置备注功能 -->
          <el-tooltip v-if="item.type === 0 || item.type === undefined" content="设置备注" placement="top">
            <el-icon @click.stop="handleEditRemark(item)"><Edit /></el-icon>
          </el-tooltip>
        </div>
        <!-- 未读红点（右侧显示） -->
        <div v-if="item.unreadCount > 0" class="unread-badge">
          {{ item.unreadCount > 99 ? '99+' : item.unreadCount }}
        </div>
      </div>

      <el-empty v-if="!filtered.length" description="暂无会话" />
    </el-scrollbar>

    <el-dialog v-model="remarkDialogVisible" title="设置备注" width="400px">
      <el-input v-model="newRemark" placeholder="请输入备注名" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="remarkDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRemark">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useFriendStore } from '../../store/friend'

const props = defineProps({
  friends: { type: Array, default: () => [] },
  showSearch: { type: Boolean, default: false },
  activeId: { type: [String, Number], default: null },
  activeType: { type: Number, default: 0 }
})
const emit = defineEmits(['select'])

const store = useFriendStore()
const remarkDialogVisible = ref(false)
const newRemark = ref('')
const currentFriend = ref(null)

const handleEditRemark = (item) => {
  // 只有私聊才能设置备注
  if (item.type === 1) return
  currentFriend.value = item
  newRemark.value = item.remark || ''
  remarkDialogVisible.value = true
}

const confirmRemark = async () => {
  if (!currentFriend.value) return
  try {
    await store.updateRemark(currentFriend.value.id, newRemark.value)
    remarkDialogVisible.value = false
  } catch (e) {
    // error handled in store
  }
}

const keyword = ref('')
const filtered = computed(() => {
  if (!props.showSearch) return props.friends
  if (!keyword.value) return props.friends
  const k = keyword.value.toLowerCase()
  return props.friends.filter(
    (f) =>
      (f.name || '').toLowerCase().includes(k) ||
      (f.nickname || '').toLowerCase().includes(k) ||
      (f.username || '').toLowerCase().includes(k) ||
      (f.remark || '').toLowerCase().includes(k)
  )
})

const onFilter = () => {}

const getInitial = (name = '') => name.slice(0, 1).toUpperCase()
</script>

<style scoped>
.friend-list {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.list-header {
  padding: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
}
.list-search :deep(.el-input__wrapper) {
  height: 38px;
  border-radius: 10px;
}
.list-search :deep(.el-input__inner) {
  font-size: 14px;
}
.list-scroll {
  padding: 8px;
}
.list-item {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.list-item:hover {
  background: var(--el-fill-color-light);
}
.list-item.active {
  background: var(--el-fill-color-light);
}
.avatar-wrap {
  margin-right: 10px;
  display: flex;
  align-items: center;
  position: relative;
}
.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ccc;
  border: 2px solid var(--el-bg-color);
}
.status-dot.online {
  background: var(--el-color-success);
}
.group-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--el-color-primary);
  border: 2px solid var(--el-bg-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #fff;
  font-weight: bold;
}
.info {
  flex: 1;
}
.name {
  font-size: 14px;
  color: var(--el-text-color-primary);
}
.sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.ops {
  color: var(--el-text-color-secondary);
  visibility: hidden;
}
.list-item:hover .ops {
  visibility: visible;
}
.unread-badge {
  background-color: var(--el-color-danger);
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  height: 18px;
  min-width: 18px;
  text-align: center;
  margin-left: 8px;
  font-weight: bold;
}
</style>
