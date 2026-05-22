import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  searchUsers,
  sendFriendRequest,
  getIncomingRequests,
  getSentRequests,
  handleFriendRequest,
  getFriendList,
  updateFriendRemark,
  getFriendGroupList,
  getFriendGroupItemList,
  updateFriendGroup,
  getFriendDetail,
  deleteFriend
} from '../api/friend'
import { batchCheckUserOnlineStatus } from '../api/userStatus'
import { useChatStore } from './chat'
import logger from '../utils/logger'

interface FriendItem {
  relationId?: number
  id: number
  friendId?: number
  userId?: number
  uid?: number
  username?: string
  nickname?: string
  avatar?: string
  remark?: string
  remarkName?: string
  online: boolean
  [key: string]: unknown
}

interface FriendRequestItem {
  id: number
  [key: string]: unknown
}

interface FriendGroup {
  groupName: string
  items: FriendItem[]
  expanded: boolean
  loaded: boolean
  [key: string]: unknown
}

interface FriendDetail {
  id: number
  userId?: number
  avatar?: string
  remark?: string
  nickname?: string
  groupName?: string
  gender?: string
  signature?: string
  online: boolean
  [key: string]: unknown
}

interface FriendState {
  friends: FriendItem[]
  incomingRequests: FriendRequestItem[]
  sentRequests: FriendRequestItem[]
  searchResults: unknown[]
  groups: FriendGroup[]
  currentFriendDetail: FriendDetail | null
  loading: boolean
  requestLoading: boolean
}

export const useFriendStore = defineStore('friend', {
  state: (): FriendState => ({
    friends: [],
    incomingRequests: [],
    sentRequests: [],
    searchResults: [],
    groups: [],
    currentFriendDetail: null,
    loading: false,
    requestLoading: false
  }),

  actions: {
    async fetchFriends() {
      this.loading = true
      try {
        const res = await getFriendList()
        const list = Array.isArray(res) ? res : []
        const normalize = (raw: Record<string, unknown>) => {
          const friendId = (raw.friendId ?? raw.friend_id ?? raw.userId ?? raw.uid ?? raw.id) as number
          return {
            ...raw,
            relationId: raw.id,
            friendId,
            id: friendId,
            remark: (raw.remark || raw.remarkName) as string | undefined,
            online: false
          } as FriendItem
        }
        this.friends = Array.isArray(list) ? (list as unknown as Record<string, unknown>[]).map(normalize) : []

        if (this.friends.length > 0) {
          const ids = this.friends.map((f) => f.id)
          try {
            const statusMap = await batchCheckUserOnlineStatus(ids)
            if (statusMap) {
              this.friends = this.friends.map((f) => ({
                ...f,
                online: !!statusMap[f.id]
              }))
            }
          } catch (err: unknown) {
            logger.error('Failed to fetch online status', err)
          }
        }
      } catch {
        ElMessage.error('获取好友列表失败')
      } finally {
        this.loading = false
      }
    },

    async fetchIncomingRequests() {
      this.requestLoading = true
      try {
        const res = await getIncomingRequests()
        this.incomingRequests = (res as FriendRequestItem[]) || []
      } catch {
        ElMessage.error('获取好友申请失败')
      } finally {
        this.requestLoading = false
      }
    },

    async fetchSentRequests() {
      try {
        const res = await getSentRequests()
        this.sentRequests = (res as FriendRequestItem[]) || []
      } catch {
        // 非关键，忽略错误
      }
    },

    updateFriendStatus(userId: number, isOnline: boolean) {
      const friendIndex = this.friends.findIndex((f) => f.id == userId || f.friendId == userId)
      if (friendIndex !== -1) {
        const friend = this.friends[friendIndex]
        if (friend.online !== isOnline) {
          this.friends[friendIndex] = { ...friend, online: isOnline }
          logger.debug(
            `好友 ${friend.nickname || friend.username || userId} (ID: ${userId}) 状态已更新为: ${isOnline ? '在线' : '离线'}`
          )
        }
      } else {
        logger.warn(`收到未知好友的状态更新，ID: ${userId}`)
      }
    },

    async doSearch(keyword: string) {
      if (!keyword) {
        this.searchResults = []
        return
      }
      this.loading = true
      try {
        const res = await searchUsers(keyword)
        this.searchResults = Array.isArray(res) ? res : []
      } catch {
        ElMessage.error('搜索用户失败')
      } finally {
        this.loading = false
      }
    },

    async applyFriend(toUserId: number, message: string = '') {
      try {
        await sendFriendRequest({ toUserId, message })
        ElMessage.success('好友申请已发送')
      } catch (e: unknown) {
        const err = e as { response?: { data?: { message?: string } } }
        ElMessage.error(err.response?.data?.message || '发送好友申请失败')
        throw e
      }
    },

    async processRequest(requestId: number, actionOrStatus: string | number) {
      const status = typeof actionOrStatus === 'string' ? (actionOrStatus === 'accept' ? 1 : 2) : actionOrStatus
      try {
        await handleFriendRequest({ requestId, status })
        ElMessage.success(status === 1 ? '已同意申请' : '已拒绝申请')
        await Promise.all([this.fetchIncomingRequests(), this.fetchFriends()])
      } catch (e: unknown) {
        const err = e as { response?: { data?: { message?: string } } }
        ElMessage.error(err.response?.data?.message || '处理好友申请失败')
        throw e
      }
    },

    async fetchFriendGroups() {
      try {
        const res = await getFriendGroupList()
        const rawGroups = Array.isArray(res) ? res : []
        this.groups = (rawGroups as unknown as Record<string, unknown>[]).map((g) => ({
          ...g,
          items: [],
          expanded: false,
          loaded: false
        })) as FriendGroup[]
      } catch (e: unknown) {
        logger.error('获取分组列表失败', e)
      }
    },

    async fetchGroupItems(groupName: string) {
      try {
        const res = await getFriendGroupItemList(groupName)
        const items = Array.isArray(res) ? res : []
        const group = this.groups.find((g) => g.groupName === groupName)
        if (group) {
          group.items = (items as unknown as Record<string, unknown>[]).map((item) => ({
            ...item,
            id: item.friendId,
            avatar: item.friendAvatar,
            remark: item.remarkName,
            nickname: item.friendNickname,
            online: item.onlineStatus === true || item.onlineStatus === 1,
            signature: null
          })) as FriendItem[]
          group.loaded = true
        }
      } catch (e: unknown) {
        logger.error(`获取分组 ${groupName} 好友失败`, e)
      }
    },

    async changeFriendGroup(friendId: number, groupName: string) {
      try {
        await updateFriendGroup({ friendId, groupName })
        ElMessage.success('好友分组修改成功')
        if (
          this.currentFriendDetail &&
          (this.currentFriendDetail.id === friendId || this.currentFriendDetail.userId === friendId)
        ) {
          this.currentFriendDetail.groupName = groupName
        }
        await this.fetchFriends()
        await this.fetchFriendGroups()
      } catch (e: unknown) {
        const err = e as { response?: { data?: { message?: string } } }
        ElMessage.error(err.response?.data?.message || '修改好友分组失败')
        throw e
      }
    },

    async fetchFriendDetail(friendId: number) {
      try {
        const res = await getFriendDetail(friendId)
        const data = res as unknown as Record<string, unknown>
        if (data) {
          this.currentFriendDetail = {
            ...data,
            id: data.friendUserId as number,
            avatar: data.friendAvatar as string,
            remark: data.remarkName as string,
            nickname: data.friendNickname as string,
            groupName: data.groupName as string,
            gender: data.gender as string,
            signature: data.signature as string,
            online: false
          }
          const friendInList = this.friends.find((f) => f.id === (data.friendUserId as number))
          if (friendInList) {
            this.currentFriendDetail.online = friendInList.online
          }
        }
      } catch (e: unknown) {
        logger.error('获取好友详情失败', e)
      }
    },

    async updateRemark(friendId: number, remark: string) {
      try {
        await updateFriendRemark({ friendId, remark })
        ElMessage.success('备注已更新')
        const friend = this.friends.find((f) => f.id === friendId)
        if (friend) {
          friend.remark = remark
        }
        if (this.groups) {
          this.groups.forEach((group) => {
            if (group.items) {
              const item = group.items.find((i) => i.id === friendId || i.friendId === friendId)
              if (item) {
                item.remark = remark
              }
            }
          })
        }
        if (
          this.currentFriendDetail &&
          (this.currentFriendDetail.id === friendId || this.currentFriendDetail.userId === friendId)
        ) {
          this.currentFriendDetail.remark = remark
        }
      } catch (e: unknown) {
        const err = e as { response?: { data?: { message?: string } } }
        ElMessage.error(err.response?.data?.message || '更新备注失败')
        throw e
      }
    },

    async deleteFriend(friendId: number) {
      try {
        await deleteFriend(friendId)
        ElMessage.success('好友删除成功')
        await this.fetchFriends()
        if (
          this.currentFriendDetail &&
          (this.currentFriendDetail.id === friendId || this.currentFriendDetail.userId === friendId)
        ) {
          this.currentFriendDetail = null
        }
        await this.fetchFriendGroups()
        const chatStore = useChatStore()
        await chatStore.fetchSessions()
      } catch (e: unknown) {
        const err = e as { response?: { data?: { message?: string } } }
        ElMessage.error(err.response?.data?.message || '删除好友失败')
        throw e
      }
    }
  }
})
