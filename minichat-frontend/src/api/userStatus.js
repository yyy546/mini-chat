import request from '../utils/request'

export const isUserOnline = (userId) => {
  return request.get(`/user-status/online/${userId}`)
}

export const isUserOnlineMe = () => {
  return request.get('/user-status/online/me')
}

export const batchCheckUserOnlineStatus = (userIds) => {
  return request.post('/user-status/online/batch', userIds)
}

export const getAllOnlineUsersId = () => {
  return request.get('/user-status/online/all')
}
