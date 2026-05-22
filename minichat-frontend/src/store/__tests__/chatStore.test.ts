import { describe, it, expect, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

// Mock API modules before importing the store
vi.mock('../../api/chat', () => ({
  markMessagesAsRead: vi.fn(() => Promise.resolve()),
  markGroupMessageRead: vi.fn(() => Promise.resolve()),
  getGroupMessageHistory: vi.fn(() => Promise.resolve({ records: [], total: 0, current: 1, size: 50 })),
  getPrivateMessageHistory: vi.fn(() => Promise.resolve({ records: [], total: 0, current: 1, size: 50 })),
  recallPrivateMessage: vi.fn(() => Promise.resolve()),
  recallGroupMessage: vi.fn(() => Promise.resolve())
}))

vi.mock('../../api/session', () => ({
  getSessionList: vi.fn(() => Promise.resolve([]))
}))

vi.mock('../../api/friend', () => ({
  getFriendList: vi.fn(() => Promise.resolve([]))
}))

vi.mock('../../utils/logger', () => ({
  default: { debug: vi.fn(), info: vi.fn(), warn: vi.fn(), error: vi.fn() }
}))

import { useChatStore } from '../chat'
import { useUserStore } from '../user'
import { useMessageActions } from '../chat/useMessageActions'
import type { UIMessage } from '../../types/message'

function makeMsg(overrides: Partial<UIMessage> = {}): UIMessage {
  return {
    id: 1,
    fromId: 2,
    toId: 3,
    content: 'hello',
    timestamp: Date.now(),
    type: 1,
    isSending: false,
    isReceived: true,
    sendError: false,
    ...overrides
  }
}

describe('useChatStore', () => {
  beforeEach(() => {
    const pinia = createPinia()
    setActivePinia(pinia)
  })

  describe('state 初始化', () => {
    it('初始状态应为空', () => {
      const store = useChatStore()
      expect(store.activeUser).toBeNull()
      expect(store.sessions).toEqual([])
      expect(store.messagesByUser).toEqual({})
      expect(store.sessionLoading).toBe(false)
    })

    it('activeMessages 在无 activeUser 时应返回空数组', () => {
      const store = useChatStore()
      expect(store.activeMessages).toEqual([])
    })
  })

  describe('会话管理', () => {
    it('setActiveUser 应该设置 activeUser', () => {
      const store = useChatStore()
      store.setActiveUser({ id: 1, name: 'Test', type: 0 })
      expect(store.activeUser).toBeTruthy()
      expect(store.activeUser!.id).toBe(1)
    })

    it('_updateSession 应该添加未读数（非活跃会话）', () => {
      const store = useChatStore()
      // 先手动添加一个 session
      store.sessions = [{ id: 1, type: 0, name: 'Test', avatar: '', lastMessageTime: 0, unreadCount: 0 }]
      store._updateSession(1, Date.now(), true, false)
      expect(store.sessions[0].unreadCount).toBe(1)
    })

    it('_updateSession 在当前活跃会话不应增加未读数', () => {
      const store = useChatStore()
      store.sessions = [{ id: 1, type: 0, name: 'Test', avatar: '', lastMessageTime: 0, unreadCount: 0 }]
      store.activeUser = { id: 1, type: 0 }
      store._updateSession(1, Date.now(), true, false)
      expect(store.sessions[0].unreadCount).toBe(0)
    })

    it('会话列表应该按最后消息时间倒序排列', () => {
      const store = useChatStore()
      const now = Date.now()
      store.sessions = [
        { id: 1, type: 0, name: 'A', avatar: '', lastMessageTime: now - 2000, unreadCount: 0 },
        { id: 2, type: 0, name: 'B', avatar: '', lastMessageTime: now - 1000, unreadCount: 0 },
        { id: 3, type: 0, name: 'C', avatar: '', lastMessageTime: now, unreadCount: 0 }
      ]
      // Trigger sort via _updateSession
      store._updateSession(1, now + 1000, false, false)
      expect(store.sessions[0].id).toBe(1)
    })

    it('markAsRead 应该清零未读数', async () => {
      const store = useChatStore()
      store.sessions = [{ id: 1, type: 0, name: 'Test', avatar: '', lastMessageTime: 0, unreadCount: 5 }]
      await store.markAsRead(1, 0)
      expect(store.sessions[0].unreadCount).toBe(0)
    })
  })

  describe('消息列表管理', () => {
    it('setActiveUser 应该初始化分页状态', () => {
      const store = useChatStore()
      store.setActiveUser({ id: 1, name: 'Test', type: 0 })
      const key = 'private_1'
      expect(store.chatPagination[key]).toBeTruthy()
      expect(store.chatPagination[key].page).toBe(1)
      expect(store.chatPagination[key].hasMore).toBe(true)
    })

    it('activeMessages getter 私聊应该返回正确的 key', () => {
      const store = useChatStore()
      store.messagesByUser['private_1'] = [makeMsg({ id: 1 }), makeMsg({ id: 2 })]
      store.activeUser = { id: 1, type: 0 }
      expect(store.activeMessages).toHaveLength(2)
    })

    it('activeMessages getter 群聊应该返回正确的 key', () => {
      const store = useChatStore()
      store.messagesByUser['group_100'] = [makeMsg({ id: 1, groupId: 100 })]
      store.activeUser = { id: 100, type: 1 }
      expect(store.activeMessages).toHaveLength(1)
    })
  })

  describe('消息去重 (通过 useMessageActions)', () => {
    it('通过 tempId 精确匹配应该替换临时消息', () => {
      const store = useChatStore()
      const userStore = useUserStore()
      userStore.userInfo = { id: 99, username: 'me', nickname: 'Me', avatar: '', gender: '', signature: '' }

      const tempMsg = makeMsg({ id: 'temp_abc', fromId: 'self', toId: 10, content: 'test', isSending: true, tempId: 'temp_abc' })
      store.messagesByUser['private_10'] = [tempMsg]
      store.activeUser = { id: 10, type: 0 }

      const msgActions = useMessageActions()
      msgActions._handleIncoming({
        messageId: 500,
        senderId: 99,
        receiverId: 10,
        content: 'test',
        messageType: 1,
        sendTime: new Date().toISOString(),
        tempId: 'temp_abc'
      })

      const list = store.messagesByUser['private_10']
      expect(list).toHaveLength(1)
      expect(list[0].id).toBe(500)
      expect(list[0].isSending).toBe(false)
      expect(list[0].sendError).toBe(false)
    })

    it('相同 messageId 的消息不应重复添加', () => {
      const store = useChatStore()
      const userStore = useUserStore()
      userStore.userInfo = { id: 99, username: 'me', nickname: 'Me', avatar: '', gender: '', signature: '' }

      const existingMsg = makeMsg({ id: 500, fromId: 10, toId: 99, content: 'original', type: 1 })
      store.messagesByUser['private_10'] = [existingMsg]

      const msgActions = useMessageActions()
      msgActions._handleIncoming({
        messageId: 500,
        senderId: 10,
        receiverId: 99,
        content: 'updated',
        messageType: 1,
        sendTime: new Date().toISOString()
      })

      const list = store.messagesByUser['private_10']
      expect(list).toHaveLength(1)
      expect(list[0].content).toBe('updated')
    })

    it('相同 content 不同 tempId 的消息应该都保留', () => {
      const store = useChatStore()
      const userStore = useUserStore()
      userStore.userInfo = { id: 99, username: 'me', nickname: 'Me', avatar: '', gender: '', signature: '' }

      store.messagesByUser['private_10'] = [
        makeMsg({ id: 1, fromId: 10, toId: 99, content: '好的' })
      ]

      const msgActions = useMessageActions()
      msgActions._handleIncoming({
        messageId: 2,
        senderId: 10,
        receiverId: 99,
        content: '好的',
        messageType: 1,
        sendTime: new Date().toISOString(),
        tempId: 'temp_xyz'
      })

      const list = store.messagesByUser['private_10']
      expect(list).toHaveLength(2)
    })

    it('群聊消息通过 messageSeq 去重', () => {
      const store = useChatStore()
      const userStore = useUserStore()
      userStore.userInfo = { id: 99, username: 'me', nickname: 'Me', avatar: '', gender: '', signature: '' }

      store.messagesByUser['group_100'] = [
        makeMsg({ id: 1, groupId: 100, messageSeq: 50, content: 'first' })
      ]

      const msgActions = useMessageActions()
      msgActions._handleGroupMessage({
        messageId: 2,
        senderId: 10,
        groupId: 100,
        content: 'should be skipped',
        messageType: 1,
        messageSeq: 50,
        senderNickname: 'User',
        sendTime: new Date().toISOString()
      })

      const list = store.messagesByUser['group_100']
      expect(list).toHaveLength(1)
      expect(list[0].content).toBe('first')
    })

    it('撤回类型消息应更新已有消息状态', () => {
      const store = useChatStore()
      const userStore = useUserStore()
      userStore.userInfo = { id: 99, username: 'me', nickname: 'Me', avatar: '', gender: '', signature: '' }

      store.messagesByUser['private_10'] = [
        makeMsg({ id: 500, fromId: 10, toId: 99, content: 'secret', type: 1 })
      ]

      const msgActions = useMessageActions()
      msgActions._handleIncoming({
        messageId: 500,
        senderId: 10,
        receiverId: 99,
        content: '对方撤回了一条消息',
        messageType: 5,
        sendTime: new Date().toISOString()
      })

      const list = store.messagesByUser['private_10']
      expect(list[0].type).toBe(5)
    })
  })
})
