import request from '../utils/request'

/**
 * 获取私聊消息历史 (旧版兼容，请使用 getPrivateMessageHistory)
 * @param {number} userId
 * @returns {Promise<Array>}
 */
export const getChatHistory = async (userId) => {
  try {
    const res = await request.get('/chat/private/history', { params: { userId } })
    return Array.isArray(res) ? res : []
  } catch (e) {
    return []
  }
}

/**
 * 获取群聊消息历史 (旧版兼容，请使用 getGroupMessageHistory)
 * @param {number} groupId
 * @returns {Promise<Array>}
 */
export const getGroupChatHistory = async (groupId) => {
  try {
    const res = await request.get('/chat/group/history', { params: { groupId } })
    return Array.isArray(res) ? res : []
  } catch (e) {
    return []
  }
}

/**
 * 标记私聊消息为已读
 * @param {number} receiverId - 消息发送者ID（即当前聊天对象）
 * @returns {Promise}
 */
export const markMessagesAsRead = (receiverId) => {
  return request.post('/chat/private/mark-read', null, {
    params: { receiverId }
  })
}

/**
 * 标记群聊消息为已读
 * @param {number} groupId
 * @returns {Promise}
 */
export const markGroupMessageRead = (groupId) => {
  return request.post('/chat/group/mark-read', null, {
    params: { groupId }
  })
}

/**
 * 获取群聊消息历史（分页）
 * @param {number} groupId
 * @param {number} page
 * @param {number} pageSize
 * @returns {Promise<{ records: Array, total: number, current: number, size: number }>}
 */
export const getGroupMessageHistory = (groupId, page = 1, pageSize = 50) => {
  return request.get('/chat/group/history', {
    params: { groupId, page, pageSize }
  })
}

/**
 * 获取私聊消息历史（分页）
 * @param {number} userId - 聊天对象ID
 * @param {number} page - 页码
 * @param {number} pageSize - 每页条数
 * @returns {Promise<{ records: Array, total: number, current: number, size: number }>}
 */
export const getPrivateMessageHistory = (userId, page = 1, pageSize = 50) => {
  return request.get('/chat/private/history', {
    params: { userId, page, pageSize }
  })
}

/**
 * 撤回私聊消息
 * @param {number} messageId
 * @returns {Promise}
 */
export const recallPrivateMessage = (messageId) => {
  return request.post('/chat/private/recall', null, {
    params: { messageId }
  })
}

/**
 * 撤回群聊消息
 * @param {number} groupId
 * @param {number} messageId
 * @returns {Promise}
 */
export const recallGroupMessage = (groupId, messageId) => {
  return request.post('/chat/group/recall', null, {
    params: { groupId, messageId }
  })
}

/**
 * 上传私聊文件或图片
 * @param {File} file
 * @param {number} type - 消息类型 (2=图片, 3=文件)
 * @returns {Promise<{ fileUrl: string, fileName: string, fileSize: number, messageType: number }>}
 */
export const uploadPrivateFile = (file, type) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/private/upload', formData, {
    params: { type },
    headers: {
      Accept: 'application/json'
    }
  })
}

/**
 * 搜索聊天消息
 * @param {string} keyword
 * @param {number} type - 0=私聊, 1=群聊
 * @param {number} targetId
 * @returns {Promise<Array>}
 */
export const searchChatMessages = (keyword, type, targetId) => {
  return request.get('/chat/search', {
    params: { keyword, type, targetId }
  })
}

/**
 * 上传群聊文件或图片
 * @param {File} file
 * @param {number} type - 消息类型 (2=图片, 3=文件)
 * @returns {Promise<{ fileUrl: string, fileName: string, fileSize: number, messageType: number }>}
 */
export const uploadGroupFile = (file, type) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/group/upload', formData, {
    params: { type },
    headers: {
      Accept: 'application/json'
    }
  })
}
