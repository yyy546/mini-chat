export class BusinessError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'BusinessError'
    this.code = code
  }
}

export class AuthError extends Error {
  constructor(message: string = '登录已过期') {
    super(message)
    this.name = 'AuthError'
  }
}
