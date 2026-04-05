package com.minichat.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecallMessageDTO {
    private Long messageId;     // 要撤回的消息ID(对应 private_message.id 或 group_message.id)
    private Long chatId;        // 聊天ID(如果是私聊，则是对方的用户 ID；如果是群聊，则是 groupId)
    private Long recallUserId;  // 撤回用户ID
    private Boolean isGroup;    // 是否为群聊
    private Long timestamp;     // 撤回时间戳
}
