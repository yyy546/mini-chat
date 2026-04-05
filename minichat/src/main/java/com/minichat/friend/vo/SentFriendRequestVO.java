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
public class SentFriendRequestVO {
    private Long requestId; // 申请ID
    private Long toUserId; // 接收人ID（发给谁）
    private String toUserNickname; // 接收人昵称
    private String toUserAvatar; // 接收人头像
    private String message; // 申请留言
    private LocalDateTime createdTime; // 申请时间
    private Integer status; // 申请状态（0-待处理、1-已同意、2-已拒绝）
}
