/**
 * 通过 tempId 在消息列表中查找临时消息
 * @param {Array} list - 消息列表
 * @param {string} tempId - 临时ID
 * @returns {number} 找到的索引，-1 表示未找到
 */
export function findTempMessage(list, tempId) {
  if (!tempId) return -1
  return list.findIndex((msg) => msg.tempId === tempId || msg.id === tempId)
}

/**
 * 通过 messageId 在消息列表中查找
 * @param {Array} list - 消息列表
 * @param {string|number} messageId
 * @returns {number} 找到的索引，-1 表示未找到
 */
export function findExistingMessage(list, messageId) {
  if (!messageId) return -1
  return list.findIndex((msg) => msg.id === messageId)
}

/**
 * 通过 messageSeq 在群聊消息列表中查找
 * @param {Array} list - 消息列表
 * @param {number} messageSeq
 * @returns {number} 找到的索引，-1 表示未找到
 */
export function findExistingBySeq(list, messageSeq) {
  if (!messageSeq) return -1
  return list.findIndex((msg) => msg.messageSeq === messageSeq)
}

/**
 * 将服务端消息 payload 转换为前端 UI 消息格式
 * @param {object} payload - 服务端消息
 * @param {number} selfId - 当前用户ID
 * @param {boolean} isGroup - 是否群聊消息
 * @returns {object} UI 消息对象
 */
export function toUIMessage(payload, selfId, isGroup = false) {
  let ts = payload.sendTime ? new Date(payload.sendTime).getTime() : Date.now()
  if (isNaN(ts)) ts = Date.now()

  const msg = {
    id: payload.messageId || payload.id,
    fromId: payload.senderId === selfId ? 'self' : payload.senderId,
    toId: isGroup ? payload.groupId : payload.receiverId,
    groupId: isGroup ? payload.groupId : undefined,
    content: payload.content,
    timestamp: ts,
    type: payload.messageType || 1,
    fileName: payload.fileName,
    fileSize: payload.fileSize,
    fileUrl: payload.fileUrl,
    senderNickname: payload.senderNickname,
    senderAvatar: payload.senderAvatar,
    messageSeq: payload.messageSeq,
    isSending: false,
    isReceived: true,
    sendError: false,
    tempId: payload.tempId
  }

  return msg
}

/**
 * 应用撤回状态到消息
 * @param {object} uiMsg - UI 消息对象（会被修改）
 * @param {number} recallUserId - 撤回用户ID
 * @param {number} selfId - 当前用户ID
 * @param {string} senderNickname - 发送者昵称
 * @returns {object} 修改后的消息对象
 */
export function applyRecall(uiMsg, recallUserId, selfId, senderNickname) {
  if (recallUserId === selfId) {
    uiMsg.content = '你撤回了一条消息'
  } else {
    uiMsg.content = `"${senderNickname || '对方'}" 撤回了一条消息`
  }
  uiMsg.type = 5
  uiMsg.recall = true
  return uiMsg
}
