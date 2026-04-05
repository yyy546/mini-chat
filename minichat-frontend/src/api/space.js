import request from '@/utils/request'

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

export function publishSpacePost(data) {
  return request({
    url: '/space/post/publish',
    method: 'post',
    data
  })
}

export function getSpacePostList(userId, friendId) {
  return request({
    url: '/space/post/list',
    method: 'get',
    params: { userId, friendId }
  })
}

export function getFeedList(lastId, offset) {
  return request({
    url: '/feed/follow',
    method: 'get',
    params: { lastId, offset }
  })
}

export function deleteSpacePost(postId) {
  return request({
    url: '/space/post/delete',
    method: 'delete',
    params: { postId }
  })
}

export function publishSpaceComment(data) {
  return request({
    url: '/space/comment/publish',
    method: 'post',
    data
  })
}

export function deleteSpaceComment(commentId) {
  return request({
    url: '/space/comment/delete',
    method: 'delete',
    params: { commentId }
  })
}

export function getDeletedSpacePostList(userId) {
  return request({
    url: '/space/post/deleted/list',
    method: 'get',
    params: { userId }
  })
}

export function recoverSpacePost(postId) {
  return request({
    url: '/space/post/recover',
    method: 'put',
    params: { postId }
  })
}

export function changeLikeStatus(postId) {
  return request({
    url: '/space/post/change/like',
    method: 'post',
    params: { postId }
  })
}

export function getSpaceCommentList(postId) {
  return request({
    url: '/space/comment/list',
    method: 'get',
    params: { postId }
  })
}
