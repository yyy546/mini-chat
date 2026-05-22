import type { Session } from '../types/session'

/**
 * 会话排序（按最后消息时间倒序）
 */
export function sortSessions(sessions: Session[]): Session[] {
  return [...sessions].sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
}

/**
 * 会话搜索过滤
 */
export function filterSessions(sessions: Session[], keyword: string): Session[] {
  if (!keyword) return sessions
  const k = keyword.toLowerCase()
  return sessions.filter(
    (s) =>
      (s.name || '').toLowerCase().includes(k) ||
      ((s as Session & { nickname?: string }).nickname || '').toLowerCase().includes(k)
  )
}

/**
 * 更新单个会话的未读数和最后消息时间
 */
export function updateSessionMeta(
  sessions: Session[],
  sessionId: number,
  type: number,
  updates: Partial<Session>
): Session[] {
  const idx = sessions.findIndex((s) => s.id === sessionId && (s.type || 0) === type)
  if (idx === -1) return sessions
  const updated = [...sessions]
  updated[idx] = { ...updated[idx], ...updates }
  return sortSessions(updated)
}
