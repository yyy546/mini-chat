<template>
  <div class="friend-detail-container">
    <div v-if="friend" class="friend-detail">
      <div class="detail-card">
        <el-tooltip content="好友动态" placement="top">
          <el-button circle class="space-btn" @click="handleShowSpace">
            <el-icon size="20"><Star /></el-icon>
          </el-button>
        </el-tooltip>
        <div class="profile-header">
          <div class="header-left">
            <el-avatar :size="80" :src="friend.avatar" class="big-avatar">
              {{ getInitial(friend.nickname || friend.username) }}
            </el-avatar>
          </div>
          <div class="header-right">
            <div class="name-row">
              <span class="display-name">{{ friend.remark || friend.nickname || friend.username }}</span>
            </div>
            <div class="nickname-row" v-if="friend.remark && friend.nickname && friend.remark !== friend.nickname">
              <span class="nickname-text">昵称: {{ friend.nickname }}</span>
            </div>
            <div class="sub-row">
              <span class="qq">ID {{ friend.id || friend.userId }}</span>
              <span class="status-badge" :class="{ online: friend.online }"></span>
              <span class="status-text">{{ friend.online ? '在线' : '离线' }}</span>
            </div>
          </div>
        </div>

        <div class="tags-row">
          <span class="tag-item male" v-if="friend.gender === '男' || friend.gender === 'MALE' || friend.gender === '1'"
            >♂ 男</span
          >
          <span
            class="tag-item female"
            v-else-if="friend.gender === '女' || friend.gender === 'FEMALE' || friend.gender === '2'"
            >♀ 女</span
          >
        </div>

        <div class="group-select-row">
          <el-icon class="row-icon"><User /></el-icon>
          <span class="label">好友分组</span>
          <el-select v-model="selectedGroup" class="group-select" @change="handleGroupChange">
            <el-option
              v-for="group in groupList"
              :key="group.groupName"
              :label="group.groupName"
              :value="group.groupName"
            />
            <el-option value="__ADD__" class="add-group-option">
              <div class="add-group-content">
                <el-icon><Plus /></el-icon> <span>添加分组</span>
              </div>
            </el-option>
          </el-select>
        </div>

        <div class="action-buttons">
          <el-button class="action-btn" @click="onEditRemark">修改备注</el-button>
          <el-button class="action-btn primary" type="primary" @click="sendMessage">发消息</el-button>
          <el-button class="action-btn danger" type="danger" plain @click="onDeleteFriend">删除好友</el-button>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      <el-empty description="请选择一位好友查看详情" />
    </div>

    <!-- Edit Remark Dialog -->
    <el-dialog v-model="remarkDialogVisible" title="设置备注" width="30%">
      <el-input v-model="remarkInput" placeholder="请输入备注名" clearable />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="remarkDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRemark">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- Space Posts Dialog -->
    <el-dialog v-model="spaceDialogVisible" title="好友动态" width="600px" append-to-body>
      <div v-loading="loadingSpace" class="space-posts-list" style="max-height: 60vh; overflow-y: auto">
        <el-empty v-if="!loadingSpace && spacePosts.length === 0" description="暂无动态" />
        <div v-for="post in spacePosts" :key="post.id" class="post-item">
          <div class="post-header">
            <el-avatar :size="40" :src="post.authorAvatar" />
            <div class="post-info">
              <div class="post-author">{{ post.authorName }}</div>
              <div class="post-time">{{ post.createdTime }}</div>
            </div>
          </div>
          <div class="post-content">{{ post.content }}</div>
          <div class="post-images" v-if="post.images && post.images.length">
            <el-image
              v-for="(img, idx) in post.images"
              :key="idx"
              :src="img"
              :preview-src-list="post.images"
              class="post-img"
              fit="cover"
            />
          </div>
          <div class="post-actions">
            <button
              type="button"
              class="action-item like-action"
              :class="{ liked: isPostLiked(post) }"
              @click="handleToggleLike(post)"
            >
              <span class="like-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path
                    d="M2 21h4V9H2v12zm20-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L13 1 7.59 6.41C7.22 6.78 7 7.3 7 7.83V19c0 1.1.9 2 2 2h8c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z"
                  />
                </svg>
              </span>
              <span v-if="post.likesCount > 0">{{ post.likesCount }}</span>
              <span v-else>点赞</span>
            </button>
            <div class="action-item" @click="handleToggleComment(post)">
              <el-icon><ChatDotRound /></el-icon>
              <span v-if="post.commentsCount > 0">{{ post.commentsCount }}</span>
              <span v-else>评论</span>
            </div>
          </div>

          <!-- 评论区域 -->
          <div class="comment-section" v-if="activeCommentPostId === getPostId(post)">
            <!-- 评论输入框 -->
            <div class="comment-input-area" v-if="activeCommentPostId === getPostId(post)">
              <el-input
                v-model="commentContent"
                type="textarea"
                :rows="2"
                placeholder="写下你的评论..."
                resize="none"
                class="comment-input"
              />
              <div class="comment-btn-group">
                <el-button type="primary" size="small" :loading="commentPublishing" @click="handlePublishComment(post)"
                  >发送</el-button
                >
              </div>
            </div>

            <!-- 评论列表 -->
            <div class="comment-list" v-if="post.comments && post.comments.length > 0">
              <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
                <div class="comment-content-wrapper">
                  <span class="comment-user">{{ comment.publishName || '未知用户' }}: </span>
                  <span class="comment-text">{{ comment.content }}</span>
                </div>
                <div class="comment-actions" v-if="sameId(comment.publishId, userStore.userInfo?.id)">
                  <el-popconfirm title="确定删除这条评论吗？" @confirm="handleDeleteComment(comment.id, post)">
                    <template #reference>
                      <el-icon class="delete-comment-icon"><Delete /></el-icon>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>
          <el-divider />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, watch, ref, onMounted } from 'vue'
import { useFriendStore } from '../../store/friend'
import { useChatStore } from '../../store/chat'
import { useUserStore } from '../../store/user'
import {
  getSpacePostList,
  changeLikeStatus,
  publishSpaceComment,
  deleteSpaceComment,
  getSpaceCommentList
} from '../../api/space'
import { Star, Edit, ChatDotRound, User, ArrowDown, ArrowRight, Plus, Picture, Delete } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import logger from '../../utils/logger'

const props = defineProps({
  friendId: { type: [Number, String], default: null }
})

const emit = defineEmits(['message'])
const store = useFriendStore()
const chatStore = useChatStore()
const userStore = useUserStore()

const remarkDialogVisible = ref(false)
const remarkInput = ref('')
const spaceDialogVisible = ref(false)
const spacePosts = ref([])
const loadingSpace = ref(false)

// Logic for Like and Comment
const likeLoadingPostId = ref(null)
const likedPostMap = ref({})
const activeCommentPostId = ref(null)
const commentContent = ref('')
const commentPublishing = ref(false)

const toIdString = (v) => (v === null || v === undefined ? '' : String(v))
const sameId = (a, b) => toIdString(a) !== '' && toIdString(a) === toIdString(b)
const getPostId = (post) => toIdString(post?.id ?? post?.postId)

const isPostLiked = (post) => {
  const postId = getPostId(post)
  if (!postId) return false
  if (Object.prototype.hasOwnProperty.call(likedPostMap.value, postId)) {
    return !!likedPostMap.value[postId]
  }
  return !!post?.liked
}

const syncLikedPostMap = (posts) => {
  const next = {}
  ;(posts || []).forEach((p) => {
    const postId = getPostId(p)
    if (postId && p?.liked) {
      next[postId] = true
    }
  })
  likedPostMap.value = next
}

const handleToggleLike = async (post) => {
  const postId = getPostId(post)
  if (!postId) return
  if (likeLoadingPostId.value === postId) return

  const wasLiked = isPostLiked(post)
  const currentCount = Number(post.likesCount || 0)
  const nextLiked = !wasLiked
  const delta = nextLiked ? 1 : -1

  // Optimistic update
  likedPostMap.value = { ...likedPostMap.value, [postId]: nextLiked }
  post.liked = nextLiked
  post.likesCount = Math.max(0, currentCount + delta)

  likeLoadingPostId.value = postId

  try {
    const res = await changeLikeStatus(postId)
    if (res.code !== 1) {
      throw new Error(res.msg || '操作失败')
    }
    // Success, keep optimistic state
  } catch (e) {
    logger.error(e)
    ElMessage.error(e.message || '点赞出错')
    // Revert
    likedPostMap.value = { ...likedPostMap.value, [postId]: wasLiked }
    post.liked = wasLiked
    post.likesCount = currentCount
  } finally {
    likeLoadingPostId.value = null
  }
}

const loadComments = async (post) => {
  const postId = getPostId(post)
  try {
    const res = await getSpaceCommentList(postId)
    if (res.code === 1) {
      post.comments = res.data || []
    }
  } catch (e) {
    logger.error('加载评论失败:', e)
  }
}

const handleToggleComment = async (post) => {
  const postId = getPostId(post)
  if (activeCommentPostId.value === postId) {
    activeCommentPostId.value = null
    commentContent.value = ''
  } else {
    activeCommentPostId.value = postId
    commentContent.value = ''
    await loadComments(post)
  }
}

const handlePublishComment = async (post) => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  commentPublishing.value = true
  try {
    const data = {
      postId: getPostId(post),
      publishId: userStore.userInfo?.id,
      content: commentContent.value
    }
    const res = await publishSpaceComment(data)
    if (res.code === 1) {
      ElMessage.success('评论成功')
      commentContent.value = ''
      await loadComments(post)
      post.commentsCount = (post.commentsCount || 0) + 1
    } else {
      ElMessage.error(res.msg || '评论失败')
    }
  } catch (e) {
    logger.error(e)
    ElMessage.error('评论出错')
  } finally {
    commentPublishing.value = false
  }
}

const handleDeleteComment = async (commentId, post) => {
  try {
    const res = await deleteSpaceComment(commentId)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      await loadComments(post)
      post.commentsCount = Math.max(0, (post.commentsCount || 0) - 1)
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (e) {
    logger.error(e)
    ElMessage.error('删除出错')
  }
}

const friend = computed(() => store.currentFriendDetail)
const groupList = computed(() => store.groups)
const selectedGroup = ref('')

const handleShowSpace = async () => {
  if (!friend.value) return
  const currentUserId = userStore.userInfo?.id
  const friendId = friend.value.id || friend.value.userId
  if (!currentUserId || !friendId) return

  loadingSpace.value = true
  spaceDialogVisible.value = true
  try {
    const res = await getSpacePostList(currentUserId, friendId)
    if (res.code === 1) {
      const posts = res.data || []
      spacePosts.value = posts
      syncLikedPostMap(posts)
    }
  } catch (e) {
    logger.error(e)
  } finally {
    loadingSpace.value = false
  }
}

onMounted(() => {
  store.fetchFriendGroups()
})

watch(
  () => friend.value,
  (newVal) => {
    if (newVal) {
      selectedGroup.value = newVal.groupName || '我的好友'
    }
  },
  { immediate: true }
)

const handleGroupChange = (val) => {
  if (val === '__ADD__') {
    // Reset to previous value visually until confirmed
    selectedGroup.value = friend.value.groupName || '我的好友'

    ElMessageBox.prompt('请输入新的分组名称', '添加分组', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S/,
      inputErrorMessage: '分组名称不能为空'
    })
      .then(async ({ value }) => {
        if (!friend.value) return
        const id = friend.value.id || friend.value.userId
        await store.changeFriendGroup(id, value)
        // Optimistic update or wait for store fetch
        selectedGroup.value = value
      })
      .catch(() => {
        // cancel
      })
  } else {
    if (!friend.value) return
    const id = friend.value.id || friend.value.userId
    store.changeFriendGroup(id, val)
  }
}

watch(
  () => props.friendId,
  (newId) => {
    if (newId) {
      store.fetchFriendDetail(newId)
    } else {
      store.currentFriendDetail = null
    }
  },
  { immediate: true }
)

const getInitial = (name) => (name || '').slice(0, 1).toUpperCase()

const sendMessage = () => {
  if (!friend.value) return
  // Construct user object for chat store
  const user = {
    id: friend.value.id || friend.value.userId,
    username: friend.value.username,
    nickname: friend.value.nickname,
    avatar: friend.value.avatar,
    remark: friend.value.remark
  }
  chatStore.setActiveUser(user)
  emit('message', user)
}

const onEditRemark = () => {
  if (!friend.value) return
  remarkInput.value = friend.value.remark || ''
  remarkDialogVisible.value = true
}

const confirmRemark = async () => {
  if (!friend.value) return
  const id = friend.value.id || friend.value.userId
  await store.updateRemark(id, remarkInput.value)
  remarkDialogVisible.value = false
  // Optionally re-fetch detail to be safe, but store update should handle local state
}

const onDeleteFriend = () => {
  if (!friend.value) return
  ElMessageBox.confirm('你真的要删除该好友吗？删除后将无法恢复。', '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      const id = friend.value.id || friend.value.userId
      await store.deleteFriend(id)
      // store.deleteFriend handles success message and state update
    })
    .catch(() => {
      // cancel
    })
}
</script>

<style scoped>
.space-btn {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 42px;
  height: 42px;
  border: none;
  background: #f2f3f5;
  color: #606266;
  transition: all 0.3s;
}
.space-btn:hover {
  background: #fff3e0;
  color: #ff9800;
  transform: scale(1.1);
}

.post-item {
  padding: 0 10px;
}
.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.post-info {
  margin-left: 12px;
}
.post-author {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}
.post-time {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.post-content {
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
}
.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.post-img {
  width: 110px;
  height: 110px;
  border-radius: 6px;
  border: 1px solid #eee;
}
.post-actions {
  display: flex;
  align-items: center;
  gap: 30px;
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 15px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  font-size: 14px;
}

.action-item:hover {
  color: var(--el-color-primary);
}

.action-item .el-icon {
  font-size: 18px;
}

.post-actions button.action-item {
  font: inherit;
  border: none;
  background: transparent;
}

.like-action {
  border: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
  padding: 6px 10px;
  border-radius: 6px;
}

.like-action:hover {
  border-color: var(--el-color-primary);
  background: var(--el-fill-color-lighter);
}

.like-action:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.like-action:focus {
  outline: none;
}

.like-action:active {
  transform: scale(0.95);
}

.like-action.liked {
  color: var(--el-color-primary) !important;
  border-color: var(--el-color-primary) !important;
  background: var(--el-fill-color-lighter) !important;
}

.like-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
}

.like-icon svg {
  width: 18px;
  height: 18px;
  fill: currentColor;
}

/* 评论区样式 */
.comment-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid var(--el-border-color-lighter);
  background-color: var(--el-fill-color-lighter);
  padding: 15px;
  border-radius: 4px;
}

.comment-list {
  margin-bottom: 10px;
}

.comment-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.5;
}

.comment-content-wrapper {
  flex: 1;
}

.comment-user {
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-right: 5px;
}

.comment-text {
  color: var(--el-text-color-regular);
}

.comment-actions {
  margin-left: 10px;
  display: flex;
  align-items: center;
}

.delete-comment-icon {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.delete-comment-icon:hover {
  color: var(--el-color-danger);
}

.comment-input-area {
  margin-top: 10px;
}

.comment-input {
  margin-bottom: 8px;
}

.comment-btn-group {
  display: flex;
  justify-content: flex-end;
}

.friend-detail-container {
  height: 100%;
  background: var(--el-bg-color-page);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.friend-detail {
  flex: 1;
  overflow-y: auto;
  padding: 40px;
  background: var(--el-bg-color-page);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-card {
  width: 100%;
  max-width: 600px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  padding: 40px;
  display: flex;
  flex-direction: column;
  position: relative;
}

.profile-header {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  align-items: flex-start;
}
.header-left {
}
.big-avatar {
  border: 2px solid var(--el-bg-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.name-row {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}
.sub-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-regular);
  font-size: 14px;
}
.qq {
  margin-right: 4px;
}
.status-badge {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #ccc;
}
.status-badge.online {
  background: #18d070;
}
.status-text {
  margin-right: 12px;
}
.nickname-row {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}

.tags-row {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  font-size: 14px;
  color: var(--el-text-color-regular);
  padding-left: 100px;
}
.tag-item {
  padding: 0 4px;
}
.tag-item.male {
  color: var(--el-color-primary);
}
.tag-item.female {
  color: #f56c6c;
}

.group-select-row {
  display: flex;
  align-items: center;
  background: var(--el-fill-color-lighter);
  padding: 16px 20px;
  margin-bottom: 30px;
  border-radius: 8px;
}
.row-icon {
  margin-right: 12px;
  font-size: 18px;
  color: var(--el-text-color-regular);
}
.label {
  flex: 1;
  font-size: 15px;
  color: var(--el-text-color-primary);
}
.value {
  color: #666;
  font-size: 14px;
  margin-right: 8px;
}
.arrow-icon,
.arrow-right {
  color: #999;
}

.action-buttons {
  display: flex;
  gap: 16px;
  margin-top: 10px;
  justify-content: center;
  flex-wrap: wrap;
}
.action-btn {
  width: 120px;
  height: 40px;
  border-radius: 20px;
  font-size: 15px;
}
.action-btn.primary {
  background: var(--el-color-primary);
  border-color: var(--el-color-primary);
}
.action-btn.danger {
}

.group-select {
  width: 150px;
}
.add-group-content {
  display: flex;
  align-items: center;
  color: var(--el-color-primary);
  justify-content: center;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
