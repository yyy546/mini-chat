package com.minichat.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EsChatMessage {
    private String id;      // 业务主键
    private Long dbId; // 数据库ID
    private Integer type;   // 会话类型(0:私聊, 1:群聊)
    private Long senderId;  // 发送者ID
    private String senderNickName; // 发送者昵称
    private Long targetId; // 接收者ID(私聊)或群ID(群聊)
    private String content; // 消息内容
    private Integer messageType;
    private String fileName;
    private String fileUrl;
    private LocalDateTime sendTime; // 发送时间
}
