import request from '../utils/request'

// 获取会话列表（包含好友和群聊）
export const getSessionList = () => {
  return request.get('/session/list')
}
