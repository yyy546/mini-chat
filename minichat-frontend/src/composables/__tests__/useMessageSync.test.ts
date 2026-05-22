import { describe, it, expect } from 'vitest'
import { findTempMessage, findExistingMessage, findExistingBySeq, toUIMessage, applyRecall } from '../useMessageSync'
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

describe('findTempMessage', () => {
  it('应该通过 tempId 找到临时消息', () => {
    const list = [makeMsg({ id: 'temp_abc', tempId: 'temp_abc' }), makeMsg({ id: 2 })]
    expect(findTempMessage(list, 'temp_abc')).toBe(0)
  })

  it('当 tempId 为 null/空时应该返回 -1', () => {
    const list = [makeMsg()]
    expect(findTempMessage(list, '')).toBe(-1)
    expect(findTempMessage(list, null as unknown as string)).toBe(-1)
  })

  it('应该通过 id 匹配 tempId（临时消息场景）', () => {
    const list = [makeMsg({ id: 'temp_abc' })]
    expect(findTempMessage(list, 'temp_abc')).toBe(0)
  })

  it('未匹配时应该返回 -1', () => {
    const list = [makeMsg({ id: 1, tempId: 'xyz' })]
    expect(findTempMessage(list, 'abc')).toBe(-1)
  })
})

describe('findExistingMessage', () => {
  it('应该通过 messageId 找到已有消息', () => {
    const list = [makeMsg({ id: 1 }), makeMsg({ id: 2 })]
    expect(findExistingMessage(list, 2)).toBe(1)
  })

  it('当 messageId 为空时应该返回 -1', () => {
    const list = [makeMsg()]
    expect(findExistingMessage(list, '')).toBe(-1)
    expect(findExistingMessage(list, null as unknown as string)).toBe(-1)
  })
})

describe('findExistingBySeq', () => {
  it('应该通过 messageSeq 找到群聊消息', () => {
    const list = [makeMsg({ messageSeq: 100 }), makeMsg({ messageSeq: 200 })]
    expect(findExistingBySeq(list, 200)).toBe(1)
  })

  it('当 messageSeq 为空时应该返回 -1', () => {
    const list = [makeMsg()]
    expect(findExistingBySeq(list, 0)).toBe(-1)
  })
})

describe('toUIMessage', () => {
  it('应该将私聊 payload 转换为 UI 消息格式', () => {
    const payload = {
      messageId: 1,
      senderId: 10,
      receiverId: 20,
      content: 'hello',
      messageType: 1,
      sendTime: '2025-01-01T00:00:00.000Z'
    }
    const result = toUIMessage(payload, 99, false)
    expect(result.id).toBe(1)
    expect(result.fromId).toBe(10)
    expect(result.toId).toBe(20)
    expect(result.content).toBe('hello')
    expect(result.type).toBe(1)
    expect(result.isSending).toBe(false)
    expect(result.isReceived).toBe(true)
  })

  it('fromId 应该为 self_ 当 senderId 等于 selfId', () => {
    const payload = { messageId: 1, senderId: 99, receiverId: 20, content: 'hi' }
    const result = toUIMessage(payload, 99, false)
    expect(result.fromId).toBe('self')
  })

  it('应该将群聊 payload 转换为 UI 消息格式', () => {
    const payload = {
      messageId: 2,
      senderId: 10,
      groupId: 100,
      content: 'group msg',
      messageType: 1,
      senderNickname: 'Alice',
      messageSeq: 5
    }
    const result = toUIMessage(payload, 99, true)
    expect(result.toId).toBe(100)
    expect(result.groupId).toBe(100)
    expect(result.senderNickname).toBe('Alice')
    expect(result.messageSeq).toBe(5)
  })

  it('无效时间戳应该回退为当前时间', () => {
    const before = Date.now()
    const payload = { messageId: 1, senderId: 10, receiverId: 20, content: 'x', sendTime: 'invalid' }
    const result = toUIMessage(payload, 99, false)
    expect(result.timestamp).toBeGreaterThanOrEqual(before)
  })

  it('缺失 sendTime 应该使用当前时间', () => {
    const payload = { messageId: 1, senderId: 10, receiverId: 20, content: 'x' }
    const result = toUIMessage(payload, 99, false)
    expect(typeof result.timestamp).toBe('number')
    expect(result.timestamp).toBeGreaterThan(0)
  })

  it('默认 messageType 应为 1（文本）', () => {
    const payload = { messageId: 3, senderId: 10, receiverId: 20, content: 'test' }
    const result = toUIMessage(payload, 99, false)
    expect(result.type).toBe(1)
  })
})

describe('applyRecall', () => {
  it('自己撤回的消息内容为"你撤回了一条消息"', () => {
    const msg = makeMsg({ content: 'secret', type: 1 })
    applyRecall(msg, 99, 99, 'Alice')
    expect(msg.content).toBe('你撤回了一条消息')
    expect(msg.type).toBe(5)
    expect(msg.recall).toBe(true)
  })

  it('对方撤回的消息显示对方昵称', () => {
    const msg = makeMsg({ content: 'secret', type: 1 })
    applyRecall(msg, 10, 99, 'Bob')
    expect(msg.content).toBe('"Bob" 撤回了一条消息')
  })

  it('对方撤回且无昵称时显示默认文案', () => {
    const msg = makeMsg({ content: 'secret', type: 1 })
    applyRecall(msg, 10, 99, '')
    expect(msg.content).toBe('"对方" 撤回了一条消息')
  })
})
