/** 消息类型: 1=文本, 2=图片, 3=文件, 5=撤回 */
export type MessageType = 1 | 2 | 3 | 5

export interface PrivateMessageDTO {
  senderId: number
  receiverId: number
  content: string
  messageType: MessageType
  fileName?: string
  fileSize?: number
  fileUrl?: string
  tempId?: string
}

export interface GroupMessageDTO {
  senderId: number
  groupId: number
  content: string
  messageType: MessageType
  fileName?: string
  fileSize?: number
  fileUrl?: string
  tempId?: string
}

export interface UIMessage {
  id: string | number
  fromId: number | 'self'
  toId: number
  groupId?: number
  content: string
  timestamp: number
  type: MessageType
  fileName?: string
  fileSize?: number
  fileUrl?: string
  senderNickname?: string
  senderAvatar?: string
  messageSeq?: number
  isSending: boolean
  isReceived: boolean
  sendError: boolean
  tempId?: string
  recall?: boolean
}

export interface RecallMessageDTO {
  messageId: number
  chatId: number
  recallUserId: number
  isGroup: boolean
  timestamp: number
}
