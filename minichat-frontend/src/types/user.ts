export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  gender: string
  signature: string
  token?: string
}

export interface UserLoginVO {
  token: string
  id: number
  username: string
  nickname: string
  avatar: string
}

export interface UserDetailVO {
  nickname: string
  gender: string
  signature: string
  avatar: string
}
