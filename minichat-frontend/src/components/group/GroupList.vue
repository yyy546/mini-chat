<template>
  <div class="group-list">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="3" animated />
    </div>
    <el-scrollbar v-else>
      <div v-if="filteredGroups.length === 0" class="empty-list">暂无加入的群组</div>
      <div
        v-for="item in filteredGroups"
        :key="item.id"
        class="list-item"
        :class="{ active: activeId === item.id }"
        @click="onSelect(item)"
      >
        <div class="avatar-wrap">
          <el-avatar :size="36" :src="item.avatar">{{ getInitial(item.groupName) }}</el-avatar>
        </div>
        <div class="info">
          <div class="name">{{ item.groupName }}</div>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { getGroupList } from '../../api/group'
import logger from '../../utils/logger'

const props = defineProps({
  activeId: {
    type: [Number, String],
    default: null
  },
  filterText: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select'])
const groups = ref([])
const loading = ref(false)

const filteredGroups = computed(() => {
  if (!props.filterText) return groups.value
  const k = props.filterText.toLowerCase()
  return groups.value.filter((g) => (g.groupName || '').toLowerCase().includes(k))
})

const fetchGroups = async () => {
  loading.value = true
  try {
    const res = await getGroupList()
    if (res.code === 1) {
      groups.value = res.data
    }
  } catch (e) {
    logger.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchGroups()
})

const onSelect = (item) => {
  emit('select', item)
}

const getInitial = (name) => {
  return (name || '').slice(0, 1).toUpperCase()
}
</script>

<style scoped>
.group-list {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}
.loading {
  padding: 20px;
}
.empty-list {
  padding: 20px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 14px;
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
.info {
  flex: 1;
  overflow: hidden;
}
.name {
  font-size: 14px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
