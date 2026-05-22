import request from '../utils/request'
import type { SessionVO } from '../types/session'

export const getSessionList = (): Promise<SessionVO[]> => {
  return request.get('/session/list')
}
