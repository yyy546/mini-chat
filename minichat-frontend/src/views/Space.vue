<template>
  <div class="space-layout">
    <!-- 右侧主体内容 -->
    <div class="space-main-content">
      <!-- 顶部个人信息区 -->
      <div class="space-profile-header">
        <div class="profile-info">
          <el-avatar :size="60" :src="userStore.userInfo?.avatar" class="profile-avatar">
            {{ (userStore.userInfo?.nickname || '').slice(0, 1) }}
          </el-avatar>
          <div class="profile-text">
            <h2 class="nickname">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h2>
          </div>
        </div>
        <div class="space-header-actions">
          <el-button
            circle
            size="default"
            @click="openTrash"
            title="回收站"
            style="width: 50px; height: 50px; font-size: 24px"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 发布框区域 -->
      <div class="publish-card">
        <div class="input-area">
          <el-input
            v-model="postContent"
            type="textarea"
            :rows="4"
            placeholder="说点儿什么吧"
            resize="none"
            class="custom-textarea"
          />
        </div>

        <!-- 图片预览区 -->
        <div class="image-preview-area" v-if="imageList.length > 0">
          <div class="preview-item" v-for="(img, index) in imageList" :key="index">
            <el-image :src="img" fit="cover" class="preview-img" :preview-src-list="imageList" :initial-index="index" />
            <div class="remove-btn" @click="removeImage(index)">
              <el-icon><Close /></el-icon>
            </div>
          </div>
          <!-- 如果不满9张，还可以继续添加的按钮 -->
          <el-upload
            v-if="imageList.length < 9"
            action="#"
            :http-request="handleUploadImage"
            :show-file-list="false"
            :before-upload="beforeUpload"
            accept="image/*"
            class="add-more-uploader"
          >
            <div class="add-more-btn">
              <el-icon><Plus /></el-icon>
            </div>
          </el-upload>
        </div>

        <!-- 底部工具栏 -->
        <div class="publish-toolbar">
          <div class="left-icons">
            <!-- 表情按钮 -->
            <el-popover placement="top-start" :width="300" trigger="click">
              <template #reference>
                <div class="icon-btn" title="插入表情">
                  <span style="font-size: 20px; line-height: 1">😊</span>
                </div>
              </template>
              <div class="emoji-picker">
                <span v-for="emoji in emojiList" :key="emoji" class="emoji-item" @click="addEmoji(emoji)">{{
                  emoji
                }}</span>
              </div>
            </el-popover>

            <!-- 图片上传按钮 (移到这里) -->
            <el-upload
              action="#"
              :http-request="handleUploadImage"
              :show-file-list="false"
              :before-upload="beforeUpload"
              accept="image/*"
              multiple
              :limit="9"
              :on-exceed="handleExceed"
              class="toolbar-uploader"
            >
              <div class="icon-btn" title="插入图片">
                <el-icon><Picture /></el-icon>
              </div>
            </el-upload>
          </div>
          <div class="right-actions">
            <el-button type="primary" class="publish-btn" :loading="publishing" @click="handlePublish">发表</el-button>
          </div>
        </div>
      </div>

      <!-- 动态列表 -->
      <div v-loading="loading" class="post-list">
        <div v-for="post in postList" :key="getPostId(post)" class="post-card">
          <div class="post-header">
            <el-avatar :size="40" :src="post.authorAvatar" class="post-avatar">
              {{ (post.authorName || '').slice(0, 1) }}
            </el-avatar>
            <div class="post-info">
              <div class="post-name">{{ post.authorName }}</div>
              <div class="post-time">{{ formatTime(post.createdTime) }}</div>
            </div>
            <div class="post-ops" v-if="sameId(post.authorId, userStore.userInfo?.id)">
              <el-popconfirm
                title="确定删除这条动态吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDeletePost(getPostId(post))"
              >
                <template #reference>
                  <el-button type="danger" class="delete-btn" :loading="deletingPostId === getPostId(post)"
                    >删除</el-button
                  >
                </template>
              </el-popconfirm>
            </div>
          </div>
          <div class="post-content">{{ post.content }}</div>
          <div class="post-images" v-if="post.images && post.images.length > 0">
            <el-image
              v-for="(img, idx) in post.images"
              :key="idx"
              :src="img"
              :preview-src-list="post.images"
              :initial-index="idx"
              fit="cover"
              class="post-img-item"
            />
          </div>
          <!-- 底部互动栏 (点赞/评论) - 暂时仅展示图标 -->
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
        </div>
        <el-empty v-if="!loading && postList.length === 0" description="暂无更多动态" />
      </div>

      <el-dialog v-model="trashVisible" title="回收站" width="760px">
        <el-alert
          title="超过14天未恢复的数据将被清空"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 15px"
        />
        <div v-loading="trashLoading" class="trash-list">
          <div v-for="post in deletedPostList" :key="getPostId(post)" class="trash-card">
            <div class="trash-header">
              <div class="trash-meta">
                <el-avatar :size="36" :src="post.authorAvatar">
                  {{ (post.authorName || '').slice(0, 1) }}
                </el-avatar>
                <div class="trash-info">
                  <div class="trash-name">{{ post.authorName }}</div>
                  <div class="trash-time">{{ formatTime(post.createdTime) }}</div>
                </div>
              </div>
              <el-button
                type="primary"
                :loading="recoveringPostId === getPostId(post)"
                @click="handleRecoverPost(getPostId(post))"
                >恢复</el-button
              >
            </div>
            <div class="trash-content">{{ post.content }}</div>
            <div class="trash-images" v-if="post.images && post.images.length > 0">
              <el-image
                v-for="(img, idx) in post.images"
                :key="idx"
                :src="img"
                :preview-src-list="post.images"
                :initial-index="idx"
                fit="cover"
                class="trash-img-item"
              />
            </div>
          </div>
          <el-empty v-if="!trashLoading && deletedPostList.length === 0" description="回收站为空" />
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../store/user'
import {
  publishSpacePost,
  getFeedList,
  deleteSpacePost,
  getDeletedSpacePostList,
  recoverSpacePost,
  changeLikeStatus,
  publishSpaceComment,
  deleteSpaceComment,
  getSpaceCommentList
} from '../api/space'
import { uploadFileUnified } from '../api/upload'
import { ElMessage } from 'element-plus'
import { Picture, ChatDotRound, Delete } from '@element-plus/icons-vue'
import logger from '../utils/logger'

const userStore = useUserStore()
const postContent = ref('')
const imageList = ref([]) // 存储图片URL
const publishing = ref(false)
const postList = ref([])
const loading = ref(false)
const deletingPostId = ref(null)
const trashVisible = ref(false)
const trashLoading = ref(false)
const deletedPostList = ref([])
const recoveringPostId = ref(null)
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

const fetchPosts = async () => {
  if (!userStore.userInfo?.id) return
  loading.value = true
  try {
    // Modify to call getFeedList as requested
    // lastId use current timestamp, offset default 0
    const res = await getFeedList(Date.now(), 0)
    const posts = res?.list || []
    postList.value = posts
    syncLikedPostMap(posts)
  } catch (e) {
    logger.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchPosts()
})

const handleToggleLike = async (post) => {
  const postId = getPostId(post)
  if (!postId) {
    ElMessage.error('帖子ID为空')
    return
  }
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
    await changeLikeStatus(postId)
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

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString()
}

const emojiList = [
  '😀',
  '😃',
  '😄',
  '😁',
  '😆',
  '😅',
  '😂',
  '🤣',
  '☺️',
  '😊',
  '😇',
  '🙂',
  '🙃',
  '😉',
  '😌',
  '😍',
  '🥰',
  '😘',
  '😗',
  '😙',
  '😚',
  '😋',
  '😛',
  '😝',
  '😜',
  '🤪',
  '🤨',
  '🧐',
  '🤓',
  '😎',
  '🤩',
  '🥳',
  '😏',
  '😒',
  '😞',
  '😔',
  '😟',
  '😕',
  '🙁',
  '☹️',
  '😣',
  '😖',
  '😫',
  '😩',
  '🥺',
  '😢',
  '😭',
  '😤',
  '😠',
  '😡',
  '🤬',
  '🤯',
  '😳',
  '🥵',
  '🥶',
  '😱',
  '😨',
  '😰',
  '😥',
  '😓',
  '🤗',
  '🤔',
  '🤭',
  '🤫',
  '🤥',
  '😶',
  '😐',
  '😑',
  '😬',
  '🙄',
  '😯',
  '😦',
  '😧',
  '😮',
  '😲',
  '🥱',
  '😴',
  '🤤',
  '😪',
  '😵',
  '🤐',
  '🥴',
  '🤢',
  '🤮',
  '🤧',
  '😷',
  '🤒',
  '🤕',
  '🤑',
  '🤠'
]

const addEmoji = (emoji) => {
  postContent.value += emoji
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  if (imageList.value.length >= 9) {
    ElMessage.warning('最多上传9张图片')
    return false
  }
  return true
}

const handleUploadImage = async (options) => {
  try {
    const result = await uploadFileUnified({
      file: options.file,
      scene: 'spaceImage'
    })
    imageList.value.push(result.fileUrl)
  } catch (error) {
    logger.error(error)
    ElMessage.error(error.message || '上传出错')
  }
}

const handleExceed = () => {
  ElMessage.warning('最多只能上传 9 张图片')
}

const removeImage = (index) => {
  imageList.value.splice(index, 1)
}

const handlePublish = async () => {
  if (!postContent.value.trim() && imageList.value.length === 0) {
    ElMessage.warning('说点什么或发张图吧')
    return
  }

  publishing.value = true
  try {
    const data = {
      authorId: userStore.userInfo?.id, // 实际上后端可能从Token取，但DTO里有这个字段就传一下
      content: postContent.value,
      images: imageList.value
    }
    await publishSpacePost(data)
    ElMessage.success('发表成功')
    postContent.value = ''
    imageList.value = []
    fetchPosts()
  } catch (error) {
    logger.error(error)
    ElMessage.error('发表出错')
  } finally {
    publishing.value = false
  }
}

const loadComments = async (post) => {
  const postId = getPostId(post)
  try {
    const res = await getSpaceCommentList(postId)
    logger.debug('加载评论结果:', postId, res)
    post.comments = res || []
    logger.debug('赋值后的评论:', post.comments)
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
    await publishSpaceComment(data)
    ElMessage.success('评论成功')
    commentContent.value = ''
    await loadComments(post)
    post.commentsCount = (post.commentsCount || 0) + 1
  } catch (e) {
    logger.error(e)
    ElMessage.error('评论出错')
  } finally {
    commentPublishing.value = false
  }
}

const handleDeleteComment = async (commentId, post) => {
  try {
    await deleteSpaceComment(commentId)
    ElMessage.success('删除成功')
    await loadComments(post)
    post.commentsCount = Math.max(0, (post.commentsCount || 0) - 1)
  } catch (e) {
    logger.error(e)
    ElMessage.error('删除出错')
  }
}

const handleDeletePost = async (postId) => {
  if (!postId) {
    ElMessage.error('帖子ID为空')
    return
  }
  deletingPostId.value = postId
  try {
    await deleteSpacePost(postId)
    ElMessage.success('删除成功')
    fetchPosts()
  } catch (e) {
    logger.error(e)
    ElMessage.error('删除出错')
  } finally {
    deletingPostId.value = null
  }
}

const fetchDeletedPosts = async () => {
  if (!userStore.userInfo?.id) return
  trashLoading.value = true
  try {
    const res = await getDeletedSpacePostList(userStore.userInfo.id)
    deletedPostList.value = res || []
  } catch (e) {
    logger.error(e)
    ElMessage.error('获取回收站出错')
  } finally {
    trashLoading.value = false
  }
}

const openTrash = async () => {
  trashVisible.value = true
  await fetchDeletedPosts()
}

const handleRecoverPost = async (postId) => {
  if (!postId) {
    ElMessage.error('帖子ID为空')
    return
  }
  recoveringPostId.value = postId
  try {
    await recoverSpacePost(postId)
    ElMessage.success('恢复成功')
    await fetchDeletedPosts()
    fetchPosts()
  } catch (e) {
    logger.error(e)
    ElMessage.error('恢复出错')
  } finally {
    recoveringPostId.value = null
  }
}
</script>

<style scoped>
.space-layout {
  display: flex;
  height: 100%;
  background-color: var(--el-bg-color-page);
}

/* 右侧主体 */
.space-main-content {
  flex: 1;
  padding: 0 30px;
  overflow-y: auto;
}

/* 顶部个人信息 */
.space-profile-header {
  padding: 30px 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.profile-info {
  display: flex;
  align-items: center;
}

.space-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-avatar {
  border: 2px solid var(--el-bg-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-right: 20px;
}

.profile-text .nickname {
  margin: 0 0 10px;
  font-size: 24px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

/* 发布框卡片 */
.publish-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.input-area {
  display: flex;
  position: relative;
  flex-direction: column;
}

.custom-textarea :deep(.el-textarea__inner) {
  border: none;
  box-shadow: none;
  padding: 15px;
  font-size: 14px;
  background: var(--el-fill-color-lighter);
  min-height: 100px;
}

.custom-textarea :deep(.el-textarea__inner):focus {
  background: var(--el-bg-color);
}

/* 图片预览 */
.image-preview-area {
  padding: 10px 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  border-top: 1px dashed var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
}

.preview-img {
  width: 100%;
  height: 100%;
}

.remove-btn {
  position: absolute;
  top: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
}

.add-more-uploader .add-more-btn {
  width: 80px;
  height: 80px;
  border: 1px dashed var(--el-border-color);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  cursor: pointer;
}
.add-more-uploader .add-more-btn:hover {
  border-color: var(--el-color-primary);
  color: var(--el-color-primary);
}

/* 底部工具栏 */
.publish-toolbar {
  height: 50px;
  background: var(--el-fill-color-lighter);
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
}

.left-icons {
  display: flex;
  gap: 15px;
}

.right-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.icon-btn {
  cursor: pointer;
  color: var(--el-text-color-regular);
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  transition: all 0.2s;
}
.icon-btn:hover {
  color: var(--el-color-primary);
  background-color: var(--el-fill-color-light);
}

.emoji-picker {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 5px;
  max-height: 200px;
  overflow-y: auto;
}

.emoji-item {
  font-size: 20px;
  cursor: pointer;
  text-align: center;
  padding: 4px;
  border-radius: 4px;
  transition: background 0.2s;
}

.emoji-item:hover {
  background-color: var(--el-fill-color-light);
}

.text-icon {
  font-size: 16px;
  font-weight: bold;
  font-family: Arial;
}

.publish-btn {
  padding: 8px 25px;
  border-radius: 2px;
}

/* 动态列表样式 */
.post-list {
  margin-top: 20px;
}

.post-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
  padding: 20px;
}

.post-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.post-info {
  margin-left: 15px;
}

.post-ops {
  margin-left: auto;
}

.delete-btn {
  font-size: 14px;
  padding: 8px 14px;
}

.post-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}

.post-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.post-content {
  font-size: 15px;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  margin-bottom: 15px;
  white-space: pre-wrap;
}

.post-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.post-img-item {
  width: 120px;
  height: 120px;
  border-radius: 4px;
  border: 1px solid var(--el-border-color-lighter);
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

.trash-list {
  min-height: 120px;
}

.trash-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
  background: var(--el-bg-color);
}

.trash-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.trash-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.trash-info {
  display: flex;
  flex-direction: column;
}

.trash-name {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.trash-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.trash-content {
  font-size: 14px;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  white-space: pre-wrap;
  margin-bottom: 10px;
}

.trash-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.trash-img-item {
  width: 110px;
  height: 110px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-lighter);
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
</style>
