/** 会话类型: 0=私聊, 1=群聊 */
export type SessionType = 0 | 1

export interface Session {
  id: number
  type: SessionType
  name: string
  avatar: string
  lastMessageTime: number
  unreadCount: number
  lastReadSeq?: number
  lastMessageSeq?: number
}

export interface SessionVO {
  id: number
  type: number
  name: string
  avatar: string
  lastMessageTime: string
  unreadCount: number
  lastMessageSeq: number
  lastReadSeq: number
}
