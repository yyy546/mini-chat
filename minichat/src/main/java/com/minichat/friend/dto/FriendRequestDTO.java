package com.minichat.friend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDTO {
    @NotNull(message = "用户ID不能为空")
    private Long toUserId;       //接收好友请求的用户ID
    private String message;      //好友请求消息
}
