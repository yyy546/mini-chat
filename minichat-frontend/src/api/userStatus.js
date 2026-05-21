import request from '../utils/request'

/**
 * 检查单个用户是否在线
 * @param {number} userId
 * @returns {Promise<boolean>}
 */
export const isUserOnline = (userId) => {
  return request.get(`/user-status/online/${userId}`)
}

/**
 * 获取当前用户的在线状态
 * @returns {Promise<boolean>}
 */
export const isUserOnlineMe = () => {
  return request.get('/user-status/online/me')
}

/**
 * 批量检查用户在线状态
 * @param {number[]} userIds
 * @returns {Promise<Record<number, boolean>>}
 */
export const batchCheckUserOnlineStatus = (userIds) => {
  return request.post('/user-status/online/batch', userIds)
}

/**
 * 获取所有在线用户ID列表
 * @returns {Promise<number[]>}
 */
export const getAllOnlineUsersId = () => {
  return request.get('/user-status/online/all')
}
