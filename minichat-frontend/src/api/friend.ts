import request from '../utils/request'

interface UserSearchResult {
  id: number
  username: string
  nickname: string
  avatar: string
}

interface FriendRequestDTO {
  toUserId: number
  message: string
}

interface HandleRequestDTO {
  requestId: number
  status: number
}

interface FriendVO {
  id: number
  friendId: number
  userId?: number
  uid?: number
  friend_id?: number
  username: string
  nickname: string
  avatar: string
  remark?: string
  remarkName?: string
}

interface FriendDetailVO {
  friendUserId: number
  friendAvatar: string
  remarkName: string
  friendNickname: string
  groupName: string
  gender: string
  signature: string
}

interface FriendGroupVO {
  groupName: string
}

interface FriendGroupItemVO {
  friendId: number
  friendAvatar: string
  remarkName: string
  friendNickname: string
  onlineStatus?: boolean | number
}

export const searchUsers = (keyword: string): Promise<UserSearchResult[]> => {
  return request.get('/users/search', { params: { keyword } })
}

export const sendFriendRequest = (data: FriendRequestDTO): Promise<void> => {
  return request.post('/friend/request/send', data)
}

export const getIncomingRequests = (): Promise<unknown[]> => {
  return request.get('/friend/request/list')
}

export const getSentRequests = (): Promise<unknown[]> => {
  return request.get('/friend/request/sent')
}

export const handleFriendRequest = (payload: HandleRequestDTO): Promise<void> => {
  return request.post('/friend/request/handle', payload)
}

export const getFriendList = (): Promise<FriendVO[]> => {
  return request.get('/friend/list')
}

export const setFriendRemark = (friendId: number, remark: string): Promise<void> => {
  return request.patch(`/friend/${friendId}/remark`, { remark })
}

export const updateFriendRemark = (data: {
  friendId?: number
  id?: number
  remark?: string
  remarkName?: string
}): Promise<void> => {
  const payload = {
    ...data,
    id: data.friendId || data.id,
    friendId: data.friendId || data.id,
    remarkName: data.remark || data.remarkName,
    remark: data.remark || data.remarkName
  }
  return request.put('/friend/remark', payload)
}

export const getFriendGroupList = (): Promise<FriendGroupVO[]> => {
  return request.get('/friend/group/list')
}

export const getFriendGroupItemList = (groupName: string): Promise<FriendGroupItemVO[]> => {
  return request.get(`/friend/group/${groupName}`)
}

export const updateFriendGroup = (data: { friendId: number; groupName: string }): Promise<void> => {
  return request.put('/friend/group', data)
}

export const getFriendDetail = (friendId: number): Promise<FriendDetailVO> => {
  return request.get(`/friend/detail/${friendId}`)
}

export const deleteFriend = (friendId: number): Promise<void> => {
  return request.delete(`/friend/delete/${friendId}`)
}
