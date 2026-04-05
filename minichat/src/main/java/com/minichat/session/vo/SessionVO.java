package com.minichat.session.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionVO {
    private Long id;    // 会话ID(可能是用户ID或群组ID)
    private Integer type;   // 会话类型(0:私聊, 1:群聊)
    private String name;    // 会话名称(用户昵称或群聊名称)
    private String avatar;
    private LocalDateTime lastMessageTime; // 最后一条消息时间
    private Long unreadCount; // 未读消息数量
    private Long lastReadSeq; // 最后已读Seq (群聊用)
    private Long lastMessageSeq; // 最后一条消息Seq (群聊用)
}
