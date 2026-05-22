import request from '../utils/request'
import type { UserLoginVO, UserInfo, UserDetailVO } from '../types/user'

export const register = (data: { username: string; password: string; nickname: string }): Promise<void> => {
  return request.post('/auth/register', data)
}

export const login = (data: { username: string; password: string }): Promise<UserLoginVO> => {
  return request.post('/auth/login', data)
}

export const getCurrentUser = (): Promise<UserInfo> => {
  return request.get('/auth/me')
}

export const logoutApi = (): Promise<void> => {
  return request.post('/auth/logout')
}

export const getUserDetail = (): Promise<UserDetailVO> => {
  return request.get('/users/me')
}

export const updateUserDetail = (data: UserDetailVO): Promise<void> => {
  return request.put('/users/me', data)
}

export const uploadAvatar = (file: File): Promise<{ fileUrl: string }> => {
  const formData = new FormData()
  formData.append('avatar', file)
  return request.post('/users/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
