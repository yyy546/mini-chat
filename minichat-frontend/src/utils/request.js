import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { BusinessError, AuthError } from './errors'
import logger from './logger'

const SUCCESS_CODE = 1

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    if (!config.headers.Accept && !config.headers.accept) {
      config.headers.Accept = 'application/json'
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 —— 核心改动：统一解包 {code, msg, data}
request.interceptors.response.use(
  (response) => {
    const body = response.data

    // 兼容非标准响应（blob 下载等）
    if (!body || typeof body.code === 'undefined') {
      return body
    }

    // 业务成功：直接返回 data 层
    if (body.code === SUCCESS_CODE) {
      return body.data
    }

    // 业务失败：统一抛 BusinessError
    ElMessage.error(body.msg || '请求失败')
    return Promise.reject(new BusinessError(body.code || 0, body.msg || '请求失败'))
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      sessionStorage.removeItem('token')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
      return Promise.reject(new AuthError())
    }
    // 网络错误或其他 HTTP 错误
    if (!status || status >= 500) {
      ElMessage.error('服务器异常，请稍后重试')
    }
    logger.warn('HTTP error:', status, error.message)
    return Promise.reject(error)
  }
)

export default request
