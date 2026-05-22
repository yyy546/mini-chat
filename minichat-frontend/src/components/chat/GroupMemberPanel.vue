<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)" title="群成员列表" width="450px" append-to-body @open="fetchMembers">
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
        <el-radio-group v-model="removeTarget" style="width:100%;display:block">
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
      <el-button type="danger" @click="handleRemoveMember" :loading="removeLoading" :disabled="!removeTarget">确定移除</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Delete, SwitchButton, MoreFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useFriendStore } from '@/store/friend'
import { getGroupProfile, getGroupMemberList, inviteToGroup, removeGroupMember, exitGroup, updateGroupMemberRole, transferGroupOwner } from '@/api/group'
import logger from '@/utils/logger'

const props = defineProps<{ groupId: number; modelValue: boolean }>()
defineEmits<{ 'update:modelValue': [value: boolean] }>()

const userStore = useUserStore()
const friendStore = useFriendStore()

const loadingMembers = ref(false)
const memberList = ref<{ userId: number; nickname?: string; nicknameInGroup?: string; avatar?: string; role: number }[]>([])
const currentGroup = ref<Record<string, unknown> | null>(null)

const showInviteDialog = ref(false)
const inviteList = ref<number[]>([])
const searchKeyword = ref('')
const inviteLoading = ref(false)

const showRemoveDialog = ref(false)
const removeTarget = ref<number | null>(null)
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
  if (!props.groupId) return
  loadingMembers.value = true
  try {
    const [profileRes, membersRes] = await Promise.all([
      getGroupProfile(props.groupId),
      getGroupMemberList(props.groupId)
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

function openRemoveDialog() { removeTarget.value = null; removeSearchKeyword.value = ''; showRemoveDialog.value = true }

async function handleInvite() {
  if (!inviteList.value.length) return
  inviteLoading.value = true
  try {
    await inviteToGroup({ groupId: props.groupId, userIds: inviteList.value })
    ElMessage.success('邀请成功'); showInviteDialog.value = false; fetchMembers()
  } catch { ElMessage.error('邀请出错') }
  finally { inviteLoading.value = false }
}

async function handleRemoveMember() {
  if (!removeTarget.value) return
  const member = memberList.value.find((m) => m.userId === removeTarget.value)
  try { await ElMessageBox.confirm(`确定要将 "${member?.nicknameInGroup || member?.nickname}" 移出群组吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  removeLoading.value = true
  try {
    await removeGroupMember({ groupId: props.groupId, userId: removeTarget.value })
    ElMessage.success('移除成员成功'); showRemoveDialog.value = false; fetchMembers()
  } catch { ElMessage.error('移除出错') }
  finally { removeLoading.value = false }
}

async function handleExitGroup() {
  if (!props.groupId) return
  try { await ElMessageBox.confirm('确定要退出该群聊吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try { await exitGroup(props.groupId); ElMessage.success('退出群组成功'); window.location.reload() }
  catch { ElMessage.error('退出群组出错') }
}

async function handleMemberAction(command: string, member: { userId: number; nicknameInGroup?: string; nickname?: string }) {
  const name = member.nicknameInGroup || member.nickname
  try {
    if (command === 'setAdmin') {
      await ElMessageBox.confirm(`确定要将 "${name}" 设为管理员吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' })
      await updateGroupMemberRole({ groupId: props.groupId, userId: member.userId, role: 1 })
      ElMessage.success('设置管理员成功')
    } else if (command === 'cancelAdmin') {
      await ElMessageBox.confirm(`确定要取消 "${name}" 的管理员身份吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
      await updateGroupMemberRole({ groupId: props.groupId, userId: member.userId, role: 0 })
      ElMessage.success('取消管理员成功')
    } else if (command === 'transferOwner') {
      await ElMessageBox.confirm(`确定要将群主转让给 "${name}" 吗？`, '警告', { confirmButtonText: '确定转让', cancelButtonText: '取消', type: 'warning' })
      await transferGroupOwner({ groupId: props.groupId, newOwnerId: member.userId })
      ElMessage.success('转让群主成功')
    }
    fetchMembers()
  } catch { /* cancelled or failed */ }
}

defineExpose({ memberRoleMap, fetchMembers })
</script>

<style scoped>
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
</style>
