<template>
  <el-main class="chat-body">
    <DynamicScroller
      ref="scrollerRef"
      :items="scrollerItems"
      :min-item-size="42"
      key-field="_key"
      class="messages"
      @scroll="onScrollerScroll"
    >
      <template #default="{ item, active, index }">
        <DynamicScrollerItem
          :item="item"
          :active="active"
          :data-index="index"
          :size-dependencies="[item.content?.length, item.type, item.fileName]"
        >
          <div
            :id="'msg-' + item.id"
            :class="['msg', item.type === 4 || item.type === 5 ? 'system-row' : item.fromId === 'self' ? 'self' : 'other']"
          >
            <MessageBubble
              :message="item"
              :is-self="item.fromId === 'self'"
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
        </DynamicScrollerItem>
      </template>
    </DynamicScroller>
    <div v-if="loadingMore" class="loading-more">
      <el-icon class="is-loading"><RefreshLeft /></el-icon> 加载中...
    </div>
    <el-empty v-if="!messages.length" description="暂无消息" />
  </el-main>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { RefreshLeft } from '@element-plus/icons-vue'
import { DynamicScroller, DynamicScrollerItem } from 'vue-virtual-scroller'
import { useChatStore } from '@/store/chat'
import MessageBubble from './MessageBubble.vue'
import type { UIMessage } from '@/types/message'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

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
const scrollerRef = ref<any>(null)
const loadingMore = ref(false)
const isFirstLoad = ref(true)

const scrollerItems = computed(() =>
  props.messages.map((m) => ({ ...m, _key: `${m.timestamp}_${m.id || ''}` }))
)

let scrollEndTimer: ReturnType<typeof setTimeout> | null = null

function onScrollerScroll() {
  if (!scrollerRef.value || isFirstLoad.value || loadingMore.value) return
  const el = scrollerRef.value.$el as HTMLElement
  if (!el) return
  if (el.scrollTop < 30) {
    if (scrollEndTimer) clearTimeout(scrollEndTimer)
    scrollEndTimer = setTimeout(() => {
      if (el.scrollTop < 30) triggerLoadMore(el)
    }, 150)
  }
}

async function triggerLoadMore(el: HTMLElement) {
  const key = `${props.friendType === 1 ? 'group' : 'private'}_${props.friendId}`
  const pagination = store.chatPagination[key]
  if (!pagination || !pagination.hasMore || pagination.loading) return
  loadingMore.value = true
  const oldScrollHeight = el.scrollHeight
  await store.loadHistory(props.friendId, props.friendType, true)
  nextTick(() => {
    if (scrollerRef.value) {
      const newEl = scrollerRef.value.$el as HTMLElement
      newEl.scrollTop = newEl.scrollHeight - oldScrollHeight
    }
    loadingMore.value = false
  })
}

function scrollToBottom(smooth = true) {
  nextTick(() => {
    scrollerRef.value?.scrollToBottom()
  })
}

watch(() => props.messages, async (newVal, oldVal) => {
  if (!newVal || !newVal.length) return
  const newLast = newVal[newVal.length - 1]
  const oldLast = oldVal && oldVal.length ? oldVal[oldVal.length - 1] : null
  if (isFirstLoad.value || !oldLast || newLast.id !== oldLast.id) {
    await nextTick()
    if (isFirstLoad.value) {
      scrollToBottom(false)
      setTimeout(() => { isFirstLoad.value = false; scrollToBottom(false) }, 500)
    } else {
      scrollToBottom(true)
    }
  }
})

watch(() => props.friendId, () => { isFirstLoad.value = true })

defineExpose({ scrollToBottom, scrollerRef })
</script>

<style scoped>
.chat-body { background: var(--el-bg-color-page); flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.messages { flex: 1; }
.messages :deep(.vue-recycle-scroller) { padding: 16px; }
.messages :deep(.vue-recycle-scroller__item-wrapper) { /* container */ }
.msg { max-width: 70%; margin-bottom: 12px; display: flex; align-items: flex-start; gap: 8px; width: fit-content; }
.msg.self { margin-left: auto; }
.msg.other { margin-right: auto; }
.system-row { width: 100%; max-width: 100%; display: flex; justify-content: center; margin-bottom: 12px; }
.loading-more { text-align: center; padding: 10px; color: var(--el-text-color-secondary); font-size: 12px; display: flex; align-items: center; justify-content: center; gap: 5px; }
</style>
