import request from '../utils/request'

/**
 * 创建群组
 * @param {{ groupName: string, announcement: string, maxMembers: number, joinPolicy: number, invitePolicy: number, memberIds: number[] }} data
 * @returns {Promise<{ id: number, groupName: string, avatar: string }>}
 */
export const createGroup = (data) => {
  return request.post('/group/create', data)
}

/**
 * 获取已加入的群组列表
 * @returns {Promise<Array<{ id: number, groupName: string, avatar: string, memberCount: number }>>}
 */
export const getGroupList = () => {
  return request.get('/group/list')
}

/**
 * 获取群组详情
 * @param {number} groupId
 * @returns {Promise<{ id: number, groupName: string, avatar: string, announcement: string, ownerId: number, memberCount: number, maxMembers: number, joinPolicy: number, invitePolicy: number, createdTime: string }>}
 */
export const getGroupProfile = (groupId) => {
  return request.get(`/group/profile/${groupId}`)
}

/**
 * 上传群组头像
 * @param {number} groupId
 * @param {FormData} data
 * @returns {Promise<{ fileUrl: string }>}
 */
export const uploadGroupAvatar = (groupId, data) => {
  return request.post(`/group/avatar/${groupId}`, data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 更新群组资料
 * @param {number} groupId
 * @param {{ groupName: string, announcement: string, avatar: string, maxMembers: number, joinPolicy: number, invitePolicy: number }} data
 * @returns {Promise}
 */
export const updateGroupProfile = (groupId, data) => {
  return request.put(`/group/profile/update/${groupId}`, data)
}

/**
 * 获取群组成员列表
 * @param {number} groupId
 * @returns {Promise<Array<{ userId: number, nickname: string, nicknameInGroup: string, avatar: string, role: number }>>}
 */
export const getGroupMemberList = (groupId) => {
  return request.get(`/group/member/list/${groupId}`)
}

/**
 * 邀请用户加入群组
 * @param {{ groupId: number, userIds: number[] }} data
 * @returns {Promise}
 */
export const inviteToGroup = (data) => {
  return request.post(`/group/invite/${data.groupId}`, data)
}

/**
 * 移除群组成员
 * @param {{ groupId: number, userId: number }} data
 * @returns {Promise}
 */
export const removeGroupMember = (data) => {
  return request.delete('/group/member/remove', { data })
}

/**
 * 退出群组
 * @param {number} groupId
 * @returns {Promise}
 */
export const exitGroup = (groupId) => {
  return request.post(`/group/exit/${groupId}`)
}

/**
 * 更新群组成员角色 (升职/降职)
 * @param {{ groupId: number, userId: number, role: number }} data - role: 0=普通成员, 1=管理员
 * @returns {Promise}
 */
export const updateGroupMemberRole = (data) => {
  return request.put('/group/member/role', data)
}

/**
 * 转让群主
 * @param {{ groupId: number, newOwnerId: number }} data
 * @returns {Promise}
 */
export const transferGroupOwner = (data) => {
  return request.put('/group/owner/transfer', data)
}

/**
 * 解散群组
 * @param {number} groupId
 * @returns {Promise}
 */
export const dismissGroup = (groupId) => {
  return request.delete(`/group/dismiss/${groupId}`)
}

/**
 * 搜索群组
 * @param {string} keyword
 * @returns {Promise<Array<{ id: number, groupName: string, avatar: string }>>}
 */
export const searchGroups = (keyword) => {
  return request.get('/group/search', { params: { keyword } })
}

/**
 * 发送群聊申请
 * @param {{ groupId: number, message: string }} data
 * @returns {Promise}
 */
export const sendGroupRequest = (data) => {
  return request.post('/group/request/send', data)
}

/**
 * 获取已发送的群聊申请列表
 * @returns {Promise<Array>}
 */
export const getSentGroupRequests = () => {
  return request.get('/group/request/sent')
}

/**
 * 获取收到的群聊申请列表
 * @returns {Promise<Array>}
 */
export const getReceivedGroupRequests = () => {
  return request.get('/group/request/list')
}

/**
 * 处理群聊申请
 * @param {{ groupId: number, applicantId: number, status: number }} data - status: 1=同意, 2=拒绝
 * @returns {Promise}
 */
export const handleGroupRequest = (data) => {
  return request.post('/group/request/handle', data)
}
