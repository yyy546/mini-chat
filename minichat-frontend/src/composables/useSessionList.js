/**
 * 会话排序（按最后消息时间倒序）
 * @param {Array} sessions
 * @returns {Array}
 */
export function sortSessions(sessions) {
  return [...sessions].sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0))
}

/**
 * 会话搜索过滤
 * @param {Array} sessions
 * @param {string} keyword
 * @returns {Array}
 */
export function filterSessions(sessions, keyword) {
  if (!keyword) return sessions
  const k = keyword.toLowerCase()
  return sessions.filter(
    (s) => (s.name || '').toLowerCase().includes(k) || (s.nickname || '').toLowerCase().includes(k)
  )
}

/**
 * 更新单个会话的未读数和最后消息时间
 * @param {Array} sessions
 * @param {number} sessionId
 * @param {number} type - 0=私聊, 1=群聊
 * @param {object} updates
 * @returns {Array}
 */
export function updateSessionMeta(sessions, sessionId, type, updates) {
  const idx = sessions.findIndex((s) => s.id === sessionId && (s.type || 0) === type)
  if (idx === -1) return sessions
  const updated = [...sessions]
  updated[idx] = { ...updated[idx], ...updates }
  return sortSessions(updated)
}
