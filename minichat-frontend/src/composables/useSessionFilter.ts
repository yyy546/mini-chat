import { computed, ref } from 'vue'
import { useChatStore } from '@/store/chat'
import { useFriendStore } from '@/store/friend'

export function useSessionFilter() {
  const filter = ref('')
  const chatStore = useChatStore()
  const friendStore = useFriendStore()

  function findFriendById(id: number | string | null) {
    if (!id) return null
    const list = friendStore.friends || []
    return list.find((f) => f.id == id || f.friendId == id || f.userId == id) || null
  }

  function buildChatUser(source: Record<string, unknown> | null) {
    if (!source) return null
    const base = { ...source } as Record<string, unknown>
    const type = (base.type as number) ?? 0
    if (type !== 0) return base
    const friend = findFriendById(base.id as number)
    if (!friend) return base
    return {
      ...base,
      name: (base.name as string) || friend.remark || friend.nickname || friend.username,
      nickname: base.nickname || friend.nickname || friend.username,
      username: base.username || friend.username,
      remark: (base.remark as string) ?? friend.remark,
      avatar: (base.avatar as string) || friend.avatar,
      online: friend.online
    }
  }

  const sessionList = computed(() => {
    return chatStore.sessions.map((session) => {
      let displayUnread = session.unreadCount || 0
      if (session.type === 1 && session.lastMessageSeq && session.lastReadSeq) {
        if (session.lastMessageSeq <= session.lastReadSeq) {
          displayUnread = 0
        }
      }

      const base = {
        id: session.id,
        name: session.name,
        nickname: session.name,
        username: session.name,
        avatar: session.avatar,
        type: session.type,
        lastMessageTime: session.lastMessageTime || 0,
        unreadCount: displayUnread,
        _last: session.lastMessageTime || 0
      }
      const withStatus = buildChatUser(base)
      return {
        ...withStatus,
        lastMessageTime: session.lastMessageTime || 0,
        unreadCount: displayUnread,
        _last: session.lastMessageTime || 0
      }
    })
  })

  const filteredList = computed(() => {
    if (!filter.value) return sessionList.value
    const k = filter.value.toLowerCase()
    return sessionList.value.filter(
      (f: Record<string, unknown>) =>
        ((f.name || '') as string).toLowerCase().includes(k) ||
        ((f.nickname || '') as string).toLowerCase().includes(k) ||
        ((f.username || '') as string).toLowerCase().includes(k) ||
        ((f.remark || '') as string).toLowerCase().includes(k)
    )
  })

  return { filter, sessionList, filteredList, buildChatUser }
}
