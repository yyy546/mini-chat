import request from '../utils/request'

export const isUserOnline = (userId: number): Promise<boolean> => {
  return request.get(`/user-status/online/${userId}`)
}

export const isUserOnlineMe = (): Promise<boolean> => {
  return request.get('/user-status/online/me')
}

export const batchCheckUserOnlineStatus = (userIds: number[]): Promise<Record<number, boolean>> => {
  return request.post('/user-status/online/batch', userIds)
}

export const getAllOnlineUsersId = (): Promise<number[]> => {
  return request.get('/user-status/online/all')
}
