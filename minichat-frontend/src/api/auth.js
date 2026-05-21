import request from '../utils/request'

/**
 * 用户注册
 * @param {{ username: string, password: string, nickname: string }} data
 * @returns {Promise}
 */
export const register = (data) => {
  return request.post('/auth/register', data)
}

/**
 * 用户登录
 * @param {{ username: string, password: string }} data
 * @returns {Promise<{ token: string, id: number, username: string, nickname: string, avatar: string }>}
 */
export const login = (data) => {
  return request.post('/auth/login', data)
}

/**
 * 获取当前登录用户信息
 * @returns {Promise<{ id: number, username: string, nickname: string, avatar: string, gender: string, signature: string }>}
 */
export const getCurrentUser = () => {
  return request.get('/auth/me')
}

/**
 * 用户登出
 * @returns {Promise}
 */
export const logoutApi = () => {
  return request.post('/auth/logout')
}

/**
 * 获取用户详细信息
 * @returns {Promise<{ nickname: string, gender: string, signature: string, avatar: string }>}
 */
export const getUserDetail = () => {
  return request.get('/users/me')
}

/**
 * 更新用户详细信息
 * @param {{ nickname: string, gender: string, signature: string, avatar: string }} data
 * @returns {Promise}
 */
export const updateUserDetail = (data) => {
  return request.put('/users/me', data)
}

/**
 * 上传用户头像
 * @param {File} file
 * @returns {Promise<{ fileUrl: string }>}
 */
export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('avatar', file)
  return request.post('/users/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
