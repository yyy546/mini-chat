import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import { useChatStore } from '../chat'
import { useWebSocketStore } from './useWebSocketStore'
import { useUserStore } from '../user'
import { recallPrivateMessage, recallGroupMessage } from '../../api/chat'
import { findTempMessage, findExistingMessage, findExistingBySeq, applyRecall } from '../../composables/useMessageSync'
import logger from '../../utils/logger'
import type { UIMessage } from '../../types/message'

interface MessagePayload {
  error?: boolean
  errorMessage?: string
  receiverId?: number
  tempId?: string
  senderId?: number
  content?: string
  messageId?: number
  id?: number
  sendTime?: string
  messageType?: number
  fileName?: string
  fileSize?: number
  fileUrl?: string
  groupId?: number
  senderNickname?: string
  senderAvatar?: string
  messageSeq?: number
  isGroup?: boolean
  recallUserId?: number
  chatId?: number
  [key: string]: unknown
}

export const useMessageActions = defineStore('msgActions', {
  state: () => ({}),

  actions: {
    sendMessage(
      toUserId: number,
      content: string,
      type: number = 1,
      fileName: string | null = null,
      fileSize: number | null = null,
      fileUrl: string | null = null
    ) {
      const chatStore = useChatStore()
      const wsStore = useWebSocketStore()
      const userStore = useUserStore()
      const senderId = userStore.userInfo?.id

      if (!senderId) {
        ElMessage.error('未获取到当前用户信息')
        return Promise.resolve(false)
      }

      if (!wsStore.isConnected) {
        ElMessage.error('连接未建立，请先连接WebSocket')
        return Promise.resolve(false)
      }

      if (!content || content.trim() === '') {
        ElMessage.error('消息内容不能为空')
        return Promise.resolve(false)
      }

      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

      const dto = {
        senderId,
        receiverId: toUserId,
        content: content.trim(),
        messageType: type,
        fileName: fileName || undefined,
        fileSize: fileSize || undefined,
        fileUrl: fileUrl || undefined,
        tempId
      }

      try {
        logger.debug('发送消息:', dto)

        wsStore.stomp!.send('/app/chat.private', {}, JSON.stringify(dto))

        const selfMsg: UIMessage = {
          id: tempId,
          fromId: 'self',
          toId: toUserId,
          content: content.trim(),
          timestamp: Date.now(),
          type: type as 1 | 2 | 3 | 5,
          fileName: fileName || undefined,
          fileSize: fileSize || undefined,
          fileUrl: fileUrl || undefined,
          isSending: true,
          isReceived: false,
          sendError: false,
          tempId
        }

        const key = `private_${toUserId}`
        const list = chatStore.messagesByUser[key] || []
        chatStore.messagesByUser[key] = [...list, selfMsg]

        chatStore._updateSession(toUserId, Date.now(), false, false)

        logger.debug('临时消息已添加，等待服务器确认，临时ID:', tempId)
        return Promise.resolve(true)
      } catch (e: unknown) {
        const err = e as Error
        logger.error('发送消息失败:', e)
        ElMessage.error('发送失败: ' + (err.message || '未知错误'))

        const key = `private_${toUserId}`
        const list = chatStore.messagesByUser[key] || []
        const failMsg: UIMessage = {
          id: tempId,
          fromId: 'self',
          toId: toUserId,
          content: content.trim(),
          timestamp: Date.now(),
          type: type as 1 | 2 | 3 | 5,
          fileName: fileName || undefined,
          fileSize: fileSize || undefined,
          fileUrl: fileUrl || undefined,
          isSending: false,
          isReceived: false,
          sendError: true,
          tempId
        }
        chatStore.messagesByUser[key] = [...list, failMsg]

        return Promise.resolve(false)
      }
    },

    sendGroupMessage(
      groupId: number,
      content: string,
      type: number = 1,
      fileName: string | null = null,
      fileSize: number | null = null,
      fileUrl: string | null = null
    ) {
      const chatStore = useChatStore()
      const wsStore = useWebSocketStore()
      const userStore = useUserStore()
      const senderId = userStore.userInfo?.id

      if (!senderId) {
        ElMessage.error('未获取到当前用户信息')
        return Promise.resolve(false)
      }

      if (!wsStore.isConnected) {
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

      const tempId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`

      const dto = {
        senderId,
        groupId,
        content: content.trim(),
        messageType: type,
        fileName: fileName || undefined,
        fileSize: fileSize || undefined,
        fileUrl: fileUrl || undefined,
        tempId
      }

      try {
        logger.debug('发送群聊消息:', dto)

        wsStore.stomp!.send('/app/chat.group', {}, JSON.stringify(dto))

        const selfMsg: UIMessage = {
          id: tempId,
          fromId: 'self',
          toId: groupId,
          groupId,
          content: content.trim(),
          timestamp: Date.now(),
          type: type as 1 | 2 | 3 | 5,
          fileName: fileName || undefined,
          fileSize: fileSize || undefined,
          fileUrl: fileUrl || undefined,
          isSending: true,
          isReceived: false,
          sendError: false,
          tempId
        }

        const key = `group_${groupId}`
        const list = chatStore.messagesByUser[key] || []
        chatStore.messagesByUser[key] = [...list, selfMsg]

        chatStore._updateSession(groupId, Date.now(), false, true)

        logger.debug('群聊临时消息已添加，等待服务器确认，临时ID:', tempId)
        return Promise.resolve(true)
      } catch (e: unknown) {
        const err = e as Error
        logger.error('发送群聊消息失败:', e)
        ElMessage.error('发送失败: ' + (err.message || '未知错误'))

        const key = `group_${groupId}`
        const list = chatStore.messagesByUser[key] || []
        const failMsg: UIMessage = {
          id: tempId,
          fromId: 'self',
          toId: groupId,
          groupId,
          content: content.trim(),
          timestamp: Date.now(),
          type: type as 1 | 2 | 3 | 5,
          fileName: fileName || undefined,
          fileSize: fileSize || undefined,
          fileUrl: fileUrl || undefined,
          isSending: false,
          isReceived: false,
          sendError: true,
          tempId
        }
        chatStore.messagesByUser[key] = [...list, failMsg]

        return Promise.resolve(false)
      }
    },

    _handleIncoming(payload: MessagePayload) {
      const chatStore = useChatStore()
      try {
        logger.debug('收到WebSocket消息:', payload)

        if (payload.error) {
          logger.warn('处理发送失败消息:', payload)
          const otherId = payload.receiverId
          const tempId = payload.tempId

          if (otherId && tempId) {
            const key = `private_${otherId}`
            const list = chatStore.messagesByUser[key]

            if (list) {
              const idx = list.findIndex((m) => m.id == tempId || m.tempId == tempId)
              if (idx !== -1) {
                const newList = [...list]
                newList[idx] = {
                  ...newList[idx],
                  isSending: false,
                  sendError: true
                }
                chatStore.messagesByUser[key] = newList
                logger.debug('已标记消息为发送失败, tempId:', tempId)
              } else {
                logger.warn('未找到对应的临时消息:', tempId)
              }
            } else {
              logger.warn('未找到对应的会话列表:', otherId)
            }
          }
          return
        }

        const userStore = useUserStore()
        const selfId = userStore.userInfo?.id

        if (!selfId) {
          logger.error('未获取到当前用户信息')
          ElMessage.error('未获取到当前用户信息，无法接收消息')
          return
        }

        let otherId: number | undefined
        let isIncoming = false
        if (payload.senderId === selfId) {
          otherId = payload.receiverId
          logger.debug('收到自己的消息回执，对方 ID:', otherId)
        } else {
          otherId = payload.senderId
          isIncoming = true
          logger.debug('收到对方消息，对方 ID:', otherId)
        }

        if (!otherId) {
          logger.error('无法确定消息对方ID:', payload)
          return
        }

        let ts: number
        if (payload.sendTime) {
          ts = new Date(payload.sendTime).getTime()
          if (isNaN(ts)) {
            ts = Date.now()
            logger.warn('消息时间格式不正确，使用当前时间:', payload.sendTime)
          }
        } else {
          ts = Date.now()
          logger.warn('消息缺少 sendTime，使用当前时间')
        }

        const uiMsg: UIMessage = {
          id: payload.messageId || payload.id || `temp_${Date.now()}`,
          fromId: payload.senderId === selfId ? 'self' : (payload.senderId as number),
          toId: payload.receiverId as number,
          content: payload.content as string,
          timestamp: ts,
          type: (payload.messageType as 1 | 2 | 3 | 5) || 1,
          fileName: payload.fileName,
          fileSize: payload.fileSize,
          fileUrl: payload.fileUrl,
          isSending: false,
          isReceived: true,
          sendError: false
        }

        if (uiMsg.type === 5) {
          if (uiMsg.fromId === 'self') {
            uiMsg.content = '你撤回了一条消息'
          } else {
            uiMsg.content = '对方撤回了一条消息'
          }
        }

        logger.debug('构建的UI消息:', uiMsg)

        const key = `private_${otherId}`
        const list = chatStore.messagesByUser[key] || []

        if (payload.tempId) {
          const tempIdx = findTempMessage(list, payload.tempId)
          if (tempIdx !== -1) {
            const newList = [...list]
            newList[tempIdx] = { ...newList[tempIdx], ...uiMsg, id: uiMsg.id, isSending: false, sendError: false }
            chatStore.messagesByUser[key] = newList
            logger.debug('临时消息已确认为真实消息:', uiMsg.id)
            return
          }
        }

        const existIdx = findExistingMessage(list, uiMsg.id)
        if (existIdx !== -1) {
          const newList = [...list]
          newList[existIdx] = { ...newList[existIdx], ...uiMsg }
          chatStore.messagesByUser[key] = newList
          logger.debug('更新已有消息:', uiMsg.id)
          return
        }

        chatStore.messagesByUser[key] = [...list, uiMsg]
        logger.debug('消息已添加到列表，当前消息数:', chatStore.messagesByUser[key].length)
        chatStore._updateSession(otherId, ts, isIncoming, false)
      } catch (e: unknown) {
        logger.error('处理收到的消息失败:', e, '原始payload:', payload)
      }
    },

    _handleGroupMessage(payload: MessagePayload) {
      const chatStore = useChatStore()
      try {
        logger.debug('收到群聊消息:', payload)

        if (payload.isGroup && payload.recallUserId) {
          const groupId = payload.chatId
          const messageId = payload.messageId
          if (groupId && messageId) {
            const key = `group_${groupId}`
            const list = chatStore.messagesByUser[key] || []

            const idx = list.findIndex((m) => m.id === messageId)
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
              chatStore.messagesByUser[key] = newList
              logger.debug('群聊消息已更新为撤回状态:', messageId)
            } else {
              logger.warn('收到群聊撤回通知，但本地未找到对应消息:', messageId)
            }
          }
          return
        }

        const userStore = useUserStore()
        const selfId = userStore.userInfo?.id

        if (!selfId) {
          logger.error('未获取到当前用户信息')
          return
        }

        const groupId = payload.groupId
        if (!groupId) {
          logger.error('群聊消息缺少 groupId:', payload)
          return
        }

        let ts: number
        if (payload.sendTime) {
          ts = new Date(payload.sendTime).getTime()
          if (isNaN(ts)) {
            ts = Date.now()
            logger.warn('群聊消息时间格式不正确，使用当前时间:', payload.sendTime)
          }
        } else {
          ts = Date.now()
          logger.warn('群聊消息缺少 sendTime，使用当前时间')
        }

        const uiMsg: UIMessage = {
          id: payload.messageId || payload.id || `group_${Date.now()}`,
          fromId: payload.senderId === selfId ? 'self' : (payload.senderId as number),
          toId: groupId,
          groupId,
          content: payload.content as string,
          timestamp: ts,
          type: (payload.messageType as 1 | 2 | 3 | 5) || 1,
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

        if (uiMsg.type === 5) {
          uiMsg.content =
            uiMsg.fromId === 'self'
              ? '你撤回了一条消息'
              : `"${uiMsg.senderNickname || '成员'}" 撤回了一条消息`
        }

        logger.debug('构建的群聊UI消息:', uiMsg)

        const key = `group_${groupId}`
        const list = chatStore.messagesByUser[key] || []

        if (payload.tempId) {
          const tempIdx = findTempMessage(list, payload.tempId)
          if (tempIdx !== -1) {
            const newList = [...list]
            newList[tempIdx] = {
              ...newList[tempIdx],
              ...uiMsg,
              id: uiMsg.id,
              isSending: false,
              sendError: false
            }
            chatStore.messagesByUser[key] = newList
            logger.debug('群聊临时消息已确认为真实消息:', uiMsg.id)
            return
          }
        }

        const existIdx = findExistingMessage(list, uiMsg.id)
        if (existIdx !== -1) {
          const newList = [...list]
          newList[existIdx] = {
            ...newList[existIdx],
            ...uiMsg,
            type: uiMsg.type,
            content:
              uiMsg.type === 5
                ? uiMsg.fromId === 'self'
                  ? '你撤回了一条消息'
                  : `"${uiMsg.senderNickname || '成员'}" 撤回了一条消息`
                : uiMsg.content,
            recall: uiMsg.type === 5
          }
          chatStore.messagesByUser[key] = newList
          logger.debug('更新群聊已有消息:', uiMsg.id)
          return
        }

        if (uiMsg.messageSeq) {
          if (findExistingBySeq(list, uiMsg.messageSeq) !== -1) {
            logger.debug('跳过重复的群聊消息 (messageSeq):', uiMsg.messageSeq)
            return
          }
        }

        chatStore.messagesByUser[key] = [...list, uiMsg]
        logger.debug('群聊消息已添加到列表，当前消息数:', chatStore.messagesByUser[key].length)
        const isIncoming = payload.senderId !== selfId
        chatStore._updateSession(groupId, ts, isIncoming, true, uiMsg.messageSeq || null)
      } catch (e: unknown) {
        logger.error('处理群聊消息失败:', e, '原始payload:', payload)
      }
    },

    _handleRecallNotification(payload: MessagePayload) {
      const chatStore = useChatStore()
      logger.debug('收到撤回通知:', payload)
      const messageId = payload.messageId
      if (!messageId) return

      const userStore = useUserStore()
      const selfId = userStore.userInfo?.id

      let targetUserId: number | undefined
      if (payload.recallUserId === selfId) {
        targetUserId = payload.chatId
      } else {
        targetUserId = payload.recallUserId
      }

      if (!targetUserId) return

      const key = `private_${targetUserId}`
      const list = chatStore.messagesByUser[key] || []

      const idx = list.findIndex((m) => m.id === messageId)
      if (idx !== -1) {
        const newList = [...list]
        if (selfId) {
          newList[idx] = applyRecall(
            newList[idx],
            payload.recallUserId as number,
            selfId,
            newList[idx].senderNickname || '对方'
          )
        }
        chatStore.messagesByUser[key] = newList
        logger.debug('已更新撤回消息状态:', messageId)
      } else {
        logger.warn('未找到要撤回的消息:', messageId)
      }
    },

    async recallMessage(messageId: number) {
      const chatStore = useChatStore()
      if (!messageId || !chatStore.activeUser) return false

      const isGroup = chatStore.activeUser.type === 1

      try {
        if (isGroup) {
          await recallGroupMessage(chatStore.activeUser.id, messageId)
        } else {
          await recallPrivateMessage(messageId)
        }

        const key = `${isGroup ? 'group' : 'private'}_${chatStore.activeUser.id}`
        const list = chatStore.messagesByUser[key] || []
        const idx = list.findIndex((m) => m.id === messageId)
        if (idx !== -1) {
          const newList = [...list]
          newList[idx] = {
            ...newList[idx],
            content: '你撤回了一条消息',
            type: 5,
            recall: true
          }
          chatStore.messagesByUser[key] = newList
        }
        return true
      } catch (e: unknown) {
        logger.error('撤回消息失败:', e)
        ElMessage.error('撤回失败')
        return false
      }
    }
  }
})
