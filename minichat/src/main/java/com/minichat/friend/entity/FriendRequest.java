package com.minichat.friend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    private Long id;    //好友请求ID
    private Long fromUserId;     //发送好友请求的用户ID
    private Long toUserId;       //接收好友请求的用户ID
    private String message;      //好友请求消息
    private Integer status;      //0:待处理 1:已同意 2:已拒绝
    private LocalDateTime createdTime;    //创建时间
    private LocalDateTime processedTime;    //处理时间
}
