<template>
  <el-footer class="chat-input">
    <div class="input-wrap">
      <div class="inner-tools">
        <span class="tool-icon emoji-icon" @click="toggleEmojiPicker">😊</span>
        <el-icon size="20" class="tool-icon" @click="$emit('trigger-image')"><Picture /></el-icon>
        <el-icon size="20" class="tool-icon" @click="$emit('trigger-file')"><Paperclip /></el-icon>
        <el-icon size="20" class="tool-icon"><Microphone /></el-icon>
        <el-icon size="20" class="tool-icon"><VideoCamera /></el-icon>
      </div>
      <el-input
        ref="textInput"
        v-model="text"
        type="textarea"
        :rows="3"
        :disabled="disabled"
        :placeholder="placeholder"
        @keydown.enter.exact.prevent="handleSend"
      />
      <el-button class="send-btn" type="primary" @click="handleSend" :disabled="disabled || !text.trim()">发送</el-button>
      <div v-if="showEmojiPicker" class="emoji-picker" @click.stop>
        <div class="emoji-header">
          <span class="emoji-title">表情</span>
          <el-icon class="close-icon" @click="closeEmojiPicker"><Close /></el-icon>
        </div>
        <div class="emoji-grid">
          <span v-for="emoji in emojiList" :key="emoji" class="emoji-item" @click="insertEmoji(emoji)">{{ emoji }}</span>
        </div>
      </div>
    </div>
  </el-footer>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { Close, Picture, Paperclip, Microphone, VideoCamera } from '@element-plus/icons-vue'

defineProps<{
  disabled?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  send: [content: string]
  'trigger-image': []
  'trigger-file': []
}>()

const text = ref('')
const textInput = ref<{ textarea?: HTMLTextAreaElement; $el?: HTMLElement; input?: HTMLTextAreaElement } | null>(null)
const showEmojiPicker = ref(false)

function toggleEmojiPicker() { showEmojiPicker.value = !showEmojiPicker.value }
function closeEmojiPicker() { showEmojiPicker.value = false }

function handleSend() {
  const content = text.value.trim()
  if (!content) return
  emit('send', content)
  text.value = ''
}

function insertEmoji(emoji: string) {
  let textarea: HTMLTextAreaElement | null = null
  if (textInput.value) {
    textarea = textInput.value.textarea || textInput.value.$el?.querySelector('textarea') || textInput.value.input || null
  }
  if (textarea && typeof textarea.selectionStart !== 'undefined') {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const currentText = text.value
    text.value = currentText.substring(0, start) + emoji + currentText.substring(end)
    nextTick(() => {
      const newPos = start + emoji.length
      if (textarea && typeof textarea.setSelectionRange === 'function') {
        textarea.setSelectionRange(newPos, newPos)
        textarea.focus()
      }
    })
  } else {
    text.value += emoji
  }
  closeEmojiPicker()
}

const emojiList = [
  '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','🙃','😉','😊','😇','🥰','😍','🤩',
  '😘','😗','😚','😙','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐','🤨',
  '😐','😑','😶','😏','😒','🙄','😬','🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕',
  '🤢','🤮','🤧','🥵','🥶','😵','🤯','🤠','🥳','😎','🤓','🧐','😕','😟','🙁','☹️',
  '😮','😯','😲','😳','🥺','😦','😧','😨','😰','😥','😢','😭','😱','😖','😣','😞',
  '😓','😩','😫','🥱','😤','😡','😠','🤬','😈','👿','💀','☠️','💩','🤡','👹','👺',
  '👻','👽','👾','🤖','😺','😸','😹','😻','😼','😽','🙀','😿','😾','🙈','🙉','🙊',
  '💋','💌','💘','💝','💖','💗','💓','💞','💕','💟','❣️','💔','❤️','🧡','💛','💚',
  '💙','💜','🤎','🖤','🤍','💯','💢','💥','💫','💦','💨','💣','💬','🗨️','🗯️','💭',
  '💤','👋','🤚','🖐️','✋','🖖','👌','🤌','🤏','✌️','🤞','🤟','🤘','🤙','👈','👉',
  '👆','👇','☝️','👍','👎','✊','👊','🤛','🤜','👏','🙌','👐','🤲','🤝','🙏','✍️',
  '💪','🦵','🦶','👂','👃','🧠','🦷','🦴','👀','👁️','👅','👄'
]
</script>

<style scoped>
.chat-input {
  border-top: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 200px;
  padding: 12px;
}
.inner-tools {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 10px;
  color: var(--el-text-color-secondary);
  z-index: 2;
}
.input-wrap { position: relative; flex: 1; }
.input-wrap :deep(.el-textarea__inner) {
  height: 120px;
  padding-right: 92px;
  padding-bottom: 12px;
  padding-top: 36px;
  border: none;
  box-shadow: none;
  outline: none;
  background: var(--el-bg-color);
  resize: none;
}
.input-wrap :deep(.el-textarea__inner:focus) {
  border: none;
  box-shadow: none;
  outline: none;
}
.send-btn { position: absolute; right: 16px; bottom: 16px; }
.tool-icon { cursor: pointer; transition: color 0.3s; }
.tool-icon:hover { color: var(--el-color-primary); }
.emoji-icon { font-size: 20px; display: inline-flex; align-items: center; justify-content: center; width: 20px; height: 20px; }
.emoji-picker {
  position: absolute;
  bottom: 100%;
  left: 0;
  width: 320px;
  height: 280px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  z-index: 1000;
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
}
.emoji-header { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; border-bottom: 1px solid var(--el-border-color-light); }
.emoji-title { font-size: 14px; font-weight: 500; color: var(--el-text-color-primary); }
.close-icon { cursor: pointer; color: var(--el-text-color-secondary); font-size: 16px; }
.close-icon:hover { color: var(--el-color-primary); }
.emoji-grid { flex: 1; overflow-y: auto; padding: 8px; display: grid; grid-template-columns: repeat(8, 1fr); gap: 4px; }
.emoji-item { font-size: 24px; cursor: pointer; display: flex; align-items: center; justify-content: center; padding: 4px; border-radius: 4px; transition: background-color 0.2s; user-select: none; }
.emoji-item:hover { background-color: var(--el-fill-color-light); }
</style>
