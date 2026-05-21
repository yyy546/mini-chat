import request from '../utils/request'

export const getChatHistory = async (userId) => {
  try {
    const res = await request.get('/chat/private/history', { params: { userId } })
    if (res?.code === 1) {
      const data = res.data
      return Array.isArray(data) ? data : []
    }
    return []
  } catch (e) {
    return []
  }
}

export const getGroupChatHistory = async (groupId) => {
  try {
    const res = await request.get('/chat/group/history', { params: { groupId } })
    if (res?.code === 1) {
      const data = res.data
      return Array.isArray(data) ? data : []
    }
    return []
  } catch (e) {
    return []
  }
}

export const markMessagesAsRead = (receiverId) => {
  return request.post('/chat/private/mark-read', null, {
    params: { receiverId }
  })
}

export const markGroupMessageRead = (groupId) => {
  return request.post('/chat/group/mark-read', null, {
    params: { groupId }
  })
}

export const getGroupMessageHistory = (groupId, page = 1, pageSize = 50) => {
  return request.get('/chat/group/history', {
    params: { groupId, page, pageSize }
  })
}

export const getPrivateMessageHistory = (userId, page = 1, pageSize = 50) => {
  return request.get('/chat/private/history', {
    params: { userId, page, pageSize }
  })
}

export const recallPrivateMessage = (messageId) => {
  return request.post('/chat/private/recall', null, {
    params: { messageId }
  })
}

export const recallGroupMessage = (groupId, messageId) => {
  return request.post('/chat/group/recall', null, {
    params: { groupId, messageId }
  })
}

// 上传私聊文件或图片
export const uploadPrivateFile = (file, type) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/private/upload', formData, {
    params: { type },
    headers: {
      // 不设置 Content-Type，让浏览器自动设置（包含 boundary）
      Accept: 'application/json' // 明确指定接受 JSON 响应
    }
  })
}

export const searchChatMessages = (keyword, type, targetId) => {
  return request.get('/chat/search', {
    params: { keyword, type, targetId }
  })
}

// 上传群聊文件或图片
export const uploadGroupFile = (file, type) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/group/upload', formData, {
    params: { type },
    headers: {
      // 不设置 Content-Type，让浏览器自动设置（包含 boundary）
      Accept: 'application/json' // 明确指定接受 JSON 响应
    }
  })
}
