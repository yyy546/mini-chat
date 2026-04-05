package com.minichat.friend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestVO {
    private Long requestId; // 申请ID
    private Long fromUserId; // 申请人ID
    private String fromUserNickname; // 申请人昵称
    private String fromUserAvatar; // 申请人头像
    private String message; // 申请留言
    private LocalDateTime createdTime; // 申请时间
    private Integer status; // 申请状态
}


