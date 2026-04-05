import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { getChatHistory, getGroupChatHistory, markMessagesAsRead, markGroupMessageRead, recallPrivateMessage, recallGroupMessage, getGroupMessageHistory, getPrivateMessageHistory } from '../api/chat'
import { getSessionList } from '../api/session'
import { getFriendList } from '../api/friend'
import Stomp from 'stompjs'
import SockJS from 'sockjs-client'
import { useUserStore } from './user'
import { useFriendStore } from './friend'
import request from '../utils/request'

const sockPath = '/api/minichat-websocket'

const getWebSocketUrl = (token) => {
  const apiBase = import.meta.env.VITE_API_BASE || ''
  const wsBase = import.meta.env.VITE_WS_BASE
  if (wsBase) {
    const url = wsBase
    return token ? `${url}?token=${encodeURIComponent(token)}` : url
  }
  // 使用相对路径，让浏览器自动处理协议和端口（http -> ws, https -> wss）
  // 这样请求就会发送到当前页面的端口（8090），并由 Nginx 转发
  const path = sockPath
  return token ? `${path}?token=${encodeURIComponent(token)}` : path
}

export const useChatStore = defineStore('chat', {
  state: () => ({
    activeUser: null,
    messagesByUser: {}, // 存储消息，key格式: "private_{id}" 或 "group_{id}"
    chatPagination: {}, // 存储分页信息，key同上: { page: 1, hasMore: true, loading: false }
    sessions: [], // 会话列表（包含好友和群聊）
    sessionLoading: false,
    stomp: null,
    connecting: false,
    connected: false,
    connectionError: null,
    subscriptions: new Map(), // 添加订阅管理
    groupSubscriptions: new Map(), // 群聊订阅管理，key为groupId
    heartbeatTimer: null, // 心跳定时器
    heartbeatCount: 0 // 心跳计数器
  }),

  getters: {
    activeMessages(state) {
      if (!state.activeUser) return []
      const id = state.activeUser.id
      const type = state.activeUser.type || 0
      const key = `${type === 1 ? 'group' : 'private'}_${id}`
      return state.messagesByUser[key] || []
    },

    isConnected(state) {
      return state.connected && state.stomp && state.stomp.connected
    }
  },

  actions: {
    // 获取会话列表
    async fetchSessions() {
      this.sessionLoading = true
      try {
        const res = await getSessionList()
        const unwrap = (res) => {
          if (Array.isArray(res)) return res
          if (res && typeof res === 'object') {
            if (Array.isArray(res.data)) return res.data
            if (res.data) return res.data
          }
          return []
        }
        let list = unwrap(res) || []
        
        // 获取好友列表，用于过滤已删除的好友
        try {
          const friendRes = await getFriendList()
          const friendList = unwrap(friendRes) || []
          // 提取所有未删除的好友ID
          const validFriendIds = new Set()
          friendList.forEach(friend => {
            const friendId = friend.friendId ?? friend.friend_id ?? friend.userId ?? friend.id
            if (friendId) {
              validFriendIds.add(friendId)
              validFriendIds.add(String(friendId)) // 同时添加字符串形式，以防ID类型不一致
              validFriendIds.add(Number(friendId)) // 同时添加数字形式
            }
          })
          
          // 过滤会话列表：只保留群聊（type=1）或未删除的好友的私聊（type=0且在好友列表中）
          list = list.filter(session => {
            const sessionType = session.type || 0
            // 群聊直接保留
            if (sessionType === 1) {
              return true
            }
            // 私聊需要检查是否在好友列表中
            const sessionId = session.id
            return validFriendIds.has(sessionId) || 
                   validFriendIds.has(String(sessionId)) || 
                   validFriendIds.has(Number(sessionId))
          })
        } catch (friendErr) {
          console.warn('获取好友列表失败，将显示所有会话:', friendErr)
          // 如果获取好友列表失败，仍然显示所有会话，但记录警告
        }
        
        // 转换时间格式并排序
        this.sessions = list.map(session => ({
          ...session,
          id: session.id,
          type: session.type || 0, // 0:私聊, 1:群聊
          name: session.name || '',
          avatar: session.avatar || '',
          lastMessageTime: session.lastMessageTime ? new Date(session.lastMessageTime).getTime() : 0,
          unreadCount: session.unreadCount || 0
        })).sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))

        // 如果 WebSocket 已连接，订阅所有群聊
        if (this.isConnected) {
          this._subscribeAllGroups()
        }
      } catch (e) {
        console.error('获取会话列表失败:', e)
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
      const sessionType = user.type || 0 // 0:私聊, 1:群聊
      const key = `${sessionType === 1 ? 'group' : 'private'}_${id}`
      
      // Reset pagination
      this.chatPagination[key] = { page: 1, hasMore: true, loading: false }
      
      // 如果是群聊，确保订阅了该群聊
      if (sessionType === 1) {
        this._subscribeGroup(id)
        // 始终加载最新历史消息，确保数据同步
        await this.loadHistory(id, sessionType)
        // 标记已读
        this.markAsRead(id, 1)
      } else if (sessionType === 0) {
        // 私聊：始终加载最新历史消息
        await this.loadHistory(id, sessionType)
        // 标记已读
        this.markAsRead(id, 0)
      }
    },

    async markAsRead(targetId, type = 0) {
      if (!targetId) return
      try {
        // 调用后端接口标记消息为已读
        if (type === 1) {
             await markGroupMessageRead(targetId)
        } else {
             // 参数 receiverId 指的是消息的发送者（即当前聊天对象），因为对于他发的消息，我是接收者
             await markMessagesAsRead(targetId)
        }
        
        // 更新本地会话的未读数
        const session = this.sessions.find(s => s.id === targetId && (s.type || 0) === type)
        if (session) {
            session.unreadCount = 0
        }
        
        console.log('已标记消息为已读，对象ID:', targetId, '类型:', type)
      } catch (e) {
        console.error('标记已读失败:', e)
      }
    },

    async loadHistory(userId, sessionType = 0, isLoadMore = false) {
      const userStore = useUserStore()
      const selfId = userStore.userInfo?.id
      const key = `${sessionType === 1 ? 'group' : 'private'}_${userId}`

      // 初始化分页信息
      if (!this.chatPagination[key]) {
        this.chatPagination[key] = { page: 1, hasMore: true, loading: false }
      }
      
      const pagination = this.chatPagination[key]
      
      // 如果是加载更多但没有更多数据，直接返回
      if (isLoadMore && !pagination.hasMore) return
      
      // 防止重复加载
      if (pagination.loading) return
      pagination.loading = true

      // 如果是首次加载（非加载更多），重置页码
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
            if (res && res.code === 1 && res.data) {
                rawList = res.data.records || []
                total = res.data.total || 0
                pagination.hasMore = (res.data.current * res.data.size) < total
            }
        } else {
            const res = await getPrivateMessageHistory(userId, pageToLoad, pageSize)
            if (res && res.code === 1 && res.data) {
                rawList = res.data.records || []
                total = res.data.total || 0
                pagination.hasMore = (res.data.current * res.data.size) < total
            }
        }

        const list = rawList.map(m => {
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
             // 特殊处理撤回消息
             if (msgObj.type === 5) {
                 msgObj.content = (msgObj.fromId === 'self') ? '你撤回了一条消息' : 
                    (sessionType === 1 ? `"${msgObj.senderNickname || '成员'}" 撤回了一条消息` : '对方撤回了一条消息')
                 msgObj.recall = true
             }
            return msgObj
        })

        // 反转数组：后端返回的是 [最新, ..., 较旧]，我们需要 [较旧, ..., 最新]
        // 实际上后端分页通常是按时间倒序：
        // Page 1: [Newest Msg 1, Newest Msg 2, ...]
        // So rawList[0] is the absolute newest.
        // We want to display in time order (old -> new).
        // So we reverse it: [..., Newest Msg 2, Newest Msg 1]
        const sortedList = list.reverse()

        if (isLoadMore) {
            // 加载更多（查看历史）：把旧数据拼接到头部
            this.messagesByUser[key] = [...sortedList, ...(this.messagesByUser[key] || [])]
            if (pagination.hasMore) {
                pagination.page += 1
            }
        } else {
            // 首次加载：覆盖
            this.messagesByUser[key] = sortedList
            if (pagination.hasMore) {
                pagination.page = 2
            }
        }
      } catch (e) {
        console.error('加载历史消息失败:', e)
        if (!isLoadMore) {
            this.messagesByUser[key] = []
        }
      } finally {
        pagination.loading = false
      }
    },

    connect() {
      if (this.stomp && this.connected) {
        console.log('WebSocket 已经连接')
        return
      }

      if (this.connecting) {
        console.log('WebSocket 正在连接中...')
        return
      }

      this.connecting = true
      this.connectionError = null

      try {
        const userStore = useUserStore()
        const token = userStore.token

        console.log('开始连接 WebSocket, token:', token ? '有' : '无')

        const url = getWebSocketUrl(token)
        const ws = new SockJS(url)
        const client = Stomp.over(ws)

        // 开发阶段打开调试日志
        client.debug = (msg) => {
          // 过滤掉心跳包等噪音，只显示重要帧
          if (msg.startsWith('>>> CONNECT') ||
              msg.startsWith('<<< CONNECTED') ||
              msg.startsWith('>>> SUBSCRIBE') ||
              msg.startsWith('>>> SEND') ||
              msg.startsWith('<<< MESSAGE') ||
              msg.startsWith('<<< ERROR')) {
            console.log('STOMP:', msg)
          }
        }

        const headers = {}
        if (token) {
          headers.Authorization = token.trim()
          console.log('设置 Authorization 头:', headers.Authorization)
        }

        client.connect(
            headers,
            (frame) => { // 连接成功回调
              console.log('WebSocket 连接成功! Frame:', frame)
              this.stomp = client
              this.connecting = false
              this.connected = true
              ElMessage.success('连接成功！')

              // 订阅私聊消息
              const subscription = client.subscribe('/user/queue/private', (message) => {
                console.log('收到 STOMP 消息, headers:', message.headers)
                console.log('收到 STOMP 消息 body:', message.body)
                try {
                  const result = JSON.parse(message.body)
                  console.log('解析后的消息结果:', result)

                  // 兼容直接返回消息对象的情况
                  if (result.code === 1) {
                    this._handleIncoming(result.data)
                  } else if (result.senderId && result.content) {
                    // 如果没有code但包含消息关键字段，直接视为消息处理
                    this._handleIncoming(result)
                  } else if (result.msg) {
                     // 忽略心跳或其他ACK消息
                     console.log('收到系统消息:', result.msg)
                  } else {
                    console.warn('收到未知格式响应:', result)
                  }
                } catch (e) {
                  console.error('解析消息失败:', e, '原始消息:', message.body)
                  ElMessage.error('消息解析失败')
                }
              })

              // 订阅用户在线状态变更
              const statusSubscription = client.subscribe('/topic/user-status', (message) => {
                try {
                    const statusDto = JSON.parse(message.body)
                    console.log('收到用户状态变更:', statusDto)
                    if (statusDto && statusDto.userId) {
                        const friendStore = useFriendStore()
                        friendStore.updateFriendStatus(statusDto.userId, statusDto.isOnline)
                        console.log(`用户 ${statusDto.userId} 状态已更新为: ${statusDto.isOnline ? '在线' : '离线'}`)
                    }
                } catch (e) {
                    console.error('解析用户状态消息失败:', e)
                }
              })

              // 订阅私聊撤回消息
              const recallSubscription = client.subscribe('/user/queue/private_recall', (message) => {
                  try {
                      const recallDto = JSON.parse(message.body)
                      this._handleRecallNotification(recallDto)
                  } catch (e) {
                      console.error('解析撤回消息失败:', e)
                  }
              })

              // 保存订阅以便后续管理
              this.subscriptions.set('private', subscription)
              this.subscriptions.set('status', statusSubscription)
              this.subscriptions.set('recall', recallSubscription)
              console.log('已订阅 /user/queue/private, /topic/user-status, /user/queue/private_recall')

              // 订阅所有群聊消息（从会话列表中获取群聊ID）
              this._subscribeAllGroups()

              // WebSocket 连接成功后，立即刷新好友在线状态
              this._refreshFriendStatus()

              // 测试发送一条连接成功的消息
              this._sendConnectionTest()

              // 启动心跳
              this.startHeartbeat()
            },
            (error) => { // 连接失败回调
              console.error('WebSocket 连接失败:', error)

              // 输出详细的错误信息
              if (error.headers) {
                console.error('错误头信息:', error.headers)
                if (error.headers.message) {
                  console.error('错误消息:', error.headers.message)
                }
              }

              this.stomp = null
              this.connecting = false
              this.connected = false

              let errorMsg = '连接失败'
              if (error.headers && error.headers.message) {
                errorMsg += ': ' + error.headers.message
              } else if (error.message) {
                errorMsg += ': ' + error.message
              }

              this.connectionError = errorMsg
              ElMessage.error(errorMsg)
            }
        )

        // 添加连接超时检测
        setTimeout(() => {
          if (this.connecting) {
            console.warn('WebSocket 连接超时')
            this.connecting = false
            this.connectionError = '连接超时'
            ElMessage.error('连接超时，请检查网络或服务器状态')

            // 尝试断开连接
            if (client && client.connected) {
              client.disconnect()
            }
          }
        }, 15000)

      } catch (e) {
        console.error('WebSocket 初始化异常:', e)
        this.connecting = false
        this.connectionError = e.message
        ElMessage.error('初始化失败: ' + e.message)
      }
    },

    // 处理撤回通知
    _handleRecallNotification(payload) {
        console.log('收到撤回通知:', payload)
        const messageId = payload.messageId
        if (!messageId) return

        // 查找消息所在的会话
        // 这里的 chatId 可能是对方 ID（如果是私聊）
        // 但我们需要更新本地缓存，所以需要遍历或知道是哪个会话
        // 简单做法：遍历 activeUser 的会话，或者直接根据 ID 查找
        
        // 由于我们不知道消息在哪个会话列表（虽然 payload.chatId 有提示，但可能是 sender 或 receiver ID）
        // 我们可以尝试在当前活跃会话中查找，或者遍历所有会话
        
        // 优化：RecallMessageDTO 包含 chatId，如果是私聊，sender收到的 chatId 是 receiverId，receiver收到的 chatId 是 senderId
        // 所以我们可以构建 key
        
        const userStore = useUserStore()
        const selfId = userStore.userInfo?.id
        
        let targetUserId
        if (payload.recallUserId === selfId) {
            // 是我自己撤回的，消息在 "private_receiverId" 中
            targetUserId = payload.chatId 
        } else {
            // 是对方撤回的，消息在 "private_senderId" 中 (即 recallUserId)
            targetUserId = payload.recallUserId
        }
        
        const key = `private_${targetUserId}`
        const list = this.messagesByUser[key] || []
        
        const idx = list.findIndex(m => m.id === messageId)
        if (idx !== -1) {
             const newList = [...list]
             newList[idx] = {
               ...newList[idx],
               content: payload.recallUserId === selfId ? '你撤回了一条消息' : '对方撤回了一条消息',
               type: 5, 
               recall: true
             }
             this.messagesByUser[key] = newList
             console.log('已更新撤回消息状态:', messageId)
        } else {
            console.warn('未找到要撤回的消息:', messageId)
        }
    },

    // 发送连接测试消息
    _sendConnectionTest() {
      const userStore = useUserStore()
      const currentUser = userStore.userInfo

      if (currentUser && currentUser.id) {
        console.log('连接测试: 当前用户 ID', currentUser.id)
        // 这里可以发送一条测试消息，或者只是记录连接状态
      }
    },

    // 更新会话列表
    _updateSession(sessionId, timestamp, incrementUnread = false, isGroup = false) {
      const type = isGroup ? 1 : 0
      const sessionIndex = this.sessions.findIndex(s => s.id == sessionId && (s.type || 0) === type)
      
      if (sessionIndex !== -1) {
        const session = this.sessions[sessionIndex]
        
        // Check if we should increment unread
        let newUnreadCount = session.unreadCount || 0
        if (incrementUnread) {
            // If active user is this session, don't increment
            const isActive = this.activeUser && this.activeUser.id == sessionId && (this.activeUser.type || 0) === type
            if (!isActive) {
                newUnreadCount++
            }
        }
        
        // 只有当时间更新时才重新排序
        if (!session.lastMessageTime || session.lastMessageTime < timestamp || incrementUnread) {
          this.sessions[sessionIndex] = {
            ...session,
            lastMessageTime: timestamp > (session.lastMessageTime || 0) ? timestamp : session.lastMessageTime,
            unreadCount: newUnreadCount
          }
          // 重新排序会话列表
          this.sessions.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
        }
      }
    },

    // 刷新好友在线状态
    async _refreshFriendStatus() {
      try {
        const friendStore = useFriendStore()
        if (friendStore.friends && friendStore.friends.length > 0) {
          const ids = friendStore.friends.map(f => f.id)
          const { batchCheckUserOnlineStatus } = await import('../api/userStatus')
          const statusRes = await batchCheckUserOnlineStatus(ids)
          if (statusRes && statusRes.code === 1) {
            const statusMap = statusRes.data
            // 批量更新好友状态
            friendStore.friends.forEach(friend => {
              const isOnline = !!statusMap[friend.id]
              // 只有当状态真正改变时才更新
              if (friend.online !== isOnline) {
                friendStore.updateFriendStatus(friend.id, isOnline)
              }
            })
            console.log('好友在线状态已刷新')
          }
        }
      } catch (e) {
        console.error('刷新好友在线状态失败:', e)
      }
    },

    _handleIncoming(payload) {
      try {
        console.log('收到WebSocket消息:', payload)

        // 处理发送失败的消息
        if (payload.error) {
          console.warn('处理发送失败消息:', payload)
          const otherId = payload.receiverId // 接收者ID即为当前聊天对象ID
          const tempId = payload.tempId

          if (otherId && tempId) {
            const key = `private_${otherId}`
            const list = this.messagesByUser[key]
            
            if (list) {
              const idx = list.findIndex(m => m.id == tempId || m.tempId == tempId)
              if (idx !== -1) {
                // 更新消息状态为发送失败
                const newList = [...list]
                newList[idx] = {
                  ...newList[idx],
                  isSending: false,
                  sendError: true,
                  errorMessage: payload.errorMessage || '发送失败'
                }
                // 强制更新 store 状态
                this.messagesByUser[key] = newList
                console.log('已标记消息为发送失败, tempId:', tempId)
              } else {
                console.warn('未找到对应的临时消息:', tempId)
              }
            } else {
              console.warn('未找到对应的会话列表:', otherId)
            }
          }
          return
        }

        const userStore = useUserStore()
        const selfId = userStore.userInfo?.id

        if (!selfId) {
          console.error('未获取到当前用户信息')
          ElMessage.error('未获取到当前用户信息，无法接收消息')
          return
        }

        // 确定消息来源（对方用户ID）
        let otherId
        let isIncoming = false
        if (payload.senderId === selfId) {
          // 这是自己发送的消息回执
          otherId = payload.receiverId
          console.log('收到自己的消息回执，对方 ID:', otherId)
        } else {
          // 这是对方发来的消息
          otherId = payload.senderId
          isIncoming = true
          console.log('收到对方消息，对方 ID:', otherId)
        }

        if (!otherId) {
          console.error('无法确定消息对方ID:', payload)
          return
        }

        // 处理时间戳
        let ts
        if (payload.sendTime) {
          // 尝试多种时间格式解析
          ts = new Date(payload.sendTime).getTime()
          if (isNaN(ts)) {
            // 如果解析失败，使用当前时间
            ts = Date.now()
            console.warn('消息时间格式不正确，使用当前时间:', payload.sendTime)
          }
        } else {
          ts = Date.now()
          console.warn('消息缺少 sendTime，使用当前时间')
        }

        // 构建前端消息对象
        const uiMsg = {
          id: payload.messageId || payload.id || `temp_${Date.now()}`,
          fromId: payload.senderId === selfId ? 'self' : payload.senderId,
          toId: payload.receiverId,
          content: payload.content,
          timestamp: ts,
          type: payload.messageType || 1,
          fileName: payload.fileName,
          fileSize: payload.fileSize,
          fileUrl: payload.fileUrl,
          // 保留原始payload用于调试
          originalPayload: payload,
          // 添加消息状态
          isSending: false,
          isReceived: true,
          sendError: false
        }

        // 特殊处理撤回消息的内容展示
        if (uiMsg.type === 5) {
            if (uiMsg.fromId === 'self') {
                uiMsg.content = '你撤回了一条消息'
            } else {
                uiMsg.content = '对方撤回了一条消息'
            }
        }

        console.log('构建的UI消息:', uiMsg)

        // 更新消息列表
        const key = `private_${otherId}`
        const list = this.messagesByUser[key] || []

        // 检查是否重复消息或需要更新
        const existingIndex = list.findIndex(msg => msg.id === uiMsg.id)
        
        if (existingIndex !== -1) {
          // 如果消息已存在，更新它 (支持消息撤回等状态更新)
          const newList = [...list]
          // 保留一些本地状态 if needed, but mostly overwrite from backend
          newList[existingIndex] = {
            ...newList[existingIndex],
            ...uiMsg,
            // 确保 type 和 content 被更新
            type: uiMsg.type,
            content: uiMsg.content
          }
          this.messagesByUser[key] = newList
          console.log('更新已有消息:', uiMsg.id)
          return
        }

        // 尝试匹配临时消息（将临时消息转换为真实消息）
        let tempIndex = -1
        // 1. 优先尝试通过 tempId 匹配
        if (payload.tempId) {
            tempIndex = list.findIndex(msg => msg.id === payload.tempId || msg.tempId === payload.tempId)
        }
        // 2. 如果是自己发送的消息，尝试通过内容和时间模糊匹配
        if (tempIndex === -1 && payload.senderId === selfId) {
            tempIndex = list.findIndex(msg => {
                // 必须是临时消息
                if (!String(msg.id).startsWith('temp_')) return false
                // 类型必须一致
                if (msg.type !== uiMsg.type) return false
                // 文本消息匹配内容
                if (msg.type === 1) {
                    return msg.content === uiMsg.content && Math.abs(msg.timestamp - uiMsg.timestamp) < 5000
                }
                // 图片/文件匹配 URL
                if (msg.type === 2 || msg.type === 3) {
                    return msg.fileUrl === uiMsg.fileUrl
                }
                return false
            })
        }

        if (tempIndex !== -1) {
            // 找到了对应的临时消息，直接替换为真实消息
            const newList = [...list]
            newList[tempIndex] = {
                ...newList[tempIndex], // 保留本地的一些属性
                ...uiMsg,              // 覆盖为后端属性
                id: uiMsg.id,          // 关键：更新为真实ID
                isSending: false,
                sendError: false
            }
            this.messagesByUser[key] = newList
            console.log('临时消息已确认为真实消息:', uiMsg.id)
            return
        }

        const isDuplicate = list.some(msg => {
          // 通过临时ID去重
          if (msg.tempId && payload.tempId && msg.tempId === payload.tempId) return true
          // 对于文件/图片消息，通过fileUrl和timestamp去重
          if (uiMsg.type === 2 || uiMsg.type === 3) {
            if (msg.type === uiMsg.type && msg.fileUrl === uiMsg.fileUrl && 
                Math.abs(msg.timestamp - uiMsg.timestamp) < 2000) {
              return true
            }
          } else {
            // 对于文本消息，通过content和timestamp去重
            if (msg.content === uiMsg.content && Math.abs(msg.timestamp - uiMsg.timestamp) < 1000) {
              return true
            }
          }
          return false
        })

        if (!isDuplicate) {
          this.messagesByUser[key] = [...list, uiMsg]
          console.log('消息已添加到列表，当前消息数:', this.messagesByUser[key].length)

          // 更新会话列表
          // 如果是接收到的消息，增加未读数
          this._updateSession(otherId, ts, isIncoming, false)
        } else {
          console.log('跳过重复消息')
        }

      } catch (e) {
        console.error('处理收到的消息失败:', e, '原始payload:', payload)
      }
    },

    // 撤回消息
    async recallMessage(messageId) {
      if (!messageId) return false
      
      const isGroup = this.activeUser && (this.activeUser.type === 1)
      
      try {
        let res
        if (isGroup) {
            res = await recallGroupMessage(this.activeUser.id, messageId)
        } else {
            res = await recallPrivateMessage(messageId)
        }
        
        if (res.code === 1) {
          // 成功
          // 更新本地 store
          // 找到当前活跃会话的消息
          if (!this.activeUser) return true
          const key = `${isGroup ? 'group' : 'private'}_${this.activeUser.id}`
          const list = this.messagesByUser[key] || []
          const idx = list.findIndex(m => m.id === messageId)
          if (idx !== -1) {
             const newList = [...list]
             newList[idx] = {
               ...newList[idx],
               content: '你撤回了一条消息',
               type: 5, // 5 for recall
               recall: true
             }
             this.messagesByUser[key] = newList
          }
          return true
        } else {
          ElMessage.error(res.msg || '撤回失败')
          return false
        }
      } catch (e) {
        console.error('撤回消息失败:', e)
        ElMessage.error('撤回失败')
        return false
      }
    },

    sendMessage(toUserId, content, type = 1, fileName = null, fileSize = null, fileUrl = null) {
      const userStore = useUserStore()
      const senderId = userStore.userInfo?.id

      if (!senderId) {
        ElMessage.error('未获取到当前用户信息')
        return Promise.resolve(false)
      }

      if (!this.isConnected) {
        ElMessage.error('连接未建立，请先连接WebSocket')
        return Promise.resolve(false)
      }

      if (!content || content.trim() === '') {
        ElMessage.error('消息内容不能为空')
        return Promise.resolve(false)
      }

      // 生成临时消息ID
      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

      // 构建消息DTO
      const dto = {
        senderId: senderId,
        receiverId: toUserId,
        content: content.trim(),
        messageType: type,
        fileName: fileName || undefined,
        fileSize: fileSize || undefined,
        fileUrl: fileUrl || undefined,
        tempId: tempId
      }

      try {
        console.log('发送消息:', dto)

        // 直接发送，不等待回调（STOMP send 默认是异步的且通常无回调）
        this.stomp.send('/app/chat.private', {}, JSON.stringify(dto))

        // 立即在本地添加临时消息（优化用户体验）
        const selfMsg = {
          id: tempId,
          fromId: 'self',
          toId: toUserId,
          content: content.trim(),
          timestamp: Date.now(),
          type: type,
          fileName: fileName || null,
          fileSize: fileSize || null,
          fileUrl: fileUrl || null,
          isSending: true, // 标记为发送中状态，等待服务器回包更新状态
          sendError: false,
          tempId: tempId
        }

        const key = `private_${toUserId}`
        const list = this.messagesByUser[key] || []
        this.messagesByUser[key] = [...list, selfMsg]

        // 更新会话列表的最后消息时间 (sending message does not increment unread)
        this._updateSession(toUserId, Date.now(), false, false)

        console.log('临时消息已添加，等待服务器确认，临时ID:', tempId)
        return Promise.resolve(true)

      } catch (e) {
        console.error('发送消息失败:', e)
        ElMessage.error('发送失败: ' + (e.message || '未知错误'))
        
        // 如果发送抛出同步异常，标记失败
        const key = `private_${toUserId}`
        const list = this.messagesByUser[key] || []
        const selfMsg = {
            id: tempId,
            fromId: 'self',
            toId: toUserId,
            content: content.trim(),
            timestamp: Date.now(),
            type: type,
            fileName: fileName || null,
            fileSize: fileSize || null,
            fileUrl: fileUrl || null,
            isSending: false,
            sendError: true,
            tempId: tempId
        }
        this.messagesByUser[key] = [...list, selfMsg]
        
        return Promise.resolve(false)
      }
    },

    // 发送群聊消息
    sendGroupMessage(groupId, content, type = 1, fileName = null, fileSize = null, fileUrl = null) {
      const userStore = useUserStore()
      const senderId = userStore.userInfo?.id

      if (!senderId) {
        ElMessage.error('未获取到当前用户信息')
        return Promise.resolve(false)
      }

      if (!this.isConnected) {
        ElMessage.error('连接未建立，请先连接WebSocket')
        return Promise.resolve(false)
      }

      if (!content || content.trim() === '') {
        ElMessage.error('消息内容不能为空')
        return Promise.resolve(false)
      }

      if (!groupId) {
        ElMessage.error('群聊ID不能为空')
        return Promise.resolve(false)
      }

      // 生成临时消息ID
      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

      // 构建群聊消息DTO
      const dto = {
        senderId: senderId,
        groupId: groupId,
        content: content.trim(),
        messageType: type,
        fileName: fileName || undefined,
        fileSize: fileSize || undefined,
        fileUrl: fileUrl || undefined,
        tempId: tempId
      }

      try {
        console.log('发送群聊消息:', dto)

        // 发送到群聊端点
        this.stomp.send('/app/chat.group', {}, JSON.stringify(dto))

        // 立即在本地添加临时消息（优化用户体验）
        const selfMsg = {
          id: tempId,
          fromId: 'self',
          toId: groupId,
          groupId: groupId,
          content: content.trim(),
          timestamp: Date.now(),
          type: type,
          fileName: fileName || null,
          fileSize: fileSize || null,
          fileUrl: fileUrl || null,
          isSending: true, // 标记为发送中状态，等待服务器回包更新状态
          sendError: false,
          tempId: tempId
        }

        const key = `group_${groupId}`
        const list = this.messagesByUser[key] || []
        this.messagesByUser[key] = [...list, selfMsg]

        // 更新会话列表的最后消息时间
        this._updateSession(groupId, Date.now(), false, true)

        console.log('群聊临时消息已添加，等待服务器确认，临时ID:', tempId)
        return Promise.resolve(true)

      } catch (e) {
        console.error('发送群聊消息失败:', e)
        ElMessage.error('发送失败: ' + (e.message || '未知错误'))
        
        // 如果发送抛出同步异常，标记失败
        const key = `group_${groupId}`
        const list = this.messagesByUser[key] || []
        const selfMsg = {
            id: tempId,
            fromId: 'self',
            toId: groupId,
            groupId: groupId,
            content: content.trim(),
            timestamp: Date.now(),
            type: type,
            fileName: fileName || null,
            fileSize: fileSize || null,
            fileUrl: fileUrl || null,
            isSending: false,
            sendError: true,
            tempId: tempId
        }
        this.messagesByUser[key] = [...list, selfMsg]
        
        return Promise.resolve(false)
      }
    },

    // 处理接收到的群聊消息
    _handleGroupMessage(payload) {
      try {
        console.log('收到群聊消息:', payload)

        // 优先处理撤回消息 (RecallMessageDTO 结构不同，包含 isGroup: true 和 recallUserId)
        if (payload.isGroup && payload.recallUserId) {
            const groupId = payload.chatId
            const messageId = payload.messageId
            const key = `group_${groupId}`
            const list = this.messagesByUser[key] || []
            
            const idx = list.findIndex(m => m.id === messageId)
            if (idx !== -1) {
                 const newList = [...list]
                 const oldMsg = newList[idx]
                 const senderNickname = oldMsg.senderNickname || '成员'
                 const isSelf = oldMsg.fromId === 'self'
                 
                 newList[idx] = {
                   ...oldMsg,
                   content: isSelf ? '你撤回了一条消息' : `"${senderNickname}" 撤回了一条消息`,
                   type: 5,
                   recall: true
                 }
                 this.messagesByUser[key] = newList
                 console.log('群聊消息已更新为撤回状态:', messageId)
            } else {
                console.warn('收到群聊撤回通知，但本地未找到对应消息:', messageId)
            }
            return
        }

        const userStore = useUserStore()
        const selfId = userStore.userInfo?.id

        if (!selfId) {
          console.error('未获取到当前用户信息')
          return
        }

        const groupId = payload.groupId
        if (!groupId) {
          console.error('群聊消息缺少 groupId:', payload)
          return
        }

        // 处理时间戳
        let ts
        if (payload.sendTime) {
          ts = new Date(payload.sendTime).getTime()
          if (isNaN(ts)) {
            ts = Date.now()
            console.warn('群聊消息时间格式不正确，使用当前时间:', payload.sendTime)
          }
        } else {
          ts = Date.now()
          console.warn('群聊消息缺少 sendTime，使用当前时间')
        }

        // 构建前端消息对象
        const uiMsg = {
          id: payload.messageId || payload.id || `group_${Date.now()}`,
          fromId: payload.senderId === selfId ? 'self' : payload.senderId,
          toId: groupId,
          groupId: groupId,
          content: payload.content,
          timestamp: ts,
          type: payload.messageType || 1,
          fileName: payload.fileName,
          fileSize: payload.fileSize,
          fileUrl: payload.fileUrl,
          senderNickname: payload.senderNickname,
          senderAvatar: payload.senderAvatar,
          messageSeq: payload.messageSeq,
          isSending: false,
          isReceived: true,
          sendError: false,
          tempId: payload.tempId
        }

        // 特殊处理撤回消息的内容展示
        if (uiMsg.type === 5) {
            uiMsg.content = (uiMsg.fromId === 'self') ? '你撤回了一条消息' : `"${uiMsg.senderNickname || '成员'}" 撤回了一条消息`
        }

        console.log('构建的群聊UI消息:', uiMsg)

        // 更新消息列表
        const key = `group_${groupId}`
        const list = this.messagesByUser[key] || []

        // 检查是否重复消息或需要更新
        const existingIndex = list.findIndex(msg => msg.id === uiMsg.id)

        if (existingIndex !== -1) {
             // 如果消息已存在，更新它 (支持消息撤回等状态更新)
             const newList = [...list]
             newList[existingIndex] = {
               ...newList[existingIndex],
               ...uiMsg,
               // 确保 type 和 content 被更新
                type: uiMsg.type,
                content: uiMsg.type === 5 ? (uiMsg.fromId === 'self' ? '你撤回了一条消息' : `"${uiMsg.senderNickname || '成员'}" 撤回了一条消息`) : uiMsg.content,
                recall: uiMsg.type === 5
             }
             this.messagesByUser[key] = newList
             console.log('更新群聊已有消息:', uiMsg.id)
             return
        }

        // 尝试匹配临时消息（将临时消息转换为真实消息）
        if (payload.tempId) {
            const tempIndex = list.findIndex(msg => msg.tempId === payload.tempId || msg.id === payload.tempId)
            if (tempIndex !== -1) {
                 // 替换临时消息
                 const newList = [...list]
                 newList[tempIndex] = {
                    ...newList[tempIndex],
                    ...uiMsg,
                    id: uiMsg.id, // 关键：更新为真实ID
                    isSending: false,
                    sendError: false
                 }
                 this.messagesByUser[key] = newList
                 console.log('群聊临时消息已确认为真实消息:', uiMsg.id)
                 return
            }
        }

        const isDuplicate = list.some(msg => {
          // 通过消息序号去重（群聊特有）
          if (uiMsg.messageSeq && msg.messageSeq && msg.messageSeq === uiMsg.messageSeq) return true
          // 对于文件/图片消息，通过fileUrl和timestamp去重
          if (uiMsg.type === 2 || uiMsg.type === 3) {
            if (msg.type === uiMsg.type && msg.fileUrl === uiMsg.fileUrl && 
                Math.abs(msg.timestamp - uiMsg.timestamp) < 2000) {
              return true
            }
          } else {
            // 对于文本消息，通过content和timestamp去重
            if (msg.content === uiMsg.content && Math.abs(msg.timestamp - uiMsg.timestamp) < 1000) {
              return true
            }
          }
          return false
        })

        if (!isDuplicate) {
          this.messagesByUser[key] = [...list, uiMsg]
          console.log('群聊消息已添加到列表，当前消息数:', this.messagesByUser[key].length)

          // 更新会话列表
          // If message is from self, do not increment unread.
          const isIncoming = payload.senderId !== selfId
          this._updateSession(groupId, ts, isIncoming, true)
        } else {
          console.log('跳过重复的群聊消息')
        }

      } catch (e) {
        console.error('处理群聊消息失败:', e, '原始payload:', payload)
      }
    },

    // 订阅所有群聊消息
    _subscribeAllGroups() {
      if (!this.isConnected) {
        console.warn('WebSocket 未连接，无法订阅群聊')
        return
      }

      // 从会话列表中获取所有群聊（type === 1）
      const groupSessions = this.sessions.filter(s => s.type === 1)
      
      groupSessions.forEach(session => {
        const groupId = session.id
        if (groupId && !this.groupSubscriptions.has(groupId)) {
          this._subscribeGroup(groupId)
        }
      })

      console.log(`已订阅 ${this.groupSubscriptions.size} 个群聊`)
    },

    // 订阅单个群聊
    _subscribeGroup(groupId) {
      if (!this.isConnected || !groupId) {
        return
      }

      // 如果已经订阅，跳过
      if (this.groupSubscriptions.has(groupId)) {
        console.log(`群聊 ${groupId} 已订阅，跳过`)
        return
      }

      try {
        const topic = `/topic/group/${groupId}`
        const subscription = this.stomp.subscribe(topic, (message) => {
          console.log(`收到群聊消息，群ID: ${groupId}, body:`, message.body)
          try {
            const result = JSON.parse(message.body)
            console.log('解析后的群聊消息:', result)
            
            // 处理群聊消息
            this._handleGroupMessage(result)
          } catch (e) {
            console.error('解析群聊消息失败:', e, '原始消息:', message.body)
            ElMessage.error('群聊消息解析失败')
          }
        })

        this.groupSubscriptions.set(groupId, subscription)
        console.log(`已订阅群聊: ${groupId}, topic: ${topic}`)
      } catch (e) {
        console.error(`订阅群聊 ${groupId} 失败:`, e)
      }
    },

    // 取消订阅群聊
    _unsubscribeGroup(groupId) {
      if (!groupId) return

      const subscription = this.groupSubscriptions.get(groupId)
      if (subscription) {
        try {
          subscription.unsubscribe()
          this.groupSubscriptions.delete(groupId)
          console.log(`已取消订阅群聊: ${groupId}`)
        } catch (e) {
          console.error(`取消订阅群聊 ${groupId} 失败:`, e)
        }
      }
    },

    // 断开连接方法
    disconnect() {
      // 取消所有订阅
      this.subscriptions.forEach((subscription, key) => {
        try {
          subscription.unsubscribe()
          console.log('取消订阅:', key)
        } catch (e) {
          console.error('取消订阅失败:', key, e)
        }
      })
      this.subscriptions.clear()

      // 取消所有群聊订阅
      this.groupSubscriptions.forEach((subscription, groupId) => {
        try {
          subscription.unsubscribe()
          console.log('取消群聊订阅:', groupId)
        } catch (e) {
          console.error('取消群聊订阅失败:', groupId, e)
        }
      })
      this.groupSubscriptions.clear()

      // 停止心跳
      this.stopHeartbeat()
      this.heartbeatCount = 0 // 重置心跳计数器

      // 断开 STOMP 连接
      if (this.stomp) {
        try {
          this.stomp.disconnect(() => {
            console.log('WebSocket 已断开')
          })
        } catch (e) {
          console.error('断开连接时出错:', e)
        }
        this.stomp = null
      }

      this.connected = false
      this.connecting = false
      this.connectionError = null
    },

    // 重新连接方法
    reconnect() {
      if (this.connected || this.connecting) {
        console.log('已在连接或已连接，跳过重连')
        return
      }
      console.log('尝试重新连接...')
      this.disconnect()

      // 延迟一下再重连，避免频繁重连
      setTimeout(() => {
        this.connect()
      }, 1000)
    },

    // 检查连接状态
    checkConnectionStatus() {
      console.log('=== WebSocket 连接状态 ===')
      console.log('STOMP 实例:', this.stomp)
      console.log('连接中:', this.connecting)
      console.log('已连接:', this.connected)
      console.log('STOMP 连接状态:', this.stomp?.connected)
      console.log('连接错误:', this.connectionError)
      console.log('活跃订阅数量:', this.subscriptions.size)

      return {
        stomp: this.stomp,
        connecting: this.connecting,
        connected: this.connected,
        stompConnected: this.stomp?.connected,
        error: this.connectionError,
        subscriptions: this.subscriptions.size
      }
    },

    // 手动测试消息发送
    testSendMessage(toUserId = 4) {
      const testContent = `测试消息 ${new Date().toLocaleTimeString()}`
      console.log('发送测试消息给用户:', toUserId, '内容:', testContent)
      return this.sendMessage(toUserId, testContent)
    },

    // 启动应用层心跳
    startHeartbeat() {
      this.stopHeartbeat() // 防止重复启动
      console.log('启动应用层心跳，每30秒发送一次')
      this.heartbeatTimer = setInterval(() => {
        if (this.isConnected) {
          try {
            // 发送空消息到 /app/heartbeat
            this.stomp.send('/app/heartbeat', {}, JSON.stringify({}))
            console.log('已发送心跳包')
            
            // 每2次心跳（60秒）刷新一次好友在线状态
            if (!this.heartbeatCount) this.heartbeatCount = 0
            this.heartbeatCount++
            if (this.heartbeatCount >= 2) {
              this.heartbeatCount = 0
              this._refreshFriendStatus()
            }
          } catch (e) {
            console.error('发送心跳失败:', e)
          }
        }
      }, 30000) // 改为30秒，更及时
    },

    // 停止应用层心跳
    stopHeartbeat() {
      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer)
        this.heartbeatTimer = null
        console.log('已停止应用层心跳')
      }
    }
  }
})
