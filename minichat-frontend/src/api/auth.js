import request from '../utils/request'

// 用户注册
export const register = (data) => {
    return request.post('/auth/register', data)
}

// 用户登录
export const login = (data) => {
    return request.post('/auth/login', data)
}

// 获取当前用户信息
export const getCurrentUser = () => {
    return request.get('/auth/me')
}

// 用户登出
export const logoutApi = () => {
    return request.post('/auth/logout')
}

// 获取用户详细信息
export const getUserDetail = () => {
    return request.get('/users/me')
}

// 更新用户详细信息
export const updateUserDetail = (data) => {
    return request.put('/users/me', data)
}

// 上传用户头像
export const uploadAvatar = (file) => {
    const formData = new FormData()
    formData.append('avatar', file)
    return request.post('/users/avatar', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}