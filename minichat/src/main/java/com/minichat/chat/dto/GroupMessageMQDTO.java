package com.minichat.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageMQDTO {
    private Long messageId;     //消息ID
    private Long messageSeq;    //单群消息序号
    private Long groupId;
    private Long senderId;      //发送者ID
    private String senderNickname; //发送者昵称
    private String senderAvatar;
    private Integer messageType;    //消息类型 1:文本 2:图片 3:文件 4：系统消息
    private String content;     //消息内容
    private String fileUrl;     //文件URL
    private String fileName;    //文件名
    private Long fileSize;      //文件大小

    private String tempId;      //临时ID，用于消息去重
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sendTime;     //发送时间
}
