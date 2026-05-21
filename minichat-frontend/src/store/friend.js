import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  searchUsers,
  sendFriendRequest,
  getIncomingRequests,
  getSentRequests,
  handleFriendRequest,
  getFriendList,
  setFriendRemark,
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

export const useFriendStore = defineStore('friend', {
  state: () => ({
    friends: [],
    incomingRequests: [],
    sentRequests: [],
    searchResults: [],
    groups: [], // 好友分组列表
    currentFriendDetail: null, // 当前选中的好友详情
    loading: false,
    requestLoading: false
  }),

  actions: {
    async fetchFriends() {
      this.loading = true
      try {
        const res = await getFriendList()
        const list = Array.isArray(res) ? res : []
        const normalize = (raw) => {
          const friendId = raw.friendId ?? raw.friend_id ?? raw.userId ?? raw.uid
          return {
            ...raw,
            relationId: raw.id,
            friendId: friendId ?? raw.id,
            id: friendId ?? raw.id,
            remark: raw.remark || raw.remarkName,
            online: false
          }
        }
        this.friends = Array.isArray(list) ? list.map(normalize) : []

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
          } catch (err) {
            logger.error('Failed to fetch online status', err)
          }
        }
      } catch (e) {
        ElMessage.error('获取好友列表失败')
      } finally {
        this.loading = false
      }
    },

    async fetchIncomingRequests() {
      this.requestLoading = true
      try {
        const res = await getIncomingRequests()
        this.incomingRequests = res
      } catch (e) {
        ElMessage.error('获取好友申请失败')
      } finally {
        this.requestLoading = false
      }
    },

    async fetchSentRequests() {
      try {
        const res = await getSentRequests()
        this.sentRequests = res
      } catch (e) {
        // 非关键，忽略错误
      }
    },

    // 更新好友在线状态
    updateFriendStatus(userId, isOnline) {
      // 使用 loose equality (==) 兼容 string/number 类型的 ID
      const friendIndex = this.friends.findIndex((f) => f.id == userId || f.friendId == userId)
      if (friendIndex !== -1) {
        // 创建新对象以触发响应式更新
        const friend = this.friends[friendIndex]
        // 只有当状态真正改变时才更新，避免不必要的响应式触发
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

    async doSearch(keyword) {
      if (!keyword) {
        this.searchResults = []
        return
      }
      this.loading = true
      try {
        const res = await searchUsers(keyword)
        this.searchResults = Array.isArray(res) ? res : []
      } catch (e) {
        ElMessage.error('搜索用户失败')
      } finally {
        this.loading = false
      }
    },

    async applyFriend(toUserId, message = '') {
      try {
        const res = await sendFriendRequest({ toUserId, message })
        ElMessage.success('好友申请已发送')
        // await this.fetchOutgoingRequests()
        return res
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '发送好友申请失败')
        throw e
      }
    },

    async processRequest(requestId, actionOrStatus) {
      const status = typeof actionOrStatus === 'string' ? (actionOrStatus === 'accept' ? 1 : 2) : actionOrStatus
      try {
        await handleFriendRequest({ requestId, status })
        ElMessage.success(status === 1 ? '已同意申请' : '已拒绝申请')
        await Promise.all([this.fetchIncomingRequests(), this.fetchFriends()])
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '处理好友申请失败')
        throw e
      }
    },

    async fetchFriendGroups() {
      try {
        const res = await getFriendGroupList()
        const rawGroups = Array.isArray(res) ? res : []
        this.groups = rawGroups.map((g) => ({
          ...g,
          items: [],
          expanded: false,
          loaded: false
        }))
      } catch (e) {
        logger.error('获取分组列表失败', e)
      }
    },

    async fetchGroupItems(groupName) {
      try {
        const res = await getFriendGroupItemList(groupName)
        const items = Array.isArray(res) ? res : []
        const group = this.groups.find((g) => g.groupName === groupName)
        if (group) {
          group.items = items.map((item) => ({
            ...item,
            // Mapping from FriendGroupItemVO
            id: item.friendId,
            avatar: item.friendAvatar,
            remark: item.remarkName,
            nickname: item.friendNickname,
            online: item.onlineStatus === true || item.onlineStatus === 1,
            // No signature in VO, but we can keep nickname as fallback for display if needed
            signature: null
          }))
          group.loaded = true
        }
      } catch (e) {
        logger.error(`获取分组 ${groupName} 好友失败`, e)
      }
    },

    async changeFriendGroup(friendId, groupName) {
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
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '修改好友分组失败')
        throw e
      }
    },

    async fetchFriendDetail(friendId) {
      try {
        const res = await getFriendDetail(friendId)
        const data = res
        if (data) {
          this.currentFriendDetail = {
            ...data,
            // Mapping from FriendDetailVO
            id: data.friendUserId,
            avatar: data.friendAvatar,
            remark: data.remarkName,
            nickname: data.friendNickname,
            groupName: data.groupName,
            gender: data.gender,
            signature: data.signature,
            online: false // Detail VO doesn't have online status, defaults to false or need separate check
          }
          // Try to find online status from existing list if possible
          const friendInList = this.friends.find((f) => f.id === data.friendUserId)
          if (friendInList) {
            this.currentFriendDetail.online = friendInList.online
          }
        }
      } catch (e) {
        logger.error('获取好友详情失败', e)
      }
    },

    async updateRemark(friendId, remark) {
      try {
        await updateFriendRemark({ friendId, remark })
        ElMessage.success('备注已更新')
        // Update local state directly
        const friend = this.friends.find((f) => f.id === friendId)
        if (friend) {
          friend.remark = remark
        }
        // Sync with groups
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
        // Sync with current detail
        if (
          this.currentFriendDetail &&
          (this.currentFriendDetail.id === friendId || this.currentFriendDetail.userId === friendId)
        ) {
          this.currentFriendDetail.remark = remark
        }
      } catch (e) {
        ElMessage.error('更新备注失败')
        throw e
      }
    },

    async deleteFriend(friendId) {
      try {
        await deleteFriend(friendId)
        ElMessage.success('好友删除成功')
        // Refresh friend list
        await this.fetchFriends()
        // Clear current friend detail if it was the deleted friend
        if (
          this.currentFriendDetail &&
          (this.currentFriendDetail.id === friendId || this.currentFriendDetail.userId === friendId)
        ) {
          this.currentFriendDetail = null
        }
        // Refresh groups
        await this.fetchFriendGroups()
        // Refresh chat sessions
        const chatStore = useChatStore()
        await chatStore.fetchSessions()
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '删除好友失败')
        throw e
      }
    }
  }
})
