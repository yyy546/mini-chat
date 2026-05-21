<template>
  <div class="friend-group-list">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="3" animated />
    </div>
    <el-scrollbar v-else>
      <div v-for="group in store.groups" :key="group.groupName" class="group-item">
        <div class="group-header" @click="toggleGroup(group)">
          <el-icon class="arrow" :class="{ expanded: group.expanded }"><CaretRight /></el-icon>
          <span class="group-name">{{ group.groupName }}</span>
        </div>
        <div v-show="group.expanded" class="group-content">
          <div v-if="!group.items || group.items.length === 0" class="empty-group">暂无好友</div>
          <div
            v-for="item in group.items"
            :key="item.id"
            class="list-item"
            :class="{ active: selectedId === item.id }"
            @click="onSelect(item)"
          >
            <div class="avatar-wrap">
              <el-avatar :size="36" :src="item.avatar">{{
                getInitial(item.remark || item.nickname || item.username)
              }}</el-avatar>
              <span class="status-dot" :class="{ online: item.online }"></span>
            </div>
            <div class="info">
              <div class="name">{{ item.remark || item.nickname || item.username }}</div>
            </div>
          </div>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useFriendStore } from '../../store/friend'
import { CaretRight } from '@element-plus/icons-vue'

const store = useFriendStore()
const loading = ref(false)
const selectedId = ref(null)
const emit = defineEmits(['select'])

const props = defineProps({
  showSearch: { type: Boolean, default: true }
})

onMounted(async () => {
  loading.value = true
  await store.fetchFriendGroups()
  loading.value = false
})

const toggleGroup = async (group) => {
  group.expanded = !group.expanded
  if (group.expanded && !group.loaded) {
    await store.fetchGroupItems(group.groupName)
  }
}

const onSelect = (item) => {
  selectedId.value = item.id
  emit('select', item)
}

const getInitial = (name) => {
  return (name || '').slice(0, 1).toUpperCase()
}
</script>

<style scoped>
.friend-group-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}
.loading {
  padding: 20px;
}
.group-header {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  cursor: pointer;
  user-select: none;
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.group-header:hover {
  background: var(--el-fill-color-light);
}
.arrow {
  margin-right: 6px;
  transition: transform 0.2s;
}
.arrow.expanded {
  transform: rotate(90deg);
}
.group-name {
  flex: 1;
  font-weight: 500;
}
.group-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.group-content {
}
.list-item {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  cursor: pointer;
  transition: background 0.2s;
}
.list-item:hover,
.list-item.active {
  background: var(--el-fill-color-light);
}
.avatar-wrap {
  position: relative;
  margin-right: 12px;
}
.status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #ccc;
  border: 2px solid var(--el-bg-color);
  border-radius: 50%;
}
.status-dot.online {
  background: var(--el-color-success);
}

.info {
  flex: 1;
  overflow: hidden;
}
.name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.empty-group {
  padding: 10px 20px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
