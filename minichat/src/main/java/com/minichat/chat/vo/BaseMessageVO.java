package com.minichat.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseMessageVO {
    private Long messageId;
    private Long senderId;
    private String senderNickname;
    private String senderAvatar;
    private String content;
    private Integer messageType;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private LocalDateTime sendTime;

    private String tempId;
}
