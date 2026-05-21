import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import router from '../router'
import { login, register, getCurrentUser, logoutApi } from '../api/auth'
// 导入你的请求实例（如axios），用于统一设置请求头（关键！）
import request from '../utils/request' // 请根据实际路径修改
import logger from '../utils/logger'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: null,
    token: sessionStorage.getItem('token') || ''
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    // 新增：快速获取用户ID，避免后续重复写userInfo?.id
    userId: (state) => state.userInfo?.id || null
  },

  actions: {
    // 用户注册
    async userRegister(userData) {
      try {
        const response = await register(userData)
        ElMessage.success('注册成功，请登录')
        // 注册成功后自动跳转到登录页（优化体验）
        router.push('/login')
        return response // async函数自动返回Promise，无需手动包层
      } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || '注册失败'
        ElMessage.error(errorMsg)
        throw error // 抛出错误，让组件层可以捕获
      }
    },

    // 用户登录
    async userLogin(loginData) {
      try {
        const response = await login(loginData)

        const token = response.token || response.data?.token
        const userInfo = response.user || response.data?.user || response.data?.userInfo || response.data?.currentUser

        if (!token) {
          throw new Error('登录失败：未获取到Token')
        }

        this.token = token
        sessionStorage.setItem('token', token)

        if (userInfo && userInfo.id) {
          this.userInfo = userInfo
        } else {
          try {
            await this.fetchCurrentUser()
          } catch (e) {
            ElMessage.error('登录成功，但未获取到用户信息')
          }
        }

        ElMessage.success('登录成功')
        return response
      } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || '登录失败'
        ElMessage.error(errorMsg)
        throw error
      }
    },

    // 退出登录
    async logout() {
      try {
        // 先断开 WebSocket 连接（确保状态及时更新）
        const { useChatStore } = await import('./chat')
        const chatStore = useChatStore()
        if (chatStore.isConnected) {
          chatStore.disconnect()
        }

        // 调用退出登录接口
        await logoutApi()
      } catch (e) {
        logger.error('退出登录接口调用失败', e)
      }
      this.token = ''
      this.userInfo = null
      sessionStorage.removeItem('token')
      document.documentElement.classList.remove('dark')
      // 清除请求头的Token
      delete request.defaults.headers.common['Authorization']
      ElMessage.info('已退出登录')
      router.push('/login')
    },

    // 新增：应用初始化时的用户信息拉取（供main.js调用）
    async initUserInfo() {
      if (this.token) {
        request.defaults.headers.common['Authorization'] = this.token
        await this.fetchCurrentUser()
      }
    },
    async fetchCurrentUser() {
      const res = await getCurrentUser()
      if (res?.code === 1 && res?.data) {
        this.userInfo = res.data
        return res
      }
      if (res?.data) {
        this.userInfo = res.data
        return res
      }
      throw new Error(res?.msg || '未获取到用户信息')
    }
  }
})
