import type { UIMessage } from '../types/message'

/**
 * 通过 tempId 在消息列表中查找临时消息
 */
export function findTempMessage(list: UIMessage[], tempId: string): number {
  if (!tempId) return -1
  return list.findIndex((msg) => msg.tempId === tempId || msg.id === tempId)
}

/**
 * 通过 messageId 在消息列表中查找
 */
export function findExistingMessage(list: UIMessage[], messageId: string | number): number {
  if (!messageId) return -1
  return list.findIndex((msg) => msg.id === messageId)
}

/**
 * 通过 messageSeq 在群聊消息列表中查找
 */
export function findExistingBySeq(list: UIMessage[], messageSeq: number): number {
  if (!messageSeq) return -1
  return list.findIndex((msg) => msg.messageSeq === messageSeq)
}

/**
 * 将服务端消息 payload 转换为前端 UI 消息格式
 */
export function toUIMessage(
  payload: Record<string, unknown>,
  selfId: number,
  isGroup: boolean = false
): UIMessage {
  let ts: number = payload.sendTime
    ? new Date(payload.sendTime as string).getTime()
    : Date.now()
  if (isNaN(ts)) ts = Date.now()

  return {
    id: (payload.messageId || payload.id) as string | number,
    fromId: payload.senderId === selfId ? 'self' : (payload.senderId as number),
    toId: isGroup ? (payload.groupId as number) : (payload.receiverId as number),
    groupId: isGroup ? (payload.groupId as number) : undefined,
    content: payload.content as string,
    timestamp: ts,
    type: (payload.messageType as 1 | 2 | 3 | 5) || 1,
    fileName: payload.fileName as string | undefined,
    fileSize: payload.fileSize as number | undefined,
    fileUrl: payload.fileUrl as string | undefined,
    senderNickname: payload.senderNickname as string | undefined,
    senderAvatar: payload.senderAvatar as string | undefined,
    messageSeq: payload.messageSeq as number | undefined,
    isSending: false,
    isReceived: true,
    sendError: false,
    tempId: payload.tempId as string | undefined
  }
}

/**
 * 应用撤回状态到消息
 */
export function applyRecall(
  uiMsg: UIMessage,
  recallUserId: number,
  selfId: number,
  senderNickname: string
): UIMessage {
  if (recallUserId === selfId) {
    uiMsg.content = '你撤回了一条消息'
  } else {
    uiMsg.content = `"${senderNickname || '对方'}" 撤回了一条消息`
  }
  uiMsg.type = 5
  uiMsg.recall = true
  return uiMsg
}
