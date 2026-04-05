import request from '../utils/request'

// 创建群组
export const createGroup = (data) => {
  return request.post('/group/create', data)
}

// 获取群组列表
export const getGroupList = () => {
  return request.get('/group/list')
}

// 获取群组详情
export const getGroupProfile = (groupId) => {
  return request.get(`/group/profile/${groupId}`)
}

// 上传群组头像
export const uploadGroupAvatar = (groupId, data) => {
  return request.post(`/group/avatar/${groupId}`, data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 更新群组资料
export const updateGroupProfile = (groupId, data) => {
  return request.put(`/group/profile/update/${groupId}`, data)
}

// 获取群组成员列表
export const getGroupMemberList = (groupId) => {
  return request.get(`/group/member/list/${groupId}`)
}

// 邀请用户加入群组
export const inviteToGroup = (data) => {
  return request.post(`/group/invite/${data.groupId}`, data)
}

// 移除群组成员
export const removeGroupMember = (data) => {
  return request.delete('/group/member/remove', { data })
}

// 退出群组
export const exitGroup = (groupId) => {
  return request.post(`/group/exit/${groupId}`)
}

// 更新群组成员角色 (升职/降职)
export const updateGroupMemberRole = (data) => {
  return request.put('/group/member/role', data)
}

// 转让群主
export const transferGroupOwner = (data) => {
  return request.put('/group/owner/transfer', data)
}

// 解散群组
export const dismissGroup = (groupId) => {
  return request.delete(`/group/dismiss/${groupId}`)
}

// 搜索群组
export const searchGroups = (keyword) => {
  return request.get('/group/search', { params: { keyword } })
}

// 发送群聊申请
export const sendGroupRequest = (data) => {
  return request.post('/group/request/send', data)
}

// 获取已发送的群聊申请列表
export const getSentGroupRequests = () => {
  return request.get('/group/request/sent')
}

// 获取群聊申请列表 (收到的)
export const getReceivedGroupRequests = () => {
  return request.get('/group/request/list')
}

// 处理群聊申请
export const handleGroupRequest = (data) => {
  return request.post('/group/request/handle', data)
}
