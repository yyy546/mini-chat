import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import router from '../router'
import { login, register, getCurrentUser, logoutApi } from '../api/auth'
import request from '../utils/request'
import logger from '../utils/logger'
import type { UserInfo, UserLoginVO } from '../types/user'

interface UserState {
  userInfo: UserInfo | null
  token: string
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: null,
    token: sessionStorage.getItem('token') || ''
  }),

  getters: {
    isLoggedIn: (state: UserState) => !!state.token,
    userId: (state: UserState) => state.userInfo?.id || null
  },

  actions: {
    async userRegister(userData: { username: string; password: string; nickname: string }) {
      try {
        await register(userData)
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        const errorMsg = err.response?.data?.message || err.message || '注册失败'
        ElMessage.error(errorMsg)
        throw error
      }
    },

    async userLogin(loginData: { username: string; password: string }) {
      try {
        const response: UserLoginVO = await login(loginData)

        const token = response.token
        const userInfo = response

        if (!token) {
          throw new Error('登录失败：未获取到Token')
        }

        this.token = token
        sessionStorage.setItem('token', token)

        if (userInfo && userInfo.id) {
          this.userInfo = userInfo as UserInfo
        } else {
          try {
            await this.fetchCurrentUser()
          } catch {
            ElMessage.error('登录成功，但未获取到用户信息')
          }
        }

        ElMessage.success('登录成功')
        return response
      } catch (error: unknown) {
        const err = error as { response?: { data?: { message?: string } }; message?: string }
        const errorMsg = err.response?.data?.message || err.message || '登录失败'
        ElMessage.error(errorMsg)
        throw error
      }
    },

    async logout() {
      try {
        const { useChatStore } = await import('./chat')
        const chatStore = useChatStore()
        if (chatStore.isConnected) {
          chatStore.disconnect()
        }

        await logoutApi()
      } catch (e: unknown) {
        logger.error('退出登录接口调用失败', e)
      }
      this.token = ''
      this.userInfo = null
      sessionStorage.removeItem('token')
      document.documentElement.classList.remove('dark')
      delete request.defaults.headers.common['Authorization']
      ElMessage.info('已退出登录')
      router.push('/login')
    },

    async initUserInfo() {
      if (this.token) {
        request.defaults.headers.common['Authorization'] = this.token
        await this.fetchCurrentUser()
      }
    },

    async fetchCurrentUser() {
      const res = await getCurrentUser()
      if (res?.id) {
        this.userInfo = res
        return res
      }
      throw new Error((res as unknown as { msg?: string })?.msg || '未获取到用户信息')
    }
  }
})
