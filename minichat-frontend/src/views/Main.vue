<template>
  <el-container class="main-page">
    <el-aside :width="activeTab === 'space' ? '64px' : '360px'" class="sidebar">
      <div class="sidebar-inner">
        <div class="rail">
          <div class="rail-avatar" @click="showProfile = true">
            <el-avatar :size="40" :src="userStore.userInfo?.avatar">
              {{ (userStore.userInfo?.nickname || userStore.userInfo?.username || '').slice(0, 1).toUpperCase() }}
            </el-avatar>
          </div>
          <div :class="['rail-item', activeTab === 'message' && 'active']" @click="activeTab = 'message'">
            <el-tooltip content="消息" placement="right"
              ><el-icon><ChatLineSquare /></el-icon
            ></el-tooltip>
          </div>
          <div :class="['rail-item', activeTab === 'contacts' && 'active']" @click="activeTab = 'contacts'">
            <el-tooltip content="联系人" placement="right"
              ><el-icon><User /></el-icon
            ></el-tooltip>
          </div>
          <div :class="['rail-item', activeTab === 'space' && 'active']" @click="activeTab = 'space'">
            <el-tooltip content="空间" placement="right"
              ><el-icon><Compass /></el-icon
            ></el-tooltip>
          </div>
          <div style="flex: 1"></div>
          <div class="rail-item" @click="toggleTheme">
            <el-tooltip :content="isDark ? '切换亮色' : '切换深色'" placement="right">
              <el-icon v-if="isDark"><Sunny /></el-icon>
              <el-icon v-else><Moon /></el-icon>
            </el-tooltip>
          </div>
          <el-dropdown trigger="click" placement="right-start" @command="onMoreCommand">
            <div class="rail-item">
              <el-tooltip content="更多" placement="right">
                <el-icon><MoreFilled /></el-icon>
              </el-tooltip>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人资料
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="list" v-if="activeTab !== 'space'">
          <div class="sidebar-top">
            <div class="search-row">
              <el-input v-model="filter" :placeholder="placeholderByTab" clearable size="large" class="top-search">
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-dropdown trigger="click" @command="onQuickCommand">
                <el-button class="quick-btn">
                  <el-icon><Plus /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="addFriend">
                      <el-icon><User /></el-icon>
                      加好友
                    </el-dropdown-item>
                    <el-dropdown-item command="joinGroup">
                      <el-icon><ChatLineSquare /></el-icon>
                      加群聊
                    </el-dropdown-item>
                    <el-dropdown-item command="createGroup">
                      <el-icon><ChatLineSquare /></el-icon>
                      创建群聊
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <template v-if="activeTab === 'message'">
            <FriendList
              :friends="filteredList"
              :active-id="active?.id"
              :active-type="active?.type"
              @select="onSelect"
            />
          </template>
          <template v-else-if="activeTab === 'contacts'">
            <div class="contact-tabs">
              <div class="tab-item" :class="{ active: contactTab === 'friend' }" @click="contactTab = 'friend'">
                好友
              </div>
              <div class="tab-item" :class="{ active: contactTab === 'group' }" @click="contactTab = 'group'">群聊</div>
            </div>
            <div class="contact-list-wrap">
              <FriendGroupList v-if="contactTab === 'friend'" :show-search="false" @select="onContactSelect" />
              <GroupList v-else :active-id="selectedGroupId" :filter-text="filter" @select="onGroupSelect" />
            </div>
          </template>
          <template v-else>
            <div class="space-placeholder">
              <el-empty description="空间（占位）" />
            </div>
          </template>
        </div>
      </div>
    </el-aside>

    <el-main class="chat">
      <template v-if="activeTab === 'message'">
        <ChatWindow :friend="active" @open-profile="onOpenProfile" />
      </template>
      <template v-else-if="activeTab === 'contacts'">
        <FriendDetail v-if="contactTab === 'friend'" :friend-id="selectedContactId" @message="onSendMessage" />
        <GroupDetail v-else :group-id="selectedGroupId" @message="onSendMessage" />
      </template>
      <template v-else>
        <Space />
      </template>
    </el-main>

    <UserProfile v-model="showProfile" />
    <CreateGroupDialog v-model="showCreateGroup" @created="onGroupCreated" />
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import FriendList from '../components/friend/FriendList.vue'
import FriendGroupList from '../components/friend/FriendGroupList.vue'
import FriendDetail from '../components/friend/FriendDetail.vue'
import GroupList from '../components/group/GroupList.vue'
import GroupDetail from '../components/group/GroupDetail.vue'
import CreateGroupDialog from '../components/group/CreateGroupDialog.vue'
import ChatWindow from '../components/chat/ChatWindow.vue'
import UserProfile from '../components/user/UserProfile.vue'
import Space from './Space.vue'
import { useFriendStore } from '../store/friend'
import { useChatStore } from '../store/chat'
import { useUserStore } from '../store/user'

const friendStore = useFriendStore()
const chatStore = useChatStore()
const userStore = useUserStore()
const router = useRouter()
const filter = ref('')
const activeTab = ref('message')
const showProfile = ref(false)
const showCreateGroup = ref(false)
const selectedContactId = ref(null)
const selectedGroupId = ref(null)
const contactTab = ref('friend') // 'friend' | 'group'
const isDark = ref(false)

const getThemeStorageKey = (userId) => `theme:${String(userId)}`

const applyTheme = (dark) => {
  isDark.value = !!dark
  const html = document.documentElement
  if (isDark.value) {
    html.classList.add('dark')
  } else {
    html.classList.remove('dark')
  }
}

const resolveInitialTheme = (userId) => {
  if (userId) {
    const saved = localStorage.getItem(getThemeStorageKey(userId))
    if (saved === 'dark') return true
    if (saved === 'light') return false
  }
  return false
}

const toggleTheme = () => {
  const userId = userStore.userId
  const next = !isDark.value
  applyTheme(next)
  if (userId) {
    localStorage.setItem(getThemeStorageKey(userId), next ? 'dark' : 'light')
  }
}

const findFriendById = (id) => {
  if (!id) return null
  const list = friendStore.friends || []
  const target = list.find((f) => f.id == id || f.friendId == id || f.userId == id)
  return target || null
}

const buildChatUser = (source) => {
  if (!source) return null
  const base = { ...source }
  const type = base.type ?? 0
  if (type !== 0) return base
  const friend = findFriendById(base.id)
  if (!friend) return base
  return {
    ...base,
    name: base.name || friend.remark || friend.nickname || friend.username,
    nickname: base.nickname || friend.nickname || friend.username,
    username: base.username || friend.username,
    remark: base.remark ?? friend.remark,
    avatar: base.avatar || friend.avatar,
    online: friend.online
  }
}

onMounted(async () => {
  applyTheme(resolveInitialTheme(userStore.userId))

  try {
    await Promise.all([friendStore.fetchFriends(), chatStore.fetchSessions()])
  } catch (e) {
    await chatStore.fetchSessions()
  }
  if (chatStore.sessions.length) {
    const firstSession = chatStore.sessions[0]
    const user = buildChatUser({
      id: firstSession.id,
      name: firstSession.name,
      nickname: firstSession.name,
      username: firstSession.name,
      avatar: firstSession.avatar,
      type: firstSession.type
    })
    chatStore.setActiveUser(user)
  }
})

watch(
  () => userStore.userId,
  (newUserId, oldUserId) => {
    if (newUserId === oldUserId) return
    if (!newUserId) {
      applyTheme(false)
      return
    }
    applyTheme(resolveInitialTheme(newUserId))
  }
)

const onContactSelect = (item) => {
  selectedContactId.value = item.id
}

const onGroupSelect = (item) => {
  selectedGroupId.value = item.id
}

const onSendMessage = (user) => {
  activeTab.value = 'message'
  const chatUser = buildChatUser(user)
  chatStore.setActiveUser(chatUser)
}

const listByTab = computed(() => {
  if (activeTab.value === 'contacts') return [] // Managed by FriendGroupList
  if (activeTab.value === 'message') {
    // 使用会话列表，并根据最后消息时间排序
    return chatStore.sessions.map((session) => {
      // 方案C：如果最新一条消息的Seq <= 用户已读Seq，说明最新显示的消息已读，手动隐藏红点
      // 解决“幽灵未读数”问题（即后面有已撤回的消息导致 unreadCount > 0，但显示的是旧消息）
      let displayUnread = session.unreadCount || 0
      if (session.type === 1 && session.lastMessageSeq && session.lastReadSeq) {
        if (session.lastMessageSeq <= session.lastReadSeq) {
          displayUnread = 0
        }
      }

      const base = {
        id: session.id,
        name: session.name,
        nickname: session.name,
        username: session.name,
        avatar: session.avatar,
        type: session.type, // 0:私聊, 1:群聊
        lastMessageTime: session.lastMessageTime || 0,
        unreadCount: displayUnread,
        _last: session.lastMessageTime || 0
      }
      const withStatus = buildChatUser(base)
      return {
        ...withStatus,
        lastMessageTime: session.lastMessageTime || 0,
        unreadCount: displayUnread,
        _last: session.lastMessageTime || 0
      }
    })
  }
  return []
})

const filteredList = computed(() => {
  if (!filter.value) return listByTab.value
  const k = filter.value.toLowerCase()
  return listByTab.value.filter(
    (f) =>
      (f.name || '').toLowerCase().includes(k) ||
      (f.nickname || '').toLowerCase().includes(k) ||
      (f.username || '').toLowerCase().includes(k) ||
      (f.remark || '').toLowerCase().includes(k)
  )
})

const placeholderByTab = computed(() => {
  if (activeTab.value === 'message') return '搜索会话'
  if (activeTab.value === 'contacts') return contactTab.value === 'friend' ? '搜索好友' : '搜索群聊'
  return '空间'
})

const active = computed(() => buildChatUser(chatStore.activeUser))
const onSelect = (f) => {
  const user = buildChatUser(f)
  chatStore.setActiveUser(user)
}

const onMoreCommand = (cmd) => {
  if (cmd === 'logout') userStore.logout()
  if (cmd === 'profile') showProfile.value = true
}

const onQuickCommand = (cmd) => {
  if (cmd === 'addFriend') router.push('/friend')
  if (cmd === 'joinGroup') router.push('/group-apply')
  if (cmd === 'createGroup') showCreateGroup.value = true
}

const onGroupCreated = async (newGroup) => {
  await chatStore.fetchSessions()
  activeTab.value = 'message'
  if (newGroup) {
    const user = {
      id: newGroup.id,
      name: newGroup.groupName,
      nickname: newGroup.groupName,
      avatar: newGroup.avatar,
      type: 1 // Group
    }
    chatStore.setActiveUser(user)
  }
}

const onOpenProfile = (info) => {
  // 1. 切换到联系人 Tab
  activeTab.value = 'contacts'

  // 2. 切换到对应的子 Tab (friend/group)
  contactTab.value = info.type

  // 3. 选中对应的联系人或群组
  if (info.type === 'friend') {
    selectedContactId.value = info.id
  } else {
    selectedGroupId.value = info.id
  }
}

watch(active, () => {}, { immediate: true })

// 当切换到消息标签页时，刷新会话列表
watch(activeTab, async (newTab) => {
  if (newTab === 'message') {
    await chatStore.fetchSessions()
  }
})
</script>

<style scoped>
.main-page {
  height: calc(100vh - 20px);
  background: var(--el-bg-color-page);
}
.sidebar {
  background: var(--el-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
}
.sidebar-inner {
  display: flex;
  width: 100%;
  height: 100%;
}
.rail {
  width: 64px;
  border-right: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 8px;
  gap: 8px;
}
.rail-avatar {
  margin-bottom: 4px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.rail-avatar:hover {
  opacity: 0.8;
}
.rail-item {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}
.rail-item.active,
.rail-item:hover {
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
}
.list {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.sidebar-top {
  padding: 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}
.search-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.top-search {
  flex: 1;
}
.top-search :deep(.el-input__wrapper) {
  height: 40px;
  border-radius: 10px;
}
.top-search :deep(.el-input__inner) {
  font-size: 14px;
}
.quick-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
}
.space-placeholder {
  padding: 12px;
}
.chat {
  background: var(--el-bg-color);
  height: 100%;
  overflow: hidden;
  padding: 0;
}
.contact-tabs {
  display: flex;
  background: var(--el-bg-color-page);
  margin: 10px 10px 0;
  padding: 4px;
  border-radius: 4px;
}
.tab-item {
  flex: 1;
  text-align: center;
  padding: 6px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}
.tab-item.active {
  background: var(--el-bg-color);
  color: var(--el-color-primary);
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.contact-list-wrap {
  flex: 1;
  overflow: hidden;
  position: relative;
}
</style>
