import request from '../utils/request'

// 搜索用户
export const searchUsers = (keyword) => {
  return request.get('/users/search', { params: { keyword } })
}

// 发送好友申请
export const sendFriendRequest = (data) => {
  // data: { toUserId, message }
  return request.post('/friend/request/send', data)
}

// 获取收到的好友申请列表
export const getIncomingRequests = () => {
  return request.get('/friend/request/list')
}

// 获取我发出的好友申请列表（RESTful: /sent）
export const getSentRequests = () => {
  return request.get('/friend/request/sent')
}

// 处理好友申请（同意/拒绝）
export const handleFriendRequest = (payload) => {
  return request.post('/friend/request/handle', payload)
}

// 获取好友列表
export const getFriendList = () => {
  return request.get('/friend/list')
}

// 设置好友备注（后续拓展）
export const setFriendRemark = (friendId, remark) => {
  return request.patch(`/friend/${friendId}/remark`, { remark })
}

// 修改好友备注
export const updateFriendRemark = (data) => {
  // Ensure we send all possible ID fields and remark fields to match backend DTO
  // Backend likely expects 'friendId' (user ID of friend) and 'remark' or 'remarkName'
  const payload = {
    ...data,
    id: data.friendId || data.id,
    friendId: data.friendId || data.id,
    remarkName: data.remark || data.remarkName,
    remark: data.remark || data.remarkName
  }
  return request.put('/friend/remark', payload)
}

// 获取好友分组列表
export const getFriendGroupList = () => {
  return request.get('/friend/group/list')
}

// 获取分组下的好友列表
export const getFriendGroupItemList = (groupName) => {
  return request.get(`/friend/group/${groupName}`)
}

// 修改好友分组
export const updateFriendGroup = (data) => {
  return request.put('/friend/group', data)
}

// 获取好友详情
export const getFriendDetail = (friendId) => {
  return request.get(`/friend/detail/${friendId}`)
}

// 删除好友
export const deleteFriend = (friendId) => {
  return request.delete(`/friend/delete/${friendId}`)
}
