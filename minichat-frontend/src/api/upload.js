import request from '../utils/request'

const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024

export async function uploadFileUnified(options) {
  const { file, scene, bizType, bizId, chatFileType = 3, groupId, onProgress } = options || {}
  if (!file) {
    throw new Error('文件不能为空')
  }

  if (scene === 'userAvatar') {
    return uploadUserAvatar(file, onProgress)
  }
  if (scene === 'groupAvatar') {
    if (!groupId) {
      throw new Error('缺少群组ID')
    }
    return uploadGroupAvatar(groupId, file, onProgress)
  }
  if (scene === 'spaceImage') {
    return uploadSpaceImage(file, onProgress)
  }

  if (scene !== 'privateChat' && scene !== 'groupChat') {
    throw new Error('不支持的上传场景')
  }

  if (file.size < LARGE_FILE_THRESHOLD) {
    return uploadChatSmallFile(scene, file, chatFileType, onProgress)
  }
  return uploadLargeFileWithResume({ file, scene, bizType, bizId, chatFileType, onProgress })
}

async function uploadUserAvatar(file, onProgress) {
  const formData = new FormData()
  formData.append('avatar', file)
  const res = await request.post('/users/avatar', formData, {
    onUploadProgress: (e) => {
      if (!onProgress || !e.total) return
      onProgress((e.loaded / e.total) * 100)
    }
  })
  if (res.code !== 1) {
    throw new Error(res.msg || '头像上传失败')
  }
  return {
    fileUrl: res.data,
    fileName: file.name,
    fileSize: file.size
  }
}

async function uploadGroupAvatar(groupId, file, onProgress) {
  const formData = new FormData()
  formData.append('avatar', file)
  const res = await request.post(`/group/avatar/${groupId}`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (e) => {
      if (!onProgress || !e.total) return
      onProgress((e.loaded / e.total) * 100)
    }
  })
  if (res.code !== 1) {
    throw new Error(res.msg || '群头像上传失败')
  }
  return {
    fileUrl: res.data,
    fileName: file.name,
    fileSize: file.size
  }
}

async function uploadSpaceImage(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await request.post('/space/post/upload/image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    onUploadProgress: (e) => {
      if (!onProgress || !e.total) return
      onProgress((e.loaded / e.total) * 100)
    }
  })
  if (res.code !== 1) {
    throw new Error(res.msg || '空间图片上传失败')
  }
  return {
    fileUrl: res.data,
    fileName: file.name,
    fileSize: file.size
  }
}

async function uploadChatSmallFile(scene, file, type, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  const url = scene === 'groupChat' ? '/chat/group/upload' : '/chat/private/upload'
  const res = await request.post(url, formData, {
    params: { type },
    headers: {
      Accept: 'application/json'
    },
    onUploadProgress: (e) => {
      if (!onProgress || !e.total) return
      onProgress((e.loaded / e.total) * 100)
    }
  })
  if (res.code !== 1) {
    throw new Error(res.msg || '文件上传失败')
  }
  const data = res.data || {}
  return {
    fileUrl: data.fileUrl,
    fileName: data.fileName || file.name,
    fileSize: data.fileSize || file.size,
    messageType: data.messageType
  }
}

async function uploadLargeFileWithResume(options) {
  const { file, scene, bizType = 'chat-file', bizId, chatFileType = 3, onProgress } = options

  const fileHash = await calcSimpleHash(file)

  const initRes = await request.post('/upload/init', {
    fileName: file.name,
    fileSize: file.size,
    fileHash,
    bizType,
    bizId
  })
  if (initRes.code !== 1) {
    throw new Error(initRes.msg || '初始化断点续传失败')
  }
  const initData = initRes.data || {}
  const uploadId = initData.uploadId
  const chunkSize = initData.chunkSize
  const totalChunks = initData.totalChunks

  const statusRes = await request.get('/upload/status', {
    params: { uploadId }
  })
  if (statusRes.code !== 1) {
    throw new Error(statusRes.msg || '获取上传状态失败')
  }
  const statusData = statusRes.data || {}
  const uploadedIndexes = statusData.uploadedChunkIndexList || []
  const uploadedSet = new Set(uploadedIndexes)

  let uploadedBytes = 0
  for (let i = 1; i <= totalChunks; i++) {
    const start = (i - 1) * chunkSize
    const end = Math.min(file.size, start + chunkSize)
    const chunkSizeBytes = end - start

    if (uploadedSet.has(i)) {
      uploadedBytes += chunkSizeBytes
      if (onProgress) {
        onProgress((uploadedBytes / file.size) * 100)
      }
      continue
    }

    const chunk = file.slice(start, end)
    const form = new FormData()
    form.append('uploadId', uploadId)
    form.append('chunkIndex', String(i))
    form.append('file', chunk, file.name)

    await request.post('/upload/chunk', form, {
      onUploadProgress: (e) => {
        if (!onProgress) return
        const loaded = e.total ? e.loaded : chunkSizeBytes
        const current = uploadedBytes + loaded
        onProgress(Math.min((current / file.size) * 100, 100))
      }
    })

    uploadedBytes += chunkSizeBytes
    if (onProgress) {
      onProgress((uploadedBytes / file.size) * 100)
    }
  }

  const completeRes = await request.post('/upload/complete', null, {
    params: { uploadId }
  })
  if (completeRes.code !== 1) {
    throw new Error(completeRes.msg || '完成断点续传失败')
  }
  const data = completeRes.data || {}
  const messageType = chatFileType === 2 ? 2 : 3
  return {
    fileUrl: data.fileUrl,
    fileName: data.fileName || file.name,
    fileSize: data.fileSize || file.size,
    messageType
  }
}

async function calcSimpleHash(file) {
  const chunk = file.slice(0, Math.min(file.size, 1024 * 1024))
  const buffer = await chunk.arrayBuffer()
  let hash = 0
  const view = new Uint8Array(buffer)
  for (let i = 0; i < view.length; i++) {
    hash = (hash * 31 + view[i]) >>> 0
  }
  return `${file.size}-${hash}`
}
