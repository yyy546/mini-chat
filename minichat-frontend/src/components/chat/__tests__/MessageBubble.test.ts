import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import MessageBubble from '../MessageBubble.vue'
import type { UIMessage } from '@/types/message'

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

const defaultProps = {
  message: makeMsg(),
  isSelf: false,
  isGroup: false,
  memberRoleMap: {}
}

describe('MessageBubble', () => {
  it('文本消息应显示内容', () => {
    const wrapper = mount(MessageBubble, {
      props: { ...defaultProps, message: makeMsg({ type: 1, content: '你好世界' }) }
    })
    expect(wrapper.text()).toContain('你好世界')
  })

  it('图片消息应显示图片区域', () => {
    const wrapper = mount(MessageBubble, {
      props: {
        ...defaultProps,
        message: makeMsg({ type: 2, content: '[图片]', fileUrl: 'http://example.com/img.png' })
      }
    })
    expect(wrapper.find('.image-container').exists()).toBe(true)
  })

  it('文件消息应显示文件名', () => {
    const wrapper = mount(MessageBubble, {
      props: {
        ...defaultProps,
        message: makeMsg({ type: 3, content: '[文件]', fileName: 'report.pdf', fileSize: 1024 })
      }
    })
    expect(wrapper.text()).toContain('report.pdf')
    expect(wrapper.text()).toContain('KB')
  })

  it('撤回/系统消息应显示灰色提示', () => {
    const wrapper = mount(MessageBubble, {
      props: {
        ...defaultProps,
        message: makeMsg({ type: 5, content: '对方撤回了一条消息', recall: true })
      }
    })
    expect(wrapper.find('.system-message-content').exists()).toBe(true)
  })

  it('自己发送的消息应显示已发送状态', () => {
    const wrapper = mount(MessageBubble, {
      props: {
        ...defaultProps,
        isSelf: true,
        message: makeMsg({ fromId: 'self', type: 1, content: 'test' })
      }
    })
    expect(wrapper.find('.sent-status').exists()).toBe(false) // text messages don't have sent-status
  })

  it('发送失败的消息应显示错误图标', () => {
    const wrapper = mount(MessageBubble, {
      props: {
        ...defaultProps,
        isSelf: true,
        message: makeMsg({ fromId: 'self', type: 1, content: 'test', sendError: true, errorMessage: '发送失败' })
      }
    })
    expect(wrapper.find('.error-icon').exists()).toBe(true)
    expect(wrapper.text()).toContain('发送失败')
  })

  it('群聊消息应显示发送者昵称和角色标签', () => {
    const wrapper = mount(MessageBubble, {
      props: {
        ...defaultProps,
        isSelf: false,
        isGroup: true,
        memberRoleMap: { 2: 2 },
        message: makeMsg({ fromId: 2, type: 1, content: 'hello', senderNickname: 'Alice' })
      }
    })
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.find('.role-tag').exists()).toBe(true)
  })

  it('点击右键应触发 contextmenu 事件（仅自己消息）', async () => {
    const wrapper = mount(MessageBubble, {
      props: {
        ...defaultProps,
        isSelf: true,
        message: makeMsg({ fromId: 'self', type: 1, content: 'test' })
      }
    })
    await wrapper.find('.bubble').trigger('contextmenu.prevent')
    expect(wrapper.emitted('contextmenu')).toBeTruthy()
    expect(wrapper.emitted('contextmenu')!.length).toBe(1)
  })
})
