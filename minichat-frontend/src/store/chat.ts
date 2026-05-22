import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { markMessagesAsRead, markGroupMessageRead, getGroupMessageHistory, getPrivateMessageHistory } from '../api/chat'
import { getSessionList } from '../api/session'
import { getFriendList } from '../api/friend'
import { useUserStore } from './user'
import { useWebSocketStore } from './chat/useWebSocketStore'
import { useMessageActions } from './chat/useMessageActions'
import logger from '../utils/logger'
import type { UIMessage } from '../types/message'
import type { Session } from '../types/session'

interface ChatPagination {
  page: number
  hasMore: boolean
  loading: boolean
}

interface ActiveUser {
  id: number
  type: number
  [key: string]: unknown
}

interface ChatState {
  activeUser: ActiveUser | null
  messagesByUser: Record<string, UIMessage[]>
  chatPagination: Record<string, ChatPagination>
  sessions: Session[]
  sessionLoading: boolean
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    activeUser: null,
    messagesByUser: {},
    chatPagination: {},
    sessions: [],
    sessionLoading: false
  }),

  getters: {
    activeMessages(state: ChatState): UIMessage[] {
      if (!state.activeUser) return []
      const id = state.activeUser.id
      const type = state.activeUser.type || 0
      const key = `${type === 1 ? 'group' : 'private'}_${id}`
      return state.messagesByUser[key] || []
    },

    isConnected(): boolean {
      const wsStore = useWebSocketStore()
      return wsStore.connected && !!wsStore.stomp && !!wsStore.stomp.connected
    }
  },

  actions: {
    async fetchSessions() {
      this.sessionLoading = true
      try {
        let list: unknown[] = await getSessionList()
        list = Array.isArray(list) ? list : []

        try {
          const friendList: unknown[] = await getFriendList()
          const validFriendIds = new Set<number | string>()
          ;(Array.isArray(friendList) ? friendList : []).forEach((friend: Record<string, unknown>) => {
            const friendId = (friend.friendId ?? friend.friend_id ?? friend.userId ?? friend.id) as number
            if (friendId) {
              validFriendIds.add(friendId)
              validFriendIds.add(String(friendId))
              validFriendIds.add(Number(friendId))
            }
          })

          list = list.filter((session: Record<string, unknown>) => {
            const sessionType = (session.type as number) || 0
            if (sessionType === 1) {
              return true
            }
            const sessionId = session.id as number
            return (
              validFriendIds.has(sessionId) ||
              validFriendIds.has(String(sessionId)) ||
              validFriendIds.has(Number(sessionId))
            )
          })
        } catch (friendErr: unknown) {
          logger.warn('获取好友列表失败，将显示所有会话:', friendErr)
        }

        this.sessions = (list as Record<string, unknown>[])
          .map((session) => ({
            id: session.id as number,
            type: ((session.type as number) || 0) as 0 | 1,
            name: (session.name as string) || '',
            avatar: (session.avatar as string) || '',
            lastMessageTime: session.lastMessageTime
              ? new Date(session.lastMessageTime as string).getTime()
              : 0,
            unreadCount: (session.unreadCount as number) || 0
          }))
          .sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))

        const wsStore = useWebSocketStore()
        if (wsStore.isConnected) {
          wsStore._subscribeAllGroups()
        }
      } catch (e: unknown) {
        logger.error('获取会话列表失败:', e)
        ElMessage.error('获取会话列表失败')
        this.sessions = []
      } finally {
        this.sessionLoading = false
      }
    },

    async setActiveUser(user: ActiveUser | null) {
      this.activeUser = user
      if (!user) return
      const id = user.id
      const sessionType = user.type || 0
      const key = `${sessionType === 1 ? 'group' : 'private'}_${id}`

      this.chatPagination[key] = { page: 1, hasMore: true, loading: false }

      const wsStore = useWebSocketStore()
      if (sessionType === 1) {
        wsStore._subscribeGroup(id)
        await this.loadHistory(id, sessionType)
        this.markAsRead(id, 1)
      } else if (sessionType === 0) {
        await this.loadHistory(id, sessionType)
        this.markAsRead(id, 0)
      }
    },

    async markAsRead(targetId: number, type: number = 0) {
      if (!targetId) return
      try {
        if (type === 1) {
          await markGroupMessageRead(targetId)
        } else {
          await markMessagesAsRead(targetId)
        }

        const sessionIndex = this.sessions.findIndex((s) => s.id == targetId && (s.type || 0) === type)
        if (sessionIndex !== -1) {
          const session = this.sessions[sessionIndex]
          this.sessions[sessionIndex] = {
            ...session,
            unreadCount: 0,
            lastReadSeq: type === 1 ? session.lastMessageSeq || session.lastReadSeq || 0 : session.lastReadSeq
          }
        }

        logger.debug('已标记消息为已读，对象ID:', targetId, '类型:', type)
      } catch (e: unknown) {
        logger.error('标记已读失败:', e)
      }
    },

    async loadHistory(userId: number, sessionType: number = 0, isLoadMore: boolean = false) {
      const userStore = useUserStore()
      const selfId = userStore.userInfo?.id
      const key = `${sessionType === 1 ? 'group' : 'private'}_${userId}`

      if (!this.chatPagination[key]) {
        this.chatPagination[key] = { page: 1, hasMore: true, loading: false }
      }

      const pagination = this.chatPagination[key]

      if (isLoadMore && !pagination.hasMore) return
      if (pagination.loading) return
      pagination.loading = true

      if (!isLoadMore) {
        pagination.page = 1
        pagination.hasMore = true
      }

      const pageToLoad = pagination.page
      const pageSize = 50

      try {
        let rawList: Record<string, unknown>[] = []
        let total = 0

        if (sessionType === 1) {
          const res = await getGroupMessageHistory(userId, pageToLoad, pageSize)
          const data = res as unknown as Record<string, unknown>
          rawList = (data?.records as Record<string, unknown>[]) || []
          total = (data?.total as number) || 0
          pagination.hasMore = ((data?.current as number) || 0) * ((data?.size as number) || 0) < total
        } else {
          const res = await getPrivateMessageHistory(userId, pageToLoad, pageSize)
          const data = res as unknown as Record<string, unknown>
          rawList = (data?.records as Record<string, unknown>[]) || []
          total = (data?.total as number) || 0
          pagination.hasMore = ((data?.current as number) || 0) * ((data?.size as number) || 0) < total
        }

        const list: UIMessage[] = rawList.map((m) => {
          const msgObj: UIMessage = {
            id: (m.messageId as number) || `hist_${Date.now()}_${Math.random()}`,
            fromId: m.senderId === selfId ? 'self' : (m.senderId as number),
            toId: m.receiverId as number,
            groupId: sessionType === 1 ? userId : undefined,
            content: m.content as string,
            timestamp: m.sendTime ? new Date(m.sendTime as string).getTime() : Date.now(),
            type: (m.messageType as 1 | 2 | 3 | 5) || 1,
            fileName: m.fileName as string | undefined,
            fileSize: m.fileSize as number | undefined,
            fileUrl: m.fileUrl as string | undefined,
            senderAvatar: m.senderAvatar as string | undefined,
            senderNickname: m.senderNickname as string | undefined,
            messageSeq: m.messageSeq as number | undefined,
            isSending: false,
            isReceived: true,
            sendError: false
          }
          if (msgObj.type === 5) {
            msgObj.content =
              msgObj.fromId === 'self'
                ? '你撤回了一条消息'
                : sessionType === 1
                  ? `"${msgObj.senderNickname || '成员'}" 撤回了一条消息`
                  : '对方撤回了一条消息'
            msgObj.recall = true
          }
          return msgObj
        })

        const sortedList = list.reverse()

        if (isLoadMore) {
          this.messagesByUser[key] = [...sortedList, ...(this.messagesByUser[key] || [])]
          if (pagination.hasMore) {
            pagination.page += 1
          }
        } else {
          this.messagesByUser[key] = sortedList
          if (pagination.hasMore) {
            pagination.page = 2
          }
        }
      } catch (e: unknown) {
        logger.error('加载历史消息失败:', e)
        if (!isLoadMore) {
          this.messagesByUser[key] = []
        }
      } finally {
        pagination.loading = false
      }
    },

    _updateSession(
      sessionId: number,
      timestamp: number,
      incrementUnread: boolean = false,
      isGroup: boolean = false,
      messageSeq: number | null = null
    ) {
      const type = isGroup ? 1 : 0
      const sessionIndex = this.sessions.findIndex((s) => s.id == sessionId && (s.type || 0) === type)

      if (sessionIndex !== -1) {
        const session = this.sessions[sessionIndex]
        const isActive = this.activeUser && this.activeUser.id == sessionId && (this.activeUser.type || 0) === type

        let newUnreadCount = session.unreadCount || 0
        if (incrementUnread) {
          if (!isActive) {
            newUnreadCount++
          }
        }

        let nextLastMessageSeq = session.lastMessageSeq
        let nextLastReadSeq = session.lastReadSeq
        if (isGroup && messageSeq) {
          nextLastMessageSeq = Math.max(Number(session.lastMessageSeq || 0), Number(messageSeq))
          if (isActive) {
            nextLastReadSeq = nextLastMessageSeq
            newUnreadCount = 0
          }
        }

        if (!session.lastMessageTime || session.lastMessageTime < timestamp || incrementUnread) {
          this.sessions[sessionIndex] = {
            ...session,
            lastMessageTime: timestamp > (session.lastMessageTime || 0) ? timestamp : session.lastMessageTime,
            unreadCount: newUnreadCount,
            lastMessageSeq: nextLastMessageSeq,
            lastReadSeq: nextLastReadSeq
          }
          this.sessions.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
        }
      }
    },

    sendMessage(toUserId: number, content: string, type?: number, fileName?: string, fileSize?: number, fileUrl?: string) {
      return useMessageActions().sendMessage(toUserId, content, type, fileName, fileSize, fileUrl)
    },

    sendGroupMessage(groupId: number, content: string, type?: number, fileName?: string, fileSize?: number, fileUrl?: string) {
      return useMessageActions().sendGroupMessage(groupId, content, type, fileName, fileSize, fileUrl)
    },

    _handleIncoming(payload: Record<string, unknown>) {
      return useMessageActions()._handleIncoming(payload)
    },

    _handleGroupMessage(payload: Record<string, unknown>) {
      return useMessageActions()._handleGroupMessage(payload)
    },

    _handleRecallNotification(payload: Record<string, unknown>) {
      return useMessageActions()._handleRecallNotification(payload)
    },

    recallMessage(messageId: number) {
      return useMessageActions().recallMessage(messageId)
    },

    connect() {
      return useWebSocketStore().connect()
    },

    disconnect() {
      return useWebSocketStore().disconnect()
    },

    reconnect() {
      return useWebSocketStore().reconnect()
    },

    checkConnectionStatus() {
      return useWebSocketStore().checkConnectionStatus()
    },

    testSendMessage(toUserId: number) {
      return useWebSocketStore().testSendMessage(toUserId)
    }
  }
})
