<template>
  <el-container class="chat-window">
    <el-header class="chat-header">
      <div class="left">
        <el-avatar :size="36" :src="friend?.avatar">{{ friend?.avatar ? '' : initial }}</el-avatar>
        <div class="meta">
          <div class="name">{{ friend?.name || friend?.remark || friend?.nickname || friend?.username || '未选择' }}</div>
          <div v-if="friend?.type === 0 || friend?.type === undefined" class="status">
            <span class="dot" :class="{ on: friend?.online }"></span>{{ friend?.online ? '在线' : '离线' }}
          </div>
          <div v-else-if="friend?.type === 1" class="status"><span>群聊</span></div>
        </div>
      </div>
      <div class="right">
        <el-dropdown trigger="click" @command="handleCommand">
          <el-icon class="more-btn"><MoreFilled /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="friend?.type === 1" command="members">群组成员</el-dropdown-item>
              <el-dropdown-item command="info">{{ friend?.type === 1 ? '群组信息' : '好友信息' }}</el-dropdown-item>
              <el-dropdown-item command="search">查找聊天记录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <MessageList
      ref="msgListRef"
      :messages="messages"
      :friend-id="(friend as any)?.id"
      :friend-type="(friend as any)?.type || 0"
      :friend-avatar="(friend as any)?.avatar"
      :friend-name="(friend as any)?.name || (friend as any)?.remark || (friend as any)?.nickname || (friend as any)?.username"
      :self-avatar="currentUserAvatar"
      :self-initial="currentUserInitial"
      :member-role-map="memberRoleMap"
      @contextmenu="handleRightClick"
      @readd-friend="reAddFriend"
    />

    <FileUploader
      ref="uploaderRef"
      :friend-id="(friend as any)?.id"
      :scene="(friend as any)?.type === 1 ? 'groupChat' : 'privateChat'"
      @upload-success="onUploadSuccess"
    />

    <MessageInput
      :disabled="!friend"
      :placeholder="'输入消息...'"
      @send="onSendText"
      @trigger-image="uploaderRef?.triggerImageSelect()"
      @trigger-file="uploaderRef?.triggerFileSelect()"
    />

    <!-- 右键菜单 -->
    <div v-if="showContextMenu" class="context-menu" :style="{ top: contextMenuTop + 'px', left: contextMenuLeft + 'px' }" @click.stop>
      <div class="menu-item" @click="handleRecall">
        <el-icon><RefreshLeft /></el-icon> 撤回
      </div>
    </div>

    <!-- 群成员列表弹窗 -->
    <el-dialog v-model="showMembersDialog" title="群成员列表" width="450px" append-to-body>
      <div v-loading="loadingMembers" class="member-list-container">
        <div class="member-list-header">
          <div class="header-right" style="display:flex;gap:10px">
            <el-button type="danger" size="small" @click="handleExitGroup"><el-icon><SwitchButton /></el-icon>退出群聊</el-button>
            <el-button v-if="canRemove" type="danger" size="small" @click="openRemoveDialog"><el-icon><Delete /></el-icon>移除成员</el-button>
            <el-button v-if="canInvite" type="primary" size="small" @click="openInviteDialog"><el-icon><Plus /></el-icon>邀请好友</el-button>
          </div>
        </div>
        <el-scrollbar height="400px">
          <div v-for="m in memberList" :key="m.userId" class="member-item">
            <el-avatar :src="m.avatar" :size="40">{{ (m.nickname || m.nicknameInGroup || '').slice(0,1).toUpperCase() }}</el-avatar>
            <div class="member-info">
              <div class="member-name">
                <span class="name-text">{{ m.nicknameInGroup || m.nickname }}</span>
                <el-tag v-if="m.role===2" size="small" type="warning" effect="dark">群主</el-tag>
                <el-tag v-else-if="m.role===1" size="small" type="success" effect="dark">管理员</el-tag>
              </div>
              <div class="real-name" v-if="m.nicknameInGroup && m.nicknameInGroup !== m.nickname">原名: {{ m.nickname }}</div>
            </div>
            <div v-if="currentUserRole===2 && m.userId !== userStore.userInfo?.id" class="member-actions">
              <el-dropdown trigger="click" @command="(cmd: string) => handleMemberAction(cmd, m)">
                <el-icon class="action-icon"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="m.role===0" command="setAdmin">设为管理员</el-dropdown-item>
                    <el-dropdown-item v-if="m.role===1" command="cancelAdmin">取消管理员</el-dropdown-item>
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
            <div v-for="f in filteredFriends" :key="f.id" class="friend-item">
              <el-checkbox :label="f.id">
                <div class="friend-info">
                  <el-avatar :size="32" :src="f.avatar">{{ (f.remark || f.nickname || f.username || '').slice(0,1).toUpperCase() }}</el-avatar>
                  <span class="name">{{ f.remark || f.nickname || f.username }}</span>
                </div>
              </el-checkbox>
            </div>
          </el-checkbox-group>
          <el-empty v-if="!filteredFriends.length" description="暂无可选好友" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showInviteDialog=false">取消</el-button>
        <el-button type="primary" @click="handleInvite" :loading="inviteLoading" :disabled="!inviteList.length">确定邀请 ({{ inviteList.length }})</el-button>
      </template>
    </el-dialog>

    <!-- 移除成员弹窗 -->
    <el-dialog v-model="showRemoveDialog" title="移除群成员" width="500px" append-to-body>
      <div class="invite-container">
        <el-input v-model="removeSearchKeyword" placeholder="搜索成员" :prefix-icon="Search" clearable class="search-input" />
        <div class="friend-list">
          <el-radio-group v-model="removeList" style="width:100%;display:block">
            <div v-for="m in filteredMembersToRemove" :key="m.userId" class="friend-item">
              <el-radio :label="m.userId" style="width:100%;display:flex;align-items:center">
                <div class="friend-info">
                  <el-avatar :size="32" :src="m.avatar">{{ (m.nicknameInGroup || m.nickname || '').slice(0,1).toUpperCase() }}</el-avatar>
                  <div class="member-name">
                    <span class="name">{{ m.nicknameInGroup || m.nickname }}</span>
                    <el-tag v-if="m.role===1" size="small" type="success" effect="dark" style="margin-left:5px">管理员</el-tag>
                  </div>
                </div>
              </el-radio>
            </div>
          </el-radio-group>
          <el-empty v-if="!filteredMembersToRemove.length" description="暂无可移除成员" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showRemoveDialog=false">取消</el-button>
        <el-button type="danger" @click="handleRemoveMember" :loading="removeLoading" :disabled="!removeList">确定移除</el-button>
      </template>
    </el-dialog>

    <!-- 搜索聊天记录抽屉 -->
    <el-drawer v-model="showSearchDrawer" title="查找聊天记录" size="400px" append-to-body>
      <div class="search-container">
        <el-input v-model="chatSearchKeyword" placeholder="搜索关键词" @keyup.enter="handleChatSearch" clearable>
          <template #append><el-button :icon="Search" @click="handleChatSearch" /></template>
        </el-input>
        <div class="search-results" v-loading="searching">
          <div v-for="item in searchResults" :key="(item as any).id" class="result-item" @click="jumpToMessage(item as any)">
            <div class="result-header">
              <span class="name">{{ (item as any).senderId === userStore.userInfo?.id ? '我' : (item as any).senderNickName || (item as any).senderNickname || 'Unknown' }}</span>
              <span class="time">{{ formatTime((item as any).sendTime) }}</span>
            </div>
            <div class="result-content" v-html="(item as any).content"></div>
            <div class="result-content" v-if="(item as any).fileName" v-html="'[文件] ' + (item as any).fileName"></div>
          </div>
          <el-empty v-if="!searchResults.length && !searching" description="无搜索结果" />
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled, Search, Plus, Delete, SwitchButton, RefreshLeft } from '@element-plus/icons-vue'
import { useChatStore } from '@/store/chat'
import { useUserStore } from '@/store/user'
import { useFriendStore } from '@/store/friend'
import { searchChatMessages } from '@/api/chat'
import { getGroupProfile, getGroupMemberList, inviteToGroup, removeGroupMember, exitGroup, updateGroupMemberRole, transferGroupOwner } from '@/api/group'
import MessageList from './MessageList.vue'
import MessageInput from './MessageInput.vue'
import FileUploader from './FileUploader.vue'
import logger from '@/utils/logger'
import type { UIMessage } from '@/types/message'

const props = defineProps<{ friend: Record<string, unknown> | null }>()
const emit = defineEmits<{ 'open-profile': [info: { type: string; id: number }] }>()

const store = useChatStore()
const userStore = useUserStore()
const friendStore = useFriendStore()
const messages = computed(() => store.activeMessages)
const uploaderRef = ref<InstanceType<typeof FileUploader> | null>(null)
const msgListRef = ref<InstanceType<typeof MessageList> | null>(null)

const initial = computed(() => ((props.friend?.remark || props.friend?.nickname || props.friend?.username || '') as string).slice(0, 1).toUpperCase())
const currentUserInitial = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || '').slice(0, 1).toUpperCase())
const currentUserAvatar = computed(() => userStore.userInfo?.avatar || '')

// Context menu
const showContextMenu = ref(false)
const contextMenuTop = ref(0)
const contextMenuLeft = ref(0)
const currentMessage = ref<UIMessage | null>(null)

function handleRightClick(e: MouseEvent, message: UIMessage) {
  if (message.fromId !== 'self' || message.type === 4 || message.type === 5) return
  e.preventDefault()
  showContextMenu.value = true
  contextMenuLeft.value = e.clientX
  contextMenuTop.value = (e.currentTarget as HTMLElement).getBoundingClientRect().bottom + 10
  currentMessage.value = message
  const close = () => { showContextMenu.value = false; document.removeEventListener('click', close) }
  document.addEventListener('click', close)
}

async function handleRecall() {
  if (!currentMessage.value) return
  const msgId = currentMessage.value.id
  if (typeof msgId === 'string' && msgId.startsWith('temp_')) { ElMessage.warning('消息发送中，请稍后重试'); showContextMenu.value = false; return }
  if (!msgId) { ElMessage.warning('无法撤回此消息'); return }
  await store.recallMessage(msgId as number)
  showContextMenu.value = false
}

// Group member state
const showMembersDialog = ref(false)
const loadingMembers = ref(false)
const memberList = ref<{ userId: number; nickname?: string; nicknameInGroup?: string; avatar?: string; role: number }[]>([])
const showInviteDialog = ref(false)
const inviteList = ref<number[]>([])
const searchKeyword = ref('')
const inviteLoading = ref(false)
const currentGroup = ref<Record<string, unknown> | null>(null)
const showRemoveDialog = ref(false)
const removeList = ref<number | null>(null)
const removeLoading = ref(false)
const removeSearchKeyword = ref('')

const currentUserRole = computed(() => {
  if (!userStore.userInfo?.id) return 0
  const me = memberList.value.find((m) => m.userId === userStore.userInfo!.id)
  return me ? me.role : 0
})

const memberRoleMap = computed(() => {
  const map: Record<string | number, number> = {}
  memberList.value.forEach((m) => { map[m.userId] = m.role })
  return map
})

const canInvite = computed(() => {
  if (!currentGroup.value) return false
  return currentUserRole.value >= ((currentGroup.value.invitePolicy as number) || 0)
})

const canRemove = computed(() => currentUserRole.value > 0)

const filteredFriends = computed(() => {
  const friends = friendStore.friends.filter((f) => f.type === 0 || f.type === undefined)
  const memberIds = new Set(memberList.value.map((m) => m.userId))
  let potential = friends.filter((f) => !memberIds.has(f.id))
  if (searchKeyword.value) {
    const k = searchKeyword.value.toLowerCase()
    potential = potential.filter((f) => (f.remark || '').toLowerCase().includes(k) || (f.nickname || '').toLowerCase().includes(k))
  }
  return potential
})

const filteredMembersToRemove = computed(() => {
  const currentUserId = userStore.userInfo?.id
  const myRole = currentUserRole.value
  let candidates = memberList.value.filter((m) => m.userId !== currentUserId && m.role < myRole)
  if (removeSearchKeyword.value) {
    const k = removeSearchKeyword.value.toLowerCase()
    candidates = candidates.filter((m) => (m.nicknameInGroup || '').toLowerCase().includes(k) || (m.nickname || '').toLowerCase().includes(k))
  }
  return candidates
})

async function fetchMembers() {
  if (!props.friend?.id) return
  loadingMembers.value = true
  try {
    const [profileRes, membersRes] = await Promise.all([
      getGroupProfile(props.friend.id as number),
      getGroupMemberList(props.friend.id as number)
    ])
    if (profileRes) currentGroup.value = profileRes as unknown as Record<string, unknown>
    if (membersRes) memberList.value = membersRes as unknown as typeof memberList.value
  } catch (e) { logger.error(e); ElMessage.error('获取信息失败') }
  finally { loadingMembers.value = false }
}

function openInviteDialog() {
  if (!friendStore.friends.length) friendStore.fetchFriends()
  inviteList.value = []; searchKeyword.value = ''; showInviteDialog.value = true
}

function openRemoveDialog() { removeList.value = null; removeSearchKeyword.value = ''; showRemoveDialog.value = true }

async function handleInvite() {
  if (!inviteList.value.length) return
  inviteLoading.value = true
  try {
    await inviteToGroup({ groupId: props.friend!.id as number, userIds: inviteList.value })
    ElMessage.success('邀请成功'); showInviteDialog.value = false; fetchMembers()
  } catch { ElMessage.error('邀请出错') }
  finally { inviteLoading.value = false }
}

async function handleRemoveMember() {
  if (!removeList.value) return
  const member = memberList.value.find((m) => m.userId === removeList.value)
  try { await ElMessageBox.confirm(`确定要将 "${member?.nicknameInGroup || member?.nickname}" 移出群组吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  removeLoading.value = true
  try {
    await removeGroupMember({ groupId: props.friend!.id as number, userId: removeList.value })
    ElMessage.success('移除成员成功'); showRemoveDialog.value = false; fetchMembers()
  } catch { ElMessage.error('移除出错') }
  finally { removeLoading.value = false }
}

async function handleExitGroup() {
  if (!props.friend?.id) return
  try { await ElMessageBox.confirm('确定要退出该群聊吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await exitGroup(props.friend.id as number); ElMessage.success('退出群组成功'); showMembersDialog.value = false; window.location.reload() }
  catch { ElMessage.error('退出群组出错') }
}

async function handleMemberAction(command: string, member: { userId: number; nicknameInGroup?: string; nickname?: string }) {
  const gid = props.friend!.id as number
  const name = member.nicknameInGroup || member.nickname
  try {
    if (command === 'setAdmin') {
      await ElMessageBox.confirm(`确定要将 "${name}" 设为管理员吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' })
      await updateGroupMemberRole({ groupId: gid, userId: member.userId, role: 1 })
      ElMessage.success('设置管理员成功')
    } else if (command === 'cancelAdmin') {
      await ElMessageBox.confirm(`确定要取消 "${name}" 的管理员身份吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
      await updateGroupMemberRole({ groupId: gid, userId: member.userId, role: 0 })
      ElMessage.success('取消管理员成功')
    } else if (command === 'transferOwner') {
      await ElMessageBox.confirm(`确定要将群主转让给 "${name}" 吗？`, '警告', { confirmButtonText: '确定转让', cancelButtonText: '取消', type: 'warning' })
      await transferGroupOwner({ groupId: gid, newOwnerId: member.userId })
      ElMessage.success('转让群主成功')
    }
    fetchMembers()
  } catch { /* cancelled or failed */ }
}

// Search
const showSearchDrawer = ref(false)
const chatSearchKeyword = ref('')
const searchResults = ref<unknown[]>([])
const searching = ref(false)

async function handleChatSearch() {
  if (!chatSearchKeyword.value.trim() || !props.friend) return
  searching.value = true
  try {
    searchResults.value = await searchChatMessages(chatSearchKeyword.value, props.friend.type as number || 0, props.friend.id as number)
  } catch { ElMessage.error('搜索出错') }
  finally { searching.value = false }
}

function jumpToMessage(item: Record<string, unknown>) {
  showSearchDrawer.value = false
  const dbId = item.dbId as number
  const found = messages.value.find((m) => m.id === dbId)
  if (found) {
    nextTick(() => {
      const el = document.getElementById('msg-' + dbId)
      if (el) { el.scrollIntoView({ behavior: 'smooth', block: 'center' }); el.classList.add('highlight-flash'); setTimeout(() => el.classList.remove('highlight-flash'), 2000) }
    })
  } else { ElMessage.warning('该消息不在当前加载的历史记录中') }
}

function formatTime(ts: unknown) {
  try {
    const date = new Date(ts as number)
    return `${date.getFullYear()}/${String(date.getMonth()+1).padStart(2,'0')}/${String(date.getDate()).padStart(2,'0')} ${String(date.getHours()).padStart(2,'0')}:${String(date.getMinutes()).padStart(2,'0')}`
  } catch { return '' }
}

async function handleCommand(command: string) {
  if (command === 'info') {
    if (!props.friend) return
    emit('open-profile', { id: props.friend.id as number, type: (props.friend.type as number || 0) === 1 ? 'group' : 'friend' })
  } else if (command === 'members') { showMembersDialog.value = true; fetchMembers() }
  else if (command === 'search') { showSearchDrawer.value = true; chatSearchKeyword.value = ''; searchResults.value = [] }
}

// Send
function onSendText(content: string) {
  if (!props.friend) return
  if ((props.friend.type as number || 0) === 1) store.sendGroupMessage(props.friend.id as number, content)
  else store.sendMessage(props.friend.id as number, content)
}

function onUploadSuccess(result: { fileUrl: string; fileName: string; fileSize: number; messageType?: number }) {
  if (!props.friend) return
  const actualType = result.messageType || 3
  const content = actualType === 2 ? '[图片]' : '[文件]'
  if ((props.friend.type as number || 0) === 1) store.sendGroupMessage(props.friend.id as number, content, actualType, result.fileName, result.fileSize, result.fileUrl)
  else store.sendMessage(props.friend.id as number, content, actualType, result.fileName, result.fileSize, result.fileUrl)
}

async function reAddFriend() {
  if (!props.friend) return
  try {
    await ElMessageBox.prompt('请输入验证信息', '添加好友', { confirmButtonText: '发送申请', cancelButtonText: '取消', inputPattern: /\S/, inputErrorMessage: '验证信息不能为空' })
      .then(async ({ value }) => { await friendStore.applyFriend(props.friend!.id as number, value as string) })
  } catch { /* cancelled */ }
}

// Lifecycle
function handleClickOutside(e: MouseEvent) {
  if (!(e.target as HTMLElement).closest('.emoji-picker') && !(e.target as HTMLElement).closest('.emoji-icon')) {
    // Emoji picker is managed inside MessageInput now
  }
}

onMounted(() => { store.connect(); document.addEventListener('click', handleClickOutside) })
onUnmounted(() => { document.removeEventListener('click', handleClickOutside) })

watch(() => (props.friend as Record<string, unknown> | null)?.id, async (newId, oldId) => {
  if (newId !== oldId && props.friend?.type === 1) fetchMembers()
}, { immediate: true })
</script>

<style scoped>
.chat-window { height: 100%; display: flex; flex-direction: column; }
.chat-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--el-border-color-light); }
.chat-header .left { display: flex; align-items: center; gap: 10px; }
.chat-header .meta .name { font-weight: 600; color: var(--el-text-color-primary); }
.chat-header .meta .status { font-size: 12px; color: var(--el-text-color-secondary); display: flex; align-items: center; gap: 6px; }
.more-btn { font-size: 26px; color: var(--el-text-color-regular); cursor: pointer; padding: 4px; border-radius: 4px; }
.more-btn:hover { background-color: var(--el-fill-color-light); }
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--el-text-color-placeholder); }
.dot.on { background: var(--el-color-success); }
.msg.self .bubble { background: var(--el-color-primary); color: #fff; border-color: var(--el-color-primary); }
:global(.dark) .msg.self .bubble { background: #1e60b0; border-color: #1e60b0; }
.msg.self .sent-status { color: rgba(255,255,255,0.8); }
.context-menu { position: fixed; z-index: 9999; background: white; border: 1px solid #eee; box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1); border-radius: 4px; padding: 4px 0; min-width: 100px; }
.menu-item { padding: 8px 16px; cursor: pointer; display: flex; align-items: center; gap: 8px; font-size: 14px; color: #606266; }
.menu-item:hover { background-color: #f5f7fa; color: #409eff; }
.member-list-container { padding: 0 10px; }
.member-list-header { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.member-item { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.member-item:last-child { border-bottom: none; }
.member-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.member-name { display: flex; align-items: center; gap: 8px; font-weight: 500; color: var(--el-text-color-primary); }
.real-name { font-size: 12px; color: var(--el-text-color-secondary); }
.member-actions { margin-left: 10px; }
.action-icon { cursor: pointer; color: var(--el-text-color-secondary); font-size: 18px; }
.action-icon:hover { color: var(--el-color-primary); }
.invite-container { display: flex; flex-direction: column; gap: 12px; height: 400px; }
.search-input { margin-bottom: 8px; }
.friend-list { flex: 1; overflow-y: auto; border: 1px solid var(--el-border-color-light); border-radius: 4px; padding: 8px; }
.friend-item { display: flex; align-items: center; padding: 8px; border-bottom: 1px solid var(--el-fill-color-light); }
.friend-item:last-child { border-bottom: none; }
.friend-info { display: flex; align-items: center; gap: 10px; }
.friend-info .name { font-size: 14px; color: var(--el-text-color-primary); }
.search-container { display: flex; flex-direction: column; height: 100%; gap: 10px; }
.search-results { flex: 1; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; }
.result-item { padding: 10px; border-radius: 4px; background-color: var(--el-fill-color-light); cursor: pointer; transition: background-color 0.2s; }
.result-item:hover { background-color: var(--el-fill-color); }
.result-header { display: flex; justify-content: space-between; margin-bottom: 4px; font-size: 12px; color: var(--el-text-color-secondary); }
.result-content { font-size: 14px; color: var(--el-text-color-primary); word-break: break-word; }
.result-content :deep(em) { color: red; font-style: normal; font-weight: bold; }
@keyframes highlight-flash { 0% { background-color: rgba(64,158,255,0.5); } 100% { background-color: transparent; } }
.highlight-flash { animation: highlight-flash 2s ease-out; border-radius: 4px; }
</style>
