import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import Stomp from 'stompjs'
import SockJS from 'sockjs-client'
import { useUserStore } from '../user'
import { useFriendStore } from '../friend'
import { useChatStore } from '../chat'
import { useMessageActions } from './useMessageActions'
import logger from '../../utils/logger'

const sockPath = '/api/minichat-websocket'

function getWebSocketUrl(token: string): string {
  const wsBase = import.meta.env.VITE_WS_BASE
  if (wsBase) {
    return token ? `${wsBase}?token=${encodeURIComponent(token)}` : wsBase
  }
  return token ? `${sockPath}?token=${encodeURIComponent(token)}` : sockPath
}

interface ConnectionStatus {
  stomp: Stomp.Client | null
  connecting: boolean
  connected: boolean
  stompConnected: boolean | undefined
  error: string | null
  subscriptions: number
}

export const useWebSocketStore = defineStore('ws', {
  state: () => ({
    stomp: null as Stomp.Client | null,
    connecting: false,
    connected: false,
    connectionError: null as string | null,
    subscriptions: new Map<string, Stomp.Subscription>(),
    groupSubscriptions: new Map<number, Stomp.Subscription>(),
    heartbeatTimer: null as ReturnType<typeof setInterval> | null,
    heartbeatCount: 0
  }),

  getters: {
    isConnected(state): boolean {
      return state.connected && !!state.stomp && !!state.stomp.connected
    }
  },

  actions: {
    connect() {
      if (this.stomp && this.connected) {
        logger.debug('WebSocket 已经连接')
        return
      }

      if (this.connecting) {
        logger.debug('WebSocket 正在连接中...')
        return
      }

      this.connecting = true
      this.connectionError = null

      try {
        const userStore = useUserStore()
        const token = userStore.token

        logger.debug('开始连接 WebSocket, token:', token ? '有' : '无')

        const url = getWebSocketUrl(token)
        const ws = new SockJS(url)
        const client = Stomp.over(ws)

        client.debug = (msg: string) => {
          if (
            msg.startsWith('>>> CONNECT') ||
            msg.startsWith('<<< CONNECTED') ||
            msg.startsWith('>>> SUBSCRIBE') ||
            msg.startsWith('>>> SEND') ||
            msg.startsWith('<<< MESSAGE') ||
            msg.startsWith('<<< ERROR')
          ) {
            logger.debug('STOMP:', msg)
          }
        }

        const headers: Record<string, string> = {}
        if (token) {
          headers.Authorization = token.trim()
          logger.debug('设置 Authorization 头:', headers.Authorization)
        }

        client.connect(
          headers,
          () => {
            logger.debug('WebSocket 连接成功!')
            this.stomp = client
            this.connecting = false
            this.connected = true
            ElMessage.success('连接成功！')

            const msgActions = useMessageActions()

            const subscription = client.subscribe('/user/queue/private', (message) => {
              logger.debug('收到 STOMP 消息, headers:', message.headers)
              logger.debug('收到 STOMP 消息 body:', message.body)
              try {
                const result = JSON.parse(message.body)
                logger.debug('解析后的消息结果:', result)

                if (result.code === 1) {
                  msgActions._handleIncoming(result.data)
                } else if (result.senderId && result.content) {
                  msgActions._handleIncoming(result)
                } else if (result.msg) {
                  logger.debug('收到系统消息:', result.msg)
                } else {
                  logger.warn('收到未知格式响应:', result)
                }
              } catch (e: unknown) {
                logger.error('解析消息失败:', e, '原始消息:', message.body)
                ElMessage.error('消息解析失败')
              }
            })

            const statusSubscription = client.subscribe('/topic/user-status', (message) => {
              try {
                const statusDto = JSON.parse(message.body)
                logger.debug('收到用户状态变更:', statusDto)
                if (statusDto && statusDto.userId) {
                  const friendStore = useFriendStore()
                  friendStore.updateFriendStatus(statusDto.userId, statusDto.isOnline)
                  logger.debug(
                    `用户 ${statusDto.userId} 状态已更新为: ${statusDto.isOnline ? '在线' : '离线'}`
                  )
                }
              } catch (e: unknown) {
                logger.error('解析用户状态消息失败:', e)
              }
            })

            const recallSubscription = client.subscribe('/user/queue/private_recall', (message) => {
              try {
                const recallDto = JSON.parse(message.body)
                msgActions._handleRecallNotification(recallDto)
              } catch (e: unknown) {
                logger.error('解析撤回消息失败:', e)
              }
            })

            this.subscriptions.set('private', subscription)
            this.subscriptions.set('status', statusSubscription)
            this.subscriptions.set('recall', recallSubscription)
            logger.debug('已订阅 /user/queue/private, /topic/user-status, /user/queue/private_recall')

            this._subscribeAllGroups()
            this._refreshFriendStatus()
            this._sendConnectionTest()
            this.startHeartbeat()
          },
          (error: Stomp.Frame) => {
            logger.error('WebSocket 连接失败:', error)

            if (error.headers) {
              logger.error('错误头信息:', error.headers)
              if (error.headers.message) {
                logger.error('错误消息:', error.headers.message)
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

        setTimeout(() => {
          if (this.connecting) {
            logger.warn('WebSocket 连接超时')
            this.connecting = false
            this.connectionError = '连接超时'
            ElMessage.error('连接超时，请检查网络或服务器状态')

            if (client && client.connected) {
              client.disconnect()
            }
          }
        }, 15000)
      } catch (e: unknown) {
        const err = e as Error
        logger.error('WebSocket 初始化异常:', e)
        this.connecting = false
        this.connectionError = err.message
        ElMessage.error('初始化失败: ' + err.message)
      }
    },

    disconnect() {
      this.subscriptions.forEach((subscription, key) => {
        try {
          subscription.unsubscribe()
          logger.debug('取消订阅:', key)
        } catch (e: unknown) {
          logger.error('取消订阅失败:', key, e)
        }
      })
      this.subscriptions.clear()

      this.groupSubscriptions.forEach((subscription, groupId) => {
        try {
          subscription.unsubscribe()
          logger.debug('取消群聊订阅:', groupId)
        } catch (e: unknown) {
          logger.error('取消群聊订阅失败:', groupId, e)
        }
      })
      this.groupSubscriptions.clear()

      this.stopHeartbeat()
      this.heartbeatCount = 0

      if (this.stomp) {
        try {
          this.stomp.disconnect(() => {
            logger.debug('WebSocket 已断开')
          })
        } catch (e: unknown) {
          logger.error('断开连接时出错:', e)
        }
        this.stomp = null
      }

      this.connected = false
      this.connecting = false
      this.connectionError = null
    },

    reconnect() {
      if (this.connected || this.connecting) {
        logger.debug('已在连接或已连接，跳过重连')
        return
      }
      logger.debug('尝试重新连接...')
      this.disconnect()

      setTimeout(() => {
        this.connect()
      }, 1000)
    },

    checkConnectionStatus(): ConnectionStatus {
      logger.debug('=== WebSocket 连接状态 ===')
      logger.debug('STOMP 实例:', this.stomp)
      logger.debug('连接中:', this.connecting)
      logger.debug('已连接:', this.connected)
      logger.debug('STOMP 连接状态:', this.stomp?.connected)
      logger.debug('连接错误:', this.connectionError)
      logger.debug('活跃订阅数量:', this.subscriptions.size)

      return {
        stomp: this.stomp,
        connecting: this.connecting,
        connected: this.connected,
        stompConnected: this.stomp?.connected,
        error: this.connectionError,
        subscriptions: this.subscriptions.size
      }
    },

    testSendMessage(toUserId: number = 4) {
      const testContent = `测试消息 ${new Date().toLocaleTimeString()}`
      logger.debug('发送测试消息给用户:', toUserId, '内容:', testContent)
      return useMessageActions().sendMessage(toUserId, testContent)
    },

    startHeartbeat() {
      this.stopHeartbeat()
      logger.debug('启动应用层心跳，每30秒发送一次')
      this.heartbeatTimer = setInterval(() => {
        if (this.isConnected && this.stomp) {
          try {
            this.stomp.send('/app/heartbeat', {}, JSON.stringify({}))
            logger.debug('已发送心跳包')

            if (!this.heartbeatCount) this.heartbeatCount = 0
            this.heartbeatCount++
            if (this.heartbeatCount >= 2) {
              this.heartbeatCount = 0
              this._refreshFriendStatus()
            }
          } catch (e: unknown) {
            logger.error('发送心跳失败:', e)
          }
        }
      }, 30000)
    },

    stopHeartbeat() {
      if (this.heartbeatTimer) {
        clearInterval(this.heartbeatTimer)
        this.heartbeatTimer = null
        logger.debug('已停止应用层心跳')
      }
    },

    _subscribeAllGroups() {
      if (!this.isConnected) {
        logger.warn('WebSocket 未连接，无法订阅群聊')
        return
      }

      const groupSessions = useChatStore().sessions.filter((s) => s.type === 1)

      groupSessions.forEach((session) => {
        const groupId = session.id
        if (groupId && !this.groupSubscriptions.has(groupId)) {
          this._subscribeGroup(groupId)
        }
      })

      logger.debug(`已订阅 ${this.groupSubscriptions.size} 个群聊`)
    },

    _subscribeGroup(groupId: number) {
      if (!this.isConnected || !groupId || !this.stomp) {
        return
      }

      if (this.groupSubscriptions.has(groupId)) {
        logger.debug(`群聊 ${groupId} 已订阅，跳过`)
        return
      }

      try {
        const topic = `/topic/group/${groupId}`
        const subscription = this.stomp.subscribe(topic, (message) => {
          logger.debug(`收到群聊消息，群ID: ${groupId}, body:`, message.body)
          try {
            const result = JSON.parse(message.body)
            logger.debug('解析后的群聊消息:', result)

            useMessageActions()._handleGroupMessage(result)
          } catch (e: unknown) {
            logger.error('解析群聊消息失败:', e, '原始消息:', message.body)
            ElMessage.error('群聊消息解析失败')
          }
        })

        this.groupSubscriptions.set(groupId, subscription)
        logger.debug(`已订阅群聊: ${groupId}, topic: ${topic}`)
      } catch (e: unknown) {
        logger.error(`订阅群聊 ${groupId} 失败:`, e)
      }
    },

    _unsubscribeGroup(groupId: number) {
      if (!groupId) return

      const subscription = this.groupSubscriptions.get(groupId)
      if (subscription) {
        try {
          subscription.unsubscribe()
          this.groupSubscriptions.delete(groupId)
          logger.debug(`已取消订阅群聊: ${groupId}`)
        } catch (e: unknown) {
          logger.error(`取消订阅群聊 ${groupId} 失败:`, e)
        }
      }
    },

    _sendConnectionTest() {
      const userStore = useUserStore()
      const currentUser = userStore.userInfo

      if (currentUser && currentUser.id) {
        logger.debug('连接测试: 当前用户 ID', currentUser.id)
      }
    },

    async _refreshFriendStatus() {
      try {
        const friendStore = useFriendStore()
        if (friendStore.friends && friendStore.friends.length > 0) {
          const ids = friendStore.friends.map((f) => f.id)
          const { batchCheckUserOnlineStatus } = await import('../../api/userStatus')
          const statusRes: Record<number, boolean> = await batchCheckUserOnlineStatus(ids)
          if (statusRes) {
            friendStore.friends.forEach((friend) => {
              const isOnline = !!statusRes[friend.id]
              if (friend.online !== isOnline) {
                friendStore.updateFriendStatus(friend.id, isOnline)
              }
            })
            logger.debug('好友在线状态已刷新')
          }
        }
      } catch (e: unknown) {
        logger.error('刷新好友在线状态失败:', e)
      }
    }
  }
})
