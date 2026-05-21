export class BusinessError extends Error {
  constructor(code, message) {
    super(message)
    this.name = 'BusinessError'
    this.code = code
  }
}

export class AuthError extends Error {
  constructor(message = '登录已过期') {
    super(message)
    this.name = 'AuthError'
  }
}
