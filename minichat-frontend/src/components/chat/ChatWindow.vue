<template>
  <el-container class="chat-window">
    <el-header class="chat-header">
      <div class="left">
        <el-avatar :size="36" :src="friend?.avatar">{{ friend?.avatar ? '' : initial }}</el-avatar>
        <div class="meta">
          <div class="name">
            {{ friend?.name || friend?.remark || friend?.nickname || friend?.username || '未选择' }}
          </div>
          <!-- 只有私聊（type === 0 或 undefined）才显示在线状态 -->
          <div v-if="friend?.type === 0 || friend?.type === undefined" class="status">
            <span class="dot" :class="{ on: friend?.online }"></span>{{ friend?.online ? '在线' : '离线' }}
          </div>
          <div v-else-if="friend?.type === 1" class="status">
            <span>群聊</span>
          </div>
        </div>
      </div>
      <div class="right">
        <el-dropdown trigger="click" @command="handleCommand">
          <el-icon class="more-btn"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="friend?.type === 1" command="members"> 群组成员 </el-dropdown-item>
              <el-dropdown-item command="info">
                {{ friend?.type === 1 ? '群组信息' : '好友信息' }}
              </el-dropdown-item>
              <el-dropdown-item command="search"> 查找聊天记录 </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-main class="chat-body">
      <div class="messages" ref="listRef" @scroll="onScroll">
        <div v-if="loadingMore" class="loading-more">
          <el-icon class="is-loading"><RefreshLeft /></el-icon> 加载中...
        </div>
        <div
          v-for="m in messages"
          :key="m.timestamp + '_' + (m.id || '')"
          :id="'msg-' + m.id"
          :class="['msg', m.type === 4 || m.type === 5 ? 'system-row' : m.fromId === 'self' ? 'self' : 'other']"
        >
          <!-- 撤回/系统消息 -->
          <template v-if="m.type === 4 || m.type === 5">
            <div class="system-message-content">
              <span>{{ m.content }}</span>
            </div>
          </template>

          <!-- 对方消息：头像在左侧 -->
          <template v-else-if="m.fromId !== 'self'">
            <el-avatar :size="36" :src="m.senderAvatar || friend?.avatar" class="msg-avatar">
              {{
                (m.senderNickname || friend?.name || friend?.remark || friend?.nickname || friend?.username || '')
                  .slice(0, 1)
                  .toUpperCase()
              }}
            </el-avatar>
            <div class="msg-content">
              <!-- 群聊消息显示发送者昵称 -->
              <div v-if="friend?.type === 1 && m.senderNickname" class="sender-name">
                <span>{{ m.senderNickname }}</span>
                <el-tag v-if="memberRoleMap[m.fromId] === 2" size="small" type="warning" effect="dark" class="role-tag"
                  >群主</el-tag
                >
                <el-tag
                  v-else-if="memberRoleMap[m.fromId] === 1"
                  size="small"
                  type="success"
                  effect="dark"
                  class="role-tag"
                  >管理员</el-tag
                >
              </div>
              <div class="bubble">
                <template v-if="m.type === 1">{{ m.content }}</template>
                <div v-else-if="m.type === 2" class="image-container">
                  <el-image
                    v-if="m.fileUrl"
                    :src="m.fileUrl"
                    class="msg-image"
                    :preview-src-list="[m.fileUrl]"
                    :preview-teleported="true"
                    hide-on-click-modal
                  >
                    <template #error>
                      <div class="image-placeholder" style="display: block">图片加载失败</div>
                    </template>
                  </el-image>
                  <div v-else class="image-placeholder" style="display: block">图片加载失败</div>
                </div>
                <div v-else-if="m.type === 3" class="file-message">
                  <div class="file-info">
                    <el-icon class="file-icon"><Paperclip /></el-icon>
                    <a v-if="m.fileUrl" :href="m.fileUrl" target="_blank" download class="file-name">{{
                      m.fileName || '文件'
                    }}</a>
                    <span v-else class="file-name">{{ m.fileName || '文件' }}</span>
                  </div>
                  <div class="file-meta">
                    <span class="size">{{ formatSize(m.fileSize) }}</span>
                  </div>
                </div>
                <div v-else class="system">{{ m.content }}</div>
              </div>
              <div class="time">{{ formatTime(m.timestamp) }}</div>
            </div>
          </template>

          <!-- 自己消息：头像在右侧 -->
          <template v-else>
            <div class="msg-content">
              <div class="bubble-row">
                <el-icon v-if="m.sendError" class="error-icon"><WarningFilled /></el-icon>
                <div class="bubble" @contextmenu.prevent="handleRightClick($event, m)">
                  <template v-if="m.type === 1">{{ m.content }}</template>
                  <div v-else-if="m.type === 2" class="image-container">
                    <el-image
                      v-if="m.fileUrl"
                      :src="m.fileUrl"
                      class="msg-image"
                      :preview-src-list="[m.fileUrl]"
                      :preview-teleported="true"
                      hide-on-click-modal
                    >
                      <template #error>
                        <div class="image-placeholder" style="display: block">图片加载失败</div>
                      </template>
                    </el-image>
                    <div v-else class="image-placeholder" style="display: block">图片加载失败</div>
                  </div>
                  <div v-else-if="m.type === 3" class="file-message">
                    <div class="file-info">
                      <el-icon class="file-icon"><Paperclip /></el-icon>
                      <a v-if="m.fileUrl" :href="m.fileUrl" target="_blank" download class="file-name">{{
                        m.fileName || '文件'
                      }}</a>
                      <span v-else class="file-name">{{ m.fileName || '文件' }}</span>
                    </div>
                    <div class="file-meta">
                      <span class="size">{{ formatSize(m.fileSize) }}</span>
                      <span class="sent-status">已发送</span>
                    </div>
                  </div>
                  <div v-else class="system">{{ m.content }}</div>
                </div>
              </div>
              <div v-if="m.sendError" class="error-text">
                <span v-if="m.errorMessage && m.errorMessage.includes('添加对方为好友')">
                  发送失败，请先<span class="link-text" @click="reAddFriend">添加对方为好友</span>
                </span>
                <span v-else>{{ m.errorMessage || '发送失败' }}</span>
              </div>
              <div class="time">{{ formatTime(m.timestamp) }}</div>
            </div>
            <el-avatar :size="36" :src="currentUserAvatar" class="msg-avatar">
              {{ currentUserInitial }}
            </el-avatar>
          </template>
        </div>
        <el-empty v-if="!messages.length" description="暂无消息" />
      </div>
    </el-main>
    <div v-if="uploading" class="upload-progress">
      <el-progress :percentage="uploadProgress" :stroke-width="3" />
    </div>
    <el-footer class="chat-input">
      <div class="input-wrap">
        <div class="inner-tools">
          <span class="tool-icon emoji-icon" @click="toggleEmojiPicker">😊</span>
          <el-icon size="20" class="tool-icon" @click="triggerImageSelect"><Picture /></el-icon>
          <el-icon size="20" class="tool-icon" @click="triggerFileSelect"><Paperclip /></el-icon>
          <el-icon size="20" class="tool-icon"><Microphone /></el-icon>
          <el-icon size="20" class="tool-icon"><VideoCamera /></el-icon>
        </div>
        <input type="file" ref="imageInput" style="display: none" accept="image/*" @change="handleImageUpload" />
        <input type="file" ref="fileInput" style="display: none" @change="handleFileUpload" />
        <el-input v-model="text" type="textarea" :rows="3" ref="textInput" @keydown.enter.exact.prevent="send" />
        <el-button class="send-btn" type="primary" @click="send" :disabled="!friend || !text.trim()">发送</el-button>
        <!-- Emoji选择器 -->
        <div v-if="showEmojiPicker" class="emoji-picker" @click.stop>
          <div class="emoji-header">
            <span class="emoji-title">表情</span>
            <el-icon class="close-icon" @click="closeEmojiPicker"><Close /></el-icon>
          </div>
          <div class="emoji-grid">
            <span v-for="emoji in emojiList" :key="emoji" class="emoji-item" @click="insertEmoji(emoji)">{{
              emoji
            }}</span>
          </div>
        </div>
      </div>
    </el-footer>

    <!-- 群成员列表弹窗 -->
    <el-dialog v-model="showMembersDialog" title="群成员列表" width="450px" append-to-body>
      <div v-loading="loadingMembers" class="member-list-container">
        <div class="member-list-header">
          <div class="header-left">
            <!-- 占位，保持右对齐 -->
          </div>
          <div class="header-right" style="display: flex; gap: 10px">
            <el-button type="danger" size="small" @click="handleExitGroup">
              <el-icon><SwitchButton /></el-icon>退出群聊
            </el-button>
            <el-button v-if="canRemove" type="danger" size="small" @click="openRemoveDialog">
              <el-icon><Delete /></el-icon>移除成员
            </el-button>
            <el-button v-if="canInvite" type="primary" size="small" @click="openInviteDialog">
              <el-icon><Plus /></el-icon>邀请好友
            </el-button>
          </div>
        </div>
        <el-scrollbar height="400px">
          <div v-for="member in memberList" :key="member.userId" class="member-item">
            <el-avatar :src="member.avatar" :size="40">{{
              (member.nickname || member.nicknameInGroup || '').slice(0, 1).toUpperCase()
            }}</el-avatar>
            <div class="member-info">
              <div class="member-name">
                <span class="name-text">{{ member.nicknameInGroup || member.nickname }}</span>
                <el-tag v-if="member.role === 2" size="small" type="warning" effect="dark">群主</el-tag>
                <el-tag v-else-if="member.role === 1" size="small" type="success" effect="dark">管理员</el-tag>
              </div>
              <div class="real-name" v-if="member.nicknameInGroup && member.nicknameInGroup !== member.nickname">
                原名: {{ member.nickname }}
              </div>
            </div>
            <!-- 群主操作菜单 -->
            <div v-if="currentUserRole === 2 && member.userId !== userStore.userInfo?.id" class="member-actions">
              <el-dropdown trigger="click" @command="(cmd) => handleMemberAction(cmd, member)">
                <el-icon class="action-icon"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="member.role === 0" command="setAdmin">设为管理员</el-dropdown-item>
                    <el-dropdown-item v-if="member.role === 1" command="cancelAdmin">取消管理员</el-dropdown-item>
                    <el-dropdown-item command="transferOwner">转让群主</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <el-empty v-if="!memberList.length && !loadingMembers" description="暂无成员" />
        </el-scrollbar>
      </div>
    </el-dialog>

    <!-- 邀请好友弹窗 -->
    <el-dialog v-model="showInviteDialog" title="邀请好友入群" width="500px" append-to-body>
      <div class="invite-container">
        <el-input v-model="searchKeyword" placeholder="搜索好友" :prefix-icon="Search" clearable class="search-input" />
        <div class="friend-list">
          <el-checkbox-group v-model="inviteList">
            <div v-for="friend in filteredFriends" :key="friend.id" class="friend-item">
              <el-checkbox :label="friend.id">
                <div class="friend-info">
                  <el-avatar :size="32" :src="friend.avatar">{{
                    (friend.remark || friend.nickname || friend.username || '').slice(0, 1).toUpperCase()
                  }}</el-avatar>
                  <span class="name">{{ friend.remark || friend.nickname || friend.username }}</span>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="!filteredFriends.length" description="暂无可选好友" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showInviteDialog = false">取消</el-button>
          <el-button type="primary" @click="handleInvite" :loading="inviteLoading" :disabled="!inviteList.length">
            确定邀请 ({{ inviteList.length }})
          </el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 移除成员弹窗 -->
    <el-dialog v-model="showRemoveDialog" title="移除群成员" width="500px" append-to-body>
      <div class="invite-container">
        <el-input
          v-model="removeSearchKeyword"
          placeholder="搜索成员"
          :prefix-icon="Search"
          clearable
          class="search-input"
        />
        <div class="friend-list">
          <el-radio-group v-model="removeList" style="width: 100%; display: block">
            <div v-for="member in filteredMembersToRemove" :key="member.userId" class="friend-item">
              <el-radio :label="member.userId" style="width: 100%; display: flex; align-items: center">
                <div class="friend-info">
                  <el-avatar :size="32" :src="member.avatar">{{
                    (member.nicknameInGroup || member.nickname || '').slice(0, 1).toUpperCase()
                  }}</el-avatar>
                  <div class="member-name">
                    <span class="name">{{ member.nicknameInGroup || member.nickname }}</span>
                    <el-tag v-if="member.role === 1" size="small" type="success" effect="dark" style="margin-left: 5px"
                      >管理员</el-tag
                    >
                  </div>
                </div>
              </el-radio>
            </div>
          </el-radio-group>
          <el-empty v-if="!filteredMembersToRemove.length" description="暂无可移除成员" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showRemoveDialog = false">取消</el-button>
          <el-button type="danger" @click="handleRemoveMember" :loading="removeLoading" :disabled="!removeList">
            确定移除
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 右键菜单 -->
    <div
      v-if="showContextMenu"
      class="context-menu"
      :style="{ top: contextMenuTop + 'px', left: contextMenuLeft + 'px' }"
      @click.stop
    >
      <div class="menu-item" @click="handleRecall">
        <el-icon><RefreshLeft /></el-icon> 撤回
      </div>
    </div>

    <!-- 搜索聊天记录抽屉 -->
    <el-drawer v-model="showSearchDrawer" title="查找聊天记录" size="400px" append-to-body>
      <div class="search-container">
        <el-input v-model="chatSearchKeyword" placeholder="搜索关键词" @keyup.enter="handleChatSearch" clearable>
          <template #append>
            <el-button :icon="Search" @click="handleChatSearch" />
          </template>
        </el-input>
        <div class="search-results" v-loading="searching">
          <div v-for="item in searchResults" :key="item.id" class="result-item" @click="jumpToMessage(item)">
            <div class="result-header">
              <span class="name">{{
                item.senderId === userStore.userInfo?.id
                  ? '我'
                  : item.senderNickName || item.senderNickname || 'Unknown'
              }}</span>
              <span class="time">{{ formatTime(item.sendTime) }}</span>
            </div>
            <div class="result-content" v-html="item.content"></div>
            <div class="result-content" v-if="item.fileName" v-html="'[文件] ' + item.fileName"></div>
          </div>
          <el-empty v-if="!searchResults.length && !searching" description="无搜索结果" />
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch, onUnmounted, nextTick } from 'vue'
import { useChatStore } from '../../store/chat'
import { useUserStore } from '../../store/user'
import { useFriendStore } from '../../store/friend'
import { searchChatMessages } from '../../api/chat'
import { uploadFileUnified } from '../../api/upload'
import {
  getGroupProfile,
  getGroupMemberList,
  inviteToGroup,
  removeGroupMember,
  exitGroup,
  updateGroupMemberRole,
  transferGroupOwner
} from '../../api/group'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Close,
  WarningFilled,
  MoreFilled,
  Search,
  Plus,
  Delete,
  SwitchButton,
  UserFilled,
  Avatar,
  RefreshLeft
} from '@element-plus/icons-vue'
import logger from '../../utils/logger'

const props = defineProps({ friend: { type: Object, default: null } })
const store = useChatStore()
const userStore = useUserStore()
const friendStore = useFriendStore()
const emit = defineEmits(['open-profile'])
const messages = computed(() => store.activeMessages)
const text = ref('')
const initial = computed(() =>
  (props.friend?.remark || props.friend?.nickname || props.friend?.username || '').slice(0, 1).toUpperCase()
)
const friendInitial = computed(() =>
  (props.friend?.remark || props.friend?.nickname || props.friend?.username || '').slice(0, 1).toUpperCase()
)
const currentUserInitial = computed(() =>
  (userStore.userInfo?.nickname || userStore.userInfo?.username || '').slice(0, 1).toUpperCase()
)
const currentUserAvatar = computed(() => userStore.userInfo?.avatar || '')
const listRef = ref(null)
const imageInput = ref(null)
const fileInput = ref(null)
const textInput = ref(null)
const showEmojiPicker = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const showMembersDialog = ref(false)
const loadingMembers = ref(false)
const memberList = ref([])
const showInviteDialog = ref(false)
const inviteList = ref([])
const searchKeyword = ref('')
const inviteLoading = ref(false)
const currentGroup = ref(null)

// 聊天记录搜索
const showSearchDrawer = ref(false)
const chatSearchKeyword = ref('')
const searchResults = ref([])
const searching = ref(false)

// Context Menu State
const showContextMenu = ref(false)
const contextMenuTop = ref(0)
const contextMenuLeft = ref(0)
const currentMessage = ref(null)

const handleRightClick = (e, message) => {
  if (message.fromId !== 'self') return
  if (message.type === 4 || message.type === 5) return // Already recalled or system

  e.preventDefault()
  showContextMenu.value = true

  // Get the bubble element rect to position menu below the message
  // This ensures the menu doesn't cover the message content
  const rect = e.currentTarget.getBoundingClientRect()
  contextMenuLeft.value = e.clientX
  contextMenuTop.value = rect.bottom + 10

  currentMessage.value = message

  const close = () => {
    showContextMenu.value = false
    document.removeEventListener('click', close)
  }
  document.addEventListener('click', close)
}

const handleRecall = async () => {
  if (!currentMessage.value) return
  const msgId = currentMessage.value.id

  // 拦截临时消息
  if (typeof msgId === 'string' && msgId.startsWith('temp_')) {
    ElMessage.warning('消息发送中，请稍后重试')
    showContextMenu.value = false
    return
  }

  if (!msgId) {
    ElMessage.warning('无法撤回此消息')
    return
  }

  await store.recallMessage(msgId)
  showContextMenu.value = false
}

// 移除群成员相关状态
const showRemoveDialog = ref(false)
const removeList = ref(null) // 单选，存储选中的 memberId (userId)
const removeLoading = ref(false)
const removeSearchKeyword = ref('')

const currentUserRole = computed(() => {
  if (!userStore.userInfo?.id) return 0
  const me = memberList.value.find((m) => m.userId === userStore.userInfo.id)
  return me ? me.role : 0
})

const memberRoleMap = computed(() => {
  const map = {}
  memberList.value.forEach((m) => {
    map[m.userId] = m.role
  })
  return map
})

const canInvite = computed(() => {
  if (!currentGroup.value) return false
  const policy = currentGroup.value.invitePolicy || 0
  return currentUserRole.value >= policy
})

// 是否有权限移除成员（管理员或群主）
const canRemove = computed(() => {
  return currentUserRole.value > 0 // 1: Admin, 2: Owner
})

// Filter friends who are not in the group
const filteredFriends = computed(() => {
  const friends = friendStore.friends || []
  const userFriends = friends.filter((f) => f.type === 0 || f.type === undefined)

  // Get current member IDs
  const memberIds = new Set(memberList.value.map((m) => m.userId))

  // Filter out existing members
  const potential = userFriends.filter((f) => !memberIds.has(f.id))

  if (!searchKeyword.value) return potential

  const k = searchKeyword.value.toLowerCase()
  return potential.filter(
    (f) =>
      (f.remark || '').toLowerCase().includes(k) ||
      (f.nickname || '').toLowerCase().includes(k) ||
      (f.name || '').toLowerCase().includes(k)
  )
})

// 筛选可移除的成员（排除自己和权限更高或相等的成员）
const filteredMembersToRemove = computed(() => {
  if (!memberList.value.length) return []

  const currentUserId = userStore.userInfo?.id
  const myRole = currentUserRole.value

  const candidates = memberList.value.filter((m) => {
    // 排除自己
    if (m.userId === currentUserId) return false
    // 排除权限 >= 自己的成员 (群主不能踢群主，管理员不能踢管理员/群主)
    // 后端逻辑是 targetRole >= role 则不能删除，这里保持一致
    if (m.role >= myRole) return false
    return true
  })

  if (!removeSearchKeyword.value) return candidates

  const k = removeSearchKeyword.value.toLowerCase()
  return candidates.filter(
    (m) => (m.nicknameInGroup || '').toLowerCase().includes(k) || (m.nickname || '').toLowerCase().includes(k)
  )
})

const fetchMembers = async () => {
  if (!props.friend?.id) return
  loadingMembers.value = true
  try {
    // 并行获取群详情和成员列表
    const [profileRes, membersRes] = await Promise.all([
      getGroupProfile(props.friend.id),
      getGroupMemberList(props.friend.id)
    ])

    if (profileRes.code === 1) {
      currentGroup.value = profileRes.data
    }

    if (membersRes.code === 1) {
      memberList.value = membersRes.data
    } else {
      ElMessage.error(membersRes.msg || '获取成员列表失败')
    }
  } catch (e) {
    logger.error(e)
    ElMessage.error('获取信息失败')
  } finally {
    loadingMembers.value = false
  }
}

const openInviteDialog = async () => {
  if (!friendStore.friends.length) {
    await friendStore.fetchFriends()
  }
  inviteList.value = []
  searchKeyword.value = ''
  showInviteDialog.value = true
}

const openRemoveDialog = () => {
  removeList.value = null
  removeSearchKeyword.value = ''
  showRemoveDialog.value = true
}

const handleInvite = async () => {
  if (!inviteList.value.length) return

  inviteLoading.value = true
  try {
    const res = await inviteToGroup({
      groupId: props.friend.id,
      userIds: inviteList.value
    })

    if (res.code === 1) {
      ElMessage.success('邀请成功')
      showInviteDialog.value = false
      // 刷新成员列表
      fetchMembers()
    } else {
      ElMessage.error(res.msg || '邀请失败')
    }
  } catch (e) {
    ElMessage.error('邀请出错')
  } finally {
    inviteLoading.value = false
  }
}

const handleRemoveMember = async () => {
  if (!removeList.value) return

  // 查找要删除的成员信息用于确认提示
  const memberToRemove = memberList.value.find((m) => m.userId === removeList.value)
  const name = memberToRemove ? memberToRemove.nicknameInGroup || memberToRemove.nickname : '该成员'

  try {
    await ElMessageBox.confirm(`确定要将 "${name}" 移出群组吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return // 用户取消
  }

  removeLoading.value = true
  try {
    const res = await removeGroupMember({
      groupId: props.friend.id,
      userId: removeList.value
    })

    if (res.code === 1) {
      ElMessage.success('移除成员成功')
      showRemoveDialog.value = false
      fetchMembers()
    } else {
      ElMessage.error(res.msg || '移除失败')
    }
  } catch (e) {
    ElMessage.error('移除出错')
  } finally {
    removeLoading.value = false
  }
}

const handleExitGroup = async () => {
  if (!props.friend?.id) return

  try {
    await ElMessageBox.confirm('确定要退出该群聊吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return // 用户取消
  }

  try {
    const res = await exitGroup(props.friend.id)
    if (res.code === 1) {
      ElMessage.success('退出群组成功')
      showMembersDialog.value = false
      // 触发外部更新列表或清空当前会话
      // 这里简单刷新页面或清空当前聊天
      window.location.reload()
    } else {
      ElMessage.error(res.msg || '退出群组失败')
    }
  } catch (e) {
    ElMessage.error('退出群组出错')
  }
}

const handleMemberAction = async (command, member) => {
  if (!props.friend?.id) return

  if (command === 'setAdmin') {
    try {
      await ElMessageBox.confirm(`确定要将 "${member.nicknameInGroup || member.nickname}" 设为管理员吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      })

      const res = await updateGroupMemberRole({
        groupId: props.friend.id,
        userId: member.userId,
        role: 1 // 管理员
      })

      if (res.code === 1) {
        ElMessage.success('设置管理员成功')
        fetchMembers()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } catch (e) {
      if (e !== 'cancel') ElMessage.error('操作出错')
    }
  } else if (command === 'cancelAdmin') {
    try {
      await ElMessageBox.confirm(`确定要取消 "${member.nicknameInGroup || member.nickname}" 的管理员身份吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })

      const res = await updateGroupMemberRole({
        groupId: props.friend.id,
        userId: member.userId,
        role: 0 // 普通成员
      })

      if (res.code === 1) {
        ElMessage.success('取消管理员成功')
        fetchMembers()
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } catch (e) {
      if (e !== 'cancel') ElMessage.error('操作出错')
    }
  } else if (command === 'transferOwner') {
    try {
      await ElMessageBox.confirm(
        `确定要将群主转让给 "${member.nicknameInGroup || member.nickname}" 吗？转让后您将成为普通成员。`,
        '警告',
        {
          confirmButtonText: '确定转让',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      const res = await transferGroupOwner({
        groupId: props.friend.id,
        newOwnerId: member.userId
      })

      if (res.code === 1) {
        ElMessage.success('转让群主成功')
        fetchMembers()
        // 可能需要刷新群组信息或页面状态，因为当前用户权限已变
      } else {
        ElMessage.error(res.msg || '操作失败')
      }
    } catch (e) {
      if (e !== 'cancel') ElMessage.error('操作出错')
    }
  }
}

const handleCommand = async (command) => {
  if (command === 'info') {
    if (!props.friend) return
    const type = props.friend.type || 0 // 0: friend, 1: group

    // 通知父组件切换到联系人 Tab 并选中对应项
    emit('open-profile', {
      id: props.friend.id,
      type: type === 1 ? 'group' : 'friend'
    })
  } else if (command === 'members') {
    showMembersDialog.value = true
    fetchMembers()
  } else if (command === 'search') {
    showSearchDrawer.value = true
    chatSearchKeyword.value = ''
    searchResults.value = []
  }
}

const handleChatSearch = async () => {
  if (!chatSearchKeyword.value.trim()) return
  if (!props.friend) return

  searching.value = true
  try {
    const type = props.friend.type || 0
    const targetId = props.friend.id
    const res = await searchChatMessages(chatSearchKeyword.value, type, targetId)
    if (res.code === 1) {
      searchResults.value = res.data
    } else {
      ElMessage.error(res.msg || '搜索失败')
    }
  } catch (e) {
    ElMessage.error('搜索出错')
  } finally {
    searching.value = false
  }
}

const jumpToMessage = (item) => {
  const dbId = item.dbId
  // 查找消息是否存在于列表中
  // 注意：messages中的id对应后端BaseMessageVO的messageId，即dbId
  const found = messages.value.find((m) => m.id === dbId)

  if (found) {
    showSearchDrawer.value = false // 关闭搜索抽屉

    nextTick(() => {
      const el = document.getElementById('msg-' + dbId)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
        el.classList.add('highlight-flash')
        setTimeout(() => el.classList.remove('highlight-flash'), 2000)
      }
    })
  } else {
    ElMessage.warning('该消息不在当前加载的历史记录中')
  }
}

// 常用emoji表情列表（精选常用表情）
const emojiList = [
  '😀',
  '😃',
  '😄',
  '😁',
  '😆',
  '😅',
  '🤣',
  '😂',
  '🙂',
  '🙃',
  '😉',
  '😊',
  '😇',
  '🥰',
  '😍',
  '🤩',
  '😘',
  '😗',
  '😚',
  '😙',
  '😋',
  '😛',
  '😜',
  '🤪',
  '😝',
  '🤑',
  '🤗',
  '🤭',
  '🤫',
  '🤔',
  '🤐',
  '🤨',
  '😐',
  '😑',
  '😶',
  '😏',
  '😒',
  '🙄',
  '😬',
  '🤥',
  '😌',
  '😔',
  '😪',
  '🤤',
  '😴',
  '😷',
  '🤒',
  '🤕',
  '🤢',
  '🤮',
  '🤧',
  '🥵',
  '🥶',
  '😵',
  '🤯',
  '🤠',
  '🥳',
  '😎',
  '🤓',
  '🧐',
  '😕',
  '😟',
  '🙁',
  '☹️',
  '😮',
  '😯',
  '😲',
  '😳',
  '🥺',
  '😦',
  '😧',
  '😨',
  '😰',
  '😥',
  '😢',
  '😭',
  '😱',
  '😖',
  '😣',
  '😞',
  '😓',
  '😩',
  '😫',
  '🥱',
  '😤',
  '😡',
  '😠',
  '🤬',
  '😈',
  '👿',
  '💀',
  '☠️',
  '💩',
  '🤡',
  '👹',
  '👺',
  '👻',
  '👽',
  '👾',
  '🤖',
  '😺',
  '😸',
  '😹',
  '😻',
  '😼',
  '😽',
  '🙀',
  '😿',
  '😾',
  '🙈',
  '🙉',
  '🙊',
  '💋',
  '💌',
  '💘',
  '💝',
  '💖',
  '💗',
  '💓',
  '💞',
  '💕',
  '💟',
  '❣️',
  '💔',
  '❤️',
  '🧡',
  '💛',
  '💚',
  '💙',
  '💜',
  '🤎',
  '🖤',
  '🤍',
  '💯',
  '💢',
  '💥',
  '💫',
  '💦',
  '💨',
  '💣',
  '💬',
  '🗨️',
  '🗯️',
  '💭',
  '💤',
  '👋',
  '🤚',
  '🖐️',
  '✋',
  '🖖',
  '👌',
  '🤌',
  '🤏',
  '✌️',
  '🤞',
  '🤟',
  '🤘',
  '🤙',
  '👈',
  '👉',
  '👆',
  '👇',
  '☝️',
  '👍',
  '👎',
  '✊',
  '👊',
  '🤛',
  '🤜',
  '👏',
  '🙌',
  '👐',
  '🤲',
  '🤝',
  '🙏',
  '✍️',
  '💪',
  '🦵',
  '🦶',
  '👂',
  '👃',
  '🧠',
  '🦷',
  '🦴',
  '👀',
  '👁️',
  '👅',
  '👄'
]

const toggleEmojiPicker = () => {
  showEmojiPicker.value = !showEmojiPicker.value
}

const closeEmojiPicker = () => {
  showEmojiPicker.value = false
}

const insertEmoji = (emoji) => {
  // 尝试获取textarea元素
  let textarea = null
  if (textInput.value) {
    // Element Plus的textarea可能在不同版本中结构不同
    textarea = textInput.value.textarea || textInput.value.$el?.querySelector('textarea') || textInput.value.input
  }

  if (textarea && typeof textarea.selectionStart !== 'undefined') {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const currentText = text.value
    text.value = currentText.substring(0, start) + emoji + currentText.substring(end)
    // 设置光标位置
    nextTick(() => {
      const newPos = start + emoji.length
      if (textarea && typeof textarea.setSelectionRange === 'function') {
        textarea.setSelectionRange(newPos, newPos)
        textarea.focus()
      }
    })
  } else {
    // 如果无法获取textarea，直接追加到末尾
    text.value += emoji
  }
  closeEmojiPicker()
}

// 点击外部关闭emoji选择器
const handleClickOutside = (e) => {
  if (showEmojiPicker.value && !e.target.closest('.emoji-picker') && !e.target.closest('.emoji-icon')) {
    closeEmojiPicker()
  }
}

onMounted(() => {
  store.connect()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

const loadingMore = ref(false)
const isFirstLoad = ref(true)

const onScroll = async (e) => {
  if (isFirstLoad.value) return // 首次加载未完成，防止误触发

  const target = e.target
  // 滚动到顶部触发加载更多
  if (target.scrollTop < 20 && !loadingMore.value) {
    const friend = props.friend
    if (!friend) return
    const id = friend.id
    const type = friend.type || 0 // 0: private, 1: group
    const key = `${type === 1 ? 'group' : 'private'}_${id}`
    const pagination = store.chatPagination[key]

    if (pagination && pagination.hasMore && !pagination.loading) {
      loadingMore.value = true
      const oldScrollHeight = target.scrollHeight

      // 加载更多
      await store.loadHistory(id, type, true)

      // 恢复滚动位置
      nextTick(() => {
        const newScrollHeight = target.scrollHeight
        target.scrollTop = newScrollHeight - oldScrollHeight
        loadingMore.value = false
      })
    }
  }
}

const scrollToBottom = (smooth = true) => {
  nextTick(() => {
    if (listRef.value) {
      listRef.value.scrollTo({
        top: listRef.value.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
      })
    }
  })
}

// 监听会话切换，立即滚动到底部
watch(
  () => props.friend?.id,
  async (newId, oldId) => {
    if (newId !== oldId) {
      isFirstLoad.value = true // 切换会话时重置为首次加载状态

      // 如果是群聊，获取成员列表以显示角色标签
      if (props.friend?.type === 1) {
        fetchMembers()
      }

      // 注意：这里不需要手动 scrollToBottom，因为切换会话会触发 store 加载消息，
      // 进而触发 watch(messages)，在那里处理首次滚动会更准确。
      // 但为了更好的体验，可以先清空或显示加载中（目前store已处理清空）
    }
  },
  { immediate: true }
)

// 监听消息列表变化，如果是新消息则平滑滚动
watch(messages, async (newVal, oldVal) => {
  if (!newVal || !newVal.length) return

  // 检查是否是新消息（尾部消息ID变化）
  const newLast = newVal[newVal.length - 1]
  const oldLast = oldVal && oldVal.length ? oldVal[oldVal.length - 1] : null

  // 只有当最后一条消息变化（新消息），或者首次加载（oldLast不存在或isFirstLoad）时才滚动到底部
  if (isFirstLoad.value || !oldLast || newLast.id !== oldLast.id) {
    // 如果是首次加载，使用无动画瞬间滚动
    const smooth = !isFirstLoad.value
    await nextTick()
    scrollToBottom(smooth)

    // 如果是首次加载，延迟解除锁定，防止图片加载导致的布局跳动再次触发滚动监听
    if (isFirstLoad.value) {
      setTimeout(() => {
        isFirstLoad.value = false
        // 再次确保到底部（应对图片加载）
        scrollToBottom(false)
      }, 500)
    }
  }
})

const send = () => {
  const content = text.value.trim()
  if (!content || !props.friend) return

  // 根据会话类型选择发送方法
  const sessionType = props.friend?.type || 0
  if (sessionType === 1) {
    // 群聊
    store.sendGroupMessage(props.friend.id, content)
  } else {
    // 私聊
    store.sendMessage(props.friend.id, content)
  }
  text.value = ''
}

const triggerImageSelect = () => imageInput.value.click()
const triggerFileSelect = () => fileInput.value.click()

const handleImageUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  // 限制图片大小 5MB
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }

  await uploadAndSend(file, 2)
  e.target.value = ''
}

const handleFileUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  await uploadAndSend(file, 3)
  e.target.value = ''
}

const uploadAndSend = async (file, type) => {
  if (!props.friend) return

  try {
    uploading.value = true
    uploadProgress.value = 0
    const sessionType = props.friend?.type || 0
    const scene = sessionType === 1 ? 'groupChat' : 'privateChat'
    const result = await uploadFileUnified({
      file,
      scene,
      bizType: 'chat-file',
      bizId: props.friend.id,
      chatFileType: type,
      onProgress: (p) => {
        uploadProgress.value = Math.round(p)
      }
    })
    const actualType = result.messageType || type
    const content = actualType === 2 ? '[图片]' : '[文件]'
    if (sessionType === 1) {
      store.sendGroupMessage(
        props.friend.id,
        content,
        actualType,
        result.fileName || file.name,
        result.fileSize || file.size,
        result.fileUrl
      )
    } else {
      store.sendMessage(
        props.friend.id,
        content,
        actualType,
        result.fileName || file.name,
        result.fileSize || file.size,
        result.fileUrl
      )
    }
  } catch (e) {
    logger.error('上传出错', e)
    ElMessage.error(e.message || '上传出错')
  } finally {
    uploading.value = false
  }
}

const downloadFile = async (url, filename) => {
  try {
    const response = await fetch(url)
    if (!response.ok) throw new Error('Network response was not ok')
    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(link.href)
  } catch (error) {
    logger.error('Download failed:', error)
    // 尝试直接打开
    window.open(url, '_blank')
  }
}

const formatTime = (ts) => {
  try {
    const date = new Date(ts)
    const year = date.getFullYear()
    const month = (date.getMonth() + 1).toString().padStart(2, '0')
    const day = date.getDate().toString().padStart(2, '0')
    const hours = date.getHours().toString().padStart(2, '0')
    const minutes = date.getMinutes().toString().padStart(2, '0')
    return `${year}/${month}/${day} ${hours}:${minutes}`
  } catch {
    return ''
  }
}

const formatSize = (s) => {
  if (!s || isNaN(s)) return ''
  if (s < 1024) return `${s}B`
  if (s < 1024 * 1024) return `${(s / 1024).toFixed(1)}KB`
  return `${(s / 1024 / 1024).toFixed(1)}MB`
}

const handleImageError = (e) => {
  logger.error('图片加载失败:', e.target.src)
  e.target.style.display = 'none'
  const placeholder = e.target.nextElementSibling
  if (placeholder) {
    placeholder.style.display = 'block'
  }
}

const reAddFriend = async () => {
  if (!props.friend) return
  try {
    await ElMessageBox.prompt('请输入验证信息', '添加好友', {
      confirmButtonText: '发送申请',
      cancelButtonText: '取消',
      inputPattern: /\S/,
      inputErrorMessage: '验证信息不能为空'
    }).then(async ({ value }) => {
      await friendStore.applyFriend(props.friend.id, value)
    })
  } catch (e) {
    // cancelled or failed
  }
}
</script>

<style scoped>
.chat-window {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
}
.chat-header .left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.chat-header .meta .name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.chat-header .meta .status {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.more-btn {
  font-size: 26px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}
.more-btn:hover {
  background-color: var(--el-fill-color-light);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
}
.dot.on {
  background: var(--el-color-success);
}
.upload-progress {
  padding: 4px 16px 8px;
  background-color: var(--el-bg-color-page);
}
.chat-body {
  background: var(--el-bg-color-page);
  flex: 1;
  overflow: hidden;
}
.messages {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}
.msg {
  max-width: 70%;
  margin-bottom: 12px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: fit-content;
}
.msg.self {
  margin-left: auto;
}
.msg.other {
  margin-right: auto;
}
.msg-avatar {
  flex-shrink: 0;
}
.msg-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  max-width: 100%;
}
.msg.self .msg-content {
  align-items: flex-end;
}
.msg.other .msg-content {
  align-items: flex-start;
}
.sender-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.role-tag {
  transform: scale(0.9);
  height: 18px;
  padding: 0 4px;
}

.system-row {
  width: 100%;
  max-width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 12px;
}
.system-message-content {
  background-color: #f2f2f2;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #999;
}
.context-menu {
  position: fixed;
  z-index: 9999;
  background: white;
  border: 1px solid #eee;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  padding: 4px 0;
  min-width: 100px;
}
.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}
.menu-item:hover {
  background-color: #f5f7fa;
  color: #409eff;
}
.bubble {
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 8px 12px;
  line-height: 1.6;
  word-break: break-word;
  max-width: 100%;
  color: var(--el-text-color-primary);
}
.msg.self .bubble {
  background: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
}
:global(.dark) .msg.self .bubble {
  background: #1e60b0;
  border-color: #1e60b0;
}
.time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.image-container {
  position: relative;
}
.msg-image {
  max-width: 360px;
  max-height: 400px;
  border-radius: 8px;
  display: inline-block;
  vertical-align: top;
}
.image-placeholder {
  padding: 20px;
  text-align: center;
  color: var(--el-text-color-secondary);
  display: none;
}
.file-message {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.file-icon {
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
}
.file-name {
  color: inherit;
  text-decoration: none;
}
.file-name:hover {
  text-decoration: underline;
}
.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.size {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.sent-status {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.msg.self .sent-status {
  color: rgba(255, 255, 255, 0.8);
}
.system {
  text-align: center;
  display: block;
}
.chat-input {
  border-top: 1px solid var(--el-border-color-light);
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 200px;
  padding: 12px;
}
.inner-tools {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 10px;
  color: var(--el-text-color-secondary);
  z-index: 2;
}
.input-wrap {
  position: relative;
  flex: 1;
}
.input-wrap :deep(.el-textarea__inner) {
  height: 120px;
  padding-right: 92px;
  padding-bottom: 12px;
  padding-top: 36px;
  border: none;
  box-shadow: none;
  outline: none;
  background: var(--el-bg-color);
  resize: none;
}
.input-wrap :deep(.el-textarea__inner:focus) {
  border: none;
  box-shadow: none;
  outline: none;
}
.send-btn {
  position: absolute;
  right: 16px;
  bottom: 16px;
}
.tool-icon {
  cursor: pointer;
  transition: color 0.3s;
}
.tool-icon:hover {
  color: var(--el-color-primary);
}
.emoji-icon {
  font-size: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
}
.emoji-picker {
  position: absolute;
  bottom: 100%;
  left: 0;
  width: 320px;
  height: 280px;
  background: var(--el-bg-color-overlay);
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
}
.emoji-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}
.emoji-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.close-icon {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 16px;
}
.close-icon:hover {
  color: var(--el-color-primary);
}
.emoji-grid {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
}
.emoji-item {
  font-size: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
  user-select: none;
}
.emoji-item:hover {
  background-color: var(--el-fill-color-light);
}
.bubble-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.error-icon {
  color: var(--el-color-danger);
  font-size: 18px;
  cursor: pointer;
}
.error-text {
  font-size: 12px;
  color: var(--el-color-danger);
  margin-top: 4px;
  text-align: right;
}
.link-text {
  color: var(--el-color-primary);
  cursor: pointer;
  text-decoration: none;
}
.link-text:hover {
  text-decoration: underline;
}

.member-list-container {
  padding: 0 10px;
}
.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.member-item:last-child {
  border-bottom: none;
}
.member-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.member-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
.real-name {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.member-actions {
  margin-left: 10px;
}
.action-icon {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 18px;
}
.action-icon:hover {
  color: var(--el-color-primary);
}

.member-list-header {
  margin-bottom: 12px;
  display: flex;
  justify-content: flex-end;
}
.invite-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 400px;
}
.search-input {
  margin-bottom: 8px;
}
.friend-list {
  flex: 1;
  overflow-y: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  padding: 8px;
}
.friend-item {
  display: flex;
  align-items: center;
  padding: 8px;
  border-bottom: 1px solid var(--el-fill-color-light);
}
.friend-item:last-child {
  border-bottom: none;
}
.friend-item .el-checkbox {
  width: 100%;
  display: flex;
  align-items: center;
}
.friend-item :deep(.el-checkbox__label) {
  flex: 1;
}
.friend-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.friend-info .name {
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.search-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 10px;
}
.search-results {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.result-item {
  padding: 10px;
  border-radius: 4px;
  background-color: var(--el-fill-color-light);
  cursor: pointer;
  transition: background-color 0.2s;
}
.result-item:hover {
  background-color: var(--el-fill-color);
}
.result-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.result-content {
  font-size: 14px;
  color: var(--el-text-color-primary);
  word-break: break-word;
}
.result-content :deep(em) {
  color: red;
  font-style: normal;
  font-weight: bold;
}

@keyframes highlight-flash {
  0% {
    background-color: rgba(64, 158, 255, 0.5);
  }
  100% {
    background-color: transparent;
  }
}
.highlight-flash {
  animation: highlight-flash 2s ease-out;
  border-radius: 4px;
}
.loading-more {
  text-align: center;
  padding: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
</style>
