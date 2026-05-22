import request from '@/utils/request'

interface SpacePostDTO {
  authorId: number
  content: string
  images: string[]
}

interface SpaceCommentDTO {
  postId: number
  publishId: number
  content: string
}

interface SpaceCommentVO {
  id: number
  publishId: number
  publishName: string
  content: string
}

interface FeedResult {
  list: unknown[]
  minTime: number
  offset: number
}

export function uploadSpaceImage(file: File): Promise<{ fileUrl: string }> {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/space/post/upload/image',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function publishSpacePost(data: SpacePostDTO): Promise<void> {
  return request({
    url: '/space/post/publish',
    method: 'post',
    data
  })
}

export function getSpacePostList(userId: number, friendId: number): Promise<unknown[]> {
  return request({
    url: '/space/post/list',
    method: 'get',
    params: { userId, friendId }
  })
}

export function getFeedList(lastId: number, offset: number): Promise<FeedResult> {
  return request({
    url: '/feed/follow',
    method: 'get',
    params: { lastId, offset }
  })
}

export function deleteSpacePost(postId: number): Promise<void> {
  return request({
    url: '/space/post/delete',
    method: 'delete',
    params: { postId }
  })
}

export function publishSpaceComment(data: SpaceCommentDTO): Promise<void> {
  return request({
    url: '/space/comment/publish',
    method: 'post',
    data
  })
}

export function deleteSpaceComment(commentId: number): Promise<void> {
  return request({
    url: '/space/comment/delete',
    method: 'delete',
    params: { commentId }
  })
}

export function getDeletedSpacePostList(userId: number): Promise<unknown[]> {
  return request({
    url: '/space/post/deleted/list',
    method: 'get',
    params: { userId }
  })
}

export function recoverSpacePost(postId: number): Promise<void> {
  return request({
    url: '/space/post/recover',
    method: 'put',
    params: { postId }
  })
}

export function changeLikeStatus(postId: number): Promise<void> {
  return request({
    url: '/space/post/change/like',
    method: 'post',
    params: { postId }
  })
}

export function getSpaceCommentList(postId: number): Promise<SpaceCommentVO[]> {
  return request({
    url: '/space/comment/list',
    method: 'get',
    params: { postId }
  })
}
