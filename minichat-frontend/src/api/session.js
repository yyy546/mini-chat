import request from '../utils/request'

/**
 * 获取会话列表（包含好友和群聊会话）
 * @returns {Promise<Array<{ id: number, type: number, name: string, avatar: string, lastMessageTime: string, unreadCount: number, lastMessageSeq: number, lastReadSeq: number }>>}
 */
export const getSessionList = () => {
  return request.get('/session/list')
}
