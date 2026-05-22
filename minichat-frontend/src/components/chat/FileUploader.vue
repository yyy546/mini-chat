<template>
  <div>
    <input type="file" ref="imageInput" style="display: none" accept="image/*" @change="handleImageUpload" />
    <input type="file" ref="fileInput" style="display: none" @change="handleFileUpload" />
    <div v-if="uploading" class="upload-progress">
      <el-progress :percentage="uploadProgress" :stroke-width="3" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFileUnified } from '@/api/upload'
import logger from '@/utils/logger'

const props = defineProps<{
  friendId: number
  scene: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'upload-success': [result: { fileUrl: string; fileName: string; fileSize: number; messageType?: number }]
}>()

const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)

function triggerImageSelect() { imageInput.value?.click() }
function triggerFileSelect() { fileInput.value?.click() }

async function handleImageUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  await uploadAndSend(file, 2)
  ;(e.target as HTMLInputElement).value = ''
}

async function handleFileUpload(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  await uploadAndSend(file, 3)
  ;(e.target as HTMLInputElement).value = ''
}

async function uploadAndSend(file: File, type: number) {
  try {
    uploading.value = true
    uploadProgress.value = 0
    const result = await uploadFileUnified({
      file,
      scene: props.scene,
      bizType: 'chat-file',
      bizId: props.friendId,
      chatFileType: type,
      onProgress: (p: number) => { uploadProgress.value = Math.round(p) }
    })
    emit('upload-success', result)
  } catch (e: unknown) {
    const err = e as Error
    logger.error('上传出错', e)
    ElMessage.error(err.message || '上传出错')
  } finally {
    uploading.value = false
  }
}

defineExpose({ triggerImageSelect, triggerFileSelect })
</script>

<style scoped>
.upload-progress {
  padding: 4px 16px 8px;
  background-color: var(--el-bg-color-page);
}
</style>
