import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { markMessagesAsRead, markGroupMessageRead, getGroupMessageHistory, getPrivateMessageHistory } from '../api/chat'
import { getSessionList } from '../api/session'
import { getFriendList } from '../api/friend'
import { useUserStore } from './user'
import { useWebSocketStore } from './chat/useWebSocketStore'
import { useMessageActions } from './chat/useMessageActions'
import logger from '../utils/logger'

export const useChatStore = defineStore('chat', {
  state: () => ({
    activeUser: null,
    messagesByUser: {},
    chatPagination: {},
    sessions: [],
    sessionLoading: false
  }),

  getters: {
    activeMessages(state) {
      if (!state.activeUser) return []
      const id = state.activeUser.id
      const type = state.activeUser.type || 0
      const key = `${type === 1 ? 'group' : 'private'}_${id}`
      return state.messagesByUser[key] || []
    },

    isConnected() {
      const wsStore = useWebSocketStore()
      return wsStore.connected && wsStore.stomp && wsStore.stomp.connected
    }
  },

  actions: {
    // ---- 会话管理 ----

    async fetchSessions() {
      this.sessionLoading = true
      try {
        let list = await getSessionList()
        list = Array.isArray(list) ? list : []

        try {
          const friendList = await getFriendList()
          const validFriendIds = new Set()
          ;(Array.isArray(friendList) ? friendList : []).forEach((friend) => {
            const friendId = friend.friendId ?? friend.friend_id ?? friend.userId ?? friend.id
            if (friendId) {
              validFriendIds.add(friendId)
              validFriendIds.add(String(friendId))
              validFriendIds.add(Number(friendId))
            }
          })

          list = list.filter((session) => {
            const sessionType = session.type || 0
            if (sessionType === 1) {
              return true
            }
            const sessionId = session.id
            return (
              validFriendIds.has(sessionId) ||
              validFriendIds.has(String(sessionId)) ||
              validFriendIds.has(Number(sessionId))
            )
          })
        } catch (friendErr) {
          logger.warn('获取好友列表失败，将显示所有会话:', friendErr)
        }

        this.sessions = list
          .map((session) => ({
            ...session,
            id: session.id,
            type: session.type || 0,
            name: session.name || '',
            avatar: session.avatar || '',
            lastMessageTime: session.lastMessageTime ? new Date(session.lastMessageTime).getTime() : 0,
            unreadCount: session.unreadCount || 0
          }))
          .sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))

        const wsStore = useWebSocketStore()
        if (wsStore.isConnected) {
          wsStore._subscribeAllGroups()
        }
      } catch (e) {
        logger.error('获取会话列表失败:', e)
        ElMessage.error('获取会话列表失败')
        this.sessions = []
      } finally {
        this.sessionLoading = false
      }
    },

    async setActiveUser(user) {
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

    async markAsRead(targetId, type = 0) {
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
      } catch (e) {
        logger.error('标记已读失败:', e)
      }
    },

    async loadHistory(userId, sessionType = 0, isLoadMore = false) {
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
        let rawList = []
        let total = 0

        if (sessionType === 1) {
          const res = await getGroupMessageHistory(userId, pageToLoad, pageSize)
          rawList = res?.records || []
          total = res?.total || 0
          pagination.hasMore = (res?.current || 0) * (res?.size || 0) < total
        } else {
          const res = await getPrivateMessageHistory(userId, pageToLoad, pageSize)
          rawList = res?.records || []
          total = res?.total || 0
          pagination.hasMore = (res?.current || 0) * (res?.size || 0) < total
        }

        const list = rawList.map((m) => {
          const msgObj = {
            id: m.messageId || `hist_${Date.now()}_${Math.random()}`,
            fromId: m.senderId === selfId ? 'self' : m.senderId,
            toId: m.receiverId,
            groupId: sessionType === 1 ? userId : undefined,
            content: m.content,
            timestamp: m.sendTime ? new Date(m.sendTime).getTime() : Date.now(),
            type: m.messageType || 1,
            fileName: m.fileName,
            fileSize: m.fileSize,
            fileUrl: m.fileUrl,
            senderAvatar: m.senderAvatar,
            senderNickname: m.senderNickname,
            messageSeq: m.messageSeq
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
      } catch (e) {
        logger.error('加载历史消息失败:', e)
        if (!isLoadMore) {
          this.messagesByUser[key] = []
        }
      } finally {
        pagination.loading = false
      }
    },

    _updateSession(sessionId, timestamp, incrementUnread = false, isGroup = false, messageSeq = null) {
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

    // ---- 委托给 useMessageActions ----

    sendMessage(toUserId, content, type, fileName, fileSize, fileUrl) {
      return useMessageActions().sendMessage(toUserId, content, type, fileName, fileSize, fileUrl)
    },

    sendGroupMessage(groupId, content, type, fileName, fileSize, fileUrl) {
      return useMessageActions().sendGroupMessage(groupId, content, type, fileName, fileSize, fileUrl)
    },

    _handleIncoming(payload) {
      return useMessageActions()._handleIncoming(payload)
    },

    _handleGroupMessage(payload) {
      return useMessageActions()._handleGroupMessage(payload)
    },

    _handleRecallNotification(payload) {
      return useMessageActions()._handleRecallNotification(payload)
    },

    recallMessage(messageId) {
      return useMessageActions().recallMessage(messageId)
    },

    // ---- 委托给 useWebSocketStore ----

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

    testSendMessage(toUserId) {
      return useWebSocketStore().testSendMessage(toUserId)
    }
  }
})
