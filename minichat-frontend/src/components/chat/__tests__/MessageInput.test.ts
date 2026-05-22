import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'
import MessageInput from '../MessageInput.vue'

const mountOptions = {
  global: { plugins: [ElementPlus] }
}

describe('MessageInput', () => {
  it('Enter 键应触发 send 事件', async () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: false } })
    const textarea = wrapper.find('textarea')
    await textarea.setValue('hello')
    await textarea.trigger('keydown.enter.exact.prevent')
    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')![0]).toEqual(['hello'])
  })

  it('Shift+Enter 应换行不发送', async () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: false } })
    const textarea = wrapper.find('textarea')
    await textarea.setValue('hello')
    await textarea.trigger('keydown', { key: 'Enter', shiftKey: true })
    expect(wrapper.emitted('send')).toBeFalsy()
  })

  it('空内容不应发送', async () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: false } })
    const textarea = wrapper.find('textarea')
    await textarea.setValue('   ')
    await textarea.trigger('keydown.enter.exact.prevent')
    expect(wrapper.emitted('send')).toBeFalsy()
  })

  it('发送后输入框应清空', async () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: false } })
    const textarea = wrapper.find('textarea')
    await textarea.setValue('hello world')
    await textarea.trigger('keydown.enter.exact.prevent')
    await nextTick()
    expect((textarea.element as HTMLTextAreaElement).value).toBe('')
  })

  it('点击发送按钮应触发 send 事件', async () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: false } })
    const textarea = wrapper.find('textarea')
    await textarea.setValue('test message')
    await wrapper.find('.send-btn').trigger('click')
    expect(wrapper.emitted('send')).toBeTruthy()
    expect(wrapper.emitted('send')![0]).toEqual(['test message'])
  })

  it('disabled 状态下按钮不可点击', () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: true } })
    const btn = wrapper.find('.send-btn')
    expect(btn.attributes('disabled')).toBeDefined()
  })

  it('点击工具图标应触发对应事件', async () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: false } })
    const icons = wrapper.findAll('.tool-icon')
    // The second icon (index 1) is the image trigger
    await icons[1].trigger('click')
    expect(wrapper.emitted('trigger-image')).toBeTruthy()
    // The third icon (index 2) is the file trigger
    await icons[2].trigger('click')
    expect(wrapper.emitted('trigger-file')).toBeTruthy()
  })

  it('点击 emoji 图标应显示 emoji 选择器', async () => {
    const wrapper = mount(MessageInput, { ...mountOptions, props: { disabled: false } })
    expect(wrapper.find('.emoji-picker').exists()).toBe(false)
    await wrapper.find('.emoji-icon').trigger('click')
    expect(wrapper.find('.emoji-picker').exists()).toBe(true)
  })
})
