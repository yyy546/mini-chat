import request from '../utils/request'
import type { PageResult } from '../types/api'

interface PrivateMessageVO {
  messageId?: number
  senderId: number
  receiverId: number
  content: string
  messageType: number
  fileName?: string
  fileSize?: number
  fileUrl?: string
  senderNickname?: string
  senderAvatar?: string
  messageSeq?: number
  sendTime?: string
}

interface GroupMessageVO extends PrivateMessageVO {
  groupId: number
}

export const getChatHistory = async (userId: number): Promise<PrivateMessageVO[]> => {
  try {
    const res = await request.get('/chat/private/history', { params: { userId } })
    return Array.isArray(res) ? res : []
  } catch {
    return []
  }
}

export const getGroupChatHistory = async (groupId: number): Promise<GroupMessageVO[]> => {
  try {
    const res = await request.get('/chat/group/history', { params: { groupId } })
    return Array.isArray(res) ? res : []
  } catch {
    return []
  }
}

export const markMessagesAsRead = (receiverId: number): Promise<void> => {
  return request.post('/chat/private/mark-read', null, {
    params: { receiverId }
  })
}

export const markGroupMessageRead = (groupId: number): Promise<void> => {
  return request.post('/chat/group/mark-read', null, {
    params: { groupId }
  })
}

export const getGroupMessageHistory = (
  groupId: number,
  page: number = 1,
  pageSize: number = 50
): Promise<PageResult<GroupMessageVO>> => {
  return request.get('/chat/group/history', {
    params: { groupId, page, pageSize }
  })
}

export const getPrivateMessageHistory = (
  userId: number,
  page: number = 1,
  pageSize: number = 50
): Promise<PageResult<PrivateMessageVO>> => {
  return request.get('/chat/private/history', {
    params: { userId, page, pageSize }
  })
}

export const recallPrivateMessage = (messageId: number): Promise<void> => {
  return request.post('/chat/private/recall', null, {
    params: { messageId }
  })
}

export const recallGroupMessage = (groupId: number, messageId: number): Promise<void> => {
  return request.post('/chat/group/recall', null, {
    params: { groupId, messageId }
  })
}

interface UploadResult {
  fileUrl: string
  fileName: string
  fileSize: number
  messageType: number
}

export const uploadPrivateFile = (file: File, type: number): Promise<UploadResult> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/private/upload', formData, {
    params: { type },
    headers: {
      Accept: 'application/json'
    }
  })
}

export const searchChatMessages = (
  keyword: string,
  type: number,
  targetId: number
): Promise<PrivateMessageVO[]> => {
  return request.get('/chat/search', {
    params: { keyword, type, targetId }
  })
}

export const uploadGroupFile = (file: File, type: number): Promise<UploadResult> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/chat/group/upload', formData, {
    params: { type },
    headers: {
      Accept: 'application/json'
    }
  })
}
