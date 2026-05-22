import request from '../utils/request'

interface CreateGroupDTO {
  groupName: string
  announcement: string
  maxMembers: number
  joinPolicy: number
  invitePolicy: number
  memberIds: number[]
}

interface GroupVO {
  id: number
  groupName: string
  avatar: string
  memberCount?: number
}

interface GroupProfileVO {
  id: number
  groupName: string
  avatar: string
  announcement: string
  ownerId: number
  memberCount: number
  maxMembers: number
  joinPolicy: number
  invitePolicy: number
  createdTime: string
}

interface GroupMemberVO {
  userId: number
  nickname: string
  nicknameInGroup: string
  avatar: string
  role: number
}

interface UpdateGroupProfileDTO {
  groupName: string
  announcement: string
  avatar: string
  maxMembers: number
  joinPolicy: number
  invitePolicy: number
}

interface UpdateMemberRoleDTO {
  groupId: number
  userId: number
  role: number
}

interface TransferOwnerDTO {
  groupId: number
  newOwnerId: number
}

interface GroupRequestDTO {
  groupId: number
  message: string
}

interface HandleGroupRequestDTO {
  groupId: number
  applicantId: number
  status: number
}

export const createGroup = (data: CreateGroupDTO): Promise<GroupVO> => {
  return request.post('/group/create', data)
}

export const getGroupList = (): Promise<GroupVO[]> => {
  return request.get('/group/list')
}

export const getGroupProfile = (groupId: number): Promise<GroupProfileVO> => {
  return request.get(`/group/profile/${groupId}`)
}

export const uploadGroupAvatar = (groupId: number, data: FormData): Promise<{ fileUrl: string }> => {
  return request.post(`/group/avatar/${groupId}`, data, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const updateGroupProfile = (groupId: number, data: UpdateGroupProfileDTO): Promise<void> => {
  return request.put(`/group/profile/update/${groupId}`, data)
}

export const getGroupMemberList = (groupId: number): Promise<GroupMemberVO[]> => {
  return request.get(`/group/member/list/${groupId}`)
}

export const inviteToGroup = (data: { groupId: number; userIds: number[] }): Promise<void> => {
  return request.post(`/group/invite/${data.groupId}`, data)
}

export const removeGroupMember = (data: { groupId: number; userId: number }): Promise<void> => {
  return request.delete('/group/member/remove', { data })
}

export const exitGroup = (groupId: number): Promise<void> => {
  return request.post(`/group/exit/${groupId}`)
}

export const updateGroupMemberRole = (data: UpdateMemberRoleDTO): Promise<void> => {
  return request.put('/group/member/role', data)
}

export const transferGroupOwner = (data: TransferOwnerDTO): Promise<void> => {
  return request.put('/group/owner/transfer', data)
}

export const dismissGroup = (groupId: number): Promise<void> => {
  return request.delete(`/group/dismiss/${groupId}`)
}

export const searchGroups = (keyword: string): Promise<GroupVO[]> => {
  return request.get('/group/search', { params: { keyword } })
}

export const sendGroupRequest = (data: GroupRequestDTO): Promise<void> => {
  return request.post('/group/request/send', data)
}

export const getSentGroupRequests = (): Promise<unknown[]> => {
  return request.get('/group/request/sent')
}

export const getReceivedGroupRequests = (): Promise<unknown[]> => {
  return request.get('/group/request/list')
}

export const handleGroupRequest = (data: HandleGroupRequestDTO): Promise<void> => {
  return request.post('/group/request/handle', data)
}
