<template>
  <el-main class="chat-body">
    <div class="messages" ref="listRef" @scroll="onScroll">
      <div v-if="loadingMore" class="loading-more">
        <el-icon class="is-loading"><RefreshLeft /></el-icon> 加载中...
      </div>
      <div
        v-for="m in messages"
        :key="m.timestamp + '_' + (m.id || '')"
        :id="'msg-' + m.id"
        :class="['msg', m.type === 4 || m.type === 5 ? 'system-row' : m.fromId === 'self' ? 'self' : 'other']"
      >
        <MessageBubble
          :message="m"
          :is-self="m.fromId === 'self'"
          :is-group="friendType === 1"
          :friend-avatar="friendAvatar"
          :friend-name="friendName"
          :self-avatar="selfAvatar"
          :self-initial="selfInitial"
          :member-role-map="memberRoleMap"
          @contextmenu="(e, msg) => $emit('contextmenu', e, msg)"
          @readd-friend="$emit('readd-friend')"
        />
      </div>
      <el-empty v-if="!messages.length" description="暂无消息" />
    </div>
  </el-main>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { RefreshLeft } from '@element-plus/icons-vue'
import { useChatStore } from '@/store/chat'
import MessageBubble from './MessageBubble.vue'
import type { UIMessage } from '@/types/message'

const props = defineProps<{
  messages: UIMessage[]
  friendId: number
  friendType: number
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

const store = useChatStore()
const listRef = ref<HTMLElement | null>(null)
const loadingMore = ref(false)
const isFirstLoad = ref(true)

async function onScroll(e: Event) {
  if (isFirstLoad.value) return
  const target = e.target as HTMLElement
  if (target.scrollTop < 20 && !loadingMore.value) {
    const key = `${props.friendType === 1 ? 'group' : 'private'}_${props.friendId}`
    const pagination = store.chatPagination[key]
    if (pagination && pagination.hasMore && !pagination.loading) {
      loadingMore.value = true
      const oldScrollHeight = target.scrollHeight
      await store.loadHistory(props.friendId, props.friendType, true)
      nextTick(() => {
        const newScrollHeight = target.scrollHeight
        target.scrollTop = newScrollHeight - oldScrollHeight
        loadingMore.value = false
      })
    }
  }
}

function scrollToBottom(smooth = true) {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTo({
        top: listRef.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    }
  })
}

watch(() => props.messages, async (newVal, oldVal) => {
  if (!newVal || !newVal.length) return
  const newLast = newVal[newVal.length - 1]
  const oldLast = oldVal && oldVal.length ? oldVal[oldVal.length - 1] : null

  if (isFirstLoad.value || !oldLast || newLast.id !== oldLast.id) {
    const smooth = !isFirstLoad.value
    await nextTick()
    scrollToBottom(smooth)
    if (isFirstLoad.value) {
      setTimeout(() => {
        isFirstLoad.value = false
        scrollToBottom(false)
      }, 500)
    }
  }
})

watch(() => props.friendId, () => { isFirstLoad.value = true })

defineExpose({ scrollToBottom, listRef, isFirstLoad, loadingMore })
</script>

<style scoped>
.chat-body { background: var(--el-bg-color-page); flex: 1; overflow: hidden; }
.messages { height: 100%; overflow-y: auto; padding: 16px; }
.msg { max-width: 70%; margin-bottom: 12px; display: flex; align-items: flex-start; gap: 8px; width: fit-content; }
.msg.self { margin-left: auto; }
.msg.other { margin-right: auto; }
.system-row { width: 100%; max-width: 100%; display: flex; justify-content: center; margin-bottom: 12px; }
.loading-more { text-align: center; padding: 10px; color: var(--el-text-color-secondary); font-size: 12px; display: flex; align-items: center; justify-content: center; gap: 5px; }
</style>
