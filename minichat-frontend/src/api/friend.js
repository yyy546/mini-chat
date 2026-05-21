import request from '../utils/request'

/**
 * 搜索用户
 * @param {string} keyword
 * @returns {Promise<Array<{ id: number, username: string, nickname: string, avatar: string }>>}
 */
export const searchUsers = (keyword) => {
  return request.get('/users/search', { params: { keyword } })
}

/**
 * 发送好友申请
 * @param {{ toUserId: number, message: string }} data
 * @returns {Promise}
 */
export const sendFriendRequest = (data) => {
  return request.post('/friend/request/send', data)
}

/**
 * 获取收到的好友申请列表
 * @returns {Promise<Array>}
 */
export const getIncomingRequests = () => {
  return request.get('/friend/request/list')
}

/**
 * 获取我发出的好友申请列表
 * @returns {Promise<Array>}
 */
export const getSentRequests = () => {
  return request.get('/friend/request/sent')
}

/**
 * 处理好友申请（同意/拒绝）
 * @param {{ requestId: number, status: number }} payload - status: 1=同意, 2=拒绝
 * @returns {Promise}
 */
export const handleFriendRequest = (payload) => {
  return request.post('/friend/request/handle', payload)
}

/**
 * 获取好友列表
 * @returns {Promise<Array<{ id: number, friendId: number, username: string, nickname: string, avatar: string, remark: string }>>}
 */
export const getFriendList = () => {
  return request.get('/friend/list')
}

/**
 * 设置好友备注 (已废弃，请使用 updateFriendRemark)
 * @param {number} friendId
 * @param {string} remark
 * @returns {Promise}
 */
export const setFriendRemark = (friendId, remark) => {
  return request.patch(`/friend/${friendId}/remark`, { remark })
}

/**
 * 修改好友备注
 * @param {{ friendId: number, remark: string }} data
 * @returns {Promise}
 */
export const updateFriendRemark = (data) => {
  const payload = {
    ...data,
    id: data.friendId || data.id,
    friendId: data.friendId || data.id,
    remarkName: data.remark || data.remarkName,
    remark: data.remark || data.remarkName
  }
  return request.put('/friend/remark', payload)
}

/**
 * 获取好友分组列表
 * @returns {Promise<Array<{ groupName: string }>>}
 */
export const getFriendGroupList = () => {
  return request.get('/friend/group/list')
}

/**
 * 获取分组下的好友列表
 * @param {string} groupName
 * @returns {Promise<Array>}
 */
export const getFriendGroupItemList = (groupName) => {
  return request.get(`/friend/group/${groupName}`)
}

/**
 * 修改好友分组
 * @param {{ friendId: number, groupName: string }} data
 * @returns {Promise}
 */
export const updateFriendGroup = (data) => {
  return request.put('/friend/group', data)
}

/**
 * 获取好友详情
 * @param {number} friendId
 * @returns {Promise<{ friendUserId: number, friendAvatar: string, remarkName: string, friendNickname: string, groupName: string, gender: string, signature: string }>}
 */
export const getFriendDetail = (friendId) => {
  return request.get(`/friend/detail/${friendId}`)
}

/**
 * 删除好友
 * @param {number} friendId
 * @returns {Promise}
 */
export const deleteFriend = (friendId) => {
  return request.delete(`/friend/delete/${friendId}`)
}
