import request from '@/utils/request'

/**
 * 上传空间图片
 * @param {File} file
 * @returns {Promise<{ fileUrl: string }>}
 */
export function uploadSpaceImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/space/post/upload/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 发布空间动态
 * @param {{ authorId: number, content: string, images: string[] }} data
 * @returns {Promise}
 */
export function publishSpacePost(data) {
  return request({
    url: '/space/post/publish',
    method: 'post',
    data
  })
}

/**
 * 获取指定用户的空间动态列表
 * @param {number} userId
 * @param {number} friendId
 * @returns {Promise<Array>}
 */
export function getSpacePostList(userId, friendId) {
  return request({
    url: '/space/post/list',
    method: 'get',
    params: { userId, friendId }
  })
}

/**
 * 获取关注Feed流（时间线）
 * @param {number} lastId - 最后一条动态ID（滚动加载用，首次传当前时间戳）
 * @param {number} offset - 偏移量
 * @returns {Promise<{ list: Array, minTime: number, offset: number }>}
 */
export function getFeedList(lastId, offset) {
  return request({
    url: '/feed/follow',
    method: 'get',
    params: { lastId, offset }
  })
}

/**
 * 删除空间动态（软删除，移入回收站）
 * @param {number} postId
 * @returns {Promise}
 */
export function deleteSpacePost(postId) {
  return request({
    url: '/space/post/delete',
    method: 'delete',
    params: { postId }
  })
}

/**
 * 发布空间评论
 * @param {{ postId: number, publishId: number, content: string }} data
 * @returns {Promise}
 */
export function publishSpaceComment(data) {
  return request({
    url: '/space/comment/publish',
    method: 'post',
    data
  })
}

/**
 * 删除空间评论
 * @param {number} commentId
 * @returns {Promise}
 */
export function deleteSpaceComment(commentId) {
  return request({
    url: '/space/comment/delete',
    method: 'delete',
    params: { commentId }
  })
}

/**
 * 获取已删除（回收站）的动态列表
 * @param {number} userId
 * @returns {Promise<Array>}
 */
export function getDeletedSpacePostList(userId) {
  return request({
    url: '/space/post/deleted/list',
    method: 'get',
    params: { userId }
  })
}

/**
 * 从回收站恢复动态
 * @param {number} postId
 * @returns {Promise}
 */
export function recoverSpacePost(postId) {
  return request({
    url: '/space/post/recover',
    method: 'put',
    params: { postId }
  })
}

/**
 * 切换点赞状态（点赞/取消点赞）
 * @param {number} postId
 * @returns {Promise}
 */
export function changeLikeStatus(postId) {
  return request({
    url: '/space/post/change/like',
    method: 'post',
    params: { postId }
  })
}

/**
 * 获取动态的评论列表
 * @param {number} postId
 * @returns {Promise<Array<{ id: number, publishId: number, publishName: string, content: string }>>}
 */
export function getSpaceCommentList(postId) {
  return request({
    url: '/space/comment/list',
    method: 'get',
    params: { postId }
  })
}
